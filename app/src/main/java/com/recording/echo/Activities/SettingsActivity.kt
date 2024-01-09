package com.recording.echo.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.recording.echo.MainActivity
import com.recording.echo.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupBufferDuration()
        setupRecordingQuality()
        saveSettings()
        setDefaultSettings()
    }

    private fun setupBufferDuration() {
        val radioGroupBufferDuration: RadioGroup = findViewById(R.id.radioGroupBufferDuration)
        radioGroupBufferDuration.setOnCheckedChangeListener { _, checkedId ->
            val sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val duration = when (checkedId) {
                R.id.radioButton0Min -> 0
                R.id.radioButton1Min -> 1
                R.id.radioButton5Min -> 5
                else -> 0
            }

            editor.putInt("buffer_duration", duration)
            editor.apply()
        }
    }

    private fun setDefaultSettings() {
        val sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (!sharedPreferences.contains("buffer_duration")) {
            val defaultBufferDuration = 5 // Set your default buffer duration here
            editor.putInt("buffer_duration", defaultBufferDuration)
        }

        if (!sharedPreferences.contains("recording_quality")) {
            val defaultRecordingQuality = "Medium" // Set your default recording quality here
            editor.putString("recording_quality", defaultRecordingQuality)
        }

        editor.apply()
    }

    private fun setupRecordingQuality() {
        val spinnerRecordingQuality: Spinner = findViewById(R.id.spinnerRecordingQuality)
        val recordingQualityOptions = arrayOf("High", "Medium", "Low")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, recordingQualityOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecordingQuality.adapter = adapter

        spinnerRecordingQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val selectedQuality = recordingQualityOptions[position]
                editor.putString("recording_quality", selectedQuality)
                editor.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun saveSettings() {
        val buttonSaveSettings: Button = findViewById(R.id.buttonSaveSettings)
        buttonSaveSettings.setOnClickListener {
            // Placeholder for additional saving logic if needed
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()

            // Transition to EchoActivity after saving settings
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}
