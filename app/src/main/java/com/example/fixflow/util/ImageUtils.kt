package com.example.fixflow.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImageUtils {

    fun loadScaledImageAsync(
        imageView: ImageView,
        uriString: String?,
        parentLayout: View? = null,
        targetWidth: Int = 400,
        targetHeight: Int = 400
    ) {
        if (uriString.isNullOrEmpty()) {
            imageView.visibility = View.GONE
            parentLayout?.visibility = View.GONE
            return
        }

        val context = imageView.context.applicationContext
        val uri = uriString.toUri()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                var sampleSize = 1
                while ((options.outWidth / sampleSize) > targetWidth || (options.outHeight / sampleSize) > targetHeight) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        parentLayout?.visibility = View.VISIBLE
                    } else {
                        imageView.visibility = View.GONE
                        parentLayout?.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.visibility = View.GONE
                    parentLayout?.visibility = View.GONE
                }
            }
        }
    }
}

inline fun View.setOnSingleClickListener(crossinline onClick: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 350L) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}
