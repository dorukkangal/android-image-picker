package com.esafirm.imagepicker.features

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.esafirm.imagepicker.R
import com.esafirm.imagepicker.features.camera.CameraHelper.checkCameraAvailability
import com.esafirm.imagepicker.features.camera.DefaultCameraModule
import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig
import com.esafirm.imagepicker.features.common.BaseConfig
import com.esafirm.imagepicker.features.fileloader.DefaultImageFileLoader
import com.esafirm.imagepicker.features.recyclers.RecyclerViewManager
import com.esafirm.imagepicker.helper.ConfigUtils.shouldReturn
import com.esafirm.imagepicker.helper.ImagePickerPreferences
import com.esafirm.imagepicker.helper.IpCrasher.openIssue
import com.esafirm.imagepicker.helper.IpLogger
import com.esafirm.imagepicker.listeners.OnFolderClickListener
import com.esafirm.imagepicker.listeners.OnImageClickListener
import com.esafirm.imagepicker.listeners.OnImageSelectedListener
import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image
import com.esafirm.imagepicker.view.SnackBarView
import java.io.File

class ImagePickerFragment : Fragment(), ImagePickerView {
    val isShowDoneButton: Boolean
        get() = recyclerViewManager!!.isShowDoneButton

    private var recyclerView: RecyclerView? = null
    private var snackBarView: SnackBarView? = null
    private var progressBar: ProgressBar? = null
    private var emptyTextView: TextView? = null
    private var recyclerViewManager: RecyclerViewManager? = null
    private var presenter: ImagePickerPresenter? = null
    private var preferences: ImagePickerPreferences? = null
    private var config: ImagePickerConfig? = null
    private var interactionListener: ImagePickerInteractionListener? = null
    private var handler: Handler = Handler()
    private var observer: ContentObserver? = null
    private var isCameraOnly = false

    private val baseConfig: BaseConfig?
        get() = if (isCameraOnly) cameraOnlyConfig else imagePickerConfig

    private val imagePickerConfig: ImagePickerConfig?
        get() {
            if (config == null) {
                val bundle = arguments
                if (bundle == null) {
                    openIssue()
                }
                val hasImagePickerConfig =
                    bundle!!.containsKey(ImagePickerConfig::class.java.simpleName)
                val hasCameraOnlyConfig =
                    bundle.containsKey(ImagePickerConfig::class.java.simpleName)
                if (!hasCameraOnlyConfig && !hasImagePickerConfig) {
                    openIssue()
                }
                config = bundle.getParcelable(ImagePickerConfig::class.java.simpleName)
            }
            return config
        }

    private val cameraOnlyConfig: CameraOnlyConfig?
        get() = requireArguments().getParcelable(CameraOnlyConfig::class.java.simpleName)

    /**
     * Check permission
     */
    private val dataWithPermission: Unit
        get() {
            val result = ActivityCompat.checkSelfPermission(
                requireActivity(),
                REQUIRED_EXTERNAL_STORAGE_PERMISSION
            )
            if (result == PackageManager.PERMISSION_GRANTED) {
                data
            } else {
                requestExternalPermission()
            }
        }

