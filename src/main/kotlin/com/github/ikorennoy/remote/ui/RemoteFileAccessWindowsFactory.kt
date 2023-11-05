package com.github.ikorennoy.remote.ui

import com.github.ikorennoy.remote.operations.ConnectionListener
import com.github.ikorennoy.remote.operations.RemoteOperations
import com.github.ikorennoy.remote.edit.TemporaryFilesEditorManagerListener
import com.github.ikorennoy.remote.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remote.tree.RemoteFileSystemTree
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RemoteFileAccessWindowsFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // first try to connect on window open
        val configuration = service<RemoteFileAccessSettingsState>()
        var tryConnect = true

        if (configuration.isNotValid()) {
            // show full configuration dialogue
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
            )
            tryConnect = !configuration.isNotValid() // user cancelled settings dialog
        } else {
            if (configuration.password.isEmpty()) {
                // show password prompt dialogue
                val password = Messages.showPasswordDialog(
                    "Enter a password:",
                    "Connecting to: ${configuration.username}@${configuration.host}:${configuration.port}",
                )

                if (password != null) {
                    configuration.password = password.toCharArray()
                } else {
                    // it means user cancelled password enter dialog
                    tryConnect = false
                }
            }
        }

        if (tryConnect) {
            val connManager = project.service<RemoteOperations>()
            connManager.init()
        }

        val remoteFileSystemTree = RemoteFileSystemTree(project)
        val remoteFileAccessPanel = RemoteFileAccessPanel(remoteFileSystemTree)

        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileAccessPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)

        subscribeOnConnectionStatusUpdated(remoteFileSystemTree)
        subscribeOnEditorManagerEvents(project)
    }

    private fun subscribeOnEditorManagerEvents(project: Project) {
        val temporaryFilesEditorManagerListener = TemporaryFilesEditorManagerListener()
        project.messageBus.connect().subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, temporaryFilesEditorManagerListener)
    }

    private fun subscribeOnConnectionStatusUpdated(fsTree: RemoteFileSystemTree) {
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ConnectionListener.TOPIC, object : ConnectionListener {
                override fun connectionStatusChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        fsTree.update()
                    }
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

}

