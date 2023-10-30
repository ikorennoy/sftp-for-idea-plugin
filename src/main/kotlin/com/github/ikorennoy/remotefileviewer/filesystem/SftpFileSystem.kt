package com.github.ikorennoy.remotefileviewer.filesystem

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.ex.http.HttpFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.util.PathUtil
import com.intellij.util.io.URLUtil
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import net.schmizz.sshj.userauth.UserAuthException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.UnknownHostException
import javax.naming.OperationNotSupportedException


class SftpFileSystem : VirtualFileSystem() {

    private val client = SSHClient()
    lateinit var root: VirtualFile
    private lateinit var sftp: SFTPClient

    private val topic = ApplicationManager.getApplication().messageBus.syncPublisher(VirtualFileManager.VFS_CHANGES)

    private val openFileCache: LoadingCache<String, RemoteFile> = Caffeine.newBuilder()
            .maximumSize(1000)
            .build { k -> sftp.open(k, setOf(OpenMode.WRITE, OpenMode.READ)) }

    fun getChildren(file: SftpVirtualFile): Array<VirtualFile> {
        return sftp.ls(file.path).map {
            SftpVirtualFile(it, this)
        }.toTypedArray()
    }

    fun exists(file: VirtualFile): Boolean {
        return sftp.statExistence(file.path) != null
    }

    fun getParent(file: SftpVirtualFile): VirtualFile? {
        val components = getComponents(file.path)
        return if (components.parent == "") {
            null
        } else {
            SftpVirtualFile(RemoteResourceInfo(getComponents(components.parent), sftp.stat(components.parent)), this)
        }
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile {
        try {
            return SftpVirtualFile(RemoteResourceInfo(getComponents(path), sftp.stat(path)), this)
        } catch (ex: IOException) {
            println("Can't find file: $path")
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
            topic.before(event)
            sftp.rm(vFile.path)
            topic.after(event)
        }
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        sftp.rename(vFile.path, newParent.path)
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        sftp.rename(vFile.path, newName)
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        return sftp.open(vDir.path + "/" + fileName, setOf(OpenMode.CREAT)).convert()
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        val dirPath = vDir.path + "/" + dirName
        sftp.mkdir(dirPath)
        return sftp.open(dirPath).convert()
    }

    override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
        throw OperationNotSupportedException("copy")
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    fun fileOutputStream(sftpVirtualFile: SftpVirtualFile): OutputStream {
        return sftp.open(sftpVirtualFile.path, setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).RemoteFileOutputStream()
    }

    fun fileInputStream(sftpVirtualFile: SftpVirtualFile): InputStream {
        return openFileCache.get(sftpVirtualFile.path).RemoteFileInputStream()
    }

    private fun RemoteFile.convert(): SftpVirtualFile {
        return SftpVirtualFile(RemoteResourceInfo(sftp.sftpEngine.pathHelper.getComponents(path), fetchAttributes()), this@SftpFileSystem)
    }

    private fun getComponents(path: String): PathComponents {
        return sftp.sftpEngine.pathHelper.getComponents(path)
    }

    fun init(host: String, port: Int, root: String, username: String, password: CharArray): RequestResult {
        return try {
            client.loadKnownHosts()
            client.connect(host, port)
            client.authPassword(username, password)
            sftp = client.newSFTPClient()
            this.root = findFileByPath(root)
            Ok()
        } catch (ex: UnknownHostException) {
            CannotFindHost("Cannot connect to the host: '${ex.message}'")
        } catch (ex: UserAuthException) {
            UsernameOrPassword("Username or password is incorrect")
        } catch (ex: IOException) {
            UnknownRequestError("Unknown error")
        }
    }

    fun isReady(): Boolean {
        return client.isConnected && client.isAuthenticated
    }

    companion object {
        const val PROTOCOL = "remoteFileSysSftp"

        fun getInstance(): SftpFileSystem {
            return VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as SftpFileSystem
        }
    }
}

sealed interface RequestResult {
    val message: String
}

class CannotFindHost(override val message: String) : RequestResult

class UsernameOrPassword(override val message: String): RequestResult

class UnknownRequestError(override val message: String): RequestResult

class Ok(override val message: String = "Ok") : RequestResult