    private val data: Unit
        get() {
            presenter!!.abortLoad()
            val config = imagePickerConfig
            if (config != null) {
                presenter!!.loadImages(config)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ImagePickerInteractionListener) {
            interactionListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCameraOnly = arguments?.containsKey(CameraOnlyConfig::class.java.simpleName) ?: false
        startContentObserver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupComponents()
        if (interactionListener == null) {
            throw RuntimeException(
                "ImagePickerFragment needs an " +
                        "ImagePickerInteractionListener. This will be set automatically if the " +
                        "activity implements ImagePickerInteractionListener, and can be set manually " +
                        "with fragment.setInteractionListener(listener)."
            )
        }
        if (savedInstanceState != null) {
            presenter!!.setCameraModule(savedInstanceState.getSerializable(STATE_KEY_CAMERA_MODULE) as DefaultCameraModule)
        }

        if (isCameraOnly) {
            if (savedInstanceState == null) {
                captureImageWithPermission()
            }
        } else {
            val config = imagePickerConfig
            if (config == null) {
                openIssue()
            }
            // clone the inflater using the ContextThemeWrapper
            val localInflater = inflater.cloneInContext(
                ContextThemeWrapper(
                    activity, config!!.theme
                )
            )

            // inflate the layout using the cloned inflater, not default inflater
            val result = localInflater.inflate(R.layout.ef_fragment_image_picker, container, false)
            setupView(result)
            if (savedInstanceState == null) {
                setupRecyclerView(config, config.getSelectedImages(), null)
            } else {
                setupRecyclerView(
                    config, null, savedInstanceState.getParcelableArrayList(
                        STATE_KEY_SELECTED_IMAGES
                    )
                )
                recyclerViewManager!!.onRestoreState(
                    savedInstanceState.getParcelable(
                        STATE_KEY_RECYCLER
                    )
                )
            }
            interactionListener!!.selectionChanged(recyclerViewManager!!.selectedImages)
            return result
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        if (!isCameraOnly) {
            dataWithPermission
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenter != null) {
            presenter!!.abortLoad()
            presenter!!.detachView()
        }
        if (observer != null) {
            requireActivity().contentResolver.unregisterContentObserver(observer!!)
            observer = null
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_KEY_CAMERA_MODULE, presenter!!.getCameraModule())
        if (!isCameraOnly) {
            outState.putParcelable(STATE_KEY_RECYCLER, recyclerViewManager!!.recyclerState)
            outState.putParcelableArrayList(
                STATE_KEY_SELECTED_IMAGES,
                recyclerViewManager!!.selectedImages as ArrayList<out Parcelable?>
            )
        }
    }

    /**
     * Config recyclerView when configuration changed
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (recyclerViewManager != null) {
            // recyclerViewManager can be null here if we use cameraOnly mode
            recyclerViewManager!!.changeOrientation(newConfig.orientation)
        }
    }

    /**
     * Handle permission results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RC_PERMISSION_REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    IpLogger.d("External permission granted")
                    data
                    return
                }
                IpLogger.e("Permission not granted: results len = ${grantResults.size} Result code = ${if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"}")
                interactionListener!!.cancel()
            }
            RC_PERMISSION_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    IpLogger.d("Camera permission granted")
                    captureImage()
                    return
                }
                IpLogger.e("Permission not granted: results len = ${grantResults.size} Result code = ${if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"}")
                interactionListener!!.cancel()
            }
            else -> {
                IpLogger.d("Got unexpected permission result: $requestCode")
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    /**
     * Check if the captured image is stored successfully
     * Then reload data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                presenter!!.finishCaptureImage(activity, data, baseConfig)
            } else if (resultCode == Activity.RESULT_CANCELED && isCameraOnly) {
                presenter!!.abortCaptureImage()
                interactionListener!!.cancel()
            }
        }
    }

    private fun setupView(rootView: View) {
        progressBar = rootView.findViewById(R.id.progress_bar)
        emptyTextView = rootView.findViewById(R.id.tv_empty_images)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        snackBarView = rootView.findViewById(R.id.ef_snackbar)
    }

    private fun setupRecyclerView(
        config: ImagePickerConfig?,
        selectedImageFiles: List<File>?,
        selectedImages: List<Image>?
    ) {
        recyclerViewManager = RecyclerViewManager(
            recyclerView!!,
            config!!,
            resources.configuration.orientation
        )

        recyclerViewManager!!.setupAdapters(
            selectedImageFiles ?: selectedImages?.map { it.file },
            object : OnImageClickListener {
                override fun onImageClick(isSelected: Boolean): Boolean {
                    return recyclerViewManager!!.selectImage(isSelected)
                }
            },
            object : OnFolderClickListener {
                override fun onFolderClick(bucket: Folder) {
                    setImageAdapter(bucket.images)
                }
            }
        )

        recyclerViewManager!!.setImageSelectedListener(
            object : OnImageSelectedListener {
                override fun onSelectionUpdate(selectedImage: List<Image?>?) {
                    updateTitle()
                    interactionListener!!.selectionChanged(recyclerViewManager!!.selectedImages)
                    if (shouldReturn(config, false) && selectedImage.isNullOrEmpty().not()) {
                        onDone()
                    }
                }
            }
        )
    }

    private fun setupComponents() {
        preferences = ImagePickerPreferences(requireActivity())
        presenter = ImagePickerPresenter(DefaultImageFileLoader(requireActivity()))
        presenter!!.attachView(this)
    }

    /**
     * Set image adapter
     * 1. Set new data
     * 2. Update item decoration
     * 3. Update title
     */
    fun setImageAdapter(images: List<Image>?) {
        recyclerViewManager!!.setImageAdapter(images)
        updateTitle()
    }

    fun setFolderAdapter(folders: List<Folder>?) {
        recyclerViewManager!!.setFolderAdapter(folders)
        updateTitle()
    }

    private fun updateTitle() {
        interactionListener!!.setTitle(recyclerViewManager!!.title)
    }

    /**
     * On finish selected image
     * Get all selected images then return image to caller activity
     */
    fun onDone() {
        presenter!!.onDoneSelectImages(recyclerViewManager!!.selectedImages.toMutableList())
    }

    /**
     * Request for permission
     * If permission denied or app is first launched, request for permission
     * If permission denied and user choose 'Never Ask Again', show snackbar with an action that navigate to app settings
     */
    private fun requestExternalPermission() {
        IpLogger.w("External permission is not granted. Requesting permission")
        val permissions = arrayOf(REQUIRED_EXTERNAL_STORAGE_PERMISSION)
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                REQUIRED_EXTERNAL_STORAGE_PERMISSION
            )
        ) {
            requestPermissions(permissions, RC_PERMISSION_REQUEST_EXTERNAL_STORAGE)
        } else {
            val permission = ImagePickerPreferences.PREF_EXTERNAL_STORAGE_REQUESTED
            if (preferences!!.isPermissionRequested(permission).not()) {
                preferences!!.setPermissionRequested(permission)
                requestPermissions(permissions, RC_PERMISSION_REQUEST_EXTERNAL_STORAGE)
            } else {
                snackBarView!!.show(R.string.ef_msg_no_external_permission) { openAppSettings() }
            }
        }
    }

