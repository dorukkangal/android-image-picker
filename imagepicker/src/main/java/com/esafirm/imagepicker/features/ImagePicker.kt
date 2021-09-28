package com.esafirm.imagepicker.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.esafirm.imagepicker.features.ImagePickerConfigFactory.createDefault
import com.esafirm.imagepicker.features.cameraonly.ImagePickerCameraOnly
import com.esafirm.imagepicker.helper.ConfigUtils.checkConfig
import com.esafirm.imagepicker.helper.IpLogger
import com.esafirm.imagepicker.helper.LocaleManager
import com.esafirm.imagepicker.model.Image
import java.io.File

abstract class ImagePicker {
    private var config: ImagePickerConfig? = null

    abstract fun start()

    abstract fun start(requestCode: Int)

    class ImagePickerWithActivity(private val activity: Activity) : ImagePicker() {
        init {
            init(activity)
        }

        override fun start(requestCode: Int) {
            activity.startActivityForResult(getIntent(activity), requestCode)
        }

        override fun start() {
            activity.startActivityForResult(getIntent(activity), IpConstants.RC_IMAGE_PICKER)
        }
    }

    class ImagePickerWithFragment(private val fragment: Fragment) : ImagePicker() {
        init {
            init(fragment.requireContext())
        }

        override fun start(requestCode: Int) {
            fragment.startActivityForResult(getIntent(fragment.activity), requestCode)
        }

        override fun start() {
            fragment.startActivityForResult(
                getIntent(fragment.activity),
                IpConstants.RC_IMAGE_PICKER
            )
        }
    }

    fun init(context: Context?) {
        config = createDefault()
    }

    /* --------------------------------------------------- */
    /*  Builder                                            */
    /* --------------------------------------------------- */

    fun single(): ImagePicker {
        config!!.mode = IpConstants.MODE_SINGLE
        return this
    }

    fun multi(): ImagePicker {
        config!!.mode = IpConstants.MODE_MULTIPLE
        return this
    }

    fun returnMode(returnMode: ReturnMode): ImagePicker {
        config!!.returnMode = returnMode
        return this
    }

    fun saveImage(saveImage: Boolean): ImagePicker {
        config!!.saveImage = saveImage
        return this
    }

    fun limit(count: Int): ImagePicker {
        config!!.limit = count
        return this
    }

    fun showCamera(show: Boolean): ImagePicker {
        config!!.showCamera = show
        return this
    }

    fun toolbarArrowColor(@ColorInt color: Int): ImagePicker {
        config!!.arrowColor = color
        return this
    }

    fun toolbarFolderTitle(title: String?): ImagePicker {
        config!!.folderTitle = title
        return this
    }

    fun toolbarImageTitle(title: String?): ImagePicker {
        config!!.imageTitle = title
        return this
    }

    fun toolbarDoneButtonText(text: String?): ImagePicker {
        config!!.doneButtonText = text
        return this
    }

    fun selectImages(images: List<Image>?): ImagePicker {
        config!!.setSelectedImages(images)
        return this
    }

    fun selectFiles(images: List<File>?): ImagePicker {
        config!!.setSelectedImageFiles(images)
        return this
    }

    fun selectPaths(images: List<String>?): ImagePicker {
        config!!.setSelectedImagePaths(images)
        return this
    }

    fun selectURIs(images: List<Uri>?): ImagePicker {
        config!!.setSelectedImageURIs(images)
        return this
    }

    fun excludeImages(images: List<Image>?): ImagePicker {
        config!!.setExcludedImages(images)
        return this
    }

    fun excludeFiles(images: List<File>?): ImagePicker {
        config!!.setExcludedImageFiles(images)
        return this
    }

    fun excludePaths(images: List<String>?): ImagePicker {
        config!!.setExcludedImagePaths(images)
        return this
    }

    fun excludeURIs(images: List<Uri>?): ImagePicker {
        config!!.setExcludedImageURIs(images)
        return this
    }

    fun folderMode(folderMode: Boolean): ImagePicker {
        config!!.folderMode = folderMode
        return this
    }

    fun includeVideo(includeVideo: Boolean): ImagePicker {
        config!!.includeVideo = includeVideo
        return this
    }

    fun onlyVideo(onlyVideo: Boolean): ImagePicker {
        config!!.onlyVideo = onlyVideo
        return this
    }

    fun includeAnimation(includeAnimation: Boolean): ImagePicker {
        config!!.includeAnimation = includeAnimation
        return this
    }

    fun imageDirectory(directory: String?): ImagePicker {
        config!!.setImageDirectory(directory!!)
        return this
    }

    fun imageFullDirectory(fullPath: String?): ImagePicker {
        config!!.setImageFullDirectory(fullPath!!)
        return this
    }

    fun theme(@StyleRes theme: Int): ImagePicker {
        config!!.theme = theme
        return this
    }

    fun enableLog(isEnable: Boolean): ImagePicker {
        IpLogger.setEnable(isEnable)
        return this
    }

    fun language(language: String?): ImagePicker {
        config!!.language = language
        return this
    }

    fun getConfig(): ImagePickerConfig {
        checkConfig(config)
        LocaleManager.language = config!!.language
        return config!!
    }

    fun getIntent(context: Context?): Intent {
        return Intent(context, ImagePickerActivity::class.java).apply {
            putExtra(ImagePickerConfig::class.java.simpleName, getConfig())
        }
    }

    companion object {
        fun create(activity: Activity): ImagePickerWithActivity {
            return ImagePickerWithActivity(activity)
        }

        fun create(fragment: Fragment): ImagePickerWithFragment {
            return ImagePickerWithFragment(fragment)
        }

        fun cameraOnly(): ImagePickerCameraOnly {
            return ImagePickerCameraOnly()
        }

        fun shouldHandle(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK && requestCode == IpConstants.RC_IMAGE_PICKER && data != null
        }

        fun getImages(intent: Intent?): List<Image>? {
            return intent?.getParcelableArrayListExtra(IpConstants.EXTRA_SELECTED_IMAGES)
        }

        fun getFirstImageOrNull(intent: Intent?): Image? {
            return getImages(intent)?.firstOrNull()
        }
    }
}
