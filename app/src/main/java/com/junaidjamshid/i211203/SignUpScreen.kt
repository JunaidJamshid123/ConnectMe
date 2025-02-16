package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class SignUpScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_sign_up_screen)

        val loginLink = findViewById<TextView>(R.id.LoginLink)
        val fullName = findViewById<EditText>(R.id.FullName)
        val username = findViewById<EditText>(R.id.username)
        val phone = findViewById<EditText>(R.id.Phone)
        val email = findViewById<EditText>(R.id.Email)
        val password = findViewById<EditText>(R.id.Password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginScreem::class.java)
            startActivity(intent)
        }

        registerBtn.setOnClickListener {
            val fullNameInput = fullName.text.toString().trim()
            val usernameInput = username.text.toString().trim()
            val phoneInput = phone.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            if (fullNameInput.isEmpty() || usernameInput.isEmpty() || phoneInput.isEmpty() ||
                emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, HomePage::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}