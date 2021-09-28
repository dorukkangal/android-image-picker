package com.esafirm.imagepicker.features.common

import android.os.Parcelable
import com.esafirm.imagepicker.features.ImagePickerSavePath
import com.esafirm.imagepicker.features.ReturnMode

abstract class BaseConfig : Parcelable {
    var returnMode: ReturnMode? = null
    var saveImage: Boolean = true
    private var savePath: ImagePickerSavePath? = null

    fun getImageDirectory(): ImagePickerSavePath? {
        return savePath
    }

    fun setSavePath(savePath: ImagePickerSavePath?) {
        this.savePath = savePath
    }

    fun setImageDirectory(dirName: String) {
        savePath = ImagePickerSavePath(dirName, false)
    }

    fun setImageFullDirectory(path: String) {
        savePath = ImagePickerSavePath(path, true)
    }
}
