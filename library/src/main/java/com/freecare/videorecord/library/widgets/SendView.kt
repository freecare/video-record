package com.freecare.videorecord.library.widgets

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.freecare.videorecord.library.R
import com.freecare.videorecord.library.utils.Utils

class SendView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    var backLayout: RelativeLayout
    var selectLayout: RelativeLayout

    private val utils by lazy { Utils.getInstance(context) }

    init {
        val params = ViewGroup.LayoutParams(
                utils.getWidthPixels(),
                utils.dp2px(180f)
        )
        layoutParams = params
        val layout = LayoutInflater.from(context).inflate(R.layout.video_record_widget_send_view, null, false)
        layout.layoutParams = params
        backLayout = layout.findViewById(R.id.return_layout)
        selectLayout = layout.findViewById(R.id.select_layout)
        addView(layout)
        visibility = View.GONE
    }

    fun startAnim() {
        visibility = View.VISIBLE
        val set = AnimatorSet()
        set.playTogether(
                ObjectAnimator.ofFloat(backLayout, "translationX", 0f, -180f),
                ObjectAnimator.ofFloat(selectLayout, "translationX", 0f, 180f)
        )
        set.setDuration(250).start()
    }

    fun stopAnim() {
        val set = AnimatorSet()
        set.playTogether(
                ObjectAnimator.ofFloat(backLayout, "translationX", -180f, 0f),
                ObjectAnimator.ofFloat(selectLayout, "translationX", 180f, 0f)
        )
        set.setDuration(250).start()
        visibility = View.GONE
    }
}
