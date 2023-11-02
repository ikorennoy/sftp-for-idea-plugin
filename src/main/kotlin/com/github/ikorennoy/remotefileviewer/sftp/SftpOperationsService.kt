package com.github.ikorennoy.remotefileviewer.sftp

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.lang.IllegalStateException

@Service
class SftpOperationsService {

    @Volatile
    private var sftpClient: SFTPClient? = null

    @Volatile
    private var client: SSHClient? = null


    /**
     * Thread safe
     * Ensures that the client is connected and authenticated
     */
    fun init(): Boolean {
        var localClient = sftpClient

        if (localClient != null) {
            return sshClientIsOk()
        }

        synchronized(this) {
            localClient = sftpClient
            if (localClient != null) {
                return sshClientIsOk()
            }

            val configuration = service<RemoteFileViewerSettingsState>()

            if (configuration.isNotValid()) {
                // show full configuration dialogue
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    ProjectManager.getInstance().defaultProject,
                    "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
                )

                if (configuration.isNotValid()) {
                    return false // user cancelled configuration, just return false
                }
            } else {
                if (configuration.password.isEmpty()) {
                    // show password prompt dialogue
                    val password = Messages.showPasswordDialog(
                        "Enter a password:",
                        "Connecting to: ${configuration.username}@${configuration.host}:${configuration.port}",
                    ) ?: return false // it means cancel, we don't have a password and user don't want to enter it

                    configuration.password = password.toCharArray()
                }
            }

            tryConnect(configuration.host, configuration.port, configuration.username, configuration.password)
            val clientVal = client
            if (clientVal == null) {
                // initialization is completely failed, just return false, user is notified with com.intellij.openapi.progress.Task.Modal#reportError
                return false
            } else {
                val newSftpClient = clientVal.newSFTPClient()
                sftpClient = newSftpClient
                return true
            }
        }
    }

    fun getSessionClient(): Session {
        var res = client

        return if (res != null) {
            res.startSession()
        } else {
            if (init()) {
                res = client ?: throw IllegalStateException("Can't be null after successful initialization")
                res.startSession()
            } else {
                throw IllegalStateException("Can't connect")
            }
        }
    }

    fun getSftpClient(): SFTPClient {
        var res = sftpClient

        if (res != null) {
            return res
        }

        if (init()) {
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
        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Modal(project, "Connecting to: ${username}@${host}:${port}", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    val client = SSHClient()
                    try {
                        client.useCompression()
                        client.loadKnownHosts()
                        client.connect(host, port)
                        client.authPassword(username, password)
                        this@SftpOperationsService.client = client
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