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
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var createArchiveBtn: Button
    private lateinit var extractArchiveBtn: Button
    private lateinit var pathText: TextView
    private lateinit var archiver: MashinistArchiver
    
    private var currentPath = Environment.getExternalStorageDirectory().absolutePath
    private val STORAGE_PERMISSION_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        archiver = MashinistArchiver()
        
        recyclerView = findViewById(R.id.recyclerView)
        createArchiveBtn = findViewById(R.id.createArchiveBtn)
        extractArchiveBtn = findViewById(R.id.extractArchiveBtn)
        pathText = findViewById(R.id.pathText)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        createArchiveBtn.setOnClickListener {
            showCreateArchiveDialog()
        }
        
        extractArchiveBtn.setOnClickListener {
            showExtractArchiveDialog()
        }
        
        checkPermissions()
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
            }
        }
    }
    
    private fun loadFiles(path: String) {
        val dir = File(path)
        val files = dir.listFiles()?.toList() ?: emptyList()
        currentPath = path
        pathText.text = path
        
        recyclerView.adapter = FileAdapter(files) { file ->
            if (file.isDirectory) {
                loadFiles(file.absolutePath)
            } else {
                showFileOptions(file)
            }
        }
    }
    
    private fun showCreateArchiveDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Создать архив")
        
        val view = layoutInflater.inflate(R.layout.dialog_create_archive, null)
        val archiveNameEdit = view.findViewById<EditText>(R.id.archiveNameEdit)
        val radioMashinist = view.findViewById<RadioButton>(R.id.radioMashinist)
        val radioTep70bs = view.findViewById<RadioButton>(R.id.radioTep70bs)
        val radioTep = view.findViewById<RadioButton>(R.id.radioTep)
        
        builder.setView(view)
        builder.setPositiveButton("Создать") { dialog, _ ->
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
            }
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
    
    private fun showExtractArchiveDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Распаковать архив")
        
        val view = layoutInflater.inflate(R.layout.dialog_extract_archive, null)
        val outputPathEdit = view.findViewById<EditText>(R.id.outputPathEdit)
        
        builder.setView(view)
        builder.setPositiveButton("Распаковать") { dialog, _ ->
            val archivePath = outputPathEdit.text.toString()
            if (archivePath.isNotEmpty()) {
                val outputDir = currentPath + "/extracted"
                archiver.extractArchive(archivePath, outputDir)
                Toast.makeText(this, "Распаковано в: $outputDir", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
    
    private fun showFileOptions(file: File) {
        val options = arrayOf("Сжать в архив", "Распаковать (если архив)", "Удалить")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(file.name)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> compressFile(file)
                1 -> {
                    val outputDir = file.parent + "/extracted"
                    archiver.extractArchive(file.absolutePath, outputDir)
                    Toast.makeText(this, "Распаковано!", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    file.delete()
                    loadFiles(currentPath)
                }
            }
        }
        builder.show()
    }
    
    private fun compressFile(file: File) {
        val extensions = arrayOf(".mashinist", ".tep70bs", ".tep")
        val formats = arrayOf("mashinist", "tep70bs", "tep")
        
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Выберите формат")
        builder.setItems(extensions) { _, which ->
            val outputPath = file.absolutePath + extensions[which]
            archiver.createArchive(file.absolutePath, outputPath, formats[which])
            loadFiles(currentPath)
            Toast.makeText(this, "Архив создан: $outputPath", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }
}
