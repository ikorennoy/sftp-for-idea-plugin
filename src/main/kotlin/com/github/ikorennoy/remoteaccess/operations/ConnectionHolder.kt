package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

// connection and blocking socket operations timeout
internal const val TIMEOUT_MILLISECONDS = 10000

@Service(Service.Level.PROJECT)
internal class ConnectionHolder(private val project: Project) : Disposable {

    @Volatile
    private var sftpClient: SFTPClient? = null

    @Volatile
    private var client: SSHClient? = null

    private val lock = ReentrantLock()

    fun isInitializedAndConnected(): Boolean {
        val localClient = client ?: return false
        return localClient.isConnected && localClient.isAuthenticated
    }

    fun connect(): Exception? {
        var prevSftpClient = sftpClient

        if (prevSftpClient != null) {
            return null
        }
        try {
            lock.lock()
            prevSftpClient = sftpClient
            if (prevSftpClient != null) {
                return null
            }

            val configuration = RemoteFileAccessSettingsState.getInstance()

            val failReason =
                tryConnect(configuration.host, configuration.port, configuration.username, configuration.password)
            val localClient = client
            return if (localClient != null) {
                val disconnectNotifier = DisconnectNotifier(project)
                localClient.transport.disconnectListener = disconnectNotifier
                val newSftpClient = localClient.newSFTPClient()
                sftpClient = newSftpClient
                null
            } else {
                // initialization is completely failed, just return false
                failReason
            }
        } finally {
            lock.unlock()
        }
    }

    fun getSftpClient(): SFTPClient {
        val res = sftpClient

        if (res != null) {
            return res
        }

        throw IOException("Connection is not initialized")
    }

    fun disconnect() {
        try {
            lock.lock()
            val sft = sftpClient ?: return
            val ssh = client ?: throw IllegalStateException("Sfp client is not null, but ssh is null")
            sft.close()
            ssh.close()
            sftpClient = null
            client = null
        } catch (_: IOException) {
        } finally {
            lock.unlock()
        }
    }

    override fun dispose() {
        disconnect()
    }

    private fun tryConnect(host: String, port: Int, username: String, password: CharArray): Exception? {
        var failReason: Exception? = null
        val newClient = SSHClient()
        try {
            newClient.connectTimeout = TIMEOUT_MILLISECONDS
            newClient.timeout = TIMEOUT_MILLISECONDS
            newClient.useCompression()
            newClient.loadKnownHosts()
            newClient.connect(host, port)
            newClient.authPassword(username, password)
            client = newClient
        } catch (ex: IOException) {
            failReason = ex
            try {
                newClient.close()
            } catch (_: IOException) {
            }
        }
        return failReason
    }

    companion object {
        fun getInstance(project: Project): ConnectionHolder = project.service()
    }
}
