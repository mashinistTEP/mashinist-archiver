package com.mashinist.archiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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
    private lateinit var closeSelectionBtn: Button
    private lateinit var pathText: TextView
    private lateinit var confirmSelectionBtn: Button
    private lateinit var buttonLayout: LinearLayout
    private lateinit var archiver: MashinistArchiver
    private lateinit var fileAdapter: FileAdapter
    
    private var currentPath = "/storage/emulated/0"
    private var selectionMode = false
    private var selectionType = ""
    private val STORAGE_PERMISSION_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            logToFile("App started")
            setContentView(R.layout.activity_main)
            
            archiver = MashinistArchiver()
            
            recyclerView = findViewById(R.id.recyclerView)
            createArchiveBtn = findViewById(R.id.createArchiveBtn)
            extractArchiveBtn = findViewById(R.id.extractArchiveBtn)
            backBtn = findViewById(R.id.backBtn)
            closeSelectionBtn = findViewById(R.id.closeSelectionBtn)
            pathText = findViewById(R.id.pathText)
            confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn)
            buttonLayout = findViewById(R.id.buttonLayout)
            
            recyclerView.layoutManager = LinearLayoutManager(this)
            
            // Кликабельный путь
            pathText.setOnClickListener {
                showPathNavigator()
            }
            
            // Кнопка назад (только в режиме выбора)
            backBtn.setOnClickListener {
                if (selectionMode) {
                    exitSelectionMode()
                }
            }
            
            closeSelectionBtn.setOnClickListener {
                exitSelectionMode()
            }
            
            createArchiveBtn.setOnClickListener {
                enterSelectionMode("create")
            }
            
            extractArchiveBtn.setOnClickListener {
                enterSelectionMode("extract")
            }
            
            confirmSelectionBtn.setOnClickListener {
                val selectedFiles = fileAdapter.getSelectedFiles()
                logToFile("Confirm clicked. Selected files: ${selectedFiles.size}")
                
                if (selectedFiles.isEmpty()) {
                    Toast.makeText(this, "Не выбрано ни одного файла", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                when (selectionType) {
                    "create" -> {
                        exitSelectionMode()
                        showCreateArchiveDialogWithFiles(selectedFiles)
                    }
                    "extract" -> {
                        val archiveFile = selectedFiles.first()
                        exitSelectionMode()
                        showExtractArchiveDialogWithFile(archiveFile)
                    }
                }
            }
            
            checkPermissions()
            
        } catch (e: Exception) {
            logToFile("CRASH: ${e.message}")
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
        } catch (e: Exception) {}
    }
    
    private fun showPathNavigator() {
        val pathParts = currentPath.split("/").filter { it.isNotEmpty() }
        val displayParts = mutableListOf("/")
        displayParts.addAll(pathParts)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Перейти к папке")
            .setItems(displayParts.toTypedArray()) { _, which ->
                if (which == 0) {
                    loadFiles("/storage/emulated/0")
                } else {
                    val newPath = "/" + pathParts.take(which).joinToString("/")
                    loadFiles(newPath)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun checkPermissions() {
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
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, 
                                           grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(currentPath)
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun enterSelectionMode(type: String) {
        selectionMode = true
        selectionType = type
        confirmSelectionBtn.visibility = View.VISIBLE
        closeSelectionBtn.visibility = View.VISIBLE
        backBtn.visibility = View.VISIBLE
        buttonLayout.visibility = View.GONE
        Toast.makeText(this, "Выберите файлы и нажмите ✓", Toast.LENGTH_LONG).show()
        loadFiles(currentPath)
    }
    
    private fun exitSelectionMode() {
        selectionMode = false
        selectionType = ""
        confirmSelectionBtn.visibility = View.GONE
        closeSelectionBtn.visibility = View.GONE
        backBtn.visibility = View.GONE
        buttonLayout.visibility = View.VISIBLE
        loadFiles(currentPath)
    }
    
    private fun loadFiles(path: String) {
        try {
            val dir = File(path)
            val files = dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            currentPath = path
            pathText.text = path
            
            fileAdapter = FileAdapter(files, selectionMode) { file ->
                if (file.isDirectory) {
                    loadFiles(file.absolutePath)
                }
            }
            
            recyclerView.adapter = fileAdapter
            logToFile("Files loaded: ${files.size}, selectionMode: $selectionMode")
            
        } catch (e: Exception) {
            logToFile("Error loading files: ${e.message}")
        }
    }
    
    private fun showCreateArchiveDialogWithFiles(files: List<File>) {
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
        
        selectedFilesText.text = "Выбрано файлов: ${files.size}"
        
        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.createBtn).setOnClickListener {
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
                archiver.createArchive(files[0].absolutePath, outputPath, format)
                Toast.makeText(this, "Архив создан!", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showExtractArchiveDialogWithFile(archiveFile: File) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_extract_archive, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val selectedArchiveText = dialogView.findViewById<TextView>(R.id.selectedArchiveText)
        val outputPathEdit = dialogView.findViewById<EditText>(R.id.outputPathEdit)
        
        selectedArchiveText.text = "Архив: ${archiveFile.name}"
        outputPathEdit.setText("$currentPath/extracted")
        
        dialogView.findViewById<Button>(R.id.cancelExtractBtn).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.extractBtn).setOnClickListener {
            val outputDir = outputPathEdit.text.toString()
            
            if (outputDir.isEmpty()) {
                Toast.makeText(this, "Укажите путь распаковки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                archiver.extractArchive(archiveFile.absolutePath, outputDir)
                Toast.makeText(this, "Распаковано в: $outputDir", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        dialog.show()
    }
}
