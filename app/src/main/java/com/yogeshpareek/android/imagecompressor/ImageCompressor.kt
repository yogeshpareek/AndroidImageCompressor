package com.yogeshpareek.android.imagecompressor

import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.io.IOException
import android.graphics.Bitmap
import java.io.FileOutputStream

sealed class ImageCompressSize(var width: Int, var height: Int) {
    class HD() : ImageCompressSize(1280, 720)
    class FULL_HD() : ImageCompressSize(1920, 1080)
    class QUAD_HD() : ImageCompressSize(2560, 1440)
    class CUSTOM(val maxWidth: Int, val maxHeight: Int) : ImageCompressSize(maxWidth, maxHeight)
}

class ImageCompressor private constructor() {
    companion object {

        fun compressImage(originalPath: String, compressSize: ImageCompressSize): Bitmap? {
            return compressImageWithSize(originalPath, compressSize.width, compressSize.height, compressSize.width)
        }

        fun compressAndSaveImage(originalPath: String, compressPath: String, compressSize: ImageCompressSize): Bitmap? {

            var bitmap: Bitmap? = null
            bitmap = compressImageWithSize(originalPath, compressSize.width, compressSize.height, compressSize.width)

            if (bitmap != null) {
                val imageSave = saveImage(bitmap, File(compressPath))
                if (!imageSave) {
                    bitmap.recycle()
                    bitmap = null
                }
            }
            return bitmap
        }

        private fun saveImage(bitmap: Bitmap, file: File): Boolean {
            var extension = file.extension
            try {
                extension = "jpg"
                if (!file.exists()) {
                    file.createNewFile()
                }
                if (file.exists()) {
                    val out = FileOutputStream(file)
                    if (extension.equals("png", ignoreCase = true)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else if (extension.equals("jpg", ignoreCase = true) || extension.equals(
                            "jpeg",
                            ignoreCase = true
                        )
                    ) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    out.flush()
                    out.close()
                    return true
                } else {
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

        }

        private fun compressImageWithSize(
            originalImgPath: String, maxHeightLandscape: Int,
            maxwidthLandscape: Int, maxSqure: Int
        ): Bitmap? {

            val options = BitmapFactory.Options()
            // by setting this field as true, the actual bitmap pixels are not loaded in the memory.
            // Just the bounds are loaded. If you try the use the bitmap here, you will get null.
            options.inJustDecodeBounds = true
            val bmp = BitmapFactory.decodeFile(originalImgPath, options)

            val actualHeight = options.outHeight
            val actualWidth = options.outWidth

            val LANDSCAPE = 0
            val PORTRAIT = 1
            val SQUARE = 2

            var reqWidth = 0
            var reqHeight = 0

            var orientation = LANDSCAPE
            if (actualWidth > actualHeight) {
                orientation = LANDSCAPE
                reqWidth = maxwidthLandscape
                reqHeight = maxHeightLandscape
            } else if (actualHeight > actualWidth) {
                orientation = PORTRAIT
                reqWidth = maxHeightLandscape
                reqHeight = maxwidthLandscape
            } else if (actualWidth == actualHeight) {
                orientation = SQUARE
                reqWidth = maxSqure
                reqHeight = maxSqure
            } else {
                reqWidth = maxwidthLandscape
                reqHeight = maxHeightLandscape
            }

            var newReqWidth = reqWidth
            var newReqHeight = reqHeight

            val file = File(originalImgPath)

            val size = file.length()
            if (size < 100 * 1024) {
                newReqWidth = actualWidth
                newReqHeight = actualHeight
            } else {
                if (orientation == LANDSCAPE) {
                    if (actualWidth > maxwidthLandscape && actualHeight > maxHeightLandscape) {
                        newReqWidth = maxwidthLandscape
                        newReqHeight = maxHeightLandscape
                    } else if (actualWidth > maxwidthLandscape) {
                        newReqWidth = maxwidthLandscape
                        newReqHeight =
                                (maxwidthLandscape.toFloat() / actualWidth.toFloat() * actualHeight.toFloat()).toInt()
                    } else if (actualHeight > maxHeightLandscape) {
                        newReqWidth =
                                (maxHeightLandscape.toFloat() / actualHeight.toFloat() * actualWidth.toFloat()).toInt()
                        newReqHeight = maxHeightLandscape
                    } else {
                        newReqWidth = actualWidth
                        newReqHeight = actualHeight
                    }
                } else if (orientation == PORTRAIT) {
                    if (actualWidth > maxHeightLandscape && actualHeight > maxwidthLandscape) {
                        newReqWidth = maxHeightLandscape
                        newReqHeight = maxwidthLandscape
                    } else if (actualHeight > maxwidthLandscape) {
                        newReqWidth =
                                (maxwidthLandscape.toFloat() / actualHeight.toFloat() * actualWidth.toFloat()).toInt()
                        newReqHeight = maxwidthLandscape
                    } else if (actualWidth > maxHeightLandscape) {
                        newReqWidth = maxHeightLandscape
                        newReqHeight =
                                (maxHeightLandscape.toFloat() / actualWidth.toFloat() * actualHeight.toFloat()).toInt()
                    } else {
                        newReqWidth = actualWidth
                        newReqHeight = actualHeight
                    }
                } else if (orientation == SQUARE) {
                    if (actualWidth >= maxSqure && actualHeight >= maxSqure) {
                        newReqWidth = maxSqure
                        newReqHeight = maxSqure
                    } /*else if (actualWidth >= 1200 && actualHeight >= 1200) {
                    newReqWidth = 1200;
                    newReqHeight = 1200;
                } else if (actualWidth >= 900 && actualHeight >= 900) {
                    newReqWidth = 900;
                    newReqHeight = 900;
                } */
                    else {
                        newReqWidth = actualWidth
                        newReqHeight = actualHeight
                    }
                }
            }
            // return compressImage(originalImgPath, 816, 612);
            return compressImage(originalImgPath, newReqWidth, newReqHeight)
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
                val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
                inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
            }
            val totalPixels = (width * height).toFloat()
            val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
            return inSampleSize
        }

        private fun compressImage(originalImgPath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
            var scaledBitmap: Bitmap? = null

            val options = BitmapFactory.Options()
            // by setting this field as true, the actual bitmap pixels are not loaded in the memory.
            // Just the bounds are loaded. If you try the use the bitmap here, you will get null.
            options.inJustDecodeBounds = true
            var bmp: Bitmap? = BitmapFactory.decodeFile(originalImgPath, options)

            var actualHeight = options.outHeight
            var actualWidth = options.outWidth

            // max Height and width values of the compressed image is taken as 816x612
            // float maxHeight = 816.0f;
            // float maxWidth = 612.0f;
            val maxHeight = reqHeight.toFloat()
            val maxWidth = reqWidth.toFloat()
            var imgRatio = (actualWidth / actualHeight).toFloat()
            val maxRatio = maxWidth / maxHeight

            // width and height values are set maintaining the aspect ratio of the image
            if (actualHeight > maxHeight || actualWidth > maxWidth) {
                if (imgRatio < maxRatio) {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                } else if (imgRatio > maxRatio) {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                } else {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }

            // setting inSampleSize value allows to load a scaled down version of the original image
            options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

            // inJustDecodeBounds set to false to load the actual bitmap
            options.inJustDecodeBounds = false

            // this options allow android to claim the bitmap memory if it runs low on memory
            options.inDither = false
            options.inPurgeable = true
            options.inInputShareable = true
            options.inTempStorage = ByteArray(16 * 1024)

            try {
                // load the bitmap from its path
                bmp = BitmapFactory.decodeFile(originalImgPath, options)
                Log.e("AndroidImageCompressor", "Bitmap Size After Decoding : " + bmp!!.byteCount)
                Log.e("AndroidImageCompressor", "Bitmap Size After Decoding RowBytes : " + bmp!!.rowBytes)
                Log.e("AndroidImageCompressor", "Bitmap Size After Decoding in MB : " + bmp!!.byteCount.toFloat() / 1024f / 1024f)
                Log.e("AndroidImageCompressor", "Bitmap Width x Height After Decoding : " + bmp!!.width + " x " + bmp!!.height)
            } catch (exception: OutOfMemoryError) {
                exception.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (bmp != null) {
                try {
                    scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
                } catch (exception: OutOfMemoryError) {
                    exception.printStackTrace()
                }

                if (scaledBitmap != null) {
                    val ratioX = actualWidth / options.outWidth.toFloat()
                    val ratioY = actualHeight / options.outHeight.toFloat()
                    val middleX = actualWidth / 2.0f
                    val middleY = actualHeight / 2.0f

                    val scaleMatrix = Matrix()
                    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

                    val canvas = Canvas(scaledBitmap)
                    canvas.setMatrix(scaleMatrix)
                    canvas.drawBitmap(
                        bmp, middleX - bmp!!.width / 2,
                        middleY - bmp!!.height / 2, Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    bmp!!.recycle()
                    // check the rotation of the image and display it properly
                    Log.e("AndroidImageCompressor", "/*****************SCALED***************************/")
                    Log.e("AndroidImageCompressor", "Bitmap Size After Decoding : " + scaledBitmap!!.byteCount)
                    Log.e("AndroidImageCompressor", "Bitmap Size After Decoding RowBytes : " + scaledBitmap!!.rowBytes)
                    Log.e("AndroidImageCompressor", "Bitmap Size After Decoding in MB : " + scaledBitmap!!.byteCount.toFloat() / 1024f / 1024f)
                    Log.e("AndroidImageCompressor", "Bitmap Width x Height After Decoding : " +
                                scaledBitmap!!.width + " x " + scaledBitmap!!.height
                    )
                    Log.e("AndroidImageCompressor", "/********************************************/")

                    val exif: ExifInterface
                    try {
                        exif = ExifInterface(originalImgPath)

                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, 0
                        )
                        Log.d("EXIF", "Exif: $orientation")
                        val matrix = Matrix()
                        if (orientation == 6) {
                            matrix.postRotate(90f)
                            Log.d("EXIF", "Exif: $orientation")
                        } else if (orientation == 3) {
                            matrix.postRotate(180f)
                            Log.d("EXIF", "Exif: $orientation")
                        } else if (orientation == 8) {
                            matrix.postRotate(270f)
                            Log.d("EXIF", "Exif: $orientation")
                        }
                        scaledBitmap = Bitmap.createBitmap(
                            scaledBitmap, 0, 0,
                            scaledBitmap!!.width, scaledBitmap!!.height, matrix,
                            true
                        )

                        Log.e("AndroidImageCompressor", "/*****************SCALED 2***************************/")
                        Log.e("AndroidImageCompressor", "Bitmap Size After Decoding : " + scaledBitmap!!.byteCount)
                        Log.e("AndroidImageCompressor", "Bitmap Size After Decoding RowBytes : " + scaledBitmap!!.rowBytes)
                        Log.e("AndroidImageCompressor", "Bitmap Size After Decoding in MB : " + scaledBitmap!!.byteCount.toFloat() / 1024f / 1024f)
                        Log.e("AndroidImageCompressor", "Bitmap Width x Height After Decoding : " +
                                scaledBitmap!!.width + " x " + scaledBitmap!!.height
                        )
                        Log.e("AndroidImageCompressor", "/********************************************/")

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }

            return scaledBitmap
        }
    }
}