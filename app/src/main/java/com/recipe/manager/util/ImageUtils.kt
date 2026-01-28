package com.recipe.manager.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    
    // 最大图片尺寸（宽或高）
    private const val MAX_IMAGE_SIZE = 1920
    // 压缩质量（0-100）
    private const val COMPRESS_QUALITY = 85
    
    /**
     * 保存并压缩图片到应用私有目录
     */
    fun saveAndCompressImage(context: Context, uri: Uri, prefix: String = "IMG"): String? {
        return try {
            val imageDir = File(context.filesDir, "recipe_images")
            if (!imageDir.exists()) imageDir.mkdirs()
            
            val outputFile = File(imageDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            
            // 读取原图并压缩
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 先获取图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // 计算采样率
                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_IMAGE_SIZE)
                
                // 重新读取并解码
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    var bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                    
                    if (bitmap != null) {
                        // 处理图片旋转
                        bitmap = rotateImageIfRequired(context, uri, bitmap)
                        
                        // 如果还是太大，进一步缩放
                        bitmap = scaleDownIfNeeded(bitmap, MAX_IMAGE_SIZE)
                        
                        // 保存压缩后的图片
                        FileOutputStream(outputFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
                        }
                        
                        bitmap.recycle()
                    }
                }
            }
            
            if (outputFile.exists()) outputFile.absolutePath else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 计算采样率
     */
    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        if (width > maxSize || height > maxSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxSize || (halfHeight / sampleSize) >= maxSize) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
    
    /**
     * 如果图片仍然太大，进一步缩放
     */
    private fun scaleDownIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }
    
    /**
     * 根据 EXIF 信息旋转图片
     */
    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            
            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
    
    /**
     * 删除图片文件
     */
    fun deleteImage(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 删除多个图片文件
     */
    fun deleteImages(imagePaths: List<String?>) {
        imagePaths.forEach { deleteImage(it) }
    }
    
    /**
     * 获取图片目录
     */
    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, "recipe_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 清理未被引用的图片（可选，用于定期清理）
     */
    fun cleanupOrphanedImages(context: Context, usedPaths: Set<String>) {
        val imageDir = getImageDir(context)
        imageDir.listFiles()?.forEach { file ->
            if (!usedPaths.contains(file.absolutePath)) {
                file.delete()
            }
        }
    }
}
