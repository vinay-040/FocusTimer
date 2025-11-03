package com.focustimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.focustimer.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Signed in successfully, navigate to the main activity
            Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } catch (e: ApiException) {
            Log.w("SignupActivity", "signInResult:failed code=" + e.statusCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.signupButton.setOnClickListener {
            if (validateInput()) {
                Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // Clear previous errors
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.phoneInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null

        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return false
        }

        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Invalid email format"
            return false
        }

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = "Phone number is required"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            return false
        }

        val passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$")
        if (!passwordPattern.matcher(password).matches()) {
            binding.passwordInputLayout.error = "Password must be at least 8 characters long and include an uppercase letter, a number, and a special character."
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
            return false
        }

        return true
    }
}
