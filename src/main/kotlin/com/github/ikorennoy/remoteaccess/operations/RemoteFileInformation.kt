package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.Outcome
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.project.Project
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo

class RemoteFileInformation(
    private val remoteFile: RemoteResourceInfo,
    val project: Project,
) {

    // all these operations can require a request to a server
    private val myParent: RemoteFileInformation? by lazy { getParentInternal() }
    private val myChildren: Lazy<Outcome<Array<RemoteFileInformation>>> = lazy { getChildrenInternal() }
    private val isDir: Boolean by lazy { isDirInternal() }
    private val special: Boolean by lazy { isSpecialInternal() }
    private val myLength: Long by lazy { getLengthInternal() }
    // for symlinks
    private val originalFileAttr: FileAttributes? by lazy { resolveSymlinkAttributes() }

    fun getName(): String = remoteFile.name

    fun getPath(): String = remoteFile.path

    fun isDirectory(): Boolean = isDir

    fun getChildren(): Lazy<Outcome<Array<RemoteFileInformation>>> = myChildren

    fun getParent(): RemoteFileInformation? = myParent

    fun getLength(): Long = myLength

    fun getPresentableLength(): String {
        val lenBytes = getLength()
        return if (lenBytes < 1000) {
            "$lenBytes bytes"
        } else if (lenBytes > 1000 && lenBytes < 1000 * 1000) {
            "${lenBytes / 1000} kb"
        } else {
            "${lenBytes / 1000 / 1000} mb"
        }
    }

    fun getPresentableName(): String = remoteFile.name

    fun isHidden(): Boolean = remoteFile.name.startsWith(".")

    fun isSymlink(): Boolean = remoteFile.attributes.type == FileMode.Type.SYMLINK

    fun getUri(): String {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        return "sftp://${conf.username}@${conf.host}${remoteFile.path}"
    }

    fun isSpecial(): Boolean {
        return special
    }

    fun isPlainFile(): Boolean {
        return !isDirectory() && !isSymlink() && !isHidden() && !isSpecial()
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

    private fun resolveSymlinkAttributes(): FileAttributes? {
        return when (val res = RemoteOperations.getInstance(project).resolveOriginalFileAttributes(getPath())) {
            is Ok -> res.value
            is Er -> {
                null
            }
        }
    }

    private fun getChildrenInternal(): Outcome<Array<RemoteFileInformation>> {
        return RemoteOperations.getInstance(project).getChildren(this)
    }

    private fun getParentInternal(): RemoteFileInformation? {
        return when (val res = RemoteOperations.getInstance(project).getParent(this)) {
            is Ok -> res.value
            is Er -> {
                null
            }
        }
    }

    private fun isDirInternal(): Boolean {
        return if (remoteFile.attributes.type == FileMode.Type.SYMLINK) {
            originalFileAttr?.type == FileMode.Type.DIRECTORY
        } else {
            remoteFile.attributes.type == FileMode.Type.DIRECTORY
        }
    }

    private fun getLengthInternal(): Long {
        return if (!isSymlink()) {
            remoteFile.attributes.size
        } else {
            originalFileAttr?.size ?: remoteFile.attributes.size
        }
    }

    private fun isSpecialInternal(): Boolean {
        // first just check if it's apparently special
        return if (checkSpecial(remoteFile.attributes.type)) {
            true
        } else if (isSymlink()) {
            // then check if it's a symlink we need to resolve
            val originalAttrs = originalFileAttr ?: return true
            checkSpecial(originalAttrs.type)
        } else {
            false
        }
    }

    private fun checkSpecial(type: FileMode.Type): Boolean {
        return type == FileMode.Type.BLOCK_SPECIAL ||
                type == FileMode.Type.CHAR_SPECIAL ||
                type == FileMode.Type.FIFO_SPECIAL ||
                type == FileMode.Type.SOCKET_SPECIAL
    }
}
