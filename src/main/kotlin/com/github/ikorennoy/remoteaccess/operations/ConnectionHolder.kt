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
        return checkClientsInitializedAndConnected(sshClient, sftpClient)
    }

    fun connect(): Exception? {
        var prevSshClient = sshClient
        var prevSftpClient = sftpClient

        if (checkClientsInitializedAndConnected(prevSshClient, prevSftpClient)) {
            return null
        }
        try {
            lock.lock()
            prevSftpClient = sftpClient
            prevSshClient = sshClient

            if (checkClientsInitializedAndConnected(prevSshClient, prevSftpClient)) {
                return null
            }
            val conf = RemoteFileAccessSettingsState.getInstance(project)

            sshClient = null
            sftpClient = null
            val failReason =
                tryConnect(conf.host, conf.port, conf.username, conf.password)
            return if (failReason != null) {
                failReason
            } else {
                val localClient = sshClient
                if (localClient != null) {
                    val disconnectNotifier = DisconnectNotifier(project)
                    localClient.transport.disconnectListener = disconnectNotifier
                    val newSftpClient = localClient.newSFTPClient()
                    sftpClient = newSftpClient
                    null
                } else {
                    IOException("Fail reason is null, but client is null as well")
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun getSessionClient(): Session {
        return getSshClient().startSession()
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
            val ssh = sshClient ?: throw IllegalStateException("Sfp client is not null, but ssh is null")
            sft.close()
            ssh.close()
            sftpClient = null
            sshClient = null
        } catch (_: IOException) {
        } finally {
            lock.unlock()
        }
    }

    override fun dispose() {
        disconnect()
    }

    private fun getSshClient(): SSHClient {
        val res = sshClient

        if (res != null) {
            return res
        }

        throw IOException("Connection is not initialized")
    }

    private fun checkClientsInitializedAndConnected(sshClient: SSHClient?, sftpClient: SFTPClient?): Boolean {
        return sshClient != null && sshClient.isConnected && sshClient.isAuthenticated && sftpClient != null
    }

    private fun tryConnect(host: String, port: Int, username: String, password: CharArray): Exception? {
        var failReason: Exception? = null
        val newClient = SSHClient()
        try {
            newClient.connectTimeout = TIMEOUT_MILLISECONDS
            newClient.timeout = TIMEOUT_MILLISECONDS
            newClient.useCompression()
            loadKnownHosts(newClient)
            newClient.connect(host, port)
            newClient.authPassword(username, password)
            sshClient = newClient
        } catch (ex: IOException) {
            failReason = ex
            sshClient = null
            sftpClient = null
            try {
                newClient.close()
            } catch (_: IOException) {
            }
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
