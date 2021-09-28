package com.esafirm.imagepicker.features

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.esafirm.imagepicker.R
import com.esafirm.imagepicker.features.camera.DefaultCameraModule
import com.esafirm.imagepicker.features.camera.OnImageReadyListener
import com.esafirm.imagepicker.features.common.BaseConfig
import com.esafirm.imagepicker.features.common.BasePresenter
import com.esafirm.imagepicker.features.common.ImageLoaderListener
import com.esafirm.imagepicker.features.fileloader.DefaultImageFileLoader
import com.esafirm.imagepicker.helper.ConfigUtils.shouldReturn
import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image
import java.io.File

class ImagePickerPresenter(
    private val imageLoader: DefaultImageFileLoader
) : BasePresenter<ImagePickerView>() {

    private val main = Handler(Looper.getMainLooper())
    private var cameraModule: DefaultCameraModule? = null

    fun getCameraModule(): DefaultCameraModule {
        if (cameraModule == null) {
            cameraModule = DefaultCameraModule()
        }
        return cameraModule!!
    }

    /* Set the camera module in onRestoreInstance */
    fun setCameraModule(cameraModule: DefaultCameraModule?) {
        this.cameraModule = cameraModule
    }

    fun abortLoad() {
        imageLoader.abortLoadImages()
    }

    fun loadImages(config: ImagePickerConfig) {
        if (!isViewAttached) return

        val isFolder = config.folderMode
        val includeVideo = config.includeVideo
        val onlyVideo = config.onlyVideo
        val includeAnimation = config.includeAnimation
        val excludedImages = config.getExcludedImages()

        runOnUiIfAvailable { getView().showLoading(true) }

        imageLoader.loadDeviceImages(
            isFolder,
            onlyVideo,
            includeVideo,
            includeAnimation,
            excludedImages,
            object : ImageLoaderListener {
                override fun onImageLoaded(images: List<Image>, folders: List<Folder>?) {
                    runOnUiIfAvailable {
                        getView().showFetchCompleted(images, folders)
                        val isEmpty = folders?.isEmpty() ?: images.isEmpty()
                        if (isEmpty) {
                            getView().showEmpty()
                        } else {
                            getView().showLoading(false)
                        }
                    }
                }

                override fun onFailed(throwable: Throwable) {
                    runOnUiIfAvailable { getView().showError(throwable) }
                }
            }
        )
    }

    fun onDoneSelectImages(selectedImages: MutableList<Image>?) {
        if (selectedImages != null && selectedImages.isNotEmpty()) {
            /* Scan selected images which not existed */
            selectedImages.removeAll { it.file.exists().not() }
            getView().finishPickImages(selectedImages)
        }
    }

    fun captureImage(fragment: Fragment, config: BaseConfig?, requestCode: Int) {
        val context = fragment.requireActivity().applicationContext
        val intent = getCameraModule().getCameraIntent(fragment.requireActivity(), config!!)
        if (intent == null) {
            Toast.makeText(
                context,
                R.string.ef_error_create_image_file,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        fragment.startActivityForResult(intent, requestCode)
    }

    fun finishCaptureImage(context: Context?, data: Intent?, config: BaseConfig?) {
        getCameraModule().getImage(context!!, data, object : OnImageReadyListener {
            override fun onImageReady(images: List<Image>?) {
                if (shouldReturn(config!!, true)) {
                    getView().finishPickImages(images)
                } else {
                    getView().showCapturedImage()
                }
            }
        })
    }

    fun abortCaptureImage() {
        getCameraModule().removeImage()
    }

    private fun runOnUiIfAvailable(runnable: Runnable) {
        main.post {
            if (isViewAttached) {
                runnable.run()
            }
        }
    }
}
