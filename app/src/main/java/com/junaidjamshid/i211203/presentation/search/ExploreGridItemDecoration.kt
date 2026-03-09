package com.junaidjamshid.i211203.presentation.search

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Item decoration for the Instagram explore grid.
 * Adds uniform spacing between grid items.
 */
class ExploreGridItemDecoration(
    private val spacingDp: Int
) : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val spacingPx = (spacingDp * view.context.resources.displayMetrics.density).toInt()
        outRect.left = spacingPx
        outRect.right = spacingPx
        outRect.top = spacingPx
        outRect.bottom = spacingPx
    }
}
