package com.junaidjamshid.i211203.presentation.post

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityCommentsBinding
import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.presentation.post.adapter.CommentsAdapter
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style Comments Activity.
 */
@AndroidEntryPoint
class CommentsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_POST_ID = "post_id"

        fun newIntent(context: Context, postId: String): Intent {
            return Intent(context, CommentsActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
            }
        }
    }

    private lateinit var binding: ActivityCommentsBinding
    private val viewModel: PostViewModel by viewModels()

    private lateinit var commentsAdapter: CommentsAdapter
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Slide up animation
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
        
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId == null) {
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeState()

        viewModel.loadPost(postId!!)
        viewModel.loadComments(postId!!)
    }

    private fun setupUI() {
        // Apply window insets for proper edge-to-edge display
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, insets.bottom)
            windowInsets
        }
        
        // Load current user profile image
        // For now using default, can be enhanced to load from UserRepository
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(
            onUserClick = { userId ->
                startActivity(UserProfileActivity.newIntent(this, userId))
            },
            onLikeClick = { comment ->
                // TODO: Implement comment like
                Toast.makeText(this, "Like comment", Toast.LENGTH_SHORT).show()
            },
            onReplyClick = { comment ->
                // Focus on input and add @username
                binding.commentInput.setText("@${comment.username} ")
                binding.commentInput.setSelection(binding.commentInput.text.length)
                binding.commentInput.requestFocus()
                showKeyboard()
            }
        )

        binding.commentsRecycler.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = commentsAdapter
        }
    }

    private fun setupClickListeners() {
        // Back/close via drag handle area
        binding.dragHandle.setOnClickListener {
            finish()
        }
        
        binding.back.setOnClickListener {
            finish()
        }

        // Emoji quick reactions
        setupEmojiQuickReactions()

        // Enable/disable post button based on input
        binding.commentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnPostComment.isEnabled = hasText
                binding.btnPostComment.alpha = if (hasText) 1f else 0.5f
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Post comment
        binding.btnPostComment.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty() && postId != null) {
                viewModel.addComment(postId!!, commentText)
                binding.commentInput.text.clear()
                hideKeyboard()
            }
        }
    }

    private fun setupEmojiQuickReactions() {
        val emojiClickListener = View.OnClickListener { view ->
            val emoji = (view as? TextView)?.text?.toString() ?: return@OnClickListener
            // Append emoji to comment input
            val currentText = binding.commentInput.text.toString()
            binding.commentInput.setText("$currentText$emoji")
            binding.commentInput.setSelection(binding.commentInput.text.length)
        }

        binding.emojiHeart.setOnClickListener(emojiClickListener)
        binding.emojiClap.setOnClickListener(emojiClickListener)
        binding.emojiFire.setOnClickListener(emojiClickListener)
        binding.emojiThumbsup.setOnClickListener(emojiClickListener)
        binding.emojiCry.setOnClickListener(emojiClickListener)
        binding.emojiLove.setOnClickListener(emojiClickListener)
        binding.emojiLaugh.setOnClickListener(emojiClickListener)
        binding.emojiWow.setOnClickListener(emojiClickListener)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: PostUiState) {
        // Build comments list with post caption as first item
        val commentsList = mutableListOf<Comment>()

        // Add post caption as the first "comment" (Instagram style)
        state.post?.let { post ->
            if (post.caption.isNotEmpty()) {
                commentsList.add(
                    Comment(
                        commentId = "caption_${post.postId}",
                        postId = post.postId,
                        userId = post.userId,
                        username = post.username,
                        userProfileImage = post.userProfileImage ?: "",
                        content = post.caption,
                        timestamp = post.timestamp,
                        likesCount = 0,
                        isLikedByCurrentUser = false
                    )
                )
            }
        }

        // Add actual comments
        commentsList.addAll(state.comments)

        commentsAdapter.submitList(commentsList)

        // Show/hide empty state
        if (state.comments.isEmpty() && state.post?.caption.isNullOrEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.commentsRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.commentsRecycler.visibility = View.VISIBLE
        }

        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.commentInput.windowToken, 0)
    }
}
