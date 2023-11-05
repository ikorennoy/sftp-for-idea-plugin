package com.github.ikorennoy.remote.ui

import com.github.ikorennoy.remote.tree.RemoteFileSystemTree
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

class RemoteFileAccessPanel(
    private val tree: RemoteFileSystemTree,
) : SimpleToolWindowPanel(true, true) {

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(tree.tree))
        addDataProvider(MyDataProvider(tree))
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.UpdateTree"))
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.Disconnect"))
        toolbarGroup.addSeparator()

        toolbarGroup.add(
            CommonActionsManager.getInstance().createCollapseAllAction(DefaultTreeExpander(tree.tree), this)
        )
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.SelectOpenedFile"))

        toolbarGroup.addSeparator()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.ShowSshConfiguration"))

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FVToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    private class MyDataProvider(private val fsTree: RemoteFileSystemTree) : DataProvider {
        override fun getData(dataId: String): Any? {
            return if (RemoteFileSystemTree.DATA_KEY.`is`(dataId)) {
                fsTree
            } else {
                null
            }
        }
    }
}
