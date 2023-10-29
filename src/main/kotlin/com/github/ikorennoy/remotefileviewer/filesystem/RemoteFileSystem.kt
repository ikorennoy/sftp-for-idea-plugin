package com.github.ikorennoy.remotefileviewer.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.PathUtil
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import javax.naming.OperationNotSupportedException

private const val PROTOCOL = "remoteFileSysSftp"

class RemoteFileSystem : VirtualFileSystem() {

    val root: RemoteVirtualFile
    val sftp: SFTPClient

    init {
        val client = SSHClient()
        client.loadKnownHosts()
        client.connect("localhost")
        client.authPassword("ik", "")
        sftp = client.newSFTPClient()

        root = sftp.open("/").convert()
        sftp.sftpEngine.timeoutMs = 5000
    }

    fun getChildren(file: RemoteVirtualFile): Array<VirtualFile> {
        return sftp.ls(file.path).map {
            RemoteVirtualFile(it, this)
        }.toTypedArray()
    }

    fun getParent(file: RemoteVirtualFile): VirtualFile? {
        val parent = PathUtil.getParentPath(file.path)
        return if (parent == "") {
            null
        } else {
            RemoteVirtualFile(RemoteResourceInfo(getComponents(parent), sftp.stat(parent)), this)
        }
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile {
        try {
            return RemoteVirtualFile(RemoteResourceInfo(getComponents(path), sftp.stat(path)), this)
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
        sftp.rm(vFile.path)
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

    fun fileOutputStream(remoteVirtualFile: RemoteVirtualFile): OutputStream {
        return sftp.open(remoteVirtualFile.path, setOf(OpenMode.WRITE)).RemoteFileOutputStream()
    }

    fun fileInputStream(remoteVirtualFile: RemoteVirtualFile): InputStream {
        return sftp.open(remoteVirtualFile.path).RemoteFileInputStream()
    }

    private fun RemoteFile.convert(): RemoteVirtualFile {
        return RemoteVirtualFile(RemoteResourceInfo(sftp.sftpEngine.pathHelper.getComponents(path), fetchAttributes()), this@RemoteFileSystem)
    }

    private fun getComponents(path: String): PathComponents {
        return sftp.sftpEngine.pathHelper.getComponents(path)
    }
}
