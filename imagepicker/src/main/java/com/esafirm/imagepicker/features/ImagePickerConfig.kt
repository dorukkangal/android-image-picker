package com.esafirm.imagepicker.features

import android.os.Parcelable
import com.esafirm.imagepicker.features.common.BaseConfig
import com.esafirm.imagepicker.model.Image
import kotlinx.android.parcel.Parcelize
import java.io.File

@Parcelize
data class ImagePickerConfig(
    var mode: Int = 0,
    var limit: Int = 0,
    var theme: Int = 0,
    var folderTitle: String? = null,
    var imageTitle: String? = null,
    var doneButtonText: String? = null,
    var arrowColor: Int = NO_COLOR,
    var folderMode: Boolean = false,
    var includeVideo: Boolean = false,
    var onlyVideo: Boolean = false,
    var includeAnimation: Boolean = false,
    var showCamera: Boolean = false,
    @Transient var language: String? = null,
    private var selectedImages: ArrayList<File> = arrayListOf(),
    private var excludedImages: ArrayList<File> = arrayListOf(),
) : BaseConfig(), Parcelable {

    fun getSelectedImages(): List<File> {
        return selectedImages
    }

    fun setSelectedImages(selectedImages: List<Image>?) {
        this.selectedImages.addAll(
            selectedImages.orEmpty().map { it.file }
        )
    }

    fun setSelectedImagePaths(selectedImages: List<String>?) {
        setSelectedImageFiles(selectedImages?.map { File(it) })
    }

    fun setSelectedImageFiles(selectedImages: List<File>?) {
        this.selectedImages.addAll(
            selectedImages.orEmpty()
        )
    }

    fun getExcludedImages(): List<File> {
        return excludedImages
    }

    fun setExcludedImages(excludedImages: List<Image>?) {
        this.excludedImages.addAll(
            excludedImages.orEmpty().map { it.file }
        )
    }

    fun setExcludedImagePaths(excludedImages: List<String>?) {
        setSelectedImageFiles(excludedImages?.map { File(it) })
    }

    fun setExcludedImageFiles(excludedImages: List<File>?) {
        this.excludedImages.addAll(
            excludedImages.orEmpty()
        )
    }

    companion object {
        const val NO_COLOR = -1
    }
}
