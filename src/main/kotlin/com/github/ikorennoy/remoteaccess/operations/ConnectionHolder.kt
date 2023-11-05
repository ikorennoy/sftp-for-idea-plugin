package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

// connection and blocking socket operations timeout
internal const val TIMEOUT_MILLISECONDS = 10000

@Service
internal class ConnectionHolder : Disposable {

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
        var localClient = sftpClient

        if (localClient != null) {
            return null
        }
        try {
            lock.lock()
            localClient = sftpClient
            if (localClient != null) {
                return null
            }

            val configuration = service<RemoteFileAccessSettingsState>()

            val failReason =
                tryConnect(configuration.host, configuration.port, configuration.username, configuration.password)
            val clientVal = client
            return if (clientVal == null) {
                // initialization is completely failed, just return false, user is notified by tryConnect
                failReason
            } else {
                val newSftpClient = clientVal.newSFTPClient()
                sftpClient = newSftpClient
                null
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
        } catch (ex: IOException) {
            thisLogger().error("Error while disconnecting", ex)
        } finally {
            lock.unlock()
        }
    }

    override fun dispose() {
        disconnect()
    }

    private fun tryConnect(host: String, port: Int, username: String, password: CharArray): Exception? {
        var failReason: Exception? = null
        val thisClient = SSHClient()
        try {
            thisClient.connectTimeout = TIMEOUT_MILLISECONDS
            thisClient.timeout = TIMEOUT_MILLISECONDS
            thisClient.useCompression()
            thisClient.loadKnownHosts()
            thisClient.connect(host, port)
            thisClient.authPassword(username, password)
            this@ConnectionHolder.client = thisClient
        } catch (ex: IOException) {
            thisLogger().error("Error while connecting", ex)
            failReason = ex
            try {
                thisClient.close()
            } catch (_: IOException) {
            }
        }
        return failReason
    }

    companion object {
        fun getInstance(): ConnectionHolder = service()
    }
}
