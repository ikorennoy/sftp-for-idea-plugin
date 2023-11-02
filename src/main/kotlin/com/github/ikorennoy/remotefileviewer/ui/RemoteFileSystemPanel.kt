package com.github.ikorennoy.remotefileviewer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import java.io.IOException
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

// todo add update for this panel
//  check why not all files opening
//  prob add new create dir action
//  check that everything works with 'bind mounts'
//  start writing tests
//  fix a case when configuration is corrupted (like wrong host and etc), but only password prompt is shown, after firs unsuccessful attempt
//  we should show full configuration page
class RemoteFileSystemPanel(
    private val tree: JTree,
    private val isSimple: Boolean,
) : SimpleToolWindowPanel(true, true), Disposable {

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane(tree))

    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(ActionManager.getInstance().getAction("RemoteFileSystem.ShowSshConfiguration"))

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FVToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    override fun dispose() {

    }


}
