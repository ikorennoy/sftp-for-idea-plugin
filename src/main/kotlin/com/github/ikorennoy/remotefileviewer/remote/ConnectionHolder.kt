package com.github.ikorennoy.remotefileviewer.remote

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

internal class ConnectionHolder {

    @Volatile
    private var sftpClient: SFTPClient? = null

    @Volatile
    private var client: SSHClient? = null

    private val lock = ReentrantLock()


    fun isInitializedAndConnected(): Boolean {
        val localClient = client ?: return false
        return localClient.isConnected && localClient.isAuthenticated
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
            ex.printStackTrace()
        } finally {
            lock.unlock()
        }
    }

    fun connect(): Boolean {
        var localClient = sftpClient

        if (localClient != null) {
            return sshClientIsOk()
        }
        try {
            lock.lock()
            localClient = sftpClient
            if (localClient != null) {
                return sshClientIsOk()
            }

            val configuration = service<RemoteFileViewerSettingsState>()

            tryConnect(configuration.host, configuration.port, configuration.username, configuration.password)
            val clientVal = client
            return if (clientVal == null) {
                // initialization is completely failed, just return false, user is notified by tryConnect
                false
            } else {
                val newSftpClient = clientVal.newSFTPClient()
                sftpClient = newSftpClient
                true
            }
        } finally {
            lock.unlock()
        }
    }

    fun getSftpClient(): SFTPClient {
        var res = sftpClient

        if (res != null) {
            return res
        }

        if (connect()) {
            res = sftpClient ?: throw IllegalStateException("Can't be null after successful initialization")
            return res
        } else {
            // it means one of the followings states
            // 1. user somehow cancelled initialization, probably by clicking cancel on password prompt or in conf
            // in that case we don't need to do anything
            // 2. initialization is failed, in that case user is already notified by a message window
            // 3. user has corrupted settings, but we asked only password and failed, in that case we need to open full
            // settings and ask to fix them
            throw IllegalStateException("Can't connect")
        }
    }

    private fun sshClientIsOk(): Boolean {
        val clientVal = client ?: return false
        return clientVal.isConnected && clientVal.isAuthenticated
    }

    private fun tryConnect(host: String, port: Int, username: String, password: CharArray) {
        val project = ProjectManager.getInstance().defaultProject // todo find a way to get current project
        var failReason: Exception? = null
        CommandProcessor.getInstance()
            .executeCommand(project, {
                object : Task.Modal(project, "Connecting to: ${username}@${host}:${port}", false) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        val client = SSHClient()
                        try {
                            client.useCompression()
                            client.loadKnownHosts()
                            client.connect(host, port)
                            client.authPassword(username, password)
                            this@ConnectionHolder.client = client
                        } catch (ex: IOException) {
                            try {
                                client.close()
                            } catch (_: IOException) {
                            }
                            failReason = ex
                        }
                    }

                    override fun onFinished() {
                        reportError()
                    }

                    private fun reportError() {
                        if (failReason != null) {
                            Messages.showMessageDialog(
                                "Cannot not connect to ${username}@${host}:${port}\n ${failReason?.javaClass}",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
                    }
                }.queue()
            }, "Connecting...", null)
    }
}