package com.github.ikorennoy.remotefileviewer.remote

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.github.ikorennoy.remotefileviewer.filesystem.RemoteVirtualFile
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import java.awt.EventQueue
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

@Service
class RemoteOperations {

    @Volatile
    private var sftpClient: SFTPClient? = null

    @Volatile
    private var client: SSHClient? = null

    private val lock = ReentrantLock()

    fun isInitializedAndConnected(): Boolean {
        val currentClient = client ?: return false
        return currentClient.isConnected && currentClient.isAuthenticated
    }

    /**
     * Ensures that the client is connected and authenticated
     */
    fun init(): Boolean {
        var localClient = sftpClient

        if (localClient != null) {
            return sshClientIsOk()
        }
        try {
            lock.lock()
            localClient = sftpClient
            if (localClient != null) {
                return sshClientIsOk()
            }

            val configuration = service<RemoteFileViewerSettingsState>()

            tryConnect(configuration.host, configuration.port, configuration.username, configuration.password)
            val clientVal = client
            return if (clientVal == null) {
                // initialization is completely failed, just return false, user is notified by tryConnect
                false
            } else {
                val newSftpClient = clientVal.newSFTPClient()
                sftpClient = newSftpClient
                true
            }
        } finally {
            lock.unlock()
        }
    }

    fun disconnect() {
        assertNotEdt()
        try {
            lock.lock()
            val sft = sftpClient ?: return
            val ssh = client ?: throw IllegalStateException("Sfp client is not null, but ssh is null")
            sft.close()
            ssh.close()
            sftpClient = null
            client = null
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            lock.unlock()
        }
    }

