package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VFileProperty
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo

class RemoteFileInformation(
    private val remoteFile: RemoteResourceInfo,
    val project: Project,
) {

    private val myParent: RemoteFileInformation? by lazy { getParentInternal() }
    private val myChildren: Array<RemoteFileInformation> by lazy { getChildrenInternal() }
    private val isDir: Boolean by lazy { isDirInternal() }

    fun getName(): String = remoteFile.name

    fun getPath(): String = remoteFile.path

    fun isDirectory(): Boolean = isDir

    fun getChildren(): Array<RemoteFileInformation> = myChildren

    fun getParent(): RemoteFileInformation? = myParent

    fun getLength(): Long = remoteFile.attributes.size

    fun getPresentableName(): String = remoteFile.name

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

    private fun isDirInternal(): Boolean {
        return if (remoteFile.attributes.type == FileMode.Type.SYMLINK) {
            val originalAttrs = RemoteOperations.getInstance(project).getFileAttributes(getPath())
            originalAttrs?.type == FileMode.Type.DIRECTORY
        } else {
            remoteFile.attributes.type == FileMode.Type.DIRECTORY
        }
    }

    private fun getChildrenInternal(): Array<RemoteFileInformation> {
        return when (val res = RemoteOperations.getInstance(project).getChildren(this)) {
            is Ok -> res.value
            is Er -> {
                RemoteOperationsNotifier.getInstance(project).cannotLoadChildren(this.getName(), res.error)
                emptyArray()
            }
        }
    }

    private fun getParentInternal(): RemoteFileInformation? {
        return when (val res = RemoteOperations.getInstance(project).getParent(this)) {
            is Ok -> res.value
            is Er -> {
                null
            }
        }
    }
}
