package com.junaidjamshid.i211203.presentation.common.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * A custom RecyclerView.ItemAnimator that provides Instagram-like staggered
 * slide-up animations when items are added to the list.
 */
class StaggeredFadeAnimator : DefaultItemAnimator() {

    companion object {
        private const val ADD_DURATION = 350L
        private const val REMOVE_DURATION = 200L
        private const val MOVE_DURATION = 250L
        private const val CHANGE_DURATION = 250L
        private const val STAGGER_DELAY = 50L
    }

    init {
        addDuration = ADD_DURATION
        removeDuration = REMOVE_DURATION
        moveDuration = MOVE_DURATION
        changeDuration = CHANGE_DURATION
    }

    private val pendingAdds = mutableListOf<RecyclerView.ViewHolder>()
    private val addAnimations = mutableListOf<RecyclerView.ViewHolder>()
    private var lastAnimatedPosition = -1

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.alpha = 0f
        holder.itemView.translationY = holder.itemView.height * 0.15f
        holder.itemView.scaleX = 0.95f
        holder.itemView.scaleY = 0.95f
        pendingAdds.add(holder)
        return true
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()

        if (pendingAdds.isEmpty()) return

        val additions = ArrayList(pendingAdds)
        pendingAdds.clear()

        additions.forEachIndexed { index, holder ->
            val delay = if (holder.adapterPosition > lastAnimatedPosition) {
                lastAnimatedPosition = holder.adapterPosition
                (index * STAGGER_DELAY).coerceAtMost(300L)
            } else {
                0L
            }

            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ADD_DURATION)
                .setStartDelay(delay)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animator: Animator) {
                        addAnimations.add(holder)
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        holder.itemView.animate().setListener(null)
                        addAnimations.remove(holder)
                        dispatchAddFinished(holder)
                        dispatchFinishedWhenDone()
                    }

                    override fun onAnimationCancel(animator: Animator) {
                        clearAnimatedValues(holder.itemView)
                    }
                })
                .start()
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        
        holder.itemView.animate()
            .alpha(0f)
            .translationX(-holder.itemView.width * 0.2f)
            .setDuration(REMOVE_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    holder.itemView.animate().setListener(null)
                    clearAnimatedValues(holder.itemView)
                    dispatchRemoveFinished(holder)
                    dispatchFinishedWhenDone()
                }
            })
            .start()
        
        return false
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        item.itemView.animate().cancel()
        clearAnimatedValues(item.itemView)
        
        if (pendingAdds.remove(item)) {
            dispatchAddFinished(item)
        }
        if (addAnimations.remove(item)) {
            dispatchAddFinished(item)
        }
        
        super.endAnimation(item)
    }

    override fun endAnimations() {
        pendingAdds.forEach { holder ->
            clearAnimatedValues(holder.itemView)
            dispatchAddFinished(holder)
        }
        pendingAdds.clear()

        addAnimations.forEach { holder ->
            holder.itemView.animate().cancel()
            clearAnimatedValues(holder.itemView)
        }
        addAnimations.clear()

        super.endAnimations()
    }

    override fun isRunning(): Boolean {
        return pendingAdds.isNotEmpty() || addAnimations.isNotEmpty() || super.isRunning()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
    }

    private fun clearAnimatedValues(view: View) {
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    /**
     * Resets the animation position tracker. Call this when the list is
     * refreshed to re-enable staggered animations from the top.
     */
    fun resetAnimationState() {
        lastAnimatedPosition = -1
    }
}