    fun getChildren(remotePath: RemoteVirtualFile): Array<RemoteVirtualFile> {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            val remoteFs = RemoteFileSystem.getInstance()
            client.ls(remotePath.path)
                .map { RemoteVirtualFile(it, remoteFs) }
                .toTypedArray()
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't open a directory '${remotePath}' ${ex.message}",
                    "Error"
                )
            }
            emptyArray()
        }
    }

    fun exists(remotePath: String): Boolean {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            client.statExistence(remotePath) != null
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't execute an operation '${remotePath}' ${ex.message}",
                    "Error"
                )
            }
            false
        }
    }

    fun getParent(remotePath: String): RemoteVirtualFile? {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            val fs = RemoteFileSystem.getInstance()
            val components = getPathComponents(remotePath)
            if (components.parent == "") {
                null
            } else {
                RemoteVirtualFile(
                    RemoteResourceInfo(
                        getPathComponents(components.parent),
                        client.stat(components.parent)
                    ), fs
                )

            }
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't get a parent for '${remotePath}' ${ex.message}",
                    "Error"
                )
            }
            null
        }
    }

    fun findFileByPath(path: String): RemoteVirtualFile? {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            val fs = RemoteFileSystem.getInstance()
            RemoteVirtualFile(RemoteResourceInfo(getPathComponents(path), client.stat(path)), fs)
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't find a file for path '${path}' ${ex.message}",
                    "Error"
                )
            }
            null
        }
    }

    fun remove(file: RemoteVirtualFile) {
        assertNotEdt()
        var entity: String? = null
        try {
            val client = getSftpClient()
            if (file.isDirectory) {
                entity = "directory"
                client.rmdir(file.path)
            } else {
                entity = "file"
                client.rm(file.path)
            }
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't remove a $entity with the path '${file.path}' ${ex.message}",
                    "Error"
                )
            }
        }
    }

    fun rename(fromPath: String, toPath: String) {
        assertNotEdt()
        try {
            val client = getSftpClient()
            client.rename(fromPath, toPath)
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't rename a file with path '${fromPath}' to path '${toPath}' ${ex.message}",
                    "Error"
                )
            }
        }
    }

    fun createChildFile(parent: RemoteVirtualFile, newFileName: String): RemoteVirtualFile {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            val realPath = client.canonicalize(parent.path)
            val newFile = client.open("$realPath/$newFileName", setOf(OpenMode.CREAT, OpenMode.TRUNC))
            val newFilePath = newFile.path
            newFile.close()
            RemoteVirtualFile(
                RemoteResourceInfo(getPathComponents(newFilePath), client.stat(newFilePath)),
                RemoteFileSystem.getInstance()
            )
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't create new file in '${parent.path}' ${ex.message}",
                    "Error"
                )
            }
            throw ex
        }
    }

    fun createChildDirectory(parent: RemoteVirtualFile, newDirName: String): RemoteVirtualFile {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            val parentDirCanonicalPath = client.canonicalize(parent.path)
            val newDirPath = "$parentDirCanonicalPath/$newDirName"
            client.mkdir(newDirPath)
            val newDirStat = client.stat(newDirPath)
            RemoteVirtualFile(
                RemoteResourceInfo(getPathComponents(newDirPath), newDirStat),
                RemoteFileSystem.getInstance()
            )
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't create new directory in '${parent.path}' ${ex.message}",
                    "Error"
                )
            }
            throw ex
        }
    }

    fun fileInputStream(file: RemoteVirtualFile): InputStream {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            RemoteFileInputStream(client.open(file.path))
        } catch (ex: SFTPException) {
            throw ex
        }
    }

    fun getFileAttributes(file: String): FileAttributes {
        assertNotEdt()
        return try {
            val client = getSftpClient()
            client.stat(file)
        } catch (ex: SFTPException) {
            throw ex
        }
    }

    fun getPathComponents(path: String): PathComponents {
        assertNotEdt()
        val sftp = getSftpClient()
        return sftp.sftpEngine.pathHelper.getComponents(path)
    }

    fun getSftpClient(): SFTPClient {
        var res = sftpClient

        if (res != null) {
            return res
        }

        if (init()) {
            res = sftpClient ?: throw IllegalStateException("Can't be null after successful initialization")
            return res
        } else {
            // it means one of the followings states
            // 1. user somehow cancelled initialization, probably by clicking cancel on password prompt or in conf
            // in that case we don't need to do anything
            // 2. initialization is failed, in that case user is already notified by a message window
            // 3. user has corrupted settings, but we asked only password and failed, in that case we need to open full
            // settings and ask to fix them
            throw IllegalStateException("Can't connect")
        }
    }

    private fun assertNotEdt() {
        thisLogger().assertTrue(
            !EventQueue.isDispatchThread() || ApplicationManager.getApplication().isUnitTestMode,
            "Must not be executed on Event Dispatch Thread"
        )
    }

    private fun sshClientIsOk(): Boolean {
        val clientVal = client ?: return false
        return clientVal.isConnected && clientVal.isAuthenticated
    }

    private fun tryConnect(host: String, port: Int, username: String, password: CharArray) {
        val project = ProjectManager.getInstance().defaultProject // todo find a way to get current project
        var failReason: Exception? = null
        CommandProcessor.getInstance()
            .executeCommand(project, {
                object : Task.Modal(project, "Connecting to: ${username}@${host}:${port}", false) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        val client = SSHClient()
                        try {
                            client.useCompression()
                            client.loadKnownHosts()
                            client.connect(host, port)
                            client.authPassword(username, password)
                            this@RemoteOperations.client = client
                        } catch (ex: IOException) {
                            try {
                                client.close()
                            } catch (_: IOException) {
                            }
                            failReason = ex
                        }
                    }

                    override fun onFinished() {
                        reportError()
                    }

                    private fun reportError() {
                        if (failReason != null) {
                            Messages.showMessageDialog(
                                "Cannot not connect to ${username}@${host}:${port}\n ${failReason?.javaClass}",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
                    }
                }.queue()
            }, "Connecting...", null)
    }

}

internal class RemoteFileInputStream(private val remoteFile: RemoteFile) : InputStream() {
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