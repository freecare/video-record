package com.freecare.videorecord.library.utils

import android.content.Context
import android.content.res.Configuration

class Utils private constructor(private val context: Context) {

    companion object {
        fun getInstance(context: Context): Utils = Utils(context)
    }

    fun getWidthPixels(): Int {
        val displayMetrics = context.resources.displayMetrics
        return when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> // 横屏
                displayMetrics.heightPixels
            Configuration.ORIENTATION_PORTRAIT -> // 竖屏
                displayMetrics.widthPixels
            else -> 0
        }
    }

    fun getHeightPixels(): Int {
        val displayMetrics = context.resources.displayMetrics
        return when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> // 横屏
                displayMetrics.widthPixels
            Configuration.ORIENTATION_PORTRAIT -> // 竖屏
                displayMetrics.heightPixels
            else -> 0
        }
    }

    fun dp2px(dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dp(pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}
