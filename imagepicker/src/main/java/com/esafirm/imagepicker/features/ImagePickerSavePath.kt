package com.esafirm.imagepicker.features

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ImagePickerSavePath(
    val path: String,
    val isFullPath: Boolean
) : Parcelable {

    companion object {
        val DEFAULT = ImagePickerSavePath("Camera", false)
    }
}
