package com.mashinist.archiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var createArchiveBtn: Button
    private lateinit var extractArchiveBtn: Button
    private lateinit var backBtn: Button
    private lateinit var pathText: TextView
    private lateinit var archiver: MashinistArchiver
    private lateinit var fileAdapter: FileAdapter
    
    private var currentPath = "/storage/emulated/0"
    private val STORAGE_PERMISSION_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            logToFile("App started")
            setContentView(R.layout.activity_main)
            logToFile("ContentView set")
            
            archiver = MashinistArchiver()
            
            recyclerView = findViewById(R.id.recyclerView)
            createArchiveBtn = findViewById(R.id.createArchiveBtn)
            extractArchiveBtn = findViewById(R.id.extractArchiveBtn)
            backBtn = findViewById(R.id.backBtn)
            pathText = findViewById(R.id.pathText)
            
            logToFile("Views initialized")
            
            recyclerView.layoutManager = LinearLayoutManager(this)
            
            backBtn.setOnClickListener {
                val parentDir = File(currentPath).parentFile
                if (parentDir != null && parentDir.canRead()) {
                    loadFiles(parentDir.absolutePath)
                }
            }
            
            createArchiveBtn.setOnClickListener {
                val selectedFiles = fileAdapter.getSelectedFiles()
                if (selectedFiles.isEmpty()) {
                    Toast.makeText(this, "Выберите файлы для архивации", Toast.LENGTH_SHORT).show()
                } else {
                    showCreateArchiveDialog(selectedFiles)
                }
            }
            
            extractArchiveBtn.setOnClickListener {
                showExtractArchiveDialog()
            }
            
            logToFile("Showing permission dialog")
            showPermissionDialog()
            
        } catch (e: Exception) {
            logToFile("CRASH: ${e.message}")
            logToFile("Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun logToFile(message: String) {
        try {
            val logDir = File(getExternalFilesDir(null), "logs")
            if (!logDir.exists()) logDir.mkdirs()
            
            val logFile = File(logDir, "app.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            FileWriter(logFile, true).use { writer ->
                writer.append("[$timestamp] $message\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showPermissionDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_permission, null)
            
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val continueBtn = dialogView.findViewById<Button>(R.id.continueBtn)
            
            continueBtn.setOnClickListener {
                dialog.dismiss()
                checkPermissions()
            }
            
            dialog.show()
            logToFile("Permission dialog shown")
        } catch (e: Exception) {
            logToFile("Error showing permission dialog: ${e.message}")
        }
    }
    
    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } else {
                loadFiles(currentPath)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, 
                           Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE)
            } else {
                loadFiles(currentPath)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadFiles(currentPath)
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, 
                                           grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(currentPath)
            }
        }
    }
    
    private fun loadFiles(path: String) {
        try {
            logToFile("Loading files from: $path")
            val dir = File(path)
            val files = dir.listFiles()?.toList() ?: emptyList()
            currentPath = path
            pathText.text = path
            
            backBtn.isEnabled = File(path).parentFile != null
            
            fileAdapter = FileAdapter(files) { file ->
                if (file.isDirectory) {
                    loadFiles(file.absolutePath)
                }
            }
            
            recyclerView.adapter = fileAdapter
            logToFile("Files loaded: ${files.size}")
        } catch (e: Exception) {
            logToFile("Error loading files: ${e.message}")
        }
    }
    
    private fun showCreateArchiveDialog(selectedFiles: List<File>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_archive, null)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val archiveNameEdit = dialogView.findViewById<EditText>(R.id.archiveNameEdit)
        val selectedFilesText = dialogView.findViewById<TextView>(R.id.selectedFilesText)
        val radioMashinist = dialogView.findViewById<RadioButton>(R.id.radioMashinist)
        val radioTep70bs = dialogView.findViewById<RadioButton>(R.id.radioTep70bs)
        val radioTep = dialogView.findViewById<RadioButton>(R.id.radioTep)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val createBtn = dialogView.findViewById<Button>(R.id.createBtn)
        
        selectedFilesText.text = "Выбрано файлов: ${selectedFiles.size}"
        
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        
        createBtn.setOnClickListener {
            val fileName = archiveNameEdit.text.toString()
            
            if (fileName.isEmpty()) {
                Toast.makeText(this, "Введите имя архива", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val format = when {
                radioTep70bs.isChecked -> "tep70bs"
                radioTep.isChecked -> "tep"
                else -> "mashinist"
            }
            
            val outputPath = "$currentPath/$fileName.$format"
            
            try {
                if (selectedFiles.isNotEmpty()) {
                    logToFile("Creating archive: $outputPath")
                    archiver.createArchive(selectedFiles[0].absolutePath, outputPath, format)
                    logToFile("Archive created successfully")
                    Toast.makeText(this, "Архив создан: $outputPath", Toast.LENGTH_SHORT).show()
                    loadFiles(currentPath)
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                logToFile("Error creating archive: ${e.message}")
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showExtractArchiveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_extract_archive, null)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val outputPathEdit = dialogView.findViewById<EditText>(R.id.outputPathEdit)
        val cancelExtractBtn = dialogView.findViewById<Button>(R.id.cancelExtractBtn)
        val extractBtn = dialogView.findViewById<Button>(R.id.extractBtn)
        
        cancelExtractBtn.setOnClickListener {
            dialog.dismiss()
        }
        
        extractBtn.setOnClickListener {
            val archivePath = outputPathEdit.text.toString()
            if (archivePath.isNotEmpty()) {
                try {
                    val outputDir = currentPath + "/extracted"
                    logToFile("Extracting archive: $archivePath")
                    archiver.extractArchive(archivePath, outputDir)
                    logToFile("Archive extracted successfully")
                    Toast.makeText(this, "Распаковано в: $outputDir", Toast.LENGTH_SHORT).show()
                    loadFiles(currentPath)
                    dialog.dismiss()
                } catch (e: Exception) {
                    logToFile("Error extracting archive: ${e.message}")
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        dialog.show()
    }
}
