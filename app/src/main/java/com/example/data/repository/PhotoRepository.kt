package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PhotoRepository(private val context: Context) {

    /**
     * Decodes a Bitmap from Uri with safe downscaling to avoid OutOfMemoryError
     */
    suspend fun loadBitmapFromUri(uri: Uri, maxDimension: Int = 1920): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var input: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            var inSampleSize = 1
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            input = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a sample drawable resource
     */
    fun loadBitmapFromResource(resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    /**
     * Applies combined local adjustments (Brightness, Contrast, Saturation, Color Filters)
     * using ColorMatrix
     */
    fun applyAdjustments(
        source: Bitmap,
        brightness: Float, // -100 to +100
        contrast: Float,   // 0.5 to 2.0 (1.0 is neutral)
        saturation: Float, // 0.0 to 2.0 (1.0 is neutral)
        filter: FilterType
    ): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val combinedMatrix = ColorMatrix()

        // 1. Filter Preset Matrix
        when (filter) {
            FilterType.NONE -> {
                // Identity
            }
            FilterType.VIVID -> {
                val vivid = ColorMatrix().apply { setSaturation(1.4f) }
                combinedMatrix.postConcat(vivid)
            }
            FilterType.WARM -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 0.85f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(warm)
            }
            FilterType.COOL -> {
                val cool = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0f, 0f, 0f, -10f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 1.25f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(cool)
            }
            FilterType.B_AND_W -> {
                val bw = ColorMatrix().apply { setSaturation(0f) }
                combinedMatrix.postConcat(bw)
            }
            FilterType.SEPIA -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(sepia)
            }
            FilterType.CYBER -> {
                val cyber = ColorMatrix(
                    floatArrayOf(
                        0.8f, 0f, 0.3f, 0f, 10f,
                        0f, 1.2f, 0.2f, 0f, 15f,
                        0.2f, 0.1f, 1.4f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(cyber)
            }
            FilterType.DRAMATIC -> {
                val dramatic = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -20f,
                        0f, 1.3f, 0f, 0f, -20f,
                        0f, 0f, 1.3f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                combinedMatrix.postConcat(dramatic)
            }
        }

        // 2. Saturation
        if (saturation != 1.0f) {
            val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
            combinedMatrix.postConcat(satMatrix)
        }

        // 3. Contrast & Brightness
        if (contrast != 1.0f || brightness != 0.0f) {
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f + brightness
            val cbMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(cbMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Rotates bitmap by 90 degrees
     */
    fun rotateBitmap(source: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Flips bitmap horizontally
     */
    fun flipBitmapHorizontal(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f, source.width / 2f, source.height / 2f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Saves bitmap to device gallery via MediaStore
     */
    suspend fun saveToGallery(bitmap: Bitmap, title: String = "FotoGemini_${System.currentTimeMillis()}"): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "$title.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FotoGemini")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, values) ?: return@withContext null

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            itemUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a share intent using FileProvider
     */
    suspend fun getShareIntent(bitmap: Bitmap): Intent? = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(context.cacheDir, "images")
            cacheFolder.mkdirs()
            val file = File(cacheFolder, "fotogemini_share.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            stream.close()

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
