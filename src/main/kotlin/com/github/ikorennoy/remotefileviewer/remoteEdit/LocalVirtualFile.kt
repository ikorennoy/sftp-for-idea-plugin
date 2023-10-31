package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.filesystem.SftpVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.util.LocalTimeCounter
import java.io.InputStream
import java.io.OutputStream

class LocalVirtualFile(
    private val name: String,
    private val fileSystem: VirtualFileSystem,
    private val path: String,
    val remoteFile: SftpVirtualFile,
    private val content: ByteArray
) : VirtualFile() {

    private val delegateFile: BinaryLightVirtualFile = BinaryLightVirtualFile(name, content)
    private val modificationTimestamp: Long = LocalTimeCounter.currentTime()

    private val parent: VirtualFile = object : BinaryLightVirtualFile("") {
        override fun getFileSystem(): VirtualFileSystem {
            return this@LocalVirtualFile.fileSystem
        }

        override fun isWritable(): Boolean {
            return false
        }

        override fun isDirectory(): Boolean {
            return true
        }

        override fun getChildren(): Array<VirtualFile> {
            return arrayOf(this@LocalVirtualFile)
        }
    }

    override fun getName(): String {
        return name
    }

    override fun getFileSystem(): VirtualFileSystem {
        return fileSystem
    }

    override fun getPath(): String {
        return path
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
        return delegateFile.getOutputStream(requestor)
    }

    override fun contentsToByteArray(): ByteArray {
        return delegateFile.contentsToByteArray()
    }

    override fun getTimeStamp(): Long {
        return delegateFile.timeStamp
    }

    override fun getModificationStamp(): Long {
        return this.modificationTimestamp
    }

    override fun getLength(): Long {
        return delegateFile.length
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        postRunnable?.run()
    }

    override fun getInputStream(): InputStream {
        return delegateFile.inputStream
    }


}