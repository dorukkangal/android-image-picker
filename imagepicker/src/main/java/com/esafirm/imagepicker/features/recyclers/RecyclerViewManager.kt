package com.esafirm.imagepicker.features.recyclers

import android.content.Context
import android.content.res.Configuration
import android.os.Parcelable
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esafirm.imagepicker.R
import com.esafirm.imagepicker.adapter.FolderPickerAdapter
import com.esafirm.imagepicker.adapter.ImagePickerAdapter
import com.esafirm.imagepicker.features.ImagePickerComponentHolder
import com.esafirm.imagepicker.features.ImagePickerConfig
import com.esafirm.imagepicker.features.IpConstants
import com.esafirm.imagepicker.features.ReturnMode
import com.esafirm.imagepicker.helper.ConfigUtils.getFolderTitle
import com.esafirm.imagepicker.helper.ConfigUtils.getImageTitle
import com.esafirm.imagepicker.listeners.OnFolderClickListener
import com.esafirm.imagepicker.listeners.OnImageClickListener
import com.esafirm.imagepicker.listeners.OnImageSelectedListener
import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image
import com.esafirm.imagepicker.model.ImageWrapper
import com.esafirm.imagepicker.view.GridSpacingItemDecoration

class RecyclerViewManager(
    private val recyclerView: RecyclerView,
    private val config: ImagePickerConfig,
    orientation: Int
) {
    private val context: Context = recyclerView.context
    private var layoutManager: GridLayoutManager? = null
    private var itemOffsetDecoration: GridSpacingItemDecoration? = null
    private var imageAdapter: ImagePickerAdapter? = null
    private var folderAdapter: FolderPickerAdapter? = null
    private var foldersState: Parcelable? = null
    private var imageColumns = 0
    private var folderColumns = 0

    val recyclerState: Parcelable?
        get() = layoutManager!!.onSaveInstanceState()

    val selectedImages: List<Image>
        get() {
            checkAdapterIsInitialized()
            return imageAdapter!!.getSelectedImages()
        }

    val isShowDoneButton: Boolean
        get() = isDisplayingFolderView.not()
                && imageAdapter!!.getSelectedImages().isNotEmpty()
                && (config.returnMode !== ReturnMode.ALL && config.returnMode !== ReturnMode.GALLERY_ONLY)


    private val isDisplayingFolderView: Boolean
        get() = recyclerView.adapter is FolderPickerAdapter

    val title: String
        get() {
            if (isDisplayingFolderView) {
                return getFolderTitle(context, config)
            }

            if (config.mode == IpConstants.MODE_SINGLE) {
                return getImageTitle(context, config)
            }

            val imageSize = imageAdapter!!.getSelectedImages().size
            val useDefaultTitle = config.imageTitle.isNullOrEmpty().not() && imageSize == 0

            if (useDefaultTitle) {
                return getImageTitle(context, config)
            }
            return if (config.limit == IpConstants.MAX_LIMIT) {
                String.format(
                    context.getString(R.string.ef_selected),
                    imageSize
                )
            } else {
                String.format(
                    context.getString(R.string.ef_selected_with_limit),
                    imageSize,
                    config.limit
                )
            }
        }

    init {
        changeOrientation(orientation)
    }

    fun onRestoreState(recyclerState: Parcelable?) {
        layoutManager!!.onRestoreInstanceState(recyclerState)
    }

    /**
     * Set item size, column size base on the screen orientation
     */
    fun changeOrientation(orientation: Int) {
        imageColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
        folderColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 4

        val shouldShowFolder = config.folderMode && isDisplayingFolderView
        val columns = if (shouldShowFolder) folderColumns else imageColumns
        layoutManager = GridLayoutManager(context, columns)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        setItemDecoration(columns)
    }

    fun setupAdapters(
        selectedImages: List<ImageWrapper>?,
        onImageClickListener: OnImageClickListener?,
        onFolderClickListener: OnFolderClickListener
    ) {
        var selectedImages = selectedImages
        if (config.mode == IpConstants.MODE_SINGLE && selectedImages?.isNotEmpty() == true) {
            selectedImages = null
        }
        /* Init folder and image adapter */
        val imageLoader = ImagePickerComponentHolder.imageLoader!!
        imageAdapter =
            ImagePickerAdapter(context, imageLoader, selectedImages, onImageClickListener!!)
        folderAdapter = FolderPickerAdapter(
            context,
            imageLoader,
            object : OnFolderClickListener {
                override fun onFolderClick(bucket: Folder) {
                    foldersState = recyclerView.layoutManager!!.onSaveInstanceState()
                    onFolderClickListener.onFolderClick(bucket)
                }
            }
        )
    }

    private fun setItemDecoration(columns: Int) {
        if (itemOffsetDecoration != null) {
            recyclerView.removeItemDecoration(itemOffsetDecoration!!)
        }
        itemOffsetDecoration = GridSpacingItemDecoration(
            columns,
            context.resources.getDimensionPixelSize(R.dimen.ef_item_padding),
            false
        )
        recyclerView.addItemDecoration(itemOffsetDecoration!!)
        layoutManager!!.spanCount = columns
    }

    // Returns true if a back action was handled by going back a folder; false otherwise.
    fun handleBack(): Boolean {
        if (config.folderMode && !isDisplayingFolderView) {
            setFolderAdapter(null)
            return true
        }
        return false
    }

    fun setImageAdapter(images: List<Image>?) {
        imageAdapter!!.setItems(images)
        setItemDecoration(imageColumns)
        recyclerView.adapter = imageAdapter
    }

    fun setFolderAdapter(folders: List<Folder>?) {
        folderAdapter!!.setData(folders)
        setItemDecoration(folderColumns)
        recyclerView.adapter = folderAdapter

        if (foldersState != null) {
            layoutManager!!.spanCount = folderColumns
            recyclerView.layoutManager!!.onRestoreInstanceState(foldersState)
        }
    }

    /* --------------------------------------------------- */
    /*  Images                                             */
    /* --------------------------------------------------- */

    private fun checkAdapterIsInitialized() {
        checkNotNull(imageAdapter) { "Must call setupAdapters first!" }
    }

    fun setImageSelectedListener(listener: OnImageSelectedListener?) {
        checkAdapterIsInitialized()
        imageAdapter!!.setImageSelectedListener(listener)
    }

    fun selectImage(isSelected: Boolean): Boolean {
        if (config.mode == IpConstants.MODE_MULTIPLE) {
            if (imageAdapter!!.getSelectedImages().size >= config.limit && !isSelected) {
                Toast.makeText(context, R.string.ef_msg_limit_images, Toast.LENGTH_SHORT).show()
                return false
            }
        } else if (config.mode == IpConstants.MODE_SINGLE) {
            if (imageAdapter!!.getSelectedImages().isNotEmpty()) {
                imageAdapter!!.removeAllSelectedSingleClick()
            }
        }
        return true
    }
}
