package com.freecare.videorecord.library.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.freecare.videorecord.library.R

class VideoProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mRecordPaint by lazy { Paint() }
    private val mBgPaint by lazy { Paint() }
    private val mRectF by lazy { RectF() }

    private var isCancel = false
    private var progress = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mOnProgressEndListener: OnProgressEndListener? = null
    private var mMaxProgress = 100

    companion object {
        private const val TAG = "BothWayProgressBar"
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        mWidth = width
        mHeight = height

        if (mWidth != mHeight) {
            val min = Math.min(mWidth, mHeight)
            mWidth = min
            mHeight = min
        }

        // 设置画笔相关属性
        val mCircleLineStrokeWidth = 10f
        mRecordPaint.isAntiAlias = true
        mRecordPaint.strokeWidth = mCircleLineStrokeWidth
        mRecordPaint.style = Paint.Style.STROKE

        // 位置
        mRectF.left = mCircleLineStrokeWidth / 2 + .8f
        mRectF.top = mCircleLineStrokeWidth / 2 + .8f
        mRectF.right = mWidth - mCircleLineStrokeWidth / 2 - 1.5f
        mRectF.bottom = mHeight - mCircleLineStrokeWidth / 2 - 1.5f

        // 实心圆
        mBgPaint.isAntiAlias = true
        mBgPaint.strokeWidth = mCircleLineStrokeWidth
        mBgPaint.style = Paint.Style.FILL
        mBgPaint.color = ContextCompat.getColor(context, R.color.video_record_btn_bg)

        canvas?.drawCircle(mWidth / 2f, mWidth / 2f, mWidth / 2 - .5f, mBgPaint)

        // 绘制圆圈，进度条背景
        if (isCancel) {
            mRecordPaint.color = Color.TRANSPARENT
            canvas?.drawArc(mRectF, -90f, 360f, false, mRecordPaint)
            isCancel = false
            return
        }

        when {
            progress == 0 -> {
                mRecordPaint.color = Color.TRANSPARENT
                canvas?.drawArc(mRectF, -90f, 360f, false, mRecordPaint)
            }
            progress in 1 until mMaxProgress -> {
                mRecordPaint.color = Color.GREEN
                canvas?.drawArc(mRectF, -90f, (progress.toFloat() / mMaxProgress) * 360f, false, mRecordPaint)
            }
            progress >= mMaxProgress -> this.mOnProgressEndListener?.onProgressEndListener()
        }
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun setCancel(isCancel: Boolean) {
        this.isCancel = isCancel
        invalidate()
    }

    fun setOnProgressEndListener(listener: OnProgressEndListener) {
        this.mOnProgressEndListener = listener
    }

    fun setOnProgressEndListener(action: () -> Unit) {
        this.mOnProgressEndListener = object : OnProgressEndListener {
            override fun onProgressEndListener() {
                action()
            }
        }
    }

    interface OnProgressEndListener {
        fun onProgressEndListener()
    }

}
