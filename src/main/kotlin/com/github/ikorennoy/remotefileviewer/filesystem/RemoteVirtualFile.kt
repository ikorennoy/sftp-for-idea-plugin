package com.github.ikorennoy.remotefileviewer.filesystem

import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.InputStream
import java.io.OutputStream

class RemoteVirtualFile(
    private val remoteFile: RemoteResourceInfo,
    private val fs: RemoteFileSystem,
) : VirtualFile() {

    override fun getName(): String = remoteFile.name

    private val myParent: VirtualFile? by lazy { fs.getParent(this) }

    private val myChildren: Array<RemoteVirtualFile> by lazy { fs.getChildren(this) }

    private val isDir: Boolean by lazy {
        if (remoteFile.attributes.type == FileMode.Type.SYMLINK) {
            val originalAttrs = fs.getFileAttributes(this)
            originalAttrs.type == FileMode.Type.DIRECTORY
        } else {
            remoteFile.attributes.type == FileMode.Type.DIRECTORY
        }
    }

    override fun getFileSystem(): VirtualFileSystem {
        return fs
    }

    override fun getPath(): String {
        return remoteFile.path
    }

    override fun isWritable(): Boolean {
        return true
    }

    override fun isDirectory(): Boolean {
        return isDir
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return myParent
    }

    override fun getChildren(): Array<RemoteVirtualFile> {
        return myChildren
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("not supported")
    }

    override fun contentsToByteArray(): ByteArray {
        return fs.fileInputStream(this).use { it.readAllBytes() }
    }

    override fun getTimeStamp(): Long {
        return remoteFile.attributes.mtime
    }

    override fun getLength(): Long {
        return remoteFile.attributes.size
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {

    }


    override fun `is`(property: VFileProperty): Boolean {
        return when (property) {
            VFileProperty.HIDDEN -> remoteFile.name.startsWith(".")
            VFileProperty.SPECIAL -> isSpecial()
            VFileProperty.SYMLINK -> remoteFile.attributes.type == FileMode.Type.SYMLINK
        }
    }

    private fun isSpecial(): Boolean {
        val type = remoteFile.attributes.mode.type
        return type == FileMode.Type.BLOCK_SPECIAL ||
                type == FileMode.Type.CHAR_SPECIAL ||
                type == FileMode.Type.FIFO_SPECIAL ||
                type == FileMode.Type.SOCKET_SPECIAL
    }

    override fun getInputStream(): InputStream {
        return fs.fileInputStream(this)
    }

    override fun getModificationStamp(): Long {
        return remoteFile.attributes.mtime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteVirtualFile

        return remoteFile == other.remoteFile
    }

    override fun hashCode(): Int {
        return remoteFile.hashCode()
    }
}