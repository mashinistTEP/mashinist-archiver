package com.mashinist.archiver

import android.Manifest
import android.content.Intent
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
    private lateinit var archiver: MashinistArchiver
    private lateinit var fileAdapter: FileAdapter
    
    private var currentPath = "/storage/emulated/0"
    private var selectionMode = false
    private var selectionType = ""
    private val STORAGE_PERMISSION_CODE = 100
    private val MANAGE_STORAGE_CODE = 200
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        
        pathText.setOnClickListener { showPathNavigator() }
        backBtn.setOnClickListener { if (selectionMode) exitSelectionMode() }
        closeSelectionBtn.setOnClickListener { exitSelectionMode() }
        createArchiveBtn.setOnClickListener { enterSelectionMode("create") }
        extractArchiveBtn.setOnClickListener { enterSelectionMode("extract") }
        
        confirmSelectionBtn.setOnClickListener {
            val selectedFiles = fileAdapter.getSelectedFiles()
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
                    exitSelectionMode()
                    showExtractArchiveDialogWithFile(selectedFiles.first())
                }
            }
        }
        
        showPermissionWarningDialog()
    }
    
    private fun showPermissionWarningDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<Button>(R.id.continueBtn).setOnClickListener {
            dialog.dismiss()
            requestPermissions()
        }
        
        dialog.show()
    }
    
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:com.mashinist.archiver")
                startActivityForResult(intent, MANAGE_STORAGE_CODE)
            } else {
                loadFiles(currentPath)
            }
        } else {
            // Android 10 и ниже
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, 
                       Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANAGE_STORAGE_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadFiles(currentPath)
                    Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, 
                                           grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(currentPath)
                Toast.makeText(this, "Готово к работе!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Нужны разрешения для работы", Toast.LENGTH_LONG).show()
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
            val files = dir.listFiles()?.toList()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) 
                ?: emptyList()
            currentPath = path
            pathText.text = path
            
            fileAdapter = FileAdapter(files, selectionMode) { file ->
                if (file.isDirectory) loadFiles(file.absolutePath)
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
        
        selectedFilesText.text = "Выбрано файлов: ${files.size}"
        
        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener { dialog.dismiss() }
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
        
        selectedArchiveText.text = "Архив: ${archiveFile.name}"
        outputPathEdit.setText("$currentPath/extracted")
        
        dialogView.findViewById<Button>(R.id.cancelExtractBtn).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.extractBtn).setOnClickListener {
            val outputDir = outputPathEdit.text.toString()
            if (outputDir.isEmpty()) {
                Toast.makeText(this, "Укажите путь распаковки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                archiver.extractArchive(archiveFile.absolutePath, outputDir)
                Toast.makeText(this, "Распаковано!", Toast.LENGTH_SHORT).show()
                loadFiles(currentPath)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }
}
