package com.freecare.videorecord.library.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.freecare.videorecord.library.R
import com.freecare.videorecord.library.utils.MediaUtils
import kotlinx.android.synthetic.main.video_record_activity_main.*
import java.util.*

class VideoRecordActivity : AppCompatActivity() {

    private lateinit var mediaUtils: MediaUtils
    private var isCancel = false
    private var mProgress = 0f
    private var maxTime = 15
    private var progressStep = 1f
    private var delayMillis = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_record_activity_main)
        initView()
    }

    override fun onResume() {
        super.onResume()
        mProgressBar.setCancel(true)
    }

    private fun initView() {
        val videoSecond = intent.getIntExtra("videoSecond", maxTime)

        progressStep = 1000 / delayMillis / videoSecond.toFloat()
        // setting
        mediaUtils = MediaUtils(this)
        mediaUtils.setRecorderType(MediaUtils.MEDIA_VIDEO)
        mediaUtils.setTargetDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
        // mediaUtils.setTargetDir(getMovieStorageDir())
        mediaUtils.setTargetName("${UUID.randomUUID()}.mp4")
        mediaUtils.setSurfaceView(mSurfaceView)
        // btn
        mPressControl.setOnTouchListener(BtnTouch())
        mCloseBtn.setOnClickListener { finish() }
        // sendView
        mSendView.backLayout.setOnClickListener {
            mSendView.stopAnim()
            mRecordLayoutRl.visibility = View.VISIBLE
            mediaUtils.deleteTargetFile()
        }
        mSendView.selectLayout.setOnClickListener {
            val path = mediaUtils.getTargetFilePath()
            mSendView.stopAnim()
            mRecordLayoutRl.visibility = View.VISIBLE
            // 返回结果
            val intent = Intent()
            intent.putExtra("video_record_path", path)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        // switch
        mSwitchBtn.setOnClickListener { mediaUtils.switchCamera() }
        // progress
        mProgressBar.setOnProgressEndListener {
            mProgressBar.setCancel(true)
            mediaUtils.stopRecordSave()
        }
    }

//    private fun getMovieStorageDir(): File {
//        val file = File(this.getExternalFilesDir(Environment.DIRECTORY_MOVIES), null)
//        if (!file.mkdir()) {
//            return File("")
//        }
//        return file
//    }

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    mProgressBar.setProgress(mProgress.toInt())
                    if (mediaUtils.isRecording()) {
                        mProgress += progressStep
                        sendMessageDelayed(this.obtainMessage(0), delayMillis)
                    }
                }
            }
        }
    }

    private fun startView() {
        startAnim()
        handler.removeMessages(0)
        handler.sendMessage(handler.obtainMessage(0))
    }

    private fun stopView(isSave: Boolean) {
        stopAnim()
        mProgressBar.setCancel(true)
        mProgress = 0f
        handler.removeMessages(0)
        mInfoTv.text = "按住摄像"
        if (!isSave) return
        mRecordLayoutRl.visibility = View.GONE
        mSendView.startAnim()
    }

    private fun moveView() = when {
        isCancel -> mInfoTv.text = "松手取消"
        else -> mInfoTv.text = "上滑取消"
    }

    private fun startAnim() {
        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(mPressControl, "scaleX", 1f, 0.5f),
            ObjectAnimator.ofFloat(mPressControl, "scaleY", 1f, 0.5f),
            ObjectAnimator.ofFloat(mProgressBar, "scaleX", 1f, 1.3f),
            ObjectAnimator.ofFloat(mProgressBar, "scaleY", 1f, 1.3f)
        )
        set.setDuration(250).start()
    }

    private fun stopAnim() {
        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(mPressControl, "scaleX", 0.5f, 1f),
            ObjectAnimator.ofFloat(mPressControl, "scaleY", 0.5f, 1f),
            ObjectAnimator.ofFloat(mProgressBar, "scaleX", 1.3f, 1f),
            ObjectAnimator.ofFloat(mProgressBar, "scaleY", 1.3f, 1f)
        )
        set.setDuration(250).start()
    }

    inner class BtnTouch : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View?, event: MotionEvent?): Boolean {
            val downY = 0
            val action = event?.action

            if (view?.id != R.id.mPressControl) return false
            return when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mediaUtils.record()
                    startView()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isCancel) when {
                        mProgress == 0f -> stopView(false)
                        mProgress < 10 -> {
                            //时间太短不保存
                            mediaUtils.stopRecordUnSave()
                            Toast.makeText(this@VideoRecordActivity, "时间太短", Toast.LENGTH_SHORT).show()
                            stopView(false)
                        }
                        else -> {
                            //停止录制
                            mediaUtils.stopRecordSave()
                            stopView(true)
                        }
                    }
                    else {
                        //现在是取消状态,不保存
                        mediaUtils.stopRecordUnSave()
                        stopView(false)
                    }
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentY = event.y
                    isCancel = downY - currentY > 10
                    moveView()
                    false
                }
                else -> false
            }
        }

    }

}
