package com.esafirm.imagepicker.helper

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import com.esafirm.imagepicker.R

object ViewUtils {

    fun getArrowIcon(context: Context): Drawable? {
        val backResourceId = when (context.resources.configuration.layoutDirection) {
            View.LAYOUT_DIRECTION_RTL -> R.drawable.ef_ic_arrow_forward
            else -> R.drawable.ef_ic_arrow_back
        }
        return ContextCompat.getDrawable(context.applicationContext, backResourceId)
    }

    fun onPreDraw(view: View, runnable: Runnable) {
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                runnable.run()
                return false
            }
        })
    }
}