    private fun requestCameraPermissions() {
        IpLogger.w("Camera permission is not granted. Requesting permission")
        val permissions = ArrayList<String>(2)
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                REQUIRED_EXTERNAL_STORAGE_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(REQUIRED_EXTERNAL_STORAGE_PERMISSION)
        }
        if (checkForRationale(permissions)) {
            requestPermissions(permissions.toTypedArray(), RC_PERMISSION_REQUEST_CAMERA)
        } else {
            if (isCameraOnly) {
                Toast.makeText(
                    requireActivity(),
                    R.string.ef_msg_no_camera_permission,
                    Toast.LENGTH_SHORT
                ).show()
                interactionListener!!.cancel()
            } else {
                snackBarView!!.show(R.string.ef_msg_no_camera_permission) { openAppSettings() }
            }
        }
    }

    private fun checkForRationale(permissions: List<String>): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
        }
    }

    /**
     * Open app settings screen
     */
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireActivity().packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /**
     * Request for camera permission
     */
    fun captureImageWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isWriteGranted = !IS_EXTERNAL_STORAGE_LEGACY || ActivityCompat
                .checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            if (isWriteGranted) {
                captureImage()
            } else {
                IpLogger.w("Camera permission is not granted. Requesting permission")
                requestCameraPermissions()
            }
        } else {
            captureImage()
        }
    }

    /**
     * Start camera intent
     * Create a temporary file and pass file Uri to camera intent
     */
    private fun captureImage() {
        if (!checkCameraAvailability(requireActivity())) {
            return
        }
        presenter!!.captureImage(this, baseConfig, RC_CAPTURE)
    }

    private fun startContentObserver() {
        if (isCameraOnly) return

        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                data
            }
        }
        requireActivity().contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            false,
            observer!!
        )
    }

    // Returns true if this Fragment can do anything with a "back" event, such as the containing
    // Activity receiving onBackPressed(). Returns false if the containing Activity should handle
    // it.
    // This Fragment might handle a "back" event by, for example, going back to the list of folders.
    // Or it might have no "back" to go, and return false.
    fun handleBack(): Boolean {
        if (isCameraOnly) {
            return false
        }
        if (recyclerViewManager!!.handleBack()) {
            // Handled.
            updateTitle()
            return true
        }
        return false
    }

    fun setInteractionListener(listener: ImagePickerInteractionListener?) {
        interactionListener = listener
    }

    /* --------------------------------------------------- */ /* > View Methods */ /* --------------------------------------------------- */
    override fun finishPickImages(images: List<Image>?) {
        val data = Intent()
        images?.let {
            data.putParcelableArrayListExtra(IpConstants.EXTRA_SELECTED_IMAGES, ArrayList(it))
        }
        interactionListener!!.finishPickImages(data)
    }

    override fun showCapturedImage() {
        dataWithPermission
    }

    override fun showFetchCompleted(images: List<Image>?, folders: List<Folder>?) {
        val config = imagePickerConfig
        if (config?.folderMode == true) {
            setFolderAdapter(folders)
        } else {
            setImageAdapter(images)
        }
    }

    override fun showError(throwable: Throwable?) {
        var message = "Unknown Error"
        if (throwable is NullPointerException) {
            message = "Images do not exist"
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(isLoading: Boolean) {
        progressBar!!.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView!!.visibility = if (isLoading) View.GONE else View.VISIBLE
        emptyTextView!!.visibility = View.GONE
    }

    override fun showEmpty() {
        progressBar!!.visibility = View.GONE
        recyclerView!!.visibility = View.GONE
        emptyTextView!!.visibility = View.VISIBLE
    }

    companion object {
        private const val STATE_KEY_CAMERA_MODULE = "Key.CameraModule"
        private const val STATE_KEY_RECYCLER = "Key.Recycler"
        private const val STATE_KEY_SELECTED_IMAGES = "Key.SelectedImages"

        private const val RC_CAPTURE = 2000

        private const val RC_PERMISSION_REQUEST_EXTERNAL_STORAGE = 23
        private const val RC_PERMISSION_REQUEST_CAMERA = 24

        private val IS_EXTERNAL_STORAGE_LEGACY =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageLegacy()

        private val REQUIRED_EXTERNAL_STORAGE_PERMISSION = if (IS_EXTERNAL_STORAGE_LEGACY) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        fun newInstance(
            config: ImagePickerConfig?,
            cameraOnlyConfig: CameraOnlyConfig?
        ): ImagePickerFragment {
            return ImagePickerFragment().apply {
                arguments = Bundle().apply {
                    config?.let {
                        putParcelable(ImagePickerConfig::class.java.simpleName, it)
                    }
                    cameraOnlyConfig?.let {
                        putParcelable(CameraOnlyConfig::class.java.simpleName, it)
                    }
                }
            }
        }
    }
}
