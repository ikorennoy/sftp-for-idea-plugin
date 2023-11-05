package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.util.LocalTimeCounter
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class TempVirtualFile(
    private val name: String,
    private val fileSystem: TempVirtualFileSystem,
    val remoteFile: RemoteFileInformation,
    val localTempFile: File
) : VirtualFile() {

    private val modificationTimestamp: Long = LocalTimeCounter.currentTime()

    private val parent: VirtualFile = object : BinaryLightVirtualFile("") {
        override fun getFileSystem(): VirtualFileSystem {
            return this@TempVirtualFile.fileSystem
        }

        override fun isWritable(): Boolean {
            return false
        }

        override fun isDirectory(): Boolean {
            return true
        }

        override fun getChildren(): Array<VirtualFile> {
            return arrayOf(this@TempVirtualFile)
        }
    }

    override fun getName(): String {
        return name
    }

    override fun getFileSystem(): VirtualFileSystem {
        return fileSystem
    }

    override fun getPath(): String {
        val conf = service<RemoteFileAccessSettingsState>()
        return "sftp://${conf.username}@${conf.host}${remoteFile.getPath()}"
    }

    override fun isWritable(): Boolean {
        return true
    }

    override fun isDirectory(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile {
        return parent
    }

    override fun getChildren(): Array<VirtualFile> {
        return EMPTY_ARRAY
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return localTempFile.outputStream()
    }

    override fun contentsToByteArray(): ByteArray {
        return localTempFile.readBytes()
    }

    override fun getTimeStamp(): Long {
        return this.modificationTimestamp
    }

    override fun getModificationStamp(): Long {
        return this.modificationTimestamp
    }

    override fun getLength(): Long {
        return localTempFile.length()
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        postRunnable?.run()
    }

    override fun delete(requestor: Any?) {
        fileSystem.deleteFile(requestor, this)
    }

    override fun getInputStream(): InputStream {
        return localTempFile.inputStream()
    }
}
