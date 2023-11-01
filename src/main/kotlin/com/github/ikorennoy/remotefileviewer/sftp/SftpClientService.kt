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
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.lang.IllegalStateException

@Service
class SftpClientService {

    @Volatile
    private var sftpClient: SFTPClient? = null

    private var client: SSHClient? = null

    // tries to return an initialized client
    // must process following cases
    // 1. not enough data in config (generates dialog asking for password
    // 2. not configured at all (opens configuration dialogue)
    // 3. cannot connect (shows a messages with an error)
    fun getClient(): SFTPClient {

        val res = sftpClient

        if (res != null) {
            if (clientIsOk()) {
                return res
            }
        }

        synchronized(this) {
            val newRes = sftpClient
            if (newRes != null) {
                if (clientIsOk()) {
                    return newRes
                }
            }

            val configuration = service<RemoteFileViewerSettingsState>()

            if (configuration.isNotValid()) {
                if (configuration.password.isEmpty()) {
                    // show password prompt dialogue

                    val password = Messages.showInputDialog(
                        "Enter a password:",
                        "Connecting to: ${configuration.username}@${configuration.host}:${configuration.port}",
                        Messages.getQuestionIcon()
                    ) ?: // it means cancel, just throw an error meaning we won't be able to connect
                    TODO("error")

                    configuration.password = password.toCharArray()
                } else {
                    // show full configuration dialogue
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                        ProjectManager.getInstance().defaultProject,
                        "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
                    )
                    //  todo must processed using conf validation
                    if (configuration.isNotValidWithEmptyPassword()) {
                        TODO("error")
                    }
                }
            }

            connect(configuration.host, configuration.port, configuration.username, configuration.password)
            val clientVal =
                client ?: throw IllegalStateException("Client can't be null after successful connection")
            val newSftpClient = clientVal.newSFTPClient()
            sftpClient = newSftpClient
            return newSftpClient
        }
    }

    private fun clientIsOk(): Boolean {
        val clientVal = client ?: return false
        return clientVal.isConnected && clientVal.isAuthenticated
    }


    private fun connect(host: String, port: Int, username: String, password: CharArray) {
        val project = ProjectManager.getInstance().defaultProject // todo find a way to get current project

        var failReason: Exception? = null
        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Modal(project, "Connecting to: ${username}@${host}:${port}", true) {
                override fun run(indicator: ProgressIndicator) {
                    val client = SSHClient()
                    try {
                        client.loadKnownHosts()
                        client.connect(host, port)
                        client.authPassword(username, password)
                        this@SftpClientService.client = client
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

                override fun onSuccess() {
                    reportError()
                }

                private fun reportError() {
                    if (failReason != null) {
                        Messages.showMessageDialog(
                            "Could not connect to ${username}@${host}:${port}",
                            "Error",
                            Messages.getErrorIcon()
                        )
                    }
                }
            }.queue()
        }, "Connecting...", null)
    }
}
