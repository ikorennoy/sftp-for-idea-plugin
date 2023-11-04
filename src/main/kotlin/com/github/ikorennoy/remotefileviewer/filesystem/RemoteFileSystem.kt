package com.github.ikorennoy.remotefileviewer.filesystem

import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import net.schmizz.sshj.sftp.*
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.naming.OperationNotSupportedException

// todo check that I can read and edit symlink/hardlink file
//  check that I can correctly identify symlink dir
class RemoteFileSystem : VirtualFileSystem() {

    private val topic = ApplicationManager.getApplication().messageBus.syncPublisher(VirtualFileManager.VFS_CHANGES)
    private val writeOperationOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
    private val openedFiles: Map<String, VirtualFile> = ConcurrentHashMap()

    fun getChildren(file: RemoteVirtualFile): Array<RemoteVirtualFile> {
        return getRemoteOperations().getChildren(file)
    }

    fun exists(file: VirtualFile): Boolean {
        return getRemoteOperations().exists(file.path)
    }

    fun getParent(file: RemoteVirtualFile): VirtualFile? {
        return  getRemoteOperations().getParent(file.path)
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        return getRemoteOperations().findFileByPath(path)
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    public override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        if (vFile !is RemoteVirtualFile) return
        getRemoteOperations().remove(vFile)
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        val operations = getRemoteOperations()
        val moveEvent = listOf(VFileMoveEvent(requestor, vFile, newParent))
        topic.before(moveEvent)
        operations.rename(vFile.path, newParent.path)
        topic.after(moveEvent)
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        if (vDir !is RemoteVirtualFile) throw IllegalArgumentException("Wrong VirtualFile: $vDir")
        val operations = getRemoteOperations()
        return operations.createChildFile(vDir, fileName)
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        if (vDir !is RemoteVirtualFile) throw IllegalArgumentException("Wrong VirtualFile $vDir")
        return getRemoteOperations().createChildDirectory(vDir, dirName)
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

    fun fileInputStream(file: RemoteVirtualFile): InputStream {
        return getRemoteOperations().fileInputStream(file)
    }

    fun getFileAttributes(file: RemoteVirtualFile): FileAttributes {
        return getRemoteOperations().getFileAttributes(file.path)
    }

    fun getComponents(path: String): PathComponents {
        return getRemoteOperations().getPathComponents(path)
    }

    private fun getRemoteOperations(): RemoteOperations {
        return service()
    }

    private fun getSftpClient(): SFTPClient {
        return service<RemoteOperations>().getSftpClient()
    }

    fun openTempFile(forFile: RemoteVirtualFile): Pair<OutputStream, String> {
        val client = getSftpClient()
        val tmpName = getTmpName(forFile)
        return RemoteFileOutputStream(client.open(tmpName, writeOperationOpenFlags)) to tmpName
    }

    // we don't want to rebuild file tree
    // the operation is used only to transfer file to remote
    fun removeFile(file: RemoteVirtualFile) {
        val client = getSftpClient()
        if (!file.isDirectory) {
            client.rm(file.path)
        }
    }

    fun renameTempFile(fromFile: String, toFile: String) {
        val client = getSftpClient()
        client.rename(fromFile, toFile)
    }

    private fun getTmpName(file: RemoteVirtualFile): String {
        val client = getSftpClient()
        var i = 0
        var name = "/tmp/${file.name}.tmp"
        while (true) {
            if (client.statExistence(name) == null) {
                return name
            }
            name = "/tmp/${file.name}-${i}.tmp"
            i++
        }
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw UnsupportedOperationException("renameFile")
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {

    }

    override fun removeVirtualFileListener(listener: VirtualFileListener) {
    }


    override fun refresh(asynchronous: Boolean) {
    }


    companion object {
        const val PROTOCOL = "remoteFileSysSftp"

        private val myInstance: RemoteFileSystem by lazy { RemoteFileSystem() }

        fun getInstance(): RemoteFileSystem {
            return myInstance
        }
    }
}

// closes internal file on close


// closes internal file on close
internal class RemoteFileOutputStream(private val remoteFile: RemoteFile) : OutputStream() {

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
