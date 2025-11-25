package com.focustimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.focustimer.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        // If user is already logged in, skip this activity entirely
        if (auth.currentUser != null) {
            startActivity(Intent(this, TimerActivity::class.java))
            finish()
            return
        }

        setTheme(R.style.Theme_FocusTimer) // Switch back to the main theme
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, TimerActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for FocusTimer")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        binding.fingerprintCheckBox.isChecked = sharedPreferences.getBoolean("useFingerprint", false)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Correctly save the fingerprint preference
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("useFingerprint", binding.fingerprintCheckBox.isChecked)
                            editor.apply()

                            Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, TimerActivity::class.java))
                            finish()
                        } else {
                            // If sign in fails, display a message to the user.
                            binding.emailInputLayout.error = "Invalid email or password"
                            binding.passwordInputLayout.error = "Invalid email or password"
                        }
                    }
            } else {
                binding.emailInputLayout.error = "Email and password cannot be empty"
            }
        }

        binding.signupTextView.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        
        // Show biometric prompt if preferred
        if (binding.fingerprintCheckBox.isChecked) {
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Session check is now handled in onCreate to prevent re-triggering on every onStart
    }
}
