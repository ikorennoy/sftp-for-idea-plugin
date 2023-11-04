package com.github.ikorennoy.remotefileviewer.remote

import com.github.ikorennoy.remotefileviewer.utils.Outcome
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VFileProperty
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.PathComponents
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.InputStream
import java.io.OutputStream

class RemoteFileInformation(
    private val remoteFile: RemoteResourceInfo,
    private val project: Project,
) {

    private val myParent: RemoteFileInformation? by lazy { getRemoteOperations().getParent(getPath()) }
    private val myChildren: Outcome<Array<RemoteFileInformation>> by lazy { getRemoteOperations().getChildren(getPath()) }
    private val isDir: Boolean by lazy {
        if (remoteFile.attributes.type == FileMode.Type.SYMLINK) {
            val originalAttrs = getRemoteOperations().getFileAttributes(getPath())
            originalAttrs?.type == FileMode.Type.DIRECTORY
        } else {
            remoteFile.attributes.type == FileMode.Type.DIRECTORY
        }
    }

    fun getName(): String = remoteFile.name

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

        other as RemoteFileInformation

        return remoteFile == other.remoteFile
    }

    override fun hashCode(): Int {
        return remoteFile.hashCode()
    }

    fun getChildren(): Outcome<Array<RemoteFileInformation>> {
        return myChildren
    }

    fun getParent(): RemoteFileInformation? {
        return myParent
    }

    fun getLength(): Long {
        return remoteFile.attributes.size
    }

    fun getInputStream(): InputStream? {
        return getRemoteOperations().fileInputStream(this.getPath())
    }

    fun isValid(): Boolean {
        return true
    }

    fun getPresentableName(): String {
        return remoteFile.name
    }

    fun delete() {
        getRemoteOperations().remove(this)
    }

    fun createChildDirectory(newDirectoryName: String): RemoteFileInformation? {
        return getRemoteOperations().createChildDirectory(this, newDirectoryName)
    }

    fun createChildData(newFileName: String): RemoteFileInformation? {
        return getRemoteOperations().createChildFile(this, newFileName)
    }

    fun openTempFile(): Pair<OutputStream?, String> {
        val client = getRemoteOperations()
        val tmpName = getTmpName()
        return client.fileOutputStream(tmpName) to tmpName
    }

    private fun getTmpName(): String {
        val client = getRemoteOperations()
        var i = 0
        var name = "/tmp/${getName()}.tmp"
        while (true) {
            if (!client.exists(name)) {
                return name
            }
            name = "/tmp/${getName()}-${i}.tmp"
            i++
        }
    }

    private fun getRemoteOperations(): RemoteOperations {
        return project.service()
    }
}
