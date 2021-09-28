package com.esafirm.imagepicker.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File

@Parcelize
open class ImageWrapper(
    val image: Image? = null,
    val imageUri: Uri? = null,
    val imageFile: File? = null,
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other == null || javaClass != other.javaClass -> return false
            else -> {
                val imageWrapper = other as ImageWrapper
                when {
                    imageWrapper.image != null -> image == imageWrapper.image
                    imageWrapper.imageUri != null -> imageUri == imageWrapper.imageUri
                    imageWrapper.imageFile != null -> imageFile == imageWrapper.imageFile
                    else -> false
                }
            }
        }
    }
}
