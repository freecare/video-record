package com.freecare.videorecord.library.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File
import java.io.IOException
import java.util.*

@Suppress("DEPRECATION")
class MediaUtils(private val activity: Activity) : SurfaceHolder.Callback {

    companion object {
        private const val TAG = "MediaUtils"
        const val MEDIA_AUDIO = 0
        const val MEDIA_VIDEO = 1
    }

    private var mMediaRecorder: MediaRecorder? = null
    private var profile: CamcorderProfile? = null
    private var mCamera: Camera? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var targetDir: File? = null
    private var targetName: String? = null
    private var targetFile: File? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var recorderType: Int = 0
    private var isRecording: Boolean = false
    private var mDetector: GestureDetector? = null
    private var isZoomIn = false
    private val or = 90
    private var cameraPosition = 1  // 0代表前置摄像头，1代表后置摄像头
    private var pictureSize = 0

    fun setRecorderType(type: Int) {
        this.recorderType = type
    }

    fun setTargetDir(file: File) {
        this.targetDir = file
    }

    fun setTargetName(name: String) {
        this.targetName = name
    }

    fun getTargetFilePath(): String = targetFile?.path ?: ""

    fun getPreviewWidth(): Int = previewWidth

    fun getPreviewHeight(): Int = previewHeight

    fun isRecording(): Boolean = isRecording

    fun deleteTargetFile(): Boolean = when {
        targetFile?.exists() == true -> targetFile?.delete() ?: false
        else -> false
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setSurfaceView(view: SurfaceView) {
        this.mSurfaceView = view
        mSurfaceHolder = mSurfaceView?.holder
        mSurfaceHolder?.setFixedSize(previewWidth, previewHeight)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mSurfaceHolder?.addCallback(this)
        mDetector = GestureDetector(activity, ZoomGestureListener())
        mSurfaceView?.setOnTouchListener({ _, event ->
            mDetector?.onTouchEvent(event)
            true
        })
    }

    fun record() {
        if (isRecording) {
            try {
                mMediaRecorder?.stop()  // stop the recording
            } catch (e: RuntimeException) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                // Timber.d("record: RuntimeException: stop() is called immediately after start()")

                targetFile?.delete()
            }

            releaseMediaRecorder() // release the MediaRecorder object
            mCamera?.lock()         // take camera access back from MediaRecorder
            isRecording = false
        } else {
            startRecordThread()
        }
    }

    fun stopRecordSave() {
        if (!isRecording) return
        isRecording = false
        try {
            mMediaRecorder?.stop()
            // Timber.d(targetFile?.path)
        } catch (r: RuntimeException) {
            // Timber.d("stopRecordSave: RuntimeException: stop() is called immediately after start()")
        } finally {
            releaseMediaRecorder()
        }
    }

    fun stopRecordUnSave() {
        if (!isRecording) return
        isRecording = false
        try {
            mMediaRecorder?.stop()
        } catch (r: RuntimeException) {
            // Timber.d("stopRecordUnSave: RuntimeException: stop() is called immediately after start()")
            deleteTargetFileUnit()
        } finally {
            releaseMediaRecorder()
        }
        deleteTargetFileUnit()
    }

