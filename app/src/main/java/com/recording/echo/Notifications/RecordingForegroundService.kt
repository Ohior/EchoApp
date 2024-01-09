package com.recording.echo.Notifications


import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.recording.echo.MainActivity
import com.recording.echo.R
import java.io.*

class RecordingForegroundService : Service() {

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private var filePath: String = ""
    private var startTimeRecording = 0L
    private var endTimeRecording = 0L
    private val binder = RecordingForegroundServiceBinder()


    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "PASS_TIMES_ACTION" -> {
                    val startTime = intent.getLongExtra("START_TIME", 0L)
                    val endTime = intent.getLongExtra("END_TIME", 0L)
                    val inputFilePath = getEchoRecordingFile() // Get the input (original) file path

                    val trimmedFile = getTrimmedFilePath(startTime, endTime)
                    val outputFilePath = trimmedFile.absolutePath // Generate the output (trimmed) file path

                    trimRecording(startTime, endTime, inputFilePath, outputFilePath)
                    // Process the received start and end times here
                    // For example:
                    // Save these times or perform any other necessary tasks with startTime and endTime
                }
                "STOP_RECORDING" -> {
                    stopRecording() // Implement this method to stop the recording
                    stopSelf() // Stop the service
                }

                else -> {
                    handleRecordingIntent()
                }
            }
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleRecordingIntent() {
        // Start foreground service notification
        startForeground(1, createNotification())
        // Start recording if necessary
        startRecording()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording() {
        if (!hasRequiredPermissions()) {
            // Maybe request permissions again
            return
        } else {
            filePath = getEchoRecordingFile()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePath)

                try {
                    prepare()
                    startTimeRecording  = System.currentTimeMillis()
                    start()
                    isRecording = true


                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@RecordingForegroundService,
                        "Recording Failed",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(
                        "Recording Error",
                        "IOException: ${e.message}",
                        e
                    ) // Log exception with stack trace
                    Toast.makeText(
                        this@RecordingForegroundService,
                        "Recording failed2: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Handle the IOException appropriately, considering the specific details in the log
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    Log.e("Recording Error", "RunTime: ${e.message}")
                    Toast.makeText(
                        this@RecordingForegroundService,
                        "Recording failed3: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Handle other RuntimeExceptions
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val recordAudioPermission = checkSelfPermission(RECORD_AUDIO)
        val storagePermission = checkSelfPermission(WRITE_EXTERNAL_STORAGE)

        return recordAudioPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelId = createNotificationChannel()

        // Create intent to open the app's main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Set flags here
        )

        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = "STOP_RECORDING"
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Set flags here
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Echo Service")
            .setContentText("Recording in progress")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setContentIntent(pendingIntent)
        //    .addAction(R.drawable.ic_stop_icon, "STOP ECHO", stopPendingIntent)

        return notificationBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "RecordingServiceChannel"
        val channel = NotificationChannel(
            channelId,
            "Recording Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }


    fun trimRecording(startMillis: Long, endMillis: Long, inputFilePath: String, outputFilePath: String) {
        if (startMillis >= endMillis) {
            Log.e("Trimming", "Invalid start and end times: $startMillis, $endMillis")
            return
        }

        val inputFile = File(inputFilePath)
        if (!inputFile.exists() || !inputFile.canRead()) {
            Log.e("Trimming", "Input file doesn't exist or can't be read: $inputFilePath")
            return
        }

        val outputFile = File(outputFilePath)
        if (outputFile.exists()) {
            Log.e("Trimming", "Output file already exists: $outputFilePath")
            return
        }

        val sampleRate = 44100 // Replace with actual sample rate
        val channels = 2 // Replace with actual number of channels
        val bitsPerSample = 16 // Replace with actual bits per sample

        val bytesPerSecond = sampleRate * channels * bitsPerSample / 8 // Calculate bytes per second
        val bytesPerMillisecond = bytesPerSecond / 1000 // Calculate bytes per millisecond

        val startTimeBytes = startMillis * bytesPerMillisecond
        val endTimeBytes = endMillis * bytesPerMillisecond

        val inputStream = RandomAccessFile(inputFile, "r")
        val outputStream = FileOutputStream(outputFile)

        try {
            inputStream.use { input ->
                outputStream.use { output ->
                    input.seek(startTimeBytes)
                    val buffer = ByteArray(1024)
                    var totalBytesRead: Long = 0
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } > 0 && totalBytesRead < endTimeBytes) {
                        totalBytesRead += if (totalBytesRead + bytesRead > endTimeBytes) {
                            val trimLength = (endTimeBytes - totalBytesRead).toInt()
                            output.write(buffer, 0, trimLength)
                            trimLength
                        } else {
                            output.write(buffer, 0, bytesRead)
                            bytesRead
                        }
                    }
                    println("Total Bytes Read: $totalBytesRead")
                }
            }
            println("Trimmed file size: ${outputFile.length()} bytes")
        } catch (e: IOException) {
            // Handle the IOException while trimming the file
            println("Error while trimming the file: ${e.message}")
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }

    fun getRecordingDuration(): Long {
        return if (isRecording) {
            // If recording is ongoing, calculate duration till current time
            System.currentTimeMillis() - startTimeRecording
        } else {
            // If recording has stopped, return the duration calculated during stopRecording()
            endTimeRecording - startTimeRecording // Assuming endTimeRecording is a class-level variable
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getEchoRecordingFile(): String {

        val audioFileName = "echo.wav"
        val echoRecordsDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS),
            "Echo_Recordings"
        )

        try {
            if (!echoRecordsDirectory.exists()) {
                echoRecordsDirectory.mkdirs()
            }

            val outputFile = File(echoRecordsDirectory, audioFileName)

            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            return outputFile.absolutePath

        } catch (e: IOException) {
            // Handle exception
            return ""
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getTrimmedFilePath(startMillis: Long, endMillis: Long): File {
        val inputFilePath = getEchoRecordingFile() // Get the input (original) file path
        val inputFile = File(inputFilePath)
        val directory = inputFile.parentFile // Use the parent directory of the original file

        val fileExtension = ".wav" // Assuming the trimmed file has the same extension as the ongoing recording

        val currentTimeMillis = System.currentTimeMillis() // Get current time in milliseconds
        val durationSeconds = (endMillis - startMillis) / 1000 // Calculate duration in seconds

        val trimmedFileName = "echo_time${currentTimeMillis}_$durationSeconds$fileExtension"
        return File(directory, trimmedFileName)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun stopRecording() {
        if (isRecording) {
            val endTimeRecording = System.currentTimeMillis()
            endTimeRecording - startTimeRecording
            mediaRecorder.stop()
            mediaRecorder.release()
            isRecording = false
        }
    }
}
