package com.simplemobiletools.voicerecorder.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simplemobiletools.commons.extensions.getFormattedDuration
import com.simplemobiletools.voicerecorder.R
import com.simplemobiletools.voicerecorder.helpers.*
import com.simplemobiletools.voicerecorder.models.Events
import com.simplemobiletools.voicerecorder.services.RecorderService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class MainActivity : AppCompatActivity() {

    private var bus: EventBus? = null
    private var status = RECORDING_STOPPED
    private lateinit var toggleButton: FloatingActionButton
    private lateinit var durationText: TextView
    private lateinit var statusText: TextView
    private lateinit var recordingIndicator: View
    private lateinit var viewFilesButton: TextView
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_simple)
        
        toggleButton = findViewById(R.id.toggle_recording_button)
        durationText = findViewById(R.id.recording_duration)
        statusText = findViewById(R.id.status_text)
        recordingIndicator = findViewById(R.id.recording_indicator)
        viewFilesButton = findViewById(R.id.view_files_button)

        // 请求忽略电池优化
        requestBatteryOptimizationExemption()

        // 请求录音权限
        requestAudioPermission()
    }
    
    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            initRecorder()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRecorder()
            } else {
                Toast.makeText(this, "需要录音权限才能使用", Toast.LENGTH_LONG).show()
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

        viewFilesButton.setOnClickListener {
            showRecordingsDialog()
        }

        // 检查是否有正在运行的录音
        if (RecorderService.isRunning) {
            status = RECORDING_RUNNING
            updateUI()
        }
    }
    
    private fun showRecordingsDialog() {
        // 使用与RecorderService相同的路径
        val recordingsFolder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "Recordings")
        } else {
            File(android.os.Environment.getExternalStorageDirectory(), "Recordings")
        }
        val files = recordingsFolder.listFiles()?.filter { it.isFile && it.extension in listOf("m4a", "mp3", "ogg") }?.sortedByDescending { it.lastModified() }
        
        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "暂无录音文件", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileNames = files.map { 
            val sizeMB = String.format("%.2f", it.length() / 1024.0 / 1024.0)
            "${it.name} (${sizeMB} MB)"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("录音文件 (${files.size} 个)")
            .setItems(fileNames) { _, which ->
                openFile(files[which])
            }
            .setPositiveButton("打开文件夹") { _, _ ->
                openFolder(recordingsFolder)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "选择播放器"))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openFolder(folder: File) {
        try {
            // 方法1：尝试使用 FileProvider URI（适用于应用私有目录）
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                folder
            )
            
            // 尝试多种 Intent 方式打开文件管理器
            val intents = mutableListOf<Intent>()
            
            // 方式1：使用 ACTION_VIEW
            intents.add(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            
            // 方式2：使用 file:// URI（如果 FileProvider 失败）
            try {
                val fileUri = Uri.parse("file://${folder.absolutePath}")
                intents.add(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "resource/folder")
                })
            } catch (e: Exception) {
                // 忽略
            }
            
            // 方式3：尝试打开父目录（更通用）
            val parentFolder = folder.parentFile
            if (parentFolder != null && parentFolder.exists()) {
                try {
                    val parentUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        parentFolder
                    )
                    intents.add(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(parentUri, "resource/folder")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (e: Exception) {
                    // 忽略
                }
            }
            
            // 尝试启动 Intent
            var success = false
            for (intent in intents) {
                try {
                    startActivity(Intent.createChooser(intent, "选择文件管理器"))
                    success = true
                    break
                } catch (e: Exception) {
                    // 继续尝试下一个
                }
            }
            
            if (!success) {
                throw Exception("无法打开文件管理器")
            }
        } catch (e: Exception) {
            // 如果所有方法都失败，显示路径供用户手动访问
            val path = folder.absolutePath
            AlertDialog.Builder(this)
                .setTitle("录音文件位置")
                .setMessage("路径：\n$path\n\n请使用文件管理器手动访问此路径")
                .setPositiveButton("复制路径") { _, _ ->
                    // 尝试复制路径到剪贴板
                    try {
                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("路径", path)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "路径已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "无法复制路径", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("确定", null)
                .show()
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
                toggleButton.setImageResource(R.drawable.ic_record_button)
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
