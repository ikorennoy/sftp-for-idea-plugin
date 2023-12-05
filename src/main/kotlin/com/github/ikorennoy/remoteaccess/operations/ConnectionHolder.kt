package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

// connection and blocking socket operations timeout
internal const val TIMEOUT_MILLISECONDS = 10000

@Service(Service.Level.PROJECT)
internal class ConnectionHolder(private val project: Project) : Disposable {

    @Volatile
    private var sftpClient: SFTPClient? = null

    @Volatile
    private var sshClient: SSHClient? = null

    private val lock = ReentrantLock()

    fun isInitializedAndConnected(): Boolean {
        return isInitializedAndConnectedInternal(sshClient, sftpClient)
    }

    fun connect(): Exception? {
        val prevSsh1 = sshClient
        val prevSftp1 = sftpClient

        if (isInitializedAndConnectedInternal(prevSsh1, prevSftp1)) {
            return null
        }
        try {
            lock.lock()
            val prevSsh2 = sshClient
            val prevSftp2 = sftpClient

            if (isInitializedAndConnectedInternal(prevSsh2, prevSftp2)) {
                return null
            }

            val failReason = tryConnect()
            return if (failReason != null) {
                failReason
            } else {
                val newSshClient = sshClient
                if (newSshClient != null) {
                    val disconnectNotifier = DisconnectNotifier(project)
                    newSshClient.transport.disconnectListener = disconnectNotifier
                    null
                } else {
                    IOException("Fail reason is null, but client is null as well")
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun disconnect() {
        try {
            lock.lock()
            val sft = sftpClient ?: return
            val ssh = sshClient ?: throw IllegalStateException("Sfp client is not null, but ssh is null")
            try {
                sft.close()
            } catch (_: IOException) {
            }
            try {
                ssh.close()
            } catch (_: IOException) {
            }
        } finally {
            sftpClient = null
            sshClient = null
            lock.unlock()
        }
    }

    fun getSessionClient(): Session {
        return getSshClient().startSession()
    }

    fun getSftpClient(): SFTPClient {
        val v1 = sftpClient

        if (v1 != null) {
            return v1
        }

        try {
            lock.lock()
            val v2 = sftpClient
            if (v2 != null) {
                return v2
            }
        } finally {
            lock.unlock()
        }

        throw IOException("Connection is not initialized")
    }

    override fun dispose() {
        disconnect()
    }

    private fun getSshClient(): SSHClient {
        val v1 = sshClient

        if (v1 != null) {
            return v1
        }

        try {
            lock.lock()
            val v2 = sshClient
            if (v2 != null) {
                return v2
            }
        } finally {
            lock.unlock()
        }

        throw IOException("Connection is not initialized")
    }

    private fun isInitializedAndConnectedInternal(sshClient: SSHClient?, sftpClient: SFTPClient?): Boolean {
        return sshClient != null && sshClient.isConnected && sshClient.isAuthenticated && sftpClient != null
    }

    private fun tryConnect(): Exception? {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        val host = conf.host
        val port = conf.port
        val username = conf.username
        val password = conf.password

        var failReason: Exception? = null
        sshClient = null
        sftpClient = null
        val newSshClient = SSHClient()
        try {
            newSshClient.connectTimeout = TIMEOUT_MILLISECONDS
            newSshClient.timeout = TIMEOUT_MILLISECONDS
            newSshClient.useCompression()
            loadKnownHosts(newSshClient)
            newSshClient.connect(host, port)
            if (conf.authenticationType == RemoteFileAccessSettingsState.AuthenticationType.PASSWORD) {
                newSshClient.authPassword(username, password)
            } else {
                val certLocation = conf.certificateLocation
                val key = if (password.isEmpty()) {
                    newSshClient.loadKeys(certLocation)
                } else {
                    newSshClient.loadKeys(certLocation, password)
                }
                newSshClient.authPublickey(username, key)
            }

            sshClient = newSshClient
            sftpClient = newSshClient.newSFTPClient()
        } catch (ex: IOException) {
            failReason = ex
            try {
                sftpClient?.close()
                newSshClient.close()
            } catch (_: IOException) {
            }
            sshClient = null
            sftpClient = null
        }
        return failReason
    }

    /**
     * Copied from [SSHClient.loadKnownHosts]
     */
    private fun loadKnownHosts(client: SSHClient) {
        var loaded = false
        val sshDir = OpenSSHKnownHosts.detectSSHDir()
        if (sshDir != null) {
            for (hostFile in listOf(File(sshDir, "known_hosts"), File(sshDir, "known_hosts2"))) {
                try {
                    if (hostFile.exists()) {
                        client.addHostKeyVerifier(ModalDialogHostKeyVerifier(project, hostFile))
                    }
                    loaded = true
                } catch (e: IOException) {
                    // Ignore for now
                }
            }
        }
        if (!loaded) throw IOException("Could not load known_hosts")
    }

    companion object {
        fun getInstance(project: Project): ConnectionHolder = project.service()
    }
}
