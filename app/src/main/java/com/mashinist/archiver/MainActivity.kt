package com.mashinist.archiver

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var createArchiveBtn: Button
    private lateinit var extractArchiveBtn: Button
    private lateinit var backBtn: Button
    private lateinit var closeSelectionBtn: Button
    private lateinit var pathText: TextView
    private lateinit var confirmSelectionBtn: Button
    private lateinit var buttonLayout: LinearLayout
    private lateinit var menuBtn: Button
    private lateinit var archiver: MashinistArchiver
    private lateinit var fileAdapter: FileAdapter
    private lateinit var prefs: SharedPreferences
    
    private var currentPath = "/storage/emulated/0"
    private var selectionMode = false
    private var selectionType = ""
    private var defaultFormat = "mashinist"
    private var defaultExtractPath = "/storage/emulated/0/Extracted"
    private val STORAGE_PERMISSION_CODE = 100
    private val MANAGE_STORAGE_CODE = 200
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        loadSettings()
        
        archiver = MashinistArchiver()
        
        recyclerView = findViewById(R.id.recyclerView)
        createArchiveBtn = findViewById(R.id.createArchiveBtn)
        extractArchiveBtn = findViewById(R.id.extractArchiveBtn)
        backBtn = findViewById(R.id.backBtn)
        closeSelectionBtn = findViewById(R.id.closeSelectionBtn)
        pathText = findViewById(R.id.pathText)
        confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn)
        buttonLayout = findViewById(R.id.buttonLayout)
        menuBtn = findViewById(R.id.menuBtn)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        pathText.setOnClickListener { showPathNavigator() }
        backBtn.setOnClickListener { if (selectionMode) exitSelectionMode() }
        closeSelectionBtn.setOnClickListener { exitSelectionMode() }
        menuBtn.setOnClickListener { showMenuDialog() }
        
        createArchiveBtn.setOnClickListener {
            FileAdapter.selectedFiles.clear()
            enterSelectionMode("create")
        }
        
        extractArchiveBtn.setOnClickListener {
            FileAdapter.selectedFiles.clear()
            enterSelectionMode("extract")
        }
        
        confirmSelectionBtn.setOnClickListener {
            val selectedFiles = fileAdapter.getSelectedFiles()
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, "Не выбрано ни одного файла", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val filesToProcess = selectedFiles.toList()
            when (selectionType) {
                "create" -> {
                    exitSelectionMode()
                    showCreateArchiveDialogWithFiles(filesToProcess)
                }
                "extract" -> {
                    exitSelectionMode()
                    showExtractArchiveDialogWithFile(filesToProcess.first())
                }
            }
        }
        
        if (hasPermissions()) {
            loadFiles(currentPath)
        } else {
            showPermissionWarningDialog()
        }
    }
    
    private fun loadSettings() {
        defaultFormat = prefs.getString("default_format", "mashinist") ?: "mashinist"
        defaultExtractPath = prefs.getString("default_extract_path", "/storage/emulated/0/Extracted") ?: "/storage/emulated/0/Extracted"
    }
    
    private fun saveSettings(format: String, extractPath: String) {
        prefs.edit().putString("default_format", format).putString("default_extract_path", extractPath).apply()
        defaultFormat = format
        defaultExtractPath = extractPath
    }
    
    private fun showMenuDialog() {
        val options = arrayOf("Настройки", "О приложении")
        MaterialAlertDialogBuilder(this)
            .setTitle("Меню")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSettingsDialog()
                    1 -> showAboutDialog()
                }
            }
            .show()
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val defaultExtractPathEdit = dialogView.findViewById<EditText>(R.id.defaultExtractPathEdit)
        
        when (defaultFormat) {
            "tep70bs" -> dialogView.findViewById<RadioButton>(R.id.radioDefaultTep70bs).isChecked = true
            "tep" -> dialogView.findViewById<RadioButton>(R.id.radioDefaultTep).isChecked = true
        }
        defaultExtractPathEdit.setText(defaultExtractPath)
        
        dialogView.findViewById<Button>(R.id.saveSettingsBtn).setOnClickListener {
            val format = when {
                dialogView.findViewById<RadioButton>(R.id.radioDefaultTep70bs).isChecked -> "tep70bs"
                dialogView.findViewById<RadioButton>(R.id.radioDefaultTep).isChecked -> "tep"
                else -> "mashinist"
            }
            val extractPath = defaultExtractPathEdit.text.toString()
            saveSettings(format, extractPath)
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.closeSettingsBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    
    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<Button>(R.id.githubBtn).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mashinistTEP/mashinist-archiver")))
        }
        
        dialogView.findViewById<Button>(R.id.closeAboutBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    
    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun showPermissionWarningDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<Button>(R.id.continueBtn).setOnClickListener {
            dialog.dismiss()
            requestPermissions()
        }
        dialog.show()
    }
    
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:com.mashinist.archiver")
                startActivityForResult(intent, MANAGE_STORAGE_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANAGE_STORAGE_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                loadFiles(currentPath)
                Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(currentPath)
                Toast.makeText(this, "Готово к работе!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showPathNavigator() {
        val pathParts = currentPath.split("/").filter { it.isNotEmpty() }
        val displayParts = mutableListOf("/")
        displayParts.addAll(pathParts)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Перейти к папке")
            .setItems(displayParts.toTypedArray()) { _, which ->
                if (which == 0) loadFiles("/storage/emulated/0")
                else loadFiles("/" + pathParts.take(which).joinToString("/"))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun enterSelectionMode(type: String) {
        selectionMode = true
        selectionType = type
        confirmSelectionBtn.visibility = View.VISIBLE
        closeSelectionBtn.visibility = View.VISIBLE
        backBtn.visibility = View.VISIBLE
        buttonLayout.visibility = View.GONE
        Toast.makeText(this, "Выберите и нажмите ✓", Toast.LENGTH_LONG).show()
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
            val files = dir.listFiles()?.toList()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) 
                ?: emptyList()
            currentPath = path
            pathText.text = path
            
            fileAdapter = FileAdapter(files, selectionMode) { file ->
                if (file.isDirectory && !selectionMode) loadFiles(file.absolutePath)
            }
            recyclerView.adapter = fileAdapter
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showCreateArchiveDialogWithFiles(files: List<File>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_archive, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val archiveNameEdit = dialogView.findViewById<EditText>(R.id.archiveNameEdit)
        val selectedFilesText = dialogView.findViewById<TextView>(R.id.selectedFilesText)
        val radioMashinist = dialogView.findViewById<RadioButton>(R.id.radioMashinist)
        val radioTep70bs = dialogView.findViewById<RadioButton>(R.id.radioTep70bs)
        val radioTep = dialogView.findViewById<RadioButton>(R.id.radioTep)
        
        when (defaultFormat) {
            "tep70bs" -> radioTep70bs.isChecked = true
            "tep" -> radioTep.isChecked = true
        }
        
        selectedFilesText.text = "Выбрано: ${files.size} шт."
        
        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            FileAdapter.selectedFiles.clear()
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
            try {
                archiver.createArchive(files[0].absolutePath, "$currentPath/$fileName.$format", format)
                Toast.makeText(this, "Архив создан!", Toast.LENGTH_SHORT).show()
                FileAdapter.selectedFiles.clear()
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
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val selectedArchiveText = dialogView.findViewById<TextView>(R.id.selectedArchiveText)
        val outputPathEdit = dialogView.findViewById<EditText>(R.id.outputPathEdit)
        
        val archiveName = archiveFile.nameWithoutExtension
        selectedArchiveText.text = "Архив: ${archiveFile.name}"
        outputPathEdit.setText("$currentPath/$archiveName")
        
        dialogView.findViewById<Button>(R.id.cancelExtractBtn).setOnClickListener {
            FileAdapter.selectedFiles.clear()
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
                FileAdapter.selectedFiles.clear()
                loadFiles(currentPath)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }
}
