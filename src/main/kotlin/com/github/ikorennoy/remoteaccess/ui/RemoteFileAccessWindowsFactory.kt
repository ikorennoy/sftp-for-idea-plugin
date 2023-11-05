package com.github.ikorennoy.remoteaccess.ui

import com.github.ikorennoy.remoteaccess.operations.ConnectionListener
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.edit.TemporaryFilesEditorManagerListener
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
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
        val tryConnect = prepareConfiguration(project)

        if (tryConnect) {
            val connManager = RemoteOperations.getInstance(project)
            connManager.initWithModalDialogue(project)
        }

        val remoteFileAccessPanel = RemoteFileAccessPanel(project)
        val remoteFileSystemTree = remoteFileAccessPanel.tree

        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileAccessPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)
        toolWindowContent.setDisposer(remoteFileAccessPanel)

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

