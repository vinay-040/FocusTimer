package com.focustimer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.focustimer.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            // Simulate login failure
            binding.emailInputLayout.error = "Invalid email or password"
            binding.passwordInputLayout.error = "Invalid email or password"
        }

        binding.signupTextView.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
