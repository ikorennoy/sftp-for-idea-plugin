package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.Outcome
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import net.schmizz.sshj.sftp.*
import java.awt.EventQueue
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

@Service(Service.Level.PROJECT)
class RemoteOperations(private val project: Project) {

    private val createAndOpenIfNotExistFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.EXCL)
    private val openForWriteAndTruncateFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)

    private val connectionHolder: ConnectionHolder
        get() = ConnectionHolder.getInstance(project)

    private val sftpClient: SFTPClient
        get() = connectionHolder.getSftpClient()

    /**
     * Ensures that the client is connected and authenticated
     */
    fun initSilently(): Exception? {
        assertNotEdt()
        return connectionHolder.connect()
    }

    fun initWithModalDialogue() {
        val initWithModalDialogTask = InitWithModalDialogTask(project)
        CommandProcessor.getInstance().executeCommand(
            project, {
                initWithModalDialogTask.queue()
            }, RemoteFileAccessBundle.message("task.RemoteFileAccess.initWithModalDialogue.name"), null
        )
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
            Ok(RemoteFileOutputStream(sftpClient.open(filePath.getPath(), openForWriteAndTruncateFlags)))
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun prepareTempFile(forFile: RemoteFileInformation): Outcome<RemoteFileInformation> {
        assertNotEdt()
        var attempt = 0
        val parent = forFile.getParent() ?: return Er(IOException("Can't get a file ${forFile.getPath()} parent"))
        var tempFileAbsolutePath = prepareTempPath(parent.getPath(), forFile)
        while (true) {
            val res = createAndOpenFile(tempFileAbsolutePath)
            when (res) {
                is Ok -> return res
                is Er -> {
                    attempt++
                    tempFileAbsolutePath = prepareTempPath(parent.getPath(), forFile)
                }
            }
            // try 5 times
            if (attempt == 4) {
                return res
            }
        }
    }

    /**
     * We need to make sure that we have write and execute permissions for dir
     * And write permission for file. To check it we run:
     * test -w PARENT_DIR -a -x PARENT_DIR -a -w TARGET_FILE
     */
    fun isFileWritable(file: RemoteFileInformation): Outcome<Boolean> {
        assertNotEdt()
        return try {
            val parent = file.getParent() ?: return Ok(false)
            val session = connectionHolder.getSessionClient()
            val commandResult =
                session.exec("test -w ${parent.getPath()} -a -x ${parent.getPath()} -a -w ${file.getPath()}")
            commandResult.close()
            Ok(commandResult.exitStatus == 0)
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun resolveOriginalFileAttributes(symlinkPath: String): Outcome<FileAttributes> {
        assertNotEdt()
        return try {
            Ok(sftpClient.stat(symlinkPath))
        } catch (ex: IOException) {
            Er(ex)
        }
    }

    fun close() {
        assertNotEdt()
        connectionHolder.disconnect()
    }

    private fun createAndOpenFile(filePath: String): Outcome<RemoteFileInformation> {
        return try {
            val newFile = sftpClient.open(filePath, createAndOpenIfNotExistFlags)
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

    private fun prepareTempPath(parentPath: String, file: RemoteFileInformation): String {
        val tempFileName = if (file.isHidden()) {
            "${file.getName()}.tmp"
        } else {
            ".${file.getName()}.tmp"
        }

        return computeNewPath(parentPath, tempFileName)
    }

    private fun getPathComponents(path: String): PathComponents {
        return try {
            sftpClient.sftpEngine.pathHelper.getComponents(path)
        } catch (ex: IOException) {
            throw ex
        }
    }

    private fun computeNewPath(parentPath: String, newName: String): String {
        return if (parentPath.endsWith("/")) {
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
