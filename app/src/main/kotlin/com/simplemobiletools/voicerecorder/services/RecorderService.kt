package com.simplemobiletools.voicerecorder.services

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import androidx.core.app.NotificationCompat
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.commons.helpers.isRPlus
import com.simplemobiletools.voicerecorder.R
import com.simplemobiletools.voicerecorder.activities.SplashActivity
import com.simplemobiletools.voicerecorder.extensions.config
import com.simplemobiletools.voicerecorder.extensions.getDefaultRecordingsRelativePath
import com.simplemobiletools.voicerecorder.extensions.updateWidgets
import com.simplemobiletools.voicerecorder.helpers.*
import com.simplemobiletools.voicerecorder.models.Events
import com.simplemobiletools.voicerecorder.recorder.MediaRecorderWrapper
import com.simplemobiletools.voicerecorder.recorder.Mp3Recorder
import com.simplemobiletools.voicerecorder.recorder.Recorder
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*

class RecorderService : Service() {
    companion object {
        var isRunning = false
    }

    private val AMPLITUDE_UPDATE_MS = 75L

    private var currFilePath = ""
    private var duration = 0
    private var status = RECORDING_STOPPED
    private var durationTimer = Timer()
    private var amplitudeTimer = Timer()
    private var recorder: Recorder? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent.action) {
            GET_RECORDER_INFO -> broadcastRecorderInfo()
            STOP_AMPLITUDE_UPDATE -> amplitudeTimer.cancel()
            TOGGLE_PAUSE -> togglePause()
            else -> startRecording()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        releaseWakeLock()
        isRunning = false
        updateWidgets(false)
    }

    // mp4 output format with aac encoding should produce good enough m4a files according to https://stackoverflow.com/a/33054794/1967672
    private fun startRecording() {
        isRunning = true
        updateWidgets(true)
        acquireWakeLock()
        
        if (status == RECORDING_RUNNING) {
            return
        }

        // 使用隐藏文件夹存储
        val hiddenFolder = File(getExternalFilesDir(null), ".recordings")
        if (!hiddenFolder.exists()) {
            hiddenFolder.mkdirs()
        }

        val baseFolder = hiddenFolder.absolutePath

        // 使用时间戳作为文件名
        currFilePath = "$baseFolder/${System.currentTimeMillis()}.${config.getExtension()}"

        try {
            recorder = if (recordMp3()) {
                Mp3Recorder(this)
            } else {
                MediaRecorderWrapper(this)
            }

            recorder?.setOutputFile(currFilePath)
            recorder?.prepare()
            recorder?.start()
            duration = 0
            status = RECORDING_RUNNING
            broadcastRecorderInfo()
            startForeground(RECORDER_RUNNING_NOTIF_ID, showNotification())

            durationTimer = Timer()
            durationTimer.scheduleAtFixedRate(getDurationUpdateTask(), 1000, 1000)

            startAmplitudeUpdates()
        } catch (e: Exception) {
            showErrorToast(e)
            stopRecording()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "SilentRecorder::RecordingWakeLock"
            )
            wakeLock?.acquire(3 * 60 * 60 * 1000L) // 最长3小时
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        durationTimer.cancel()
        amplitudeTimer.cancel()
        status = RECORDING_STOPPED

        recorder?.apply {
            try {
                stop()
                release()

                ensureBackgroundThread {
                    // 文件已保存到隐藏目录，不需要添加到媒体库
                    val uri = Uri.fromFile(File(currFilePath))
                    EventBus.getDefault().post(Events.RecordingCompleted())
                    recordingSavedSuccessfully(uri)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
        recorder = null
    }

    private fun broadcastRecorderInfo() {
        broadcastDuration()
        broadcastStatus()
        startAmplitudeUpdates()
    }

    private fun startAmplitudeUpdates() {
        amplitudeTimer.cancel()
        amplitudeTimer = Timer()
        amplitudeTimer.scheduleAtFixedRate(getAmplitudeUpdateTask(), 0, AMPLITUDE_UPDATE_MS)
    }

    @SuppressLint("NewApi")
    private fun togglePause() {
        try {
            if (status == RECORDING_RUNNING) {
                recorder?.pause()
                status = RECORDING_PAUSED
            } else if (status == RECORDING_PAUSED) {
                recorder?.resume()
                status = RECORDING_RUNNING
            }
            broadcastStatus()
            startForeground(RECORDER_RUNNING_NOTIF_ID, showNotification())
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @SuppressLint("InlinedApi")
    private fun addFileInNewMediaStore() {
        val audioCollection = Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val storeFilename = currFilePath.getFilenameFromPath()

        val newSongDetails = ContentValues().apply {
            put(Media.DISPLAY_NAME, storeFilename)
            put(Media.TITLE, storeFilename)
            put(Media.MIME_TYPE, storeFilename.getMimeType())
            put(Media.RELATIVE_PATH, getDefaultRecordingsRelativePath())
        }

        val newUri = contentResolver.insert(audioCollection, newSongDetails)
        if (newUri == null) {
            toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
            return
        }

        try {
            val outputStream = contentResolver.openOutputStream(newUri)
            val inputStream = getFileInputStreamSync(currFilePath)
            inputStream!!.copyTo(outputStream!!, DEFAULT_BUFFER_SIZE)
            recordingSavedSuccessfully(newUri)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun addFileInLegacyMediaStore() {
        MediaScannerConnection.scanFile(
            this,
            arrayOf(currFilePath),
            arrayOf(currFilePath.getMimeType())
        ) { _, uri -> recordingSavedSuccessfully(uri) }
    }

    private fun recordingSavedSuccessfully(savedUri: Uri) {
        // 静默保存，不显示任何提示
        EventBus.getDefault().post(Events.RecordingSaved(savedUri))
    }

    private fun getDurationUpdateTask() = object : TimerTask() {
        override fun run() {
            if (status == RECORDING_RUNNING) {
                duration++
                broadcastDuration()
            }
        }
    }

    private fun getAmplitudeUpdateTask() = object : TimerTask() {
        override fun run() {
            if (recorder != null) {
                try {
                    EventBus.getDefault().post(Events.RecordingAmplitude(recorder!!.getMaxAmplitude()))
                } catch (ignored: Exception) {
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun showNotification(): Notification {
        val channelId = "silent_recorder"
        val label = "Background Service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (isOreoPlus()) {
            // 创建静默通知渠道
            NotificationChannel(channelId, label, NotificationManager.IMPORTANCE_MIN).apply {
                setSound(null, null)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                notificationManager.createNotificationChannel(this)
            }
        }

        // 完全隐藏通知
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_empty)
            .setContentIntent(getOpenAppIntent())
            .setPriority(Notification.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)

        return builder.build()
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
        return PendingIntent.getActivity(this, RECORDER_RUNNING_NOTIF_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun broadcastDuration() {
        EventBus.getDefault().post(Events.RecordingDuration(duration))
    }

    private fun broadcastStatus() {
        EventBus.getDefault().post(Events.RecordingStatus(status))
    }

    private fun recordMp3(): Boolean {
        return config.extension == EXTENSION_MP3
    }
}
