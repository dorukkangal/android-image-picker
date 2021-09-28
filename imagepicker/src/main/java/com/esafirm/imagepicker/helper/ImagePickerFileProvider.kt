package com.esafirm.imagepicker.helper

import androidx.core.content.FileProvider
import com.esafirm.imagepicker.features.ImagePickerComponentHolder

class ImagePickerFileProvider : FileProvider() {

    override fun onCreate(): Boolean {
        ImagePickerComponentHolder.init(context!!)
        return super.onCreate()
    }
}
