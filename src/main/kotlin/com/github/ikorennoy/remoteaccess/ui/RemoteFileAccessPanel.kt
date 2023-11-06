package com.github.ikorennoy.remoteaccess.ui

import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

class RemoteFileAccessPanel(
    project: Project,
) : SimpleToolWindowPanel(true, true), Disposable {

    val tree: RemoteFileSystemTree = RemoteFileSystemTree(project, this)

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
        addDataProvider(RemoteFileSystemTreeDataProvider(tree))
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileAccess.UpdateTree"))
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileAccess.Disconnect"))
        toolbarGroup.addSeparator()

        toolbarGroup.add(
            CommonActionsManager.getInstance().createCollapseAllAction(DefaultTreeExpander(tree.tree), this)
        )
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileAccess.SelectOpenedFile"))

        toolbarGroup.addSeparator()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileAccess.ShowSshConfiguration"))

        val actionToolbar =
            ActionManager.getInstance().createActionToolbar("RemoteFileAccessToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    private class RemoteFileSystemTreeDataProvider(private val fsTree: RemoteFileSystemTree) : DataProvider {
        override fun getData(dataId: String): Any? {
            return if (RemoteFileSystemTree.DATA_KEY.`is`(dataId)) {
                fsTree
            } else {
                null
            }
        }
    }

    override fun dispose() {
    }
}
