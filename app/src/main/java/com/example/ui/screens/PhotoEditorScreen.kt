package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AnalysisSheet
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.BeforeAfterCanvas
import com.example.ui.components.LocalAdjustmentControls
import com.example.ui.components.PresetSelector
import com.example.ui.components.SamplePhotoSelectorDialog
import com.example.ui.viewmodel.PhotoEditorViewModel
import com.example.ui.viewmodel.UiStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showSampleDialog by remember { mutableStateOf(false) }
    var showAnalysisSheet by remember { mutableStateOf(false) }

    // Media Picker for Gallery photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadUri(uri)
        }
    }

    // Handle Toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FotoGemini",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "AI Photo Studio",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("btn_undo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("btn_redo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // API Key Settings
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("btn_api_key")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Pengaturan API Key"
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            viewModel.sharePhoto { intent ->
                                if (intent != null) {
                                    context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Foto"))
                                } else {
                                    Toast.makeText(context, "Gagal menyiapkan foto untuk dibagikan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = uiState.currentBitmap != null,
                        modifier = Modifier.testTag("btn_share_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan Foto"
                        )
                    }

                    // Save to Gallery
                    IconButton(
                        onClick = { viewModel.saveToGallery { } },
                        enabled = uiState.currentBitmap != null,
                        modifier = Modifier.testTag("btn_save_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Simpan ke Galeri",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("top_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Canvas Viewport (Takes responsive height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BeforeAfterCanvas(
                    currentBitmap = uiState.currentBitmap,
                    originalBitmap = uiState.originalBitmap,
                    isHoldingBefore = uiState.isBeforeComparing,
                    isSplitView = uiState.isSplitViewActive,
                    splitPosition = uiState.splitSliderPosition,
                    onSplitPositionChange = { viewModel.updateSplitSliderPosition(it) },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating canvas controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hold to compare button
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.setHoldBeforeComparing(true)
                                        tryAwaitRelease()
                                        viewModel.setHoldBeforeComparing(false)
                                    }
                                )
                            }
                            .testTag("btn_hold_compare")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tahan Foto Asli",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Split screen toggle button
                    Surface(
                        color = if (uiState.isSplitViewActive) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    viewModel.toggleSplitView()
                                }
                            }
                            .testTag("btn_toggle_split")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isSplitViewActive) "Tutup Split" else "Bandingkan Split",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Reset button
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    viewModel.resetToOriginal()
                                }
                            }
                            .testTag("btn_reset_all")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // AI Explanation pill if model provided text
                uiState.lastAiExplanation?.let { explanation ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = explanation,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tabs and Tool Selection
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Studio AI ✨", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_ai_studio")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Filter & Penyesuaian", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_filters")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        showAnalysisSheet = true
                        if (uiState.aiAnalysis == null) {
                            viewModel.analyzePhoto()
                        }
                    },
                    text = { Text("Analisis AI", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_analysis")
                )
            }

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> {
                        PresetSelector(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            selectedPreset = uiState.selectedPreset,
                            onPresetSelected = { viewModel.selectPreset(it) },
                            customPrompt = uiState.customPrompt,
                            onCustomPromptChange = { viewModel.updateCustomPrompt(it) },
                            onApplyAiEdit = { viewModel.applyAiEdit(it) },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    1 -> {
                        LocalAdjustmentControls(
                            brightness = uiState.brightness,
                            contrast = uiState.contrast,
                            saturation = uiState.saturation,
                            selectedFilter = uiState.selectedFilter,
                            onBrightnessChange = { viewModel.updateBrightness(it) },
                            onContrastChange = { viewModel.updateContrast(it) },
                            onSaturationChange = { viewModel.updateSaturation(it) },
                            onFilterChange = { viewModel.updateFilter(it) },
                            onRotate = { viewModel.rotate() },
                            onFlipHorizontal = { viewModel.flipHorizontal() },
                            onResetLocal = {
                                viewModel.updateBrightness(0f)
                                viewModel.updateContrast(1.0f)
                                viewModel.updateSaturation(1.0f)
                                viewModel.updateFilter(com.example.data.model.FilterType.NONE)
                            },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    2 -> {
                        // Quick trigger for analysis
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Analisis Foto Gemini AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dapatkan skor estetika, analisis komposisi, pencahayaan, dan rekomendasi prompt cerdas.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = {
                                    showAnalysisSheet = true
                                    viewModel.analyzePhoto()
                                },
                                modifier = Modifier.testTag("btn_open_analysis_sheet")
                            ) {
                                Text("Buka Panel Analisis Lengkap")
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Action Bar: Gallery & Samples
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_pick_gallery")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Foto", fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { showSampleDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_pick_sample")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contoh Foto", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Modal Progress Dialog when AI is generating
    if (uiState.uiStatus is UiStatus.Loading) {
        val loadingMsg = (uiState.uiStatus as UiStatus.Loading).message
        Dialog(
            onDismissRequest = { /* Prevent dismiss during active operation */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Memproses...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadingMsg,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Error Dialog
    if (uiState.uiStatus is UiStatus.Error) {
        val errorMsg = (uiState.uiStatus as UiStatus.Error).errorMessage
        Dialog(onDismissRequest = { viewModel.clearStatus() }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Pemberitahuan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMsg,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = { viewModel.clearStatus() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mengerti")
                    }
                }
            }
        }
    }

    // Sample Photo Picker Dialog
    if (showSampleDialog) {
        SamplePhotoSelectorDialog(
            onSelectSample = { resId -> viewModel.loadSample(resId) },
            onDismiss = { showSampleDialog = false }
        )
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = uiState.userApiKey,
            onSaveKey = { key -> viewModel.setCustomApiKey(key) },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    // AI Visual Analysis Bottom Sheet
    if (showAnalysisSheet) {
        AnalysisSheet(
            analysis = uiState.aiAnalysis,
            isAnalyzing = uiState.isAnalyzing,
            onStartAnalysis = { viewModel.analyzePhoto() },
            onSelectSuggestedPrompt = { prompt ->
                viewModel.updateCustomPrompt(prompt)
                selectedTab = 0
            },
            onDismiss = { showAnalysisSheet = false }
        )
    }
}