    fun switchCamera() {
        val cameraCount: Int = Camera.getNumberOfCameras() // 得到摄像头的个数
        val cameraInfo = Camera.CameraInfo()

        loop@ for (i in 0 until cameraCount) {
            Camera.getCameraInfo(i, cameraInfo) // 得到每一个摄像头的信息
            if (cameraPosition == 1) {
                // 现在是后置，变更为前置。CAMERA_FACING_FRONT前置、CAMERA_FACING_BACK后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    onSwitchCamera(i)
                    cameraPosition = 0
                    break@loop
                }
            } else {
                // 现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    onSwitchCamera(i)
                    cameraPosition = 1
                    break@loop
                }
            }
        }
    }

    private fun onSwitchCamera(i: Int) {
        mCamera?.stopPreview()        // 停掉原来摄像头的预览
        mCamera?.release()            // 释放资源
        mCamera = null                // 取消原来摄像头
        mCamera = Camera.open(i)      // 打开当前选中的摄像头
        startPreView(mSurfaceHolder)
    }

    private fun deleteTargetFileUnit() {
        if (targetFile?.exists() == true) targetFile?.delete() //不保存直接删掉
    }

    private fun releaseMediaRecorder() {
        // clear recorder configuration
        mMediaRecorder?.reset()
        // release the recorder object
        mMediaRecorder?.release()
        mMediaRecorder = null
        // Lock camera for later use i.e taking it back from MediaRecorder.
        // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
        // Timber.d("release Recorder")
    }

    private fun releaseCamera() {
        // release the camera for other applications
        mCamera?.release()
        mCamera = null
        // Timber.d("release Camera")
    }

    private fun startRecordThread() {
        when {
            prepareRecord() -> try {
                mMediaRecorder?.start()
                isRecording = true
                // Timber.d("Start Record")
            } catch (r: RuntimeException) {
                releaseMediaRecorder()
                // Timber.d("Start Record: RuntimeException: start() is called immediately after stop()")
            }
        }
    }

    private fun prepareRecord(): Boolean {
        try {
            mMediaRecorder = MediaRecorder()
            when (recorderType) {
                MEDIA_VIDEO -> {
                    mCamera?.unlock()
                    mMediaRecorder?.setCamera(mCamera)
                    mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                    mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    mMediaRecorder?.setProfile(profile)
                    // 实际视屏录制后的方向
                    when (cameraPosition) {
                        0 -> mMediaRecorder?.setOrientationHint(270)
                        else -> mMediaRecorder?.setOrientationHint(or)
                    }
                }
                MEDIA_AUDIO -> {
                    mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                    mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
            }
            targetFile = File(targetDir, targetName)
            mMediaRecorder?.setOutputFile(targetFile?.path)

        } catch (e: Exception) {
            e.printStackTrace()
            // Timber.d("MediaRecorder: Exception prepareRecord")
            releaseMediaRecorder()
            return false
        }

        try {
            mMediaRecorder?.prepare()
        } catch (e: IllegalStateException) {
            //Timber.d("MediaRecorder: IllegalStateException preparing MediaRecorder: " + e.message)
            releaseMediaRecorder()
            return false
        } catch (e: IOException) {
            // Timber.d("MediaRecorder: IOException preparing MediaRecorder: " + e.message)
            releaseMediaRecorder()
            return false
        }

        return true
    }

    private fun startPreView(holder: SurfaceHolder?) {
        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        }
        try {
            mCamera!!.setDisplayOrientation(or)
            mCamera!!.setPreviewDisplay(holder)
            val parameters = mCamera!!.parameters

            // 方法一：
//                val mSupportedPreviewSizes = parameters.supportedPreviewSizes
//                val mSupportedVideoSizes = parameters.supportedVideoSizes
//                val w = mSurfaceView?.width ?: 1080
//                val h = mSurfaceView?.height ?: 1920
//                val optimalSize = CameraHelper.getOptimalVideoSize(
//                        mSupportedVideoSizes,
//                        mSupportedPreviewSizes,
//                        w,
//                        h
//                )
//                // Use the same size for recording profile.
//                val preW = optimalSize?.width ?: 480
//                val preH = optimalSize?.height ?: 720
//                previewWidth = preW
//                previewHeight = preH

            // 方法二：
            // 选择合适的预览尺寸
//                var previewWidth = 0
//                var previewHeight = 0
//                val sizeList = parameters.supportedPictureSizes
//                // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
//                if (sizeList.size > 1) {
//                    val itor = sizeList.iterator()
//                    while (itor.hasNext()) {
//                        val cur = itor.next()
//                        if (cur.width >= previewWidth && cur.height >= previewHeight) {
//                            previewWidth = cur.width
//                            previewHeight = cur.height
//                            break
//                        }
//                    }
//                }


            // 方法三：
            val bestSize = getPreviewSize(parameters)
            previewWidth = bestSize?.width ?: 640
            previewHeight = bestSize?.height ?: 360

            // 设置摄像区域的大小
            parameters.setPreviewSize(previewWidth, previewHeight)

            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
            // 这里是重点，分辨率和比特率
            // 分辨率越大视频大小越大，比特率越大视频越清晰
            // 清晰度由比特率决定，视频尺寸和像素量由分辨率决定
            // 比特率越高越清晰（前提是分辨率保持不变），分辨率越大视频尺寸越大
            if (parameters.supportedVideoSizes == null || parameters.supportedVideoSizes.size == 0) {
                profile?.videoFrameWidth = previewWidth
                profile?.videoFrameHeight = previewHeight
            } else {
                val bestVideoSize = getVideoSize(parameters)
                profile?.videoFrameWidth = bestVideoSize?.width ?: 640
                profile?.videoFrameHeight = bestVideoSize?.height ?: 360
            }

            // 设置可以调整清晰度，根据自己需求调节
            pictureSize = getCameraPictureSize()
            profile?.videoBitRate = when {
                pictureSize < 3000000 -> 3 * 1024 * 1024
                pictureSize <= 5000000 -> 2 * 1024 * 1024
                else -> 1024 * 1024
            }

            // 设置每秒帧数 这个设置有可能会出问题，有的手机不支持这种帧率就会录制失败，这里使用默认的帧率，当然视频的大小肯定会受影响
            // profile?.videoFrameRate = 24

            profile?.videoCodec = MediaRecorder.VideoEncoder.H264

            // 以下会导致 camera setParameters failed，去掉无影响，待研究
//            val focusModes = parameters.supportedFocusModes
//            when {
//                focusModes != null -> for (mode in focusModes) {
//                    mode.contains("continuous-video")
//                    parameters.focusMode = "continuous-video"
//                }
//            }

            mCamera!!.parameters = parameters
            mCamera!!.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 根据手机支持的视频分辨率，设置预览尺寸
     */
    private fun getPreviewSize(params: Camera.Parameters): Camera.Size? {
        // 获取手机支持的分辨率集合，并以宽度为基准降序排序
        params.supportedPreviewSizes.sortWith(Comparator { lhs, rhs ->
            when {
                lhs.width > rhs.width -> -1
                lhs.width == rhs.width -> 0
                else -> 1
            }
        })

        // 高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        val ratio = 3.0f / 4.0f
        var minDiff = 100f
        var bestSize: Camera.Size? = null
        params.supportedPreviewSizes.forEach { s ->
            val temp = Math.abs(s.height.toFloat() / s.width.toFloat() - ratio)
            if (temp < minDiff) {
                minDiff = temp
                bestSize = s
            }
        }

        return bestSize
    }

    /**
     * 根据手机支持的视频分辨率，设置录制尺寸
     */
    private fun getVideoSize(params: Camera.Parameters): Camera.Size? {
        // 获取手机支持的分辨率集合，并以宽度为基准降序排序
        params.supportedVideoSizes.sortWith(Comparator { lhs, rhs ->
            when {
                lhs.width > rhs.width -> -1
                lhs.width == rhs.width -> 0
                else -> 1
            }
        })

        // 高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        val ratio = 3.0f / 4.0f
        var minDiff = 100f
        var bestSize: Camera.Size? = null
        params.supportedVideoSizes.forEach { s ->
            val temp = Math.abs(s.height.toFloat() / s.width.toFloat() - ratio)
            if (temp < minDiff) {
                minDiff = temp
                bestSize = s
            }
        }

        return bestSize
    }

    private fun getCameraPictureSize(): Int {
        var ps = 0
        mCamera?.let {
            it.parameters.supportedPictureSizes
                    .asSequence()
                    .map { it.height * it.width }
                    .filter { it > ps }
                    .forEach { ps = it }
        }
        return ps
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        if (mCamera != null) releaseCamera()
        if (mMediaRecorder != null) releaseMediaRecorder()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mSurfaceHolder = holder
        startPreView(holder)
    }

    private inner class ZoomGestureListener : GestureDetector.SimpleOnGestureListener() {
        //双击手势事件
        override fun onDoubleTap(e: MotionEvent): Boolean {
            super.onDoubleTap(e)
            isZoomIn = if (!isZoomIn) {
                setZoom(20)
                true
            } else {
                setZoom(0)
                false
            }
            return true
        }

        private fun setZoom(zoomValue: Int) {
            if (mCamera != null) {
                val parameters = mCamera?.parameters
                if (parameters?.isZoomSupported == true) {
                    val maxZoom = parameters.maxZoom
                    when {
                        maxZoom == 0 -> return
                        zoomValue > maxZoom -> parameters.zoom = maxZoom
                    }
                    mCamera?.parameters = parameters
                }
            }
        }
    }


}
