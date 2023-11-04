package com.github.ikorennoy.remotefileviewer.filesystem

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.openapi.vfs.VFileProperty
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.InputStream

class RemoteVirtualFile(
    private val remoteFile: RemoteResourceInfo,
    private val fs: RemoteFileSystem,
) {

    fun getName(): String = remoteFile.name

    private val myParent: RemoteVirtualFile? by lazy { fs.getParent(this) }

    private val myChildren: Array<RemoteVirtualFile> by lazy { fs.getChildren(this) }

    private val isDir: Boolean by lazy {
        if (remoteFile.attributes.type == FileMode.Type.SYMLINK) {
            val originalAttrs = fs.getFileAttributes(this)
            originalAttrs.type == FileMode.Type.DIRECTORY
        } else {
            remoteFile.attributes.type == FileMode.Type.DIRECTORY
        }
    }

    fun getPath(): String {
        return remoteFile.path
    }

    fun isWritable(): Boolean {
        return true
    }

    fun isDirectory(): Boolean {
        return isDir
    }


    fun `is`(property: VFileProperty): Boolean {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteVirtualFile

        return remoteFile == other.remoteFile
    }

    override fun hashCode(): Int {
        return remoteFile.hashCode()
    }

    fun getChildren(): Array<RemoteVirtualFile> {
        return myChildren
    }

    fun getParent(): RemoteVirtualFile? {
        return myParent
    }

    fun getFileSystem(): RemoteFileSystem {
        return fs
    }

    fun getLength(): Long {
        return remoteFile.attributes.size
    }

    fun getInputStream(): InputStream {
        return fs.fileInputStream(this)
    }

    fun isValid(): Boolean {
        return true
    }

    fun getPresentableName(): String {
        return remoteFile.name
    }

    fun delete(remoteFileSystemTree: RemoteFileSystemTree) {
        fs.deleteFile(this)
    }

    fun createChildDirectory(newDirectoryName: String): RemoteVirtualFile {
        return fs.createChildDirectory(this, newDirectoryName)
    }

    fun createChildData(newFileName: String): RemoteVirtualFile {
        return fs.createChildFile(this, newFileName)
    }
}