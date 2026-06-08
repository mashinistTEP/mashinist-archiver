package com.mashinist.archiver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val files: List<File>,
    private val onFileClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.name.text = file.name
        
        if (file.isDirectory) {
            holder.icon.setImageResource(android.R.drawable.ic_dialog_info)
            val count = file.listFiles()?.size ?: 0
            holder.info.text = "Папка | $count элементов"
        } else {
            val extension = file.extension
            when (extension) {
                "mashinist", "tep70bs", "tep" -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_save)
                    holder.info.text = "Архив | ${formatSize(file.length())}"
                }
                else -> {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_gallery)
                    holder.info.text = "Файл | ${formatSize(file.length())}"
                }
            }
        }
        
        holder.itemView.setOnClickListener {
            onFileClick(file)
        }
    }
    
    override fun getItemCount() = files.size
    
    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}
