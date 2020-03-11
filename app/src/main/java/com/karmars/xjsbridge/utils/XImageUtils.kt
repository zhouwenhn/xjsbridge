package com.karmars.xjsbridge.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 处理图片工具类
 */
object XImageUtils {
    /**
     * 压缩图片到指定宽高及大小
     */
    fun compressImage(
        context: Context,
        filePath: String?,
        destWidth: Int,
        destHeight: Int,
        imageSize: Int
    ): File? {
        val degree = getBitmapDegree(filePath)
        var bitmap =
            decodeSampledBitmapFromFile(filePath, destWidth, destHeight)
        if (degree != 0) {
            val matrix = Matrix()
            matrix.postRotate(degree.toFloat())
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }
        val destFilePath =
            getImageCheDir(context) + System.nanoTime() + ".jpg"
        return saveImage(destFilePath, bitmap, imageSize.toLong())
    }

    private fun getImageCheDir(ctx: Context): String {
        val filePath = getCacheDir(ctx)
        return filePath + File.separator + "img" + File.separator
    }

    private fun getCacheDir(context: Context): String {
        val extCache = getExternalCacheDir(context)
        val isSdcardOk =
            Environment.getExternalStorageState() === "mounted" || !isExternalStorageRemovable
        return if (isSdcardOk && null != extCache) extCache.path else context.cacheDir.path
    }

    @get:SuppressLint("NewApi")
    val isExternalStorageRemovable: Boolean
        get() = if (Build.VERSION.SDK_INT >= 9) Environment.isExternalStorageRemovable() else true

    @SuppressLint("NewApi")
    private fun getExternalCacheDir(context: Context): File {
        return if (hasExternalCacheDir()) {
            context.externalCacheDir
        } else {
            val cacheDir = "/Android/data/" + context.packageName + "/cache/"
            File(Environment.getExternalStorageDirectory().path + cacheDir)
        }
    }

    private fun hasExternalCacheDir(): Boolean {
        return Build.VERSION.SDK_INT >= 8
    }

    /**
     * 保存图片到指定大小
     *
     * @param destFile  最终图片路径
     * @param bitmap    原图
     * @param imageSize 输出图片大小
     */
    private fun saveImage(
        destFile: String,
        bitmap: Bitmap,
        imageSize: Long
    ): File? { // 判断父目录是否存在，不存在则创建
        var bitmap: Bitmap? = bitmap
        var imageSize = imageSize
        val result = File(destFile.substring(0, destFile.lastIndexOf("/")))
        if (!result.exists() && !result.mkdirs()) {
            return null
        }
        if (imageSize <= 0) {
            imageSize = 400
        }
        val bos = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        var options = 100
        if (bos.toByteArray().size / 1024 > 1024) {
            bos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos)
            options = 50
        }
        while (bos.toByteArray().size.toLong() / 1024 > imageSize * 1.1 && options > 20) {
            bos.reset()
            options -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, bos)
        }
        try {
            val e = FileOutputStream(destFile)
            e.write(bos.toByteArray())
            e.flush()
            e.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        } finally {
            bitmap.recycle()
            bitmap = null
        }
        return File(destFile)
    }

    fun getBitmapDegree(path: String?): Int {
        var degree: Short = 0
        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt("Orientation", 1)
            when (orientation) {
                3 -> degree = 180
                6 -> degree = 90
                8 -> degree = 270
            }
        } catch (var4: IOException) {
            var4.printStackTrace()
        }
        return degree.toInt()
    }

    fun decodeSampledBitmapFromFile(path: String?, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio =
                Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio =
                Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }
}