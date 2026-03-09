package com.junaidjamshid.i211203.presentation.post

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.junaidjamshid.i211203.R

/**
 * Bottom sheet dialog for selecting background music for a post.
 * Provides a curated list of popular songs the user can choose from.
 */
class MusicPickerBottomSheet : BottomSheetDialogFragment() {

    data class MusicItem(
        val name: String,
        val artist: String,
        val genre: String = ""
    )

    private var onMusicSelected: ((MusicItem) -> Unit)? = null
    private var selectedItem: MusicItem? = null

    private val allMusic = listOf(
        MusicItem("Blinding Lights", "The Weeknd", "Pop"),
        MusicItem("Levitating", "Dua Lipa", "Pop"),
        MusicItem("Stay", "The Kid LAROI & Justin Bieber", "Pop"),
        MusicItem("Peaches", "Justin Bieber ft. Daniel Caesar", "R&B"),
        MusicItem("Kiss Me More", "Doja Cat ft. SZA", "Pop"),
        MusicItem("Montero", "Lil Nas X", "Pop"),
        MusicItem("Good 4 U", "Olivia Rodrigo", "Pop Rock"),
        MusicItem("Save Your Tears", "The Weeknd", "Synth-pop"),
        MusicItem("Butter", "BTS", "Pop"),
        MusicItem("drivers license", "Olivia Rodrigo", "Pop"),
        MusicItem("Heat Waves", "Glass Animals", "Indie"),
        MusicItem("Shivers", "Ed Sheeran", "Pop"),
        MusicItem("Easy On Me", "Adele", "Pop"),
        MusicItem("Industry Baby", "Lil Nas X & Jack Harlow", "Hip-Hop"),
        MusicItem("Sad Girlz Luv Money", "Amaarae", "Afrobeats"),
        MusicItem("Beggin'", "Måneskin", "Rock"),
        MusicItem("Take My Breath", "The Weeknd", "Pop"),
        MusicItem("Woman", "Doja Cat", "Pop"),
        MusicItem("Essence", "Wizkid ft. Tems", "Afrobeats"),
        MusicItem("Astronaut In The Ocean", "Masked Wolf", "Hip-Hop"),
        MusicItem("Deja Vu", "Olivia Rodrigo", "Pop"),
        MusicItem("RAPSTAR", "Polo G", "Hip-Hop"),
        MusicItem("Watermelon Sugar", "Harry Styles", "Pop"),
        MusicItem("Dynamite", "BTS", "Disco-pop"),
        MusicItem("Bad Habits", "Ed Sheeran", "Pop"),
        MusicItem("As It Was", "Harry Styles", "Synth-pop"),
        MusicItem("Anti-Hero", "Taylor Swift", "Pop"),
        MusicItem("Unholy", "Sam Smith & Kim Petras", "Pop"),
        MusicItem("Die For You", "The Weeknd", "R&B"),
        MusicItem("Flowers", "Miley Cyrus", "Pop")
    )

    fun setOnMusicSelectedListener(listener: (MusicItem) -> Unit) {
        onMusicSelected = listener
    }

    fun setCurrentSelection(name: String, artist: String) {
        if (name.isNotEmpty()) {
            selectedItem = allMusic.find { it.name == name && it.artist == artist }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.et_music_search)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_music_list)
        val selectedContainer = view.findViewById<LinearLayout>(R.id.selected_music_container)
        val tvSelectedMusic = view.findViewById<TextView>(R.id.tv_selected_music)
        val btnDone = view.findViewById<TextView>(R.id.btn_done_music)

        val adapter = MusicListAdapter(allMusic.toMutableList()) { music ->
            selectedItem = music
            selectedContainer.visibility = View.VISIBLE
            tvSelectedMusic.text = "${music.name} · ${music.artist}"
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Highlight current selection
        selectedItem?.let { current ->
            selectedContainer.visibility = View.VISIBLE
            tvSelectedMusic.text = "${current.name} · ${current.artist}"
        }

        // Search filter
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                if (query.isEmpty()) {
                    adapter.updateList(allMusic)
                } else {
                    val filtered = allMusic.filter {
                        it.name.lowercase().contains(query) ||
                                it.artist.lowercase().contains(query) ||
                                it.genre.lowercase().contains(query)
                    }
                    adapter.updateList(filtered)
                }
            }
        })

        btnDone.setOnClickListener {
            selectedItem?.let { music ->
                onMusicSelected?.invoke(music)
            }
            dismiss()
        }
    }

    /**
     * Inner adapter for the music list
     */
    private inner class MusicListAdapter(
        private var items: MutableList<MusicItem>,
        private val onItemClick: (MusicItem) -> Unit
    ) : RecyclerView.Adapter<MusicListAdapter.MusicVH>() {

        fun updateList(newList: List<MusicItem>) {
            items.clear()
            items.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_music, parent, false)
            return MusicVH(view)
        }

        override fun onBindViewHolder(holder: MusicVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class MusicVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val songName: TextView = itemView.findViewById(R.id.tv_song_name)
            private val artistName: TextView = itemView.findViewById(R.id.tv_artist_name)
            private val selectedIcon: ImageView = itemView.findViewById(R.id.iv_selected)

            fun bind(music: MusicItem) {
                songName.text = music.name
                artistName.text = "${music.artist} · ${music.genre}"

                val isSelected = selectedItem?.name == music.name &&
                        selectedItem?.artist == music.artist
                selectedIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

                itemView.setOnClickListener { onItemClick(music) }
            }
        }
    }

    companion object {
        const val TAG = "MusicPickerBottomSheet"
    }
}
