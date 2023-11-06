package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.Outcome
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.sftp.*
import java.awt.EventQueue
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

@Service(Service.Level.PROJECT)
class RemoteOperations(private val project: Project) {

    private val createAndOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.EXCL)
    private val openOutputStreamFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)

    private val connectionHolder: ConnectionHolder
        get() = ConnectionHolder.getInstance()

    private val sftpClient: SFTPClient
        get() = connectionHolder.getSftpClient()

    /**
     * Ensures that the client is connected and authenticated
     */
    fun initSilently(): Exception? {
        assertNotEdt()
        return connectionHolder.connect(project)
    }

    fun initWithModalDialogue() {
        val configuration = RemoteFileAccessSettingsState.getInstance()
        val host = configuration.host
        val port = configuration.port
        val username = configuration.username
        CommandProcessor.getInstance()
            .executeCommand(project, {
                var failReason: Exception? = null
                object : Task.Modal(
                    project,
                    "Connecting to: ${username}@${host}:${port}",
                    false
                ) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        failReason = connectionHolder.connect(project)
                    }

                    override fun onFinished() {
                        reportError()
                    }

                    private fun reportError() {
                        if (failReason != null) {
                            Messages.showMessageDialog(
                                "Cannot not connect to ${username}@${host}:${port}\n ${failReason?.message}",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
                    }
                }.queue()
            }, "Connecting...", null)
    }

    fun isInitializedAndConnected(): Boolean {
        return connectionHolder.isInitializedAndConnected()
    }

    fun getChildren(remoteFile: RemoteFileInformation): Outcome<Array<RemoteFileInformation>> {
        assertNotEdt()
        return try {
            Ok(sftpClient.ls(remoteFile.getPath())
                .map { RemoteFileInformation(it, project) }
                .toTypedArray())
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun getParent(remotePath: RemoteFileInformation): Outcome<RemoteFileInformation?> {
        assertNotEdt()
        return try {
            val components = getPathComponents(remotePath.getPath())
            if (components.parent == "") {
                Ok(null)
            } else {
                Ok(
                    RemoteFileInformation(
                        RemoteResourceInfo(getPathComponents(components.parent), sftpClient.stat(components.parent)),
                        project,
                    )
                )
            }
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun findFileByPath(path: String): Outcome<RemoteFileInformation> {
        assertNotEdt()
        return try {
            Ok(RemoteFileInformation(RemoteResourceInfo(getPathComponents(path), sftpClient.stat(path)), project))
        } catch (ex: SFTPException) {
            Er(ex)
        }
    }

    fun remove(file: RemoteFileInformation): Outcome<Unit> {
        assertNotEdt()
        return try {
            val client = sftpClient
            if (file.isDirectory()) {
                client.rmdir(file.getPath())
            } else {
                client.rm(file.getPath())
            }
            Ok(Unit)
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun rename(fromPath: RemoteFileInformation, toPath: RemoteFileInformation): Outcome<Unit> {
        assertNotEdt()
        return try {
            sftpClient.rename(fromPath.getPath(), toPath.getPath())
            Ok(Unit)
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun createChildFile(parent: RemoteFileInformation, newFileName: String): Outcome<RemoteFileInformation> {
        assertNotEdt()
        val newFileFullPath: String
        return try {
            val client = sftpClient
            val realParentPath = client.canonicalize(parent.getPath())
            newFileFullPath = computeNewPath(realParentPath, newFileName)
            val newFile = client.open(newFileFullPath, setOf(OpenMode.CREAT, OpenMode.TRUNC))
            val newFilePath = newFile.path
            newFile.close()
            Ok(
                RemoteFileInformation(
                    RemoteResourceInfo(getPathComponents(newFilePath), client.stat(newFilePath)),
                    project,
                )
            )
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun createChildDirectory(parent: RemoteFileInformation, newDirName: String): Outcome<RemoteFileInformation> {
        assertNotEdt()
        val newDirPathFullPath: String
        return try {
            val client = sftpClient
            val realParentPath = client.canonicalize(parent.getPath())
            newDirPathFullPath = computeNewPath(realParentPath, newDirName)
            client.mkdir(newDirPathFullPath)
            val newDirStat = client.stat(newDirPathFullPath)
            Ok(
                RemoteFileInformation(
                    RemoteResourceInfo(getPathComponents(newDirPathFullPath), newDirStat),
                    project
                )
            )
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun fileInputStream(filePath: RemoteFileInformation): Outcome<InputStream> {
        assertNotEdt()
        return try {
            Ok(RemoteFileInputStream(sftpClient.open(filePath.getPath())))
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun fileOutputStream(filePath: RemoteFileInformation): Outcome<OutputStream> {
        assertNotEdt()
        return try {
            Ok(RemoteFileOutputStream(sftpClient.open(filePath.getPath(), openOutputStreamFlags)))
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun createAndOpenFile(filePath: String): Outcome<RemoteFileInformation> {
        assertNotEdt()
        return try {
            val newFile = sftpClient.open(filePath, createAndOpenFlags)
            val result = RemoteFileInformation(
                RemoteResourceInfo(getPathComponents(newFile.path), newFile.fetchAttributes()),
                project
            )
            newFile.close()
            Ok(result)
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun prepareTempFile(forFile: RemoteFileInformation): Outcome<RemoteFileInformation> {
        var attempt = 0
        var name = "/tmp/${forFile.getName()}.tmp"
        while (true) {
            val result = createAndOpenFile(name)
            when (result) {
                is Ok -> return result
                is Er -> {
                    attempt++
                    name = "/tmp/${forFile.getName()}-${attempt}.tmp"
                }
            }
            // try 5 times
            if (attempt == 4) {
                return result
            }
        }
    }

    fun getFileAttributes(file: String): FileAttributes? {
        assertNotEdt()
        return try {
            sftpClient.stat(file)
        } catch (ex: IOException) {
            return null
        }
    }

    fun close() {
        assertNotEdt()
        connectionHolder.disconnect()
    }

    private fun getPathComponents(path: String): PathComponents {
        assertNotEdt()
        return try {
            sftpClient.sftpEngine.pathHelper.getComponents(path)
        } catch (ex: IOException) {
            throw ex
        }
    }

    private fun computeNewPath(parentPath: String, newName: String): String {
        return if (parentPath == "/") {
            "$parentPath$newName"
        } else {
            "$parentPath/$newName"
        }
    }

    private fun assertNotEdt() {
        thisLogger().assertTrue(
            !EventQueue.isDispatchThread() || ApplicationManager.getApplication().isUnitTestMode,
            "Must not be executed on Event Dispatch Thread"
        )
    }

    companion object {
        fun getInstance(project: Project): RemoteOperations = project.service()
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
        fileOffset = min((fileOffset + n), fileLength)
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
