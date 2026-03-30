package com.junaidjamshid.i211203.presentation.notifications

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ItemNotificationBinding
import com.junaidjamshid.i211203.databinding.ItemNotificationHeaderBinding
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.model.NotificationSection
import com.junaidjamshid.i211203.domain.model.NotificationType

/**
 * Sealed class representing items in the notification list
 */
sealed class NotificationListItem {
    data class Header(val section: NotificationSection) : NotificationListItem()
    data class Item(val notification: Notification) : NotificationListItem()
}

/**
 * Instagram-style Notifications Adapter with section headers
 */
class NotificationsAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onProfileClick: (Notification) -> Unit,
    private val onFollowClick: (Notification) -> Unit,
    private val onPostClick: (Notification) -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemNotificationHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                NotificationViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item.section)
            is NotificationListItem.Item -> (holder as NotificationViewHolder).bind(item.notification)
        }
    }

    /**
     * Submit grouped notifications to the adapter
     */
    fun submitGroupedList(groupedNotifications: Map<NotificationSection, List<Notification>>) {
        val items = mutableListOf<NotificationListItem>()

        groupedNotifications.forEach { (section, notifications) ->
            if (notifications.isNotEmpty()) {
                items.add(NotificationListItem.Header(section))
                notifications.forEach { notification ->
                    items.add(NotificationListItem.Item(notification))
                }
            }
        }

        submitList(items)
    }

    /**
     * ViewHolder for section headers
     */
    inner class HeaderViewHolder(
        private val binding: ItemNotificationHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: NotificationSection) {
            binding.sectionTitle.text = when (section) {
                NotificationSection.TODAY -> "Today"
                NotificationSection.THIS_WEEK -> "This Week"
                NotificationSection.THIS_MONTH -> "This Month"
                NotificationSection.EARLIER -> "Earlier"
            }
        }
    }

    /**
     * ViewHolder for notification items
     */
    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            // Profile image
            if (notification.userProfileImage.isNotEmpty()) {
                Glide.with(binding.profileImage)
                    .load(notification.userProfileImage)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }

            // Story ring
            binding.storyRing.isVisible = notification.hasStory

            // Build notification text with spannable
            val notificationText = buildNotificationText(notification)
            binding.notificationText.text = notificationText

            // Handle different notification types visibility
            when (notification.type) {
                NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST -> {
                    // Show follow button for follow notifications
                    binding.btnFollow.isVisible = !notification.isFollowing
                    binding.btnFollowing.isVisible = notification.isFollowing
                    binding.postThumbnail.isVisible = false
                }
                else -> {
                    // Show post thumbnail for like/comment notifications
                    binding.btnFollow.isVisible = false
                    binding.btnFollowing.isVisible = false
                    binding.postThumbnail.isVisible = notification.postId != null

                    if (notification.postThumbnail?.isNotEmpty() == true) {
                        Glide.with(binding.postThumbnail)
                            .load(notification.postThumbnail)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .error(R.drawable.bg_image_placeholder)
                            .centerCrop()
                            .into(binding.postThumbnail)
                    } else {
                        binding.postThumbnail.setImageResource(R.drawable.bg_image_placeholder)
                    }
                }
            }

            // Click listeners
            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }

            binding.profileImage.setOnClickListener {
                onProfileClick(notification)
            }

            binding.btnFollow.setOnClickListener {
                onFollowClick(notification)
            }

            binding.btnFollowing.setOnClickListener {
                onFollowClick(notification)
            }

            binding.postThumbnail.setOnClickListener {
                onPostClick(notification)
            }
        }

        /**
         * Build spannable text for notification based on type
         */
        private fun buildNotificationText(notification: Notification): SpannableStringBuilder {
            val builder = SpannableStringBuilder()

            // Bold username
            val usernameStart = 0
            builder.append(notification.username)
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                usernameStart,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Action text based on type
            when (notification.type) {
                NotificationType.LIKE -> {
                    builder.append(" liked your photo. ")
                }
                NotificationType.COMMENT -> {
                    builder.append(" commented: ")
                    notification.commentText?.let { comment ->
                        builder.append(comment)
                        builder.append(" ")
                    }
                }
                NotificationType.FOLLOW -> {
                    builder.append(" started following you. ")
                }
                NotificationType.FOLLOW_REQUEST -> {
                    builder.append(" requested to follow you. ")
                }
                NotificationType.MENTION -> {
                    builder.append(" mentioned you in a comment. ")
                }
                NotificationType.LIKE_COMMENT -> {
                    builder.append(" liked your comment. ")
                }
                NotificationType.TAG -> {
                    builder.append(" tagged you in a post. ")
                }
            }

            // Time ago (gray)
            builder.append(notification.timeAgo)

            return builder
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationListItem>() {
        override fun areItemsTheSame(
            oldItem: NotificationListItem,
            newItem: NotificationListItem
        ): Boolean {
            return when {
                oldItem is NotificationListItem.Header && newItem is NotificationListItem.Header ->
                    oldItem.section == newItem.section
                oldItem is NotificationListItem.Item && newItem is NotificationListItem.Item ->
                    oldItem.notification.notificationId == newItem.notification.notificationId
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: NotificationListItem,
            newItem: NotificationListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
