package com.junaidjamshid.i211203

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Followers : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        setContentView(R.layout.activity_followers)
    }
}