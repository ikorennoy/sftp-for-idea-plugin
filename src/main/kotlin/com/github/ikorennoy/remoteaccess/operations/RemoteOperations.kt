package com.github.ikorennoy.remoteaccess.operations

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.sftp.*
import java.awt.EventQueue
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

@Service(Service.Level.PROJECT)
class RemoteOperations(private val project: Project): Disposable {

    private val writeOperationOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
    private val connectionHolder = ConnectionHolder()

    private val sftpClient: SFTPClient
        get() = connectionHolder.getSftpClient()

    private val notifier: RemoteOperationsNotifier
        get() = project.service()

    /**
     * Ensures that the client is connected and authenticated
     */
    fun init(): Boolean {
        return connectionHolder.connect()
    }

    fun close() {
        assertNotEdt()
        connectionHolder.disconnect()
    }

    fun isInitializedAndConnected(): Boolean {
        return connectionHolder.isInitializedAndConnected()
    }

    fun getChildren(remotePath: String): Array<RemoteFileInformation> {
        assertNotEdt()
        return try {
            sftpClient.ls(remotePath)
                .map { RemoteFileInformation(it, project) }
                .toTypedArray()
        } catch (ex: SFTPException) {
            notifier.cannotLoadChildren(ex)
            emptyArray()
        }
    }

    fun exists(remotePath: String): Boolean {
        assertNotEdt()
        return try {
            sftpClient.statExistence(remotePath) != null
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

    fun getParent(remotePath: String): RemoteFileInformation? {
        assertNotEdt()
        return try {
            val components = getPathComponents(remotePath)
            if (components.parent == "") {
                null
            } else {
                RemoteFileInformation(
                    RemoteResourceInfo(getPathComponents(components.parent), sftpClient.stat(components.parent)),
                    project,
                )
            }
        } catch (ex: SFTPException) {
            null
        }
    }

    fun findFileByPath(path: String): RemoteFileInformation? {
        assertNotEdt()
        return try {
            RemoteFileInformation(RemoteResourceInfo(getPathComponents(path), sftpClient.stat(path)), project)
        } catch (ex: SFTPException) {
            null
        }
    }

    fun remove(file: RemoteFileInformation) {
        assertNotEdt()
        var entity: String? = null
        try {
            val client = sftpClient
            if (file.isDirectory()) {
                entity = "directory"
                client.rmdir(file.getPath())
            } else {
                entity = "file"
                client.rm(file.getPath())
            }
        } catch (ex: SFTPException) {
            notifier.cannotDelete(file, ex, entity ?: "")
        }
    }

    fun rename(fromPath: String, toPath: String) {
        assertNotEdt()
        try {
            sftpClient.rename(fromPath, toPath)
        } catch (ex: SFTPException) {
            notifier.cannotRename(fromPath, toPath, ex)
        }
    }

    fun createChildFile(parent: RemoteFileInformation, newFileName: String): RemoteFileInformation? {
        assertNotEdt()
        var newFileFullPath: String? = null
        return try {
            val client = sftpClient
            val realParentPath = client.canonicalize(parent.getPath())
            newFileFullPath = computeNewPath(realParentPath, newFileName)
            val newFile = client.open(newFileFullPath, setOf(OpenMode.CREAT, OpenMode.TRUNC))
            val newFilePath = newFile.path
            newFile.close()
            RemoteFileInformation(
                RemoteResourceInfo(getPathComponents(newFilePath), client.stat(newFilePath)),
                project,
            )
        } catch (ex: SFTPException) {
            notifier.cannotCreateChildFile(newFileFullPath ?: newFileName, ex)
            return null
        }
    }

    fun createChildDirectory(parent: RemoteFileInformation, newDirName: String): RemoteFileInformation? {
        assertNotEdt()
        var newDirPathFullPath: String? = null
        return try {
            val client = sftpClient
            val realParentPath = client.canonicalize(parent.getPath())
            newDirPathFullPath = computeNewPath(realParentPath, newDirName)
            client.mkdir(newDirPathFullPath)
            val newDirStat = client.stat(newDirPathFullPath)
            RemoteFileInformation(
                RemoteResourceInfo(getPathComponents(newDirPathFullPath), newDirStat),
                project
            )
        } catch (ex: SFTPException) {
            notifier.cannotCreateChildDirectory(newDirPathFullPath ?: newDirName, ex)
            return null
        }
    }

    fun fileInputStream(filePath: String): InputStream? {
        assertNotEdt()
        return try {
            RemoteFileInputStream(sftpClient.open(filePath))
        } catch (ex: SFTPException) {
            notifier.cannotOpenFile(filePath, ex)
            return null
        }
    }

    fun fileOutputStream(filePath: String): OutputStream? {
        assertNotEdt()
        return try {
            RemoteFileOutputStream(sftpClient.open(filePath, writeOperationOpenFlags))
        } catch (ex: SFTPException) {
            notifier.cannotOpenFile(filePath, ex)
            null
        }
    }

    fun getFileAttributes(file: String): FileAttributes? {
        assertNotEdt()
        return try {
            sftpClient.stat(file)
        } catch (ex: SFTPException) {
            return null
        }
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

    override fun dispose() {
        connectionHolder.disconnect()
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
