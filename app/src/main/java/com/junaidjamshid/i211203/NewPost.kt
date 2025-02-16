package com.junaidjamshid.i211203

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView

class NewPost : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)
        supportActionBar?.hide()
        val gridView = findViewById<GridView>(R.id.grid_view)

        val imageList = listOf(
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1
        )

        val adapter = newPostImageAdapter(this, imageList)
        gridView.adapter = adapter
    }
}