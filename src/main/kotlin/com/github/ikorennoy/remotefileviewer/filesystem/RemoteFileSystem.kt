package com.github.ikorennoy.remotefileviewer.filesystem

import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.intellij.openapi.components.service
import net.schmizz.sshj.sftp.*
import java.io.InputStream
import java.io.OutputStream

// todo check that I can read and edit symlink/hardlink file
//  check that I can correctly identify symlink dir
class RemoteFileSystem {

    private val writeOperationOpenFlags = setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)

    fun getChildren(file: RemoteVirtualFile): Array<RemoteVirtualFile> {
        return getRemoteOperations().getChildren(file)
    }

    fun getParent(file: RemoteVirtualFile): RemoteVirtualFile? {
        return  getRemoteOperations().getParent(file.getPath())
    }

    fun deleteFile(vFile: RemoteVirtualFile) {
        getRemoteOperations().remove(vFile)
    }

    fun createChildFile(vDir: RemoteVirtualFile, fileName: String): RemoteVirtualFile {
        val operations = getRemoteOperations()
        return operations.createChildFile(vDir, fileName)
    }

    fun createChildDirectory(vDir: RemoteVirtualFile, dirName: String): RemoteVirtualFile {
        return getRemoteOperations().createChildDirectory(vDir, dirName)
    }


    fun isReadOnly(): Boolean {
        return false
    }



    fun fileInputStream(file: RemoteVirtualFile): InputStream {
        return getRemoteOperations().fileInputStream(file)
    }

    fun getFileAttributes(file: RemoteVirtualFile): FileAttributes {
        return getRemoteOperations().getFileAttributes(file.getPath())
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
        if (!file.isDirectory()) {
            client.rm(file.getPath())
        }
    }

    fun renameTempFile(fromFile: String, toFile: String) {
        val client = getSftpClient()
        client.rename(fromFile, toFile)
    }

    private fun getTmpName(file: RemoteVirtualFile): String {
        val client = getSftpClient()
        var i = 0
        var name = "/tmp/${file.getName()}.tmp"
        while (true) {
            if (client.statExistence(name) == null) {
                return name
            }
            name = "/tmp/${file.getName()}-${i}.tmp"
            i++
        }
    }

    companion object {
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
