package com.github.ikorennoy.remote.edit

import com.github.ikorennoy.remote.operations.RemoteFileInformation
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val PROTOCOL = "remoteFileViewerLocalFs"

class TempVirtualFileSystem : VirtualFileSystem() {

    private val downloadedFiles = ConcurrentHashMap<String, TempVirtualFile>()

    override fun getProtocol(): String {
        return PROTOCOL
    }

    fun wrapIntoTempFile(originalFile: RemoteFileInformation, content: File): TempVirtualFile {
        val file = TempVirtualFile(originalFile.getName(), this, originalFile, content)
        downloadedFiles[originalFile.getPath()] = file
        return file
    }

    override fun findFileByPath(path: String): VirtualFile? {
        return downloadedFiles[path]
    }

    override fun refresh(asynchronous: Boolean) {
        throw UnsupportedOperationException("refresh is not supported")
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return null
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {

    }

    override fun removeVirtualFileListener(listener: VirtualFileListener) {

    }

    public override fun deleteFile(requestor: Any?, file: VirtualFile) {
        if (file is TempVirtualFile) {
            FileUtil.delete(file.localTempFile)
        }
    }

    override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {
        throw UnsupportedOperationException("moveFile is not supported")
    }

    override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) {
        throw UnsupportedOperationException("renameFile is not supported")
    }

    override fun createChildFile(requestor: Any?, parent: VirtualFile, file: String): VirtualFile {
        throw UnsupportedOperationException("createChildFile is not supported")
    }

    override fun createChildDirectory(requestor: Any?, parent: VirtualFile, dir: String): VirtualFile {
        throw UnsupportedOperationException("createChildDirectory is not supported")
    }

    override fun copyFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
        throw UnsupportedOperationException("copyFile is not supported")
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    companion object {
        fun getInstance(): TempVirtualFileSystem {
            return VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as TempVirtualFileSystem
        }
    }
}