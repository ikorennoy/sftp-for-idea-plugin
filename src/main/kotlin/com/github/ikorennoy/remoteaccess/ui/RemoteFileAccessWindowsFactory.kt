package com.github.ikorennoy.remoteaccess.ui

import com.github.ikorennoy.remoteaccess.edit.UnsavedChangesListener
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.tree.TreeStateListener
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RemoteFileAccessWindowsFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // first try to connect on window open
        val tryConnect = prepareConfiguration(project)

        if (tryConnect) {
            val remoteOperations = RemoteOperations.getInstance(project)
            remoteOperations.initWithModalDialogue()
        }

        val remoteFileAccessPanel = RemoteFileAccessPanel(project)
        val remoteFileSystemTree = remoteFileAccessPanel.tree

        val toolWindowContent = ContentFactory.getInstance().createContent(remoteFileAccessPanel, null, false)
        toolWindow.contentManager.addContent(toolWindowContent)
        toolWindowContent.setDisposer(remoteFileAccessPanel)

        subscribeOnConnectionStatusUpdated(remoteFileSystemTree, toolWindow)
        subscribeOnEditorManagerEvents(project, toolWindow, remoteFileSystemTree)
    }

    private fun subscribeOnEditorManagerEvents(project: Project, toolWindow: ToolWindow, tree: RemoteFileSystemTree) {
        val unsavedChangesListener = UnsavedChangesListener()
        project.messageBus.connect(toolWindow.disposable)
            .subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, unsavedChangesListener)
    }

    private fun subscribeOnConnectionStatusUpdated(fsTree: RemoteFileSystemTree, toolWindow: ToolWindow) {
        ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
            .subscribe(TreeStateListener.TOPIC, object : TreeStateListener {
                override fun updateFullTree() {
                    ApplicationManager.getApplication().invokeLater {
                        fsTree.cleanAndRebuildFullTree()
                    }
                }

                override fun updateTreeNode(node: RemoteFileInformation) {
                    fsTree.rebuildTreeNode(node)
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true
}
