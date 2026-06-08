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

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var createArchiveBtn: Button
    private lateinit var extractArchiveBtn: Button
    private lateinit var backBtn: Button
    private lateinit var pathText: TextView
    private lateinit var archiver: MashinistArchiver
    
    private var currentPath = "/storage/emulated/0"
    private val STORAGE_PERMISSION_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        archiver = MashinistArchiver()
        
        recyclerView = findViewById(R.id.recyclerView)
        createArchiveBtn = findViewById(R.id.createArchiveBtn)
        extractArchiveBtn = findViewById(R.id.extractArchiveBtn)
        backBtn = findViewById(R.id.backBtn)
        pathText = findViewById(R.id.pathText)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        backBtn.setOnClickListener {
            val parentDir = File(currentPath).parentFile
            if (parentDir != null && parentDir.canRead()) {
                loadFiles(parentDir.absolutePath)
            }
        }
        
        createArchiveBtn.setOnClickListener {
            showCreateArchiveDialog()
        }
        
        extractArchiveBtn.setOnClickListener {
            showExtractArchiveDialog()
        }
        
        showPermissionDialog()
    }
    
    private fun showPermissionDialog() {
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
        val dir = File(path)
        val files = dir.listFiles()?.toList() ?: emptyList()
        currentPath = path
        pathText.text = path
        
        backBtn.isEnabled = File(path).parentFile != null
        
        recyclerView.adapter = FileAdapter(files) { file ->
            if (file.isDirectory) {
                loadFiles(file.absolutePath)
            }
        }
    }
    
    private fun showCreateArchiveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_archive, null)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val archiveNameEdit = dialogView.findViewById<EditText>(R.id.archiveNameEdit)
        val radioMashinist = dialogView.findViewById<RadioButton>(R.id.radioMashinist)
        val radioTep70bs = dialogView.findViewById<RadioButton>(R.id.radioTep70bs)
        val radioTep = dialogView.findViewById<RadioButton>(R.id.radioTep)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val createBtn = dialogView.findViewById<Button>(R.id.createBtn)
        
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        
        createBtn.setOnClickListener {
            val fileName = archiveNameEdit.text.toString()
            val format = when {
                radioTep70bs.isChecked -> "tep70bs"
                radioTep.isChecked -> "tep"
                else -> "mashinist"
            }
            
            if (fileName.isNotEmpty()) {
                val outputPath = "$currentPath/$fileName.$format"
                archiver.createArchive(currentPath + "/test.txt", outputPath, format)
                Toast.makeText(this, "Архив создан: $outputPath", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
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
                val outputDir = currentPath + "/extracted"
                archiver.extractArchive(archivePath, outputDir)
                Toast.makeText(this, "Распаковано в: $outputDir", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
}
