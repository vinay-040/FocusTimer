package com.focustimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.focustimer.databinding.ActivityTimerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var timer: CountDownTimer? = null
    private var timerRunning = false
    private var timeLeftInMillis: Long = 0
    private var focusLengthInMillis: Long = 0

    private var isFocusSession = true
    private var focusSessionsCompleted = 0

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        createNotificationChannel()
        requestNotificationPermission()

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        binding.startPauseButton.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.resetButton.setOnClickListener {
            resetTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra("startBreak")) {
            isFocusSession = false
            loadTimerSettings()
            resetTimer()
            startTimer()
            intent.removeExtra("startBreak") // Clear the extra
        } else if (intent.hasExtra("startFocus")) {
            isFocusSession = true
            loadTimerSettings()
            resetTimer()
            startTimer()
            intent.removeExtra("startFocus") // Clear the extra
        } else {
            loadTimerSettings()
            updateTimerText()
        }
    }

    private fun loadTimerSettings() {
        val sharedPreferences = getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        val focusLength = sharedPreferences.getLong("focusLength", 25)
        focusLengthInMillis = focusLength * 60 * 1000
        val shortBreakLength = sharedPreferences.getLong("shortBreakLength", 5)
        val longBreakLength = sharedPreferences.getLong("longBreakLength", 15)

        if (!timerRunning) {
            timeLeftInMillis = if (isFocusSession) {
                focusLengthInMillis
            } else {
                if ((focusSessionsCompleted % 4) == 0 && focusSessionsCompleted != 0) {
                    longBreakLength * 60 * 1000
                } else {
                    shortBreakLength * 60 * 1000
                }
            }
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerRunning = false
                binding.startPauseButton.text = "Start"
                if (isFocusSession) {
                    focusSessionsCompleted++
                    updateStats(focusLengthInMillis)
                    isFocusSession = false
                    showBreakNotification()
                    showQuote()
                } else {
                    isFocusSession = true
                    showFocusNotification()
                }
                loadTimerSettings()
                updateTimerText()
                binding.startPauseButton.text = if(isFocusSession) "Start Focus" else "Start Break"
            }
        }.start()

        timerRunning = true
        binding.startPauseButton.text = "Pause"
    }

    private fun pauseTimer() {
        timer?.cancel()
        timerRunning = false
        binding.startPauseButton.text = "Start"
    }

    private fun resetTimer() {
        timer?.cancel()
        timerRunning = false
        loadTimerSettings()
        updateTimerText()
        binding.startPauseButton.text = if (isFocusSession) "Start Focus" else "Start Break"
        binding.progressIndicator.progress = 100
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        binding.timerTextView.text = timeFormatted

        val sharedPreferences = getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        val totalTime = if (isFocusSession) {
            sharedPreferences.getLong("focusLength", 25) * 60 * 1000
        } else {
            if ((focusSessionsCompleted % 4) == 0 && focusSessionsCompleted != 0) {
                sharedPreferences.getLong("longBreakLength", 15) * 60 * 1000
            } else {
                sharedPreferences.getLong("shortBreakLength", 5) * 60 * 1000
            }
        }
        if (totalTime > 0) {
            binding.progressIndicator.progress = (timeLeftInMillis * 100 / totalTime).toInt()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FocusTimer Channel"
            val descriptionText = "Channel for FocusTimer notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("FOCUS_TIMER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showBreakNotification() {
        val intent = Intent(this, TimerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("startBreak", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val sharedPreferences = getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        val breakLength = if ((focusSessionsCompleted % 4) == 0 && focusSessionsCompleted != 0) {
            sharedPreferences.getLong("longBreakLength", 15)
        } else {
            sharedPreferences.getLong("shortBreakLength", 5)
        }

        val builder = NotificationCompat.Builder(this, "FOCUS_TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Session Complete!")
            .setContentText("Time for a ${breakLength}-minute break.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                notify(1, builder.build())
            }
        }
    }

    private fun showFocusNotification() {
        val intent = Intent(this, TimerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("startFocus", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, "FOCUS_TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Break's Over!")
            .setContentText("Time to get back to focus.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                notify(2, builder.build())
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showQuote() {
        FetchQuoteTask().execute()
    }

    private fun showQuoteDialog(quote: String, author: String) {
        AlertDialog.Builder(this)
            .setTitle("Quote of the Session")
            .setMessage("\"$quote\" - $author")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateStats(focusTime: Long) {
        val user = auth.currentUser
        if (user != null) {
            val userDocRef = db.collection("users").document(user.uid)
            userDocRef.update("totalFocusTime", FieldValue.increment(focusTime / 1000))
            userDocRef.update("sessionsCompleted", FieldValue.increment(1))
                .addOnSuccessListener { 
                    Log.d("TimerActivity", "Stats updated successfully!") 
                }
                .addOnFailureListener { e -> 
                    Log.w("TimerActivity", "Error updating stats", e) 
                }
        }
    }

    private inner class FetchQuoteTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String? {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL("https://zenquotes.io/api/random")
                urlConnection = url.openConnection() as HttpURLConnection
                val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                return stringBuilder.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                urlConnection?.disconnect()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            if (result != null) {
                try {
                    val jsonArray = JSONArray(result)
                    val jsonObject = jsonArray.getJSONObject(0)
                    val quote = jsonObject.getString("q")
                    val author = jsonObject.getString("a")
                    showQuoteDialog(quote, author)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
