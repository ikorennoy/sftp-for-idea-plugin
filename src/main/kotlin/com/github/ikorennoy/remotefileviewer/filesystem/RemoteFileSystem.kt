package com.github.ikorennoy.remotefileviewer.filesystem

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.naming.OperationNotSupportedException
import kotlin.math.min

// todo check that I can read and edit symlink/hardlink file
//  check that I can correctly identify symlink dir
class RemoteFileSystem : VirtualFileSystem() {

    private val topic = ApplicationManager.getApplication().messageBus.syncPublisher(VirtualFileManager.VFS_CHANGES)
    private val writeOperationOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)

    fun getRoot(): VirtualFile {
        val conf = service<RemoteFileViewerSettingsState>()
        return findFileByPath(conf.root)
    }

    fun getChildren(file: RemoteVirtualFile): Array<VirtualFile> {
        val sftp = getSftpClient()
        return sftp.ls(file.path).map {
            RemoteVirtualFile(it, this)
        }.toTypedArray()
    }

    fun exists(file: VirtualFile): Boolean {
        val sftp = getSftpClient()
        return sftp.statExistence(file.path) != null
    }

    fun getParent(file: RemoteVirtualFile): VirtualFile? {
        val components = getComponents(file.path)
        return if (components.parent == "") {
            null
        } else {
            val sftp = getSftpClient()
            RemoteVirtualFile(RemoteResourceInfo(getComponents(components.parent), sftp.stat(components.parent)), this)
        }
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile {
        val sftp = getSftpClient()
        try {
            return RemoteVirtualFile(RemoteResourceInfo(getComponents(path), sftp.stat(path)), this)
        } catch (ex: IOException) {
            println("Can't find a file: $path")
            throw ex
        }
    }

    override fun refresh(asynchronous: Boolean) {
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile {
        return findFileByPath(path)
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {

    }

    override fun removeVirtualFileListener(listener: VirtualFileListener) {
    }

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        val event = listOf(VFileDeleteEvent(requestor, vFile, false))
        if (vFile.isWritable) {
            val sftp = getSftpClient()
            topic.before(event)
            if (vFile.isDirectory) {
                sftp.rmdir(vFile.path)
            } else {
                sftp.rm(vFile.path)
            }
            topic.after(event)
        }
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        val sftp = getSftpClient()
        val moveEvent = listOf(VFileMoveEvent(requestor, vFile, newParent))
        topic.before(moveEvent)
        sftp.rename(vFile.path, newParent.path)
        topic.after(moveEvent)
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        val sftp = getSftpClient()
        val event =
            listOf(VFilePropertyChangeEvent(requestor, vFile, VirtualFile.PROP_NAME, vFile.name, newName, false))
        topic.before(event)
        sftp.rename(vFile.path, newName)
        topic.after(event)
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        val sftp = getSftpClient()
        val event = listOf(VFileCreateEvent(requestor, vDir, fileName, false, null, null, false, emptyArray()))
        topic.before(event)
        val result = sftp.open(vDir.path + "/" + fileName, setOf(OpenMode.CREAT)).convert()
        topic.after(event)
        return result
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        val sftp = getSftpClient()
        val dirPath = vDir.path + "/" + dirName
        sftp.mkdir(dirPath)
        return sftp.open(dirPath).convert()
    }

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile {
        throw OperationNotSupportedException("copy")
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    fun fileInputStream(remoteVirtualFile: RemoteVirtualFile): InputStream {
        val sftp = getSftpClient()
        return RemoteFileInputStream(sftp.open(remoteVirtualFile.path))
    }

    fun getComponents(path: String): PathComponents {
        val sftp = getSftpClient()
        return sftp.sftpEngine.pathHelper.getComponents(path)
    }

    private fun RemoteFile.convert(): RemoteVirtualFile {
        val sftp = getSftpClient()
        return RemoteVirtualFile(
            RemoteResourceInfo(sftp.sftpEngine.pathHelper.getComponents(path), fetchAttributes()),
            this@RemoteFileSystem
        )
    }

    private fun getSftpClient(): SFTPClient {
        return service<RemoteConnectionManager>().getSftpClient()
    }

    private fun getSessionClient(): Session {
        return service<RemoteConnectionManager>().getSessionClient()
    }

    fun resolveSymlink(file: RemoteVirtualFile): FileAttributes {
        val client = getSftpClient()
        return client.stat(file.path)
    }

    fun openTempFile(forFile: RemoteVirtualFile): OutputStream {
        val client = getSftpClient()
        return RemoteFileOutputStream(client.open(getTmpName(forFile), writeOperationOpenFlags))
    }

    // we don't want to rebuild file tree
    // the operation is used only to transfer file to remote
    fun removeFile(file: RemoteVirtualFile) {
        val client = getSftpClient()
        if (!file.isDirectory) {
            client.rm(file.path)
        }
    }

    fun renameTempFile(forFile: RemoteVirtualFile) {
        val client = getSftpClient()
        client.rename(getTmpName(forFile), forFile.path)
    }


    companion object {
        const val PROTOCOL = "remoteFileSysSftp"

        private fun getTmpName(file: RemoteVirtualFile): String {
            return "/tmp/${file.name}.tmp"
        }

        fun getInstance(): RemoteFileSystem {
            return VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as RemoteFileSystem
        }
    }
}

// closes internal file on close
internal class RemoteFileInputStream(private val remoteFile: RemoteFile): InputStream() {
    private val b = ByteArray(1)

    private var fileOffset: Long = 0
    private var markPos: Long = 0
    private var readLimit: Long = 0

    override fun markSupported(): Boolean {
        return true
    }

    override fun mark(readLimit: Int) {
        this.readLimit = readLimit.toLong()
        markPos = fileOffset
    }

    override fun reset() {
        fileOffset = markPos
    }

    override fun skip(n: Long): Long {
        val fileLength: Long = remoteFile.length()
        val previousFileOffset = fileOffset
        fileOffset = min((fileOffset + n).toDouble(), fileLength.toDouble()).toLong()
        return fileOffset - previousFileOffset
    }

    override fun read(): Int {
        return if (read(b, 0, 1) == -1) -1 else b[0].toInt() and 0xff
    }

    override fun read(into: ByteArray, off: Int, len: Int): Int {
        val read: Int = this.remoteFile.read(fileOffset, into, off, len)
        if (read != -1) {
            fileOffset += read.toLong()
            if (markPos != 0L && read > readLimit) {
                markPos = 0
            }
        }
        return read
    }

    override fun close() {
        remoteFile.close()
    }
}

// closes internal file on close
internal class RemoteFileOutputStream(private val remoteFile: RemoteFile): OutputStream() {

    private val b = ByteArray(1)
    private var fileOffset: Long = 0

    override fun write(w: Int) {
        b[0] = w.toByte()
        write(b, 0, 1)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        remoteFile.write(fileOffset, buf, off, len)
        fileOffset += len.toLong()
    }

    override fun close() {
        remoteFile.close()
    }
}


sealed interface RequestResult {
    val message: String
}

class CannotFindHost(override val message: String) : RequestResult

class UsernameOrPassword(override val message: String) : RequestResult

class UnknownRequestError(override val message: String) : RequestResult

class Ok(override val message: String = "Ok") : RequestResult
