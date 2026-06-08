package com.mashinist.archiver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private val files: List<File>,
    private val selectionMode: Boolean,
    private val onFileClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    
    private val selectedFiles = mutableSetOf<File>()
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
        val checkBox: CheckBox = view.findViewById(R.id.fileCheckBox)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.name.text = file.name
        
        // Иконки в зависимости от типа файла
        if (file.isDirectory) {
            holder.icon.setImageResource(android.R.drawable.ic_dialog_info)
            val count = file.listFiles()?.size ?: 0
            holder.info.text = "Папка | $count элементов"
        } else {
            val extension = file.extension.lowercase()
            when {
                extension in listOf("mashinist", "tep70bs", "tep") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_save)
                    holder.info.text = "Архив | ${formatSize(file.length())}"
                }
                extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_gallery)
                    holder.info.text = "Изображение | ${formatSize(file.length())}"
                }
                extension in listOf("mp4", "avi", "mkv", "mov", "flv") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_media_play)
                    holder.info.text = "Видео | ${formatSize(file.length())}"
                }
                extension in listOf("mp3", "wav", "ogg", "flac", "aac") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_media_play)
                    holder.info.text = "Аудио | ${formatSize(file.length())}"
                }
                extension in listOf("pdf", "doc", "docx", "txt", "xml", "json") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_edit)
                    holder.info.text = "Документ | ${formatSize(file.length())}"
                }
                extension in listOf("apk") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_compass)
                    holder.info.text = "APK | ${formatSize(file.length())}"
                }
                extension in listOf("zip", "rar", "7z", "tar", "gz") -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_save)
                    holder.info.text = "Архив | ${formatSize(file.length())}"
                }
                else -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_gallery)
                    holder.info.text = "Файл | ${formatSize(file.length())}"
                }
            }
        }
        
        // Чекбокс только для обычных файлов в режиме выбора
        if (selectionMode && !file.isDirectory) {
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.isChecked = selectedFiles.contains(file)
        } else {
            holder.checkBox.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            if (file.isDirectory) {
                onFileClick(file)
            } else if (selectionMode) {
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file)
                    holder.checkBox.isChecked = false
                } else {
                    selectedFiles.add(file)
                    holder.checkBox.isChecked = true
                }
            }
        }
    }
    
    override fun getItemCount() = files.size
    
    fun getSelectedFiles(): List<File> = selectedFiles.toList()
    
    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${"%.1f".format(size / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}
