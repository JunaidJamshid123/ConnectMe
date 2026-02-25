package com.junaidjamshid.i211203.presentation.contacts

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.databinding.FragmentContactsBinding
import com.junaidjamshid.i211203.presentation.chat.ChatActivity
import com.junaidjamshid.i211203.presentation.contacts.adapter.ContactsAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Contacts Fragment.
 */
@AndroidEntryPoint
class ContactsFragmentNew : Fragment() {
    
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapterNew
    
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
        
        setupRecyclerView()
        observeUiState()
    }
    
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
