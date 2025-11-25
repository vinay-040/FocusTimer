package com.focustimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.focustimer.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadUserProfile()

        val sharedPreferences = getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)

        // Load saved settings
        binding.focusLengthEditText.setText(sharedPreferences.getLong("focusLength", 25).toString())
        binding.shortBreakEditText.setText(sharedPreferences.getLong("shortBreakLength", 5).toString())
        binding.longBreakEditText.setText(sharedPreferences.getLong("longBreakLength", 15).toString())

        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightThemeRadioButton.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkThemeRadioButton.isChecked = true
            else -> binding.systemThemeRadioButton.isChecked = true
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.lightThemeRadioButton -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.darkThemeRadioButton -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                R.id.systemThemeRadioButton -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        binding.versionValue.text = packageManager.getPackageInfo(packageName, 0).versionName

        binding.aboutLabel.setOnClickListener {
            showAboutDialog()
        }

        binding.saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putLong("focusLength", binding.focusLengthEditText.text.toString().toLong())
            editor.putLong("shortBreakLength", binding.shortBreakEditText.text.toString().toLong())
            editor.putLong("longBreakLength", binding.longBreakEditText.text.toString().toLong())
            editor.apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.profileName.text = document.getString("name")
                        binding.profileEmail.text = document.getString("email")
                    }
                }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About FocusTimer")
            .setMessage("FocusTimer is a simple and effective Pomodoro timer designed to help you stay focused and productive.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
