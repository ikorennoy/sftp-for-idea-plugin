package com.github.ikorennoy.remotefileviewer.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.containers.toArray
import com.intellij.util.io.directoryStreamIfExists
import com.intellij.util.io.exists
import com.intellij.util.io.isWritable
import com.intellij.util.io.lastModified
import org.apache.sshd.sftp.client.fs.SftpPath
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class RemoteVirtualFile(
        val path: Path,
        val fs: RemoteFileSystem
) : VirtualFile() {

    val lock = ReentrantLock()


    override fun getName(): String {
        return path.name
    }

    override fun getFileSystem(): VirtualFileSystem {
        return fs
    }

    override fun getPath(): String {
        return path.toAbsolutePath().toString()
    }

    override fun isWritable(): Boolean {
        try {
            lock.lock()
            return path.isWritable
        } finally {
            lock.unlock()
        }
    }

    override fun isDirectory(): Boolean {
        try {
            lock.lock()
            return path.isDirectory()
        } finally {
            lock.unlock()
        }
    }

    override fun isValid(): Boolean {
        try {
            lock.lock()
            return path.exists()
        } finally {
            lock.unlock()
        }
    }

    override fun getParent(): VirtualFile? {
        return if (path.parent == null) {
            null
        } else {
            RemoteVirtualFile(path.parent, fs)
        }
    }

    override fun getChildren(): Array<VirtualFile> {
        return path.directoryStreamIfExists { it.map { RemoteVirtualFile(it, fs) } }?.toTypedArray() ?: emptyArray()
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return Files.newOutputStream(path)
    }

    override fun contentsToByteArray(): ByteArray {
        return Files.readAllBytes(path)
    }

    override fun getTimeStamp(): Long {
        try {
            lock.lock()
            return path.lastModified().toMillis()
        } finally {
            lock.unlock()
        }
    }

    override fun getLength(): Long {
        try {
            lock.lock()
            return Files.size(path)
        } finally {
            lock.unlock()
        }
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {

    }

    override fun getInputStream(): InputStream {
        try {
            lock.lock()
            return Files.newInputStream(path)
        } finally {
            lock.unlock()
        }
    }


    override fun getModificationStamp(): Long {
        try {
            lock.lock()
            return path.lastModified().toMillis()
        } finally {
            lock.unlock()
        }
    }
}