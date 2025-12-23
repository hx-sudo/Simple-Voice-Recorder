package com.simplemobiletools.voicerecorder.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.voicerecorder.BuildConfig
import com.simplemobiletools.voicerecorder.R
import com.simplemobiletools.voicerecorder.extensions.config
import com.simplemobiletools.voicerecorder.helpers.*
import com.simplemobiletools.voicerecorder.models.Events
import com.simplemobiletools.voicerecorder.services.RecorderService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var bus: EventBus? = null
    private var status = RECORDING_STOPPED
    private lateinit var toggleButton: FloatingActionButton
    private lateinit var durationText: TextView
    private lateinit var statusText: TextView
    private lateinit var recordingIndicator: View

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_simple)
        
        toggleButton = findViewById(R.id.toggle_recording_button)
        durationText = findViewById(R.id.recording_duration)
        statusText = findViewById(R.id.status_text)
        recordingIndicator = findViewById(R.id.recording_indicator)

        // 请求忽略电池优化
        requestBatteryOptimizationExemption()

        handlePermission(PERMISSION_RECORD_AUDIO) {
            if (it) {
                initRecorder()
            } else {
                toast(com.simplemobiletools.commons.R.string.no_audio_permissions)
                finish()
            }
        }
    }

    private fun initRecorder() {
        bus = EventBus.getDefault()
        bus!!.register(this)

        toggleButton.setOnClickListener {
            toggleRecording()
        }

        // 检查是否有正在运行的录音
        if (RecorderService.isRunning) {
            status = RECORDING_RUNNING
            updateUI()
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            // 忽略异常，某些设备可能不支持
        }
    }

    private fun toggleRecording() {
        if (status == RECORDING_STOPPED) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        Intent(this, RecorderService::class.java).apply {
            startService(this)
        }
        status = RECORDING_RUNNING
        updateUI()
    }

    private fun stopRecording() {
        Intent(this, RecorderService::class.java).apply {
            stopService(this)
        }
        status = RECORDING_STOPPED
        updateUI()
    }

    private fun updateUI() {
        when (status) {
            RECORDING_RUNNING -> {
                toggleButton.setImageResource(com.simplemobiletools.commons.R.drawable.ic_stop_vector)
                durationText.visibility = View.VISIBLE
                recordingIndicator.visibility = View.VISIBLE
                statusText.text = getString(R.string.recording)
            }
            else -> {
                toggleButton.setImageResource(com.simplemobiletools.commons.R.drawable.ic_microphone_vector)
                durationText.visibility = View.GONE
                recordingIndicator.visibility = View.GONE
                statusText.text = getString(R.string.ready_to_record)
                durationText.text = "00:00:00"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun gotDurationEvent(event: Events.RecordingDuration) {
        durationText.text = event.duration.getFormattedDuration()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun gotStatusEvent(event: Events.RecordingStatus) {
        status = event.status
        updateUI()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun recordingCompleted(event: Events.RecordingCompleted) {
        // 静默完成，不显示任何提示
    }
}
