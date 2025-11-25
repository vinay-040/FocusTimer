package com.focustimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.focustimer.databinding.ActivityStatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadStats()
    }

    private fun loadStats() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val totalFocusTime = document.getLong("totalFocusTime") ?: 0
                        val sessionsCompleted = document.getLong("sessionsCompleted") ?: 0

                        val hours = totalFocusTime / 3600
                        val minutes = (totalFocusTime % 3600) / 60

                        binding.totalFocusTimeValue.text = "${hours}h ${minutes}m"
                        binding.sessionsCompletedValue.text = sessionsCompleted.toString()
                    }
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
