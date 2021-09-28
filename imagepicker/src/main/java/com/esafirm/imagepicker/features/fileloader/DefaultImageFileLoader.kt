package com.esafirm.imagepicker.features.fileloader

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.esafirm.imagepicker.features.common.ImageLoaderListener
import com.esafirm.imagepicker.helper.ImagePickerUtils
import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultImageFileLoader(context: Context) : ImageFileLoader {

    private val context: Context = context.applicationContext

    private var executorService: ExecutorService? = null

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    override fun loadDeviceImages(
        isFolderMode: Boolean,
        onlyVideo: Boolean,
        includeVideo: Boolean,
        includeAnimation: Boolean,
        excludedImages: List<File>?,
        listener: ImageLoaderListener
    ) {
        getExecutorService()!!.execute(
            ImageLoadRunnable(
                isFolderMode,
                onlyVideo,
                includeVideo,
                includeAnimation,
                excludedImages,
                listener
            )
        )
    }

    override fun abortLoadImages() {
        executorService?.shutdown()
        executorService = null
    }

    private fun getExecutorService(): ExecutorService? {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor()
        }
        return executorService
    }

    private inner class ImageLoadRunnable(
        private val isFolderMode: Boolean,
        private val onlyVideo: Boolean,
        private val includeVideo: Boolean,
        private val includeAnimation: Boolean,
        private val excludedImages: List<File>?,
        private val listener: ImageLoaderListener
    ) : Runnable {
        private val querySelection: String?
            get() {
                return when {
                    onlyVideo -> {
                        (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    }
                    includeVideo -> {
                        (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    }
                    else -> {
                        null
                    }
                }
            }

        private val sourceUri: Uri
            get() = if (onlyVideo || includeVideo) {
                MediaStore.Files.getContentUri("external")
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        override fun run() {
            val cursor = context.contentResolver.query(
                sourceUri,
                projection,
                querySelection,
                null,
                MediaStore.Images.Media.DATE_ADDED
            )

            if (cursor == null) {
                listener.onFailed(NullPointerException())
                return
            }

            val temp = ArrayList<Image>()
            var folderMap: MutableMap<String?, Folder?>? = null
            if (isFolderMode) {
                folderMap = HashMap()
            }
            if (cursor.moveToLast()) {
                do {
                    val path = cursor.getString(cursor.getColumnIndex(projection[2]))
                    val file = makeSafeFile(path) ?: continue
                    if (excludedImages != null && excludedImages.contains(file)) continue

                    // Exclude GIF when we don't want it
                    if (!includeAnimation && ImagePickerUtils.isGifFormat(path)) {
                        continue
                    }

                    val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
                    val name = cursor.getString(cursor.getColumnIndex(projection[1]))
                    var bucket = cursor.getString(cursor.getColumnIndex(projection[3]))

                    if (bucket == null) {
                        val parent = File(path).parentFile
                        bucket = if (parent != null) {
                            parent.name
                        } else {
                            "SDCARD"
                        }
                    }

                    if (name != null) {
                        val image = Image(id, name, path)
                        temp.add(image)
                        if (folderMap != null && bucket != null) {
                            var folder = folderMap[bucket]
                            if (folder == null) {
                                folder = Folder(bucket)
                                folderMap[bucket] = folder
                            }
                            folder.images.add(image)
                        }
                    }
                } while (cursor.moveToPrevious())
            }
            cursor.close()

            /* Convert HashMap to ArrayList if not null */
            var folders: List<Folder>? = null
            if (folderMap != null) {
                folders = ArrayList(folderMap.values.filterNotNull())
            }
            listener.onImageLoaded(temp, folders)
        }
    }

    companion object {
        private fun makeSafeFile(path: String?): File? {
            return if (path == null || path.isEmpty()) {
                null
            } else try {
                File(path)
            } catch (ignored: Exception) {
                null
            }
        }
    }
}
