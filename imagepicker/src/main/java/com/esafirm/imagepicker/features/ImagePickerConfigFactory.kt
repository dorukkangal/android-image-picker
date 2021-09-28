package com.esafirm.imagepicker.features

import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig

object ImagePickerConfigFactory {
    fun createCameraDefault(): CameraOnlyConfig {
        val config = CameraOnlyConfig()
        config.setSavePath(ImagePickerSavePath.DEFAULT)
        config.returnMode = ReturnMode.ALL
        config.saveImage = true
        return config
    }

    @JvmStatic
    fun createDefault(): ImagePickerConfig {
        val config = ImagePickerConfig()
        config.mode = IpConstants.MODE_MULTIPLE
        config.limit = IpConstants.MAX_LIMIT
        config.showCamera = true
        config.folderMode = false
        config.setSelectedImages(ArrayList())
        config.setSavePath(ImagePickerSavePath.DEFAULT)
        config.returnMode = ReturnMode.NONE
        config.saveImage = true
        return config
    }
}
