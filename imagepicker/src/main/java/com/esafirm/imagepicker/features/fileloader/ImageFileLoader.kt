package com.esafirm.imagepicker.features.fileloader

import com.esafirm.imagepicker.features.common.ImageLoaderListener
import com.esafirm.imagepicker.model.ImageWrapper
import java.io.File

interface ImageFileLoader {
    fun loadDeviceImages(
        isFolderMode: Boolean,
        onlyVideo: Boolean,
        includeVideo: Boolean,
        includeAnimation: Boolean,
        excludedImages: List<ImageWrapper>?,
        listener: ImageLoaderListener
    )

    fun abortLoadImages()
}
