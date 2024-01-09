package com.recording.echo

import android.app.AlertDialog
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Dialog
import android.os.IBinder
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.recording.echo.Activities.SettingsActivity
import com.recording.echo.Notifications.RecordingForegroundService
import com.recording.echo.Notifications.RecordingForegroundServiceBinder
import com.recording.echo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 123
    private val REQUEST_PERMISSION_CODE = 124
    private lateinit var SaveDuration: Button
    private lateinit var binding: ActivityMainBinding


    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == "com.recording.echo.REQUEST_PERMISSIONS") {
                    val permissions = it.getStringArrayExtra("permissions")
                    permissions?.let { perms ->
                        requestPermissions(perms, REQUEST_PERMISSION_CODE)
                    }
                }
            }
        }
    }

    private lateinit var recordingService: RecordingForegroundService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val recordingBinder = service as? RecordingForegroundServiceBinder
            recordingBinder?.let {
                recordingService = it.getServiceInstance() ?: return@let // Assign nullable service
                it.setServiceInstance(recordingService) // Set the service if it's not null

                val recordingDuration = recordingService.getRecordingDuration()

            }
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            // Handle disconnection if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction("com.recording.echo.REQUEST_PERMISSIONS")

        }

        registerReceiver(permissionReceiver, filter)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button5Minutes.setOnClickListener {
            showDurationSelectionDialog(10 * 60 * 1000)
        }

        binding.button60Minutes.setOnClickListener {
            showDurationSelectionDialog(6 * 60 * 10000)
        }

        binding.button24Minutes.setOnClickListener {
            showDurationSelectionDialog(24 * 60 * 60 * 1000)
        }

        binding.settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        bindRecordingService()
        requestPermissions()
        startRecordingService()
    }


    override fun onStart() {
        super.onStart()
        bindRecordingService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun bindRecordingService() {
        val intent = Intent(this, RecordingForegroundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unbind the service to avoid leaks
        unbindService(connection)
        unregisterReceiver(permissionReceiver)
    }

    private fun showDurationSelectionDialog(initialDurationMillis: Long) {

        val dialog = Dialog(this)
        val dialogLayout = LinearLayout(this)
        dialogLayout.orientation = LinearLayout.VERTICAL

        val seekBar = SeekBar(this)
        val totalSeconds = initialDurationMillis / 1000

        if (totalSeconds < 1) {
            seekBar.max = 1
        } else {
            seekBar.max = totalSeconds.toInt()
        }

        val seekBarLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        seekBar.layoutParams = seekBarLayoutParams

        dialogLayout.addView(seekBar)

        val timeTextView = TextView(this)
        timeTextView.text = "00:00"
        timeTextView.gravity = Gravity.CENTER

        val timeTextLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeTextView.layoutParams = timeTextLayoutParams

        dialogLayout.addView(timeTextView)

        val confirmButton = Button(this)
        confirmButton.text = "Confirm"

        val confirmButtonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        confirmButton.layoutParams = confirmButtonLayoutParams

        confirmButton.setOnClickListener {

            val selectedSeconds = seekBar.progress
            val progressSeconds = (selectedSeconds * totalSeconds) / seekBar.max

            val selectedDurationMillis = progressSeconds * 1000
            val recordingDurationMillis = calculateRecordingDuration()

            if (selectedDurationMillis > recordingDurationMillis) {

                val toast = Toast.makeText(
                    this,
                    "Selected time exceeds recording!",
                    Toast.LENGTH_SHORT
                )
                toast.show()

            } else {

                val currentTimeMillis = System.currentTimeMillis()
                val startTimeMillis = currentTimeMillis - selectedDurationMillis
                val remainingTimeMillis = recordingDurationMillis - selectedDurationMillis
                val endTimeMillis = currentTimeMillis - remainingTimeMillis

                passTimesToService(startTimeMillis, endTimeMillis)
                dialog.dismiss() // Dismiss dialog for valid time selection

                Toast.makeText(
                    this@MainActivity,
                    "Time Duration Successfully Saved to Recordings",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

        dialogLayout.addView(confirmButton)

        dialog.setContentView(dialogLayout)

        val dialogWidthInDp = 250
        val density = resources.displayMetrics.density
        val dialogWidthInPixels = (dialogWidthInDp * density).toInt()

        dialog.window?.setLayout(dialogWidthInPixels, WindowManager.LayoutParams.WRAP_CONTENT)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                val seconds = progress
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val remainingSeconds = seconds % 60

                val timeString = String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
                timeTextView.text = timeString
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        dialog.show()
    }


    private fun calculateRecordingDuration(): Long {
        return if (::recordingService.isInitialized) {
            val recordingDuration = recordingService.getRecordingDuration()
            recordingDuration
        } else {
            // Handle the case where recordingService is not initialized
            0L // Return a default value or handle the case as needed
        }
    }

    private fun passTimesToService(startTime: Long, endTime: Long) {
        val serviceIntent = Intent(this, RecordingForegroundService::class.java)
        serviceIntent.action = "PASS_TIMES_ACTION"
        serviceIntent.putExtra("START_TIME", startTime)
        serviceIntent.putExtra("END_TIME", endTime)
        startService(serviceIntent)
    }


    private fun startRecordingService() {
        // Check if permissions already granted
        if (hasRequiredPermissions()) {
            // Start service if permissions granted
            val serviceIntent = Intent(this, RecordingForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            // Request permissions
            requestPermissions()
        }

    }

    private fun hasRequiredPermissions(): Boolean {
        return hasRecordAudioPermission() && hasStoragePermission()
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasRecordAudioPermission()) {
            permissionsToRequest.add(RECORD_AUDIO)
        }
        if (!hasStoragePermission()) {
            permissionsToRequest.add(WRITE_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {

            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, can start service
                startRecordingService()

            } else {
                // Permissions denied, handle this
                showPermissionDeniedDialog()
            }

        }

    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Echo requires your permission to function. Please grant the audio and storage permission in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                redirectToAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Handle cancellation or guide the user accordingly
                startRecordingService()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun redirectToAppSettings() {
        // Redirect the user to the app settings to manually enable the permission
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

}

