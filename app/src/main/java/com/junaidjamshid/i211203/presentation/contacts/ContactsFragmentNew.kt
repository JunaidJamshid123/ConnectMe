package com.junaidjamshid.i211203.presentation.contacts

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentContactsBinding
import com.junaidjamshid.i211203.presentation.chat.ChatActivity
import com.junaidjamshid.i211203.presentation.contacts.adapter.ContactsAdapterNew
import com.junaidjamshid.i211203.presentation.contacts.adapter.NoteItem
import com.junaidjamshid.i211203.presentation.contacts.adapter.NotesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Instagram-style DM / Contacts Fragment.
 */
@AndroidEntryPoint
class ContactsFragmentNew : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapterNew
    private lateinit var notesAdapter: NotesAdapter

    // Currently selected tab
    private var selectedTab = Tab.MESSAGES

    private enum class Tab { MESSAGES, CHANNELS, REQUESTS }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotesRow()
        setupTabs()
        setupRecyclerView()
        setupSwipeActions()
        observeUiState()

        // Populate dummy notes for the story/notes row (UI-only, no backend)
        loadDummyNotes()
    }

    // ─── NOTES / STORIES ROW ────────────────────────────────────────────

    private fun setupNotesRow() {
        notesAdapter = NotesAdapter { note ->
            // Placeholder click – no backend
        }

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = notesAdapter
        }
    }

    /**
     * Populate the horizontal notes row with placeholder data.
     * This is purely UI – no backend calls.
     */
    private fun loadDummyNotes() {
        val dummyNotes = listOf(
            NoteItem(
                userId = "self",
                username = "Your note",
                noteText = "Share a\nthought!",
                isCurrentUser = true
            ),
            NoteItem(
                userId = "1",
                username = "Jihoon Song",
                noteText = "\uD83C\uDFD6\uFE0FSea ranch\nthis weekend!",
                isOnline = true
            ),
            NoteItem(
                userId = "2",
                username = "Ricky Padilla",
                isOnline = false
            ),
            NoteItem(
                userId = "3",
                username = "Alex Walker",
                noteText = "Boo!",
                isOnline = true
            ),
            NoteItem(
                userId = "4",
                username = "Maria Lopez",
                isOnline = false
            ),
            NoteItem(
                userId = "5",
                username = "Sam Chen",
                noteText = "\uD83C\uDF89 Party!",
                isOnline = true
            )
        )
        notesAdapter.submitList(dummyNotes)
    }

    // ─── TAB BAR ────────────────────────────────────────────────────────

    private fun setupTabs() {
        updateTabUI()

        binding.tabMessages.setOnClickListener {
            selectedTab = Tab.MESSAGES
            updateTabUI()
        }
        binding.tabChannels.setOnClickListener {
            selectedTab = Tab.CHANNELS
            updateTabUI()
        }
        binding.tabRequests.setOnClickListener {
            selectedTab = Tab.REQUESTS
            updateTabUI()
        }
    }

    private fun updateTabUI() {
        val tabs = listOf(
            binding.tabMessages to Tab.MESSAGES,
            binding.tabChannels to Tab.CHANNELS,
            binding.tabRequests to Tab.REQUESTS
        )
        for ((textView, tab) in tabs) {
            if (tab == selectedTab) {
                textView.setBackgroundResource(R.drawable.bg_tab_selected)
                textView.setTextColor(0xFFFFFFFF.toInt())
            } else {
                textView.setBackgroundResource(R.drawable.bg_tab_unselected)
                textView.setTextColor(0xFF262626.toInt())
            }
        }
    }

    // ─── CHAT LIST ──────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapterNew { contact ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("USER_ID", contact.user.userId)
            startActivity(intent)
        }

        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }
    }

    // ─── SWIPE-TO-REVEAL (Pin / Mute / Delete) ─────────────────────────

    private fun setupSwipeActions() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Show the swipe background, then reset after a moment
                val itemView = viewHolder.itemView
                val swipeBg = itemView.findViewById<View>(R.id.swipeBackground)
                val foreground = itemView.findViewById<View>(R.id.foregroundLayout)

                swipeBg?.visibility = View.VISIBLE

                // Reset the swipe after 2 seconds (no backend logic)
                foreground?.postDelayed({
                    contactsAdapter.notifyItemChanged(viewHolder.adapterPosition)
                }, 2000)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val foreground = viewHolder.itemView.findViewById<View>(R.id.foregroundLayout)
                val swipeBg = viewHolder.itemView.findViewById<View>(R.id.swipeBackground)

                // Show background when swiping
                if (dX < 0) {
                    swipeBg?.visibility = View.VISIBLE
                }

                // Only move the foreground
                foreground?.translationX = dX.coerceAtMost(0f)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                val foreground = viewHolder.itemView.findViewById<View>(R.id.foregroundLayout)
                foreground?.translationX = 0f
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.3f
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.contactsRecyclerView)
    }

    // ─── OBSERVE STATE ──────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: ContactsUiState) {
        // Update contacts list
        contactsAdapter.submitList(state.contacts)

        // Handle error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
