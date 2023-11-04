package com.github.ikorennoy.remotefileviewer.remote

import com.github.ikorennoy.remotefileviewer.utils.Er
import com.github.ikorennoy.remotefileviewer.utils.Ok
import com.github.ikorennoy.remotefileviewer.utils.Outcome
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.sftp.*
import java.awt.EventQueue
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

@Service
class RemoteOperations {

    private val writeOperationOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
    private val connectionHolder = ConnectionHolder()

    fun isInitializedAndConnected(): Boolean {
        return connectionHolder.isInitializedAndConnected()
    }

    private val sftpClientNew: SFTPClient
        get() = connectionHolder.getSftpClient()

    /**
     * Ensures that the client is connected and authenticated
     */
    fun init(): Boolean {
        return connectionHolder.connect()
    }

    fun disconnect() {
        assertNotEdt()
        connectionHolder.disconnect()
    }

    fun getChildren(remotePath: String): Outcome<Array<RemoteVirtualFile>> {
        assertNotEdt()
        return try {
            Ok(sftpClientNew.ls(remotePath)
                .map { RemoteVirtualFile(it) }
                .toTypedArray())
        } catch (ex: SFTPException) {
            Er(ex)
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
            val components = getPathComponents(remotePath)
            if (components.parent == "") {
                null
            } else {
                RemoteVirtualFile(
                    RemoteResourceInfo(
                        getPathComponents(components.parent),
                        client.stat(components.parent)
                    )
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
            RemoteVirtualFile(RemoteResourceInfo(getPathComponents(path), client.stat(path)))
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
            if (file.isDirectory()) {
                entity = "directory"
                client.rmdir(file.getPath())
            } else {
                entity = "file"
                client.rm(file.getPath())
            }
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't remove a $entity with the path '${file.getPath()}' ${ex.message}",
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
            val realPath = client.canonicalize(parent.getPath())
            val newFile = client.open("$realPath/$newFileName", setOf(OpenMode.CREAT, OpenMode.TRUNC))
            val newFilePath = newFile.path
            newFile.close()
            RemoteVirtualFile(
                RemoteResourceInfo(getPathComponents(newFilePath), client.stat(newFilePath))
            )
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't create new file in '${parent.getPath()}' ${ex.message}",
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
            val parentDirCanonicalPath = client.canonicalize(parent.getPath())
            val newDirPath = "$parentDirCanonicalPath/$newDirName"
            client.mkdir(newDirPath)
            val newDirStat = client.stat(newDirPath)
            RemoteVirtualFile(
                RemoteResourceInfo(getPathComponents(newDirPath), newDirStat)
            )
        } catch (ex: SFTPException) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    "Can't create new directory in '${parent.getPath()}' ${ex.message}",
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
            RemoteFileInputStream(client.open(file.getPath()))
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

    private fun assertNotEdt() {
        thisLogger().assertTrue(
            !EventQueue.isDispatchThread() || ApplicationManager.getApplication().isUnitTestMode,
            "Must not be executed on Event Dispatch Thread"
        )
    }


    fun fileOutputStream(remoteVirtualFile: String): OutputStream {
        return RemoteFileOutputStream(getSftpClient().open(remoteVirtualFile, writeOperationOpenFlags))
    }


    private fun getSftpClient(): SFTPClient {
        return connectionHolder.getSftpClient()
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
