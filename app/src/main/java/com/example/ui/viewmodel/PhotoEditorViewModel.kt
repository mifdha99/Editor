package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiResult
import com.example.data.model.EditPreset
import com.example.data.model.FilterType
import com.example.data.model.PhotoAnalysis
import com.example.data.model.PresetCategory
import com.example.data.model.PresetRepository
import com.example.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiStatus {
    data object Idle : UiStatus
    data class Loading(val message: String) : UiStatus
    data class Success(val message: String) : UiStatus
    data class Error(val errorMessage: String) : UiStatus
}

data class PhotoEditorUiState(
    val originalBitmap: Bitmap? = null,
    val currentBitmap: Bitmap? = null,
    val baseAiBitmap: Bitmap? = null,
    val undoStack: List<Bitmap> = emptyList(),
    val redoStack: List<Bitmap> = emptyList(),
    val isBeforeComparing: Boolean = false,
    val isSplitViewActive: Boolean = false,
    val splitSliderPosition: Float = 0.5f,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val selectedFilter: FilterType = FilterType.NONE,
    val selectedCategory: PresetCategory = PresetCategory.ALL,
    val selectedPreset: EditPreset? = null,
    val customPrompt: String = "",
    val aiAnalysis: PhotoAnalysis? = null,
    val isAnalyzing: Boolean = false,
    val uiStatus: UiStatus = UiStatus.Idle,
    val toastMessage: String? = null,
    val userApiKey: String = "",
    val lastAiExplanation: String? = null
)

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiClient = GeminiApiClient()
    private val repository = PhotoRepository(application.applicationContext)
    private val prefs = application.getSharedPreferences("foto_gemini_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PhotoEditorUiState())
    val uiState: StateFlow<PhotoEditorUiState> = _uiState.asStateFlow()

    init {
        // Load saved API key if user provided one previously
        val savedKey = prefs.getString("user_gemini_key", "") ?: ""
        _uiState.update { it.copy(userApiKey = savedKey) }

        // Load default sample photo so the user immediately has an image to edit and play with!
        loadSample(R.drawable.sample_portrait_1791024593875)
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("user_gemini_key", key).apply()
        _uiState.update { it.copy(userApiKey = key, toastMessage = "Kunci API berhasil disimpan.") }
    }

    fun loadUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uiStatus = UiStatus.Loading("Memuat foto dari perangkat...")) }
            val bitmap = repository.loadBitmapFromUri(uri)
            if (bitmap != null) {
                setNewBaseImage(bitmap)
                _uiState.update {
                    it.copy(
                        uiStatus = UiStatus.Success("Foto berhasil dimuat."),
                        aiAnalysis = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(uiStatus = UiStatus.Error("Gagal membaca foto yang dipilih."))
                }
            }
        }
    }

    fun loadSample(resId: Int) {
        viewModelScope.launch {
            try {
                val bitmap = repository.loadBitmapFromResource(resId)
                setNewBaseImage(bitmap)
                _uiState.update {
                    it.copy(
                        uiStatus = UiStatus.Idle,
                        aiAnalysis = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setNewBaseImage(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                originalBitmap = bitmap,
                baseAiBitmap = bitmap,
                currentBitmap = bitmap,
                undoStack = emptyList(),
                redoStack = emptyList(),
                brightness = 0f,
                contrast = 1.0f,
                saturation = 1.0f,
                selectedFilter = FilterType.NONE,
                lastAiExplanation = null
            )
        }
    }

    fun selectCategory(category: PresetCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectPreset(preset: EditPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                customPrompt = preset.prompt
            )
        }
    }

    fun updateCustomPrompt(prompt: String) {
        _uiState.update { it.copy(customPrompt = prompt) }
    }

    /**
     * Executes AI Photo Editing with Gemini 2.5 Flash Image
     */
    fun applyAiEdit(promptToUse: String? = null) {
        val prompt = promptToUse ?: _uiState.value.customPrompt
        if (prompt.isBlank()) {
            _uiState.update { it.copy(toastMessage = "Tulis instruksi atau pilih preset edit terlebih dahulu.") }
            return
        }

        val baseBitmap = _uiState.value.currentBitmap ?: _uiState.value.baseAiBitmap
        if (baseBitmap == null) {
            _uiState.update { it.copy(toastMessage = "Pilih foto terlebih dahulu.") }
            return
        }

        val effectiveApiKey = geminiClient.resolveApiKey(_uiState.value.userApiKey)
        if (effectiveApiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    uiStatus = UiStatus.Error(
                        "Kunci API Gemini diperlukan! Masukkan API Key di pengaturan atas atau secrets AI Studio."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    uiStatus = UiStatus.Loading("Gemini AI sedang mengedit foto Anda... ✨"),
                    lastAiExplanation = null
                )
            }

            val result = geminiClient.editPhotoWithGemini(
                sourceBitmap = baseBitmap,
                prompt = prompt,
                apiKey = effectiveApiKey
            )

            when (result) {
                is GeminiResult.Success -> {
                    val newBitmap = result.data.bitmap
                    // Push previous state to undo stack
                    val currentList = _uiState.value.undoStack.toMutableList()
                    _uiState.value.currentBitmap?.let { currentList.add(it) }

                    _uiState.update {
                        it.copy(
                            baseAiBitmap = newBitmap,
                            currentBitmap = newBitmap,
                            undoStack = currentList,
                            redoStack = emptyList(),
                            brightness = 0f,
                            contrast = 1.0f,
                            saturation = 1.0f,
                            selectedFilter = FilterType.NONE,
                            lastAiExplanation = result.data.textExplanation,
                            uiStatus = UiStatus.Success("Foto berhasil diedit dengan Gemini AI! ✨")
                        )
                    }
                }
                is GeminiResult.Error -> {
                    _uiState.update {
                        it.copy(uiStatus = UiStatus.Error(result.errorMessage))
                    }
                }
            }
        }
    }

    /**
     * Analyzes the photo visually with Gemini 3.5 Flash
     */
    fun analyzePhoto() {
        val target = _uiState.value.currentBitmap ?: _uiState.value.originalBitmap
        if (target == null) {
            _uiState.update { it.copy(toastMessage = "Pilih foto terlebih dahulu.") }
            return
        }

        val effectiveApiKey = geminiClient.resolveApiKey(_uiState.value.userApiKey)
        if (effectiveApiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    toastMessage = "Masukkan Kunci API Gemini untuk mengaktifkan Analisis Foto AI."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val result = geminiClient.analyzePhoto(target, effectiveApiKey)
            when (result) {
                is GeminiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            aiAnalysis = result.data,
                            toastMessage = "Analisis foto selesai!"
                        )
                    }
                }
                is GeminiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            toastMessage = "Analisis gagal: ${result.errorMessage}"
                        )
                    }
                }
            }
        }
    }

    // Local filter and adjustment updates
    fun updateFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
        recomputeLocalAdjustments()
    }

    fun updateBrightness(value: Float) {
        _uiState.update { it.copy(brightness = value) }
        recomputeLocalAdjustments()
    }

    fun updateContrast(value: Float) {
        _uiState.update { it.copy(contrast = value) }
        recomputeLocalAdjustments()
    }

    fun updateSaturation(value: Float) {
        _uiState.update { it.copy(saturation = value) }
        recomputeLocalAdjustments()
    }

    private fun recomputeLocalAdjustments() {
        val base = _uiState.value.baseAiBitmap ?: return
        val adjusted = repository.applyAdjustments(
            source = base,
            brightness = _uiState.value.brightness,
            contrast = _uiState.value.contrast,
            saturation = _uiState.value.saturation,
            filter = _uiState.value.selectedFilter
        )
        _uiState.update { it.copy(currentBitmap = adjusted) }
    }

    fun rotate() {
        val current = _uiState.value.currentBitmap ?: return
        val rotated = repository.rotateBitmap(current, 90f)
        val origRotated = _uiState.value.originalBitmap?.let { repository.rotateBitmap(it, 90f) }

        val newUndo = _uiState.value.undoStack.toMutableList().also { it.add(current) }
        _uiState.update {
            it.copy(
                baseAiBitmap = rotated,
                currentBitmap = rotated,
                originalBitmap = origRotated ?: it.originalBitmap,
                undoStack = newUndo,
                redoStack = emptyList()
            )
        }
    }

    fun flipHorizontal() {
        val current = _uiState.value.currentBitmap ?: return
        val flipped = repository.flipBitmapHorizontal(current)
        val origFlipped = _uiState.value.originalBitmap?.let { repository.flipBitmapHorizontal(it) }

        val newUndo = _uiState.value.undoStack.toMutableList().also { it.add(current) }
        _uiState.update {
            it.copy(
                baseAiBitmap = flipped,
                currentBitmap = flipped,
                originalBitmap = origFlipped ?: it.originalBitmap,
                undoStack = newUndo,
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        val stack = _uiState.value.undoStack
        if (stack.isEmpty()) return
        val current = _uiState.value.currentBitmap ?: return
        val previous = stack.last()
        val newUndo = stack.dropLast(1)
        val newRedo = _uiState.value.redoStack.toMutableList().also { it.add(current) }

        _uiState.update {
            it.copy(
                currentBitmap = previous,
                baseAiBitmap = previous,
                undoStack = newUndo,
                redoStack = newRedo,
                brightness = 0f,
                contrast = 1.0f,
                saturation = 1.0f,
                selectedFilter = FilterType.NONE
            )
        }
    }

    fun redo() {
        val stack = _uiState.value.redoStack
        if (stack.isEmpty()) return
        val current = _uiState.value.currentBitmap ?: return
        val next = stack.last()
        val newRedo = stack.dropLast(1)
        val newUndo = _uiState.value.undoStack.toMutableList().also { it.add(current) }

        _uiState.update {
            it.copy(
                currentBitmap = next,
                baseAiBitmap = next,
                undoStack = newUndo,
                redoStack = newRedo,
                brightness = 0f,
                contrast = 1.0f,
                saturation = 1.0f,
                selectedFilter = FilterType.NONE
            )
        }
    }

    fun resetToOriginal() {
        val orig = _uiState.value.originalBitmap ?: return
        val current = _uiState.value.currentBitmap
        val newUndo = _uiState.value.undoStack.toMutableList()
        current?.let { newUndo.add(it) }

        _uiState.update {
            it.copy(
                baseAiBitmap = orig,
                currentBitmap = orig,
                undoStack = newUndo,
                redoStack = emptyList(),
                brightness = 0f,
                contrast = 1.0f,
                saturation = 1.0f,
                selectedFilter = FilterType.NONE,
                toastMessage = "Kembali ke foto asli."
            )
        }
    }

    fun setHoldBeforeComparing(holding: Boolean) {
        _uiState.update { it.copy(isBeforeComparing = holding) }
    }

    fun toggleSplitView() {
        _uiState.update { it.copy(isSplitViewActive = !it.isSplitViewActive) }
    }

    fun updateSplitSliderPosition(pos: Float) {
        _uiState.update { it.copy(splitSliderPosition = pos.coerceIn(0f, 1f)) }
    }

    fun saveToGallery(onSaved: (Boolean) -> Unit) {
        val bitmap = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(uiStatus = UiStatus.Loading("Menyimpan foto ke galeri...")) }
            val uri = repository.saveToGallery(bitmap)
            if (uri != null) {
                _uiState.update {
                    it.copy(
                        uiStatus = UiStatus.Success("Foto berhasil disimpan ke Galeri Pictures/FotoGemini! 💾"),
                        toastMessage = "Tersimpan di Galeri Pictures/FotoGemini"
                    )
                }
                onSaved(true)
            } else {
                _uiState.update {
                    it.copy(uiStatus = UiStatus.Error("Gagal menyimpan foto ke galeri."))
                }
                onSaved(false)
            }
        }
    }

    fun sharePhoto(onShare: (Intent?) -> Unit) {
        val bitmap = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            val intent = repository.getShareIntent(bitmap)
            onShare(intent)
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(uiStatus = UiStatus.Idle) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
