package com.esafirm.imagepicker.features

import android.annotation.SuppressLint
import android.content.Context
import com.esafirm.imagepicker.features.fileloader.DefaultImageFileLoader
import com.esafirm.imagepicker.features.fileloader.ImageFileLoader
import com.esafirm.imagepicker.features.imageloader.DefaultImageLoader
import com.esafirm.imagepicker.features.imageloader.ImageLoader

@SuppressLint("StaticFieldLeak")
object ImagePickerComponentHolder {
    private lateinit var context: Context

    var imageLoader: ImageLoader? = null
        get() = if (field == null) {
            defaultImageLoader
        } else {
            field
        }

    var imageFileLoader: ImageFileLoader? = null
        get() = if (field == null) {
            defaultImageFileLoader
        } else {
            field
        }

    var defaultImageLoader: ImageLoader? = null
        get() {
            if (field == null) {
                field = DefaultImageLoader()
            }
            return field
        }
        private set

    var defaultImageFileLoader: ImageFileLoader? = null
        get() {
            if (field == null) {
                field = DefaultImageFileLoader(context)
            }
            return field
        }
        private set

    fun init(context: Context) {
        this.context = context
    }
}
