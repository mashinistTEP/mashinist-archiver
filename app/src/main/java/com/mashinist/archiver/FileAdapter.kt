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
    
    companion object {
        val selectedFiles = mutableSetOf<File>()
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
        val checkBox: CheckBox = view.findViewById(R.id.fileCheckBox)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
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
            val ext = file.extension.lowercase()
            holder.icon.setImageResource(when {
                ext in listOf("mashinist", "tep70bs", "tep") -> android.R.drawable.ic_menu_save
                ext in listOf("jpg", "jpeg", "png", "gif") -> android.R.drawable.ic_menu_gallery
                else -> android.R.drawable.ic_menu_gallery
            })
            holder.info.text = "Файл | ${formatSize(file.length())}"
        }
        
        if (selectionMode) {
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.isChecked = selectedFiles.contains(file)
        } else {
            holder.checkBox.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            if (file.isDirectory && !selectionMode) {
                onFileClick(file)
            } else if (selectionMode) {
                if (file.isDirectory) {
                    onFileClick(file)
                } else {
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
        
        holder.itemView.setOnLongClickListener {
            if (selectionMode && file.isDirectory) {
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file)
                    holder.checkBox.isChecked = false
                } else {
                    selectedFiles.add(file)
                    holder.checkBox.isChecked = true
                }
                true
            } else false
        }
    }
    
    override fun getItemCount() = files.size
    fun getSelectedFiles(): List<File> = selectedFiles.toList()
    
    private fun formatSize(size: Long): String = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
