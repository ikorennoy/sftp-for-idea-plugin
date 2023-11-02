package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionManager
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.impl.FileTreeStructure
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tree.LeafState
import com.intellij.util.io.isFile
import com.intellij.util.ui.UIUtil

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
            val connManager = service<RemoteConnectionManager>()
            connManager.init()
        }

        val remoteFileSystemTree = RemoteFileSystemTree(project)
        val remoteFileSystemPanel = RemoteFileSystemPanel(remoteFileSystemTree)


        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileSystemPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)

        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(RemoteConnectionListener.TOPIC, object : RemoteConnectionListener {
                override fun connectionEstablished() {
                    UIUtil.invokeLaterIfNeeded {
                        remoteFileSystemTree.invalidate()
                    }
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

}

class RemoteFileTreeStructure(
    project: Project,
    fileChooserDescriptor: FileChooserDescriptor,
) : FileTreeStructure(project, fileChooserDescriptor) {

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return true
    }

    override fun getLeafState(element: Any): LeafState {
        return if (element is FileElement) {
            if (element.file.isDirectory) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is FileElement) {
            element.file.toNioPath().isFile()
        } else {
            false
        }
    }
}

