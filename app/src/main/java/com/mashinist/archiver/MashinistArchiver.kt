package com.mashinist.archiver

import java.io.*

class MashinistArchiver {
    
    fun createArchive(inputPath: String, outputPath: String, format: String) {
        try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) return
            
            DataOutputStream(FileOutputStream(outputPath)).use { dos ->
                when (format) {
                    "mashinist" -> dos.write(byteArrayOf(0x4D, 0x41, 0x53, 0x48))
                    "tep70bs" -> dos.write(byteArrayOf(0x54, 0x37, 0x30, 0x42))
                    "tep" -> dos.write(byteArrayOf(0x54, 0x45, 0x50, 0x00))
                }
                dos.writeByte(1)
                dos.writeUTF(inputFile.name)
                
                val inputBytes = inputFile.readBytes()
                val compressedData = compress(inputBytes)
                
                dos.writeInt(compressedData.size)
                dos.writeInt(inputBytes.size)
                dos.write(compressedData)
                dos.writeLong(checksum(inputBytes))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun extractArchive(archivePath: String, outputDir: String) {
        try {
            DataInputStream(FileInputStream(archivePath)).use { dis ->
                val signature = ByteArray(4)
                dis.readFully(signature)
                dis.readByte()
                val fileName = dis.readUTF()
                
                val compressedSize = dis.readInt()
                val originalSize = dis.readInt()
                val compressedData = ByteArray(compressedSize)
                dis.readFully(compressedData)
                
                val decompressed = decompress(compressedData, originalSize)
                
                val outputFile = File(outputDir, fileName)
                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(decompressed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun compress(data: ByteArray): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.DeflaterOutputStream(baos, java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION)).use {
            it.write(data)
        }
        return baos.toByteArray()
    }
    
    private fun decompress(data: ByteArray, originalSize: Int): ByteArray {
        val bais = java.io.ByteArrayInputStream(data)
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.InflaterInputStream(bais).use {
            it.copyTo(baos)
        }
        return baos.toByteArray()
    }
    
    private fun checksum(data: ByteArray): Long {
        var checksum = 0L
        for (byte in data) {
            checksum = (checksum shl 8) or (byte.toLong() and 0xFF)
            checksum = checksum xor (checksum ushr 32)
        }
        return checksum
    }
}
