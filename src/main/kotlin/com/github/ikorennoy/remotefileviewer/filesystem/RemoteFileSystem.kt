package com.github.ikorennoy.remotefileviewer.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.EditSourceOnDoubleClickHandler
import org.apache.sshd.client.SshClient
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


private const val PROTOCOL = "remoteFileSysSftp"

class RemoteFileSystem: VirtualFileSystem() {

    val fs: FileSystem
    val root: RemoteVirtualFile

    init {
        val client = SshClient.setUpDefaultClient()
        client.start()
        val fsProvider = SftpFileSystemProvider(client)
        val uri = SftpFileSystemProvider.createFileSystemURI("localhost", 22, "ik", "")
        fs = fsProvider.newFileSystem(uri, emptyMap<String, Any>())
        root = RemoteVirtualFile(fs.rootDirectories.first(), this)
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile {
        return RemoteVirtualFile(fs.getPath(path), this)
    }

    override fun refresh(asynchronous: Boolean) {
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile {
        return RemoteVirtualFile(fs.getPath(path), this)
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {

    }

    override fun removeVirtualFileListener(listener: VirtualFileListener) {
    }

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        Files.delete(vFile.toNioPath())
    }

    override fun getNioPath(file: VirtualFile): Path {
        if (file is RemoteVirtualFile) {
            return file.path
        } else {
            throw Exception("wrong file: $file")
        }
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        Files.move(vFile.toNioPath(), newParent.toNioPath())
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        Files.move(vFile.toNioPath(), fs.getPath(newName))
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        return RemoteVirtualFile(Files.createFile(fs.getPath(vDir.path, fileName)), this)
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        return RemoteVirtualFile(Files.createDirectory(fs.getPath(vDir.path, dirName)), this)
    }

    override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
        return RemoteVirtualFile(Files.copy(virtualFile.toNioPath(), newParent.toNioPath()), this)
    }

    override fun isReadOnly(): Boolean {
        return false
    }
}
