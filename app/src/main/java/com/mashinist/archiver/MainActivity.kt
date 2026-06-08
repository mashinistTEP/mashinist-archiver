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
    private lateinit var closeSelectionBtn: TextView
    private lateinit var pathText: TextView
    private lateinit var confirmSelectionBtn: TextView
    private lateinit var buttonLayout: LinearLayout
    private lateinit var menuBtn: TextView
    private lateinit var archiver: MashinistArchiver
    private lateinit var fileAdapter: FileAdapter
    private lateinit var prefs: SharedPreferences
    
    private var currentPath = "/storage/emulated/0"
    private var selectionMode = false
    private var selectionType = ""
    private var defaultFormat = "mashinist"
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
        closeSelectionBtn = findViewById(R.id.closeSelectionBtn)
        pathText = findViewById(R.id.pathText)
        confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn)
        buttonLayout = findViewById(R.id.buttonLayout)
        menuBtn = findViewById(R.id.menuBtn)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        pathText.setOnClickListener { showPathNavigator() }
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
                "create" -> { exitSelectionMode(); showCreateArchiveDialogWithFiles(filesToProcess) }
                "extract" -> { exitSelectionMode(); showExtractArchiveDialogWithFile(filesToProcess.first()) }
            }
        }
        
        if (hasPermissions()) loadFiles(currentPath) else showPermissionWarningDialog()
    }
    
    private fun loadSettings() {
        defaultFormat = prefs.getString("default_format", "mashinist") ?: "mashinist"
    }
    
    private fun saveSettings(format: String) {
        prefs.edit().putString("default_format", format).apply()
        defaultFormat = format
    }
    
    private fun showMenuDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Меню")
            .setItems(arrayOf("Настройки", "О приложении")) { _, which ->
                if (which == 0) showSettingsDialog() else showAboutDialog()
            }.show()
    }
    
    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val rTep70bs = view.findViewById<RadioButton>(R.id.radioDefaultTep70bs)
        val rTep = view.findViewById<RadioButton>(R.id.radioDefaultTep)
        when (defaultFormat) { "tep70bs" -> rTep70bs.isChecked = true; "tep" -> rTep.isChecked = true }
        view.findViewById<Button>(R.id.saveSettingsBtn).setOnClickListener {
            saveSettings(when { rTep70bs.isChecked -> "tep70bs"; rTep.isChecked -> "tep"; else -> "mashinist" })
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show(); dialog.dismiss()
        }
        view.findViewById<Button>(R.id.closeSettingsBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    
    private fun showAboutDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_about, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<Button>(R.id.githubBtn).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mashinistTEP/mashinist-archiver")))
        }
        view.findViewById<Button>(R.id.closeAboutBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    
    private fun hasPermissions(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    
    private fun showPermissionWarningDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_permission, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(view).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<Button>(R.id.continueBtn).setOnClickListener { dialog.dismiss(); requestPermissions() }
        dialog.show()
    }
    
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivityForResult(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:com.mashinist.archiver")), MANAGE_STORAGE_CODE)
            }
        } else ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANAGE_STORAGE_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadFiles(currentPath); Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadFiles(currentPath); Toast.makeText(this, "Готово!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPathNavigator() {
        val parts = currentPath.split("/").filter { it.isNotEmpty() }
        MaterialAlertDialogBuilder(this).setTitle("Перейти к папке")
            .setItems((listOf("/") + parts).toTypedArray()) { _, which ->
                if (which == 0) loadFiles("/storage/emulated/0")
                else loadFiles("/" + parts.take(which).joinToString("/"))
            }.setNegativeButton("Отмена", null).show()
    }
    
    private fun enterSelectionMode(type: String) {
        selectionMode = true; selectionType = type
        confirmSelectionBtn.visibility = View.VISIBLE; closeSelectionBtn.visibility = View.VISIBLE
        buttonLayout.visibility = View.GONE
        Toast.makeText(this, "Выберите и нажмите ✓", Toast.LENGTH_LONG).show(); loadFiles(currentPath)
    }
    
    private fun exitSelectionMode() {
        selectionMode = false; selectionType = ""
        confirmSelectionBtn.visibility = View.GONE; closeSelectionBtn.visibility = View.GONE
        buttonLayout.visibility = View.VISIBLE; loadFiles(currentPath)
    }
    
    private fun loadFiles(path: String) {
        try {
            val dir = File(path)
            val files = dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            currentPath = path; pathText.text = path
            fileAdapter = FileAdapter(files, selectionMode) { f ->
                if (f.isDirectory && !selectionMode) loadFiles(f.absolutePath)
            }
            recyclerView.adapter = fileAdapter
        } catch (e: Exception) { Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show() }
    }
    
    private fun showCreateArchiveDialogWithFiles(files: List<File>) {
        val view = layoutInflater.inflate(R.layout.dialog_create_archive, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val nameEdit = view.findViewById<EditText>(R.id.archiveNameEdit)
        view.findViewById<TextView>(R.id.selectedFilesText).text = "Выбрано: ${files.size} шт."
        val r70 = view.findViewById<RadioButton>(R.id.radioTep70bs)
        val rT = view.findViewById<RadioButton>(R.id.radioTep)
        when (defaultFormat) { "tep70bs" -> r70.isChecked = true; "tep" -> rT.isChecked = true }
        view.findViewById<Button>(R.id.cancelBtn).setOnClickListener { FileAdapter.selectedFiles.clear(); dialog.dismiss() }
        view.findViewById<Button>(R.id.createBtn).setOnClickListener {
            val name = nameEdit.text.toString()
            if (name.isEmpty()) { Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val fmt = when { r70.isChecked -> "tep70bs"; rT.isChecked -> "tep"; else -> "mashinist" }
            try {
                archiver.createArchive(files[0].absolutePath, "$currentPath/$name.$fmt", fmt)
                Toast.makeText(this, "Создан!", Toast.LENGTH_SHORT).show()
                FileAdapter.selectedFiles.clear(); loadFiles(currentPath); dialog.dismiss()
            } catch (e: Exception) { Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show() }
        }
        dialog.show()
    }
    
    private fun showExtractArchiveDialogWithFile(archiveFile: File) {
        val view = layoutInflater.inflate(R.layout.dialog_extract_archive, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<TextView>(R.id.selectedArchiveText).text = "Архив: ${archiveFile.name}"
        val outPath = view.findViewById<EditText>(R.id.outputPathEdit)
        outPath.setText("${archiveFile.parent}/${archiveFile.nameWithoutExtension}")
        view.findViewById<Button>(R.id.cancelExtractBtn).setOnClickListener { FileAdapter.selectedFiles.clear(); dialog.dismiss() }
        view.findViewById<Button>(R.id.extractBtn).setOnClickListener {
            val dir = outPath.text.toString()
            if (dir.isEmpty()) { Toast.makeText(this, "Укажите путь", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            try {
                archiver.extractArchive(archiveFile.absolutePath, dir)
                Toast.makeText(this, "Распаковано!", Toast.LENGTH_SHORT).show()
                FileAdapter.selectedFiles.clear(); loadFiles(currentPath); dialog.dismiss()
            } catch (e: Exception) { Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show() }
        }
        dialog.show()
    }
}
