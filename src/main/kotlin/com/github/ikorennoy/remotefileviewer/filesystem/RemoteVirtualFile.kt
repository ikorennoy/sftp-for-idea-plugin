package com.github.ikorennoy.remotefileviewer.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.xfer.FilePermission
import java.io.InputStream
import java.io.OutputStream

class RemoteVirtualFile(
        val remoteFile: RemoteResourceInfo,
        private val fs: RemoteFileSystem,
) : VirtualFile() {

    override fun getName(): String = remoteFile.name

    override fun getFileSystem(): VirtualFileSystem {
        return fs
    }

    override fun getPath(): String {
        return remoteFile.path
    }

    override fun isWritable(): Boolean {
        return remoteFile.attributes.permissions.contains(FilePermission.USR_R)
    }

    override fun isDirectory(): Boolean {
        return remoteFile.attributes.type == FileMode.Type.DIRECTORY
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return fs.getParent(this)
    }

    override fun getChildren(): Array<VirtualFile> {
        return fs.getChildren(this)
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return fs.fileOutputStream(this)
    }

    override fun contentsToByteArray(): ByteArray {
        return fs.fileInputStream(this).readAllBytes()
    }

    override fun getTimeStamp(): Long {
        return remoteFile.attributes.mtime
    }

    override fun getLength(): Long {
        return remoteFile.attributes.size
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {

    }

    override fun getInputStream(): InputStream {
        return fs.fileInputStream(this)
    }

    override fun getModificationStamp(): Long {
        return remoteFile.attributes.mtime
    }
}