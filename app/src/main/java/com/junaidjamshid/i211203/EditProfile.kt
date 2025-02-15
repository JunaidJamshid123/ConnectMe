package com.junaidjamshid.i211203

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class EditProfile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_edit_profile)
    }
}