package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.remoteEdit.TemporaryFilesEditorManagerListener
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
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

class FileViewerWindowsFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // first try to connect on window open
        val configuration = service<RemoteFileViewerSettingsState>()
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
            val connManager = service<RemoteOperations>()
            connManager.init()
        }

        val remoteFileSystemTree = RemoteFileSystemTree(project)
        val remoteFileSystemPanel = RemoteFileSystemPanel(remoteFileSystemTree)

        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileSystemPanel, null, false)
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
            .subscribe(RemoteConnectionListener.TOPIC, object : RemoteConnectionListener {
                override fun connectionStatusChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        fsTree.update()
                    }
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

}

