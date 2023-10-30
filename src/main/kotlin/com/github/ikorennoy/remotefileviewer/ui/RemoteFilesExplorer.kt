package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.SftpFileSystem
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.impl.FileComparator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JPanel
import javax.swing.ToolTipManager

class RemoteFilesExplorer(
        private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable {

    private val fs = SftpFileSystem()

    init {
        toolbar = createToolbarPanel()
        setContent(ScrollPaneFactory.createScrollPane())
    }

    private fun showPasswordDialog() {
        val dialog = ConnectionConfigurationDialog(project, fs)
        if (dialog.showAndGet() && fs.isReady()) {
            drawTree()
        }
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(object : AnAction(FileViewerBundle.messagePointer("add.new.connection.action.name"),
                FileViewerBundle.messagePointer("add.new.connection.action.description"),
                IconUtil.getAddIcon()) {
            override fun actionPerformed(e: AnActionEvent) {
                showPasswordDialog()
            }
        })

//        val expandAction = CommonActionsManager.getInstance().createExpandAllAction(defaultExpander, this)
//        toolbarGroup.add(expandAction)
//
//        val collapseAction = CommonActionsManager.getInstance().createCollapseAllAction(defaultExpander, this)
//        toolbarGroup.add(collapseAction)

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FVToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this
        return JBUI.Panels.simplePanel(actionToolbar.component)
    }

    private fun drawTree() {
        val descriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor()

        descriptor.setRoots(fs.root)
        descriptor.withTreeRootVisible(true)
        val treeStr = RemoteFileTreeStructure(project, descriptor)
        val structureModel = StructureTreeModel(treeStr, FileComparator.getInstance(), this)
        val asyncModel = AsyncTreeModel(structureModel, this)
        val tree = Tree(asyncModel)
        TreeUtil.installActions(tree)
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree)
        ToolTipManager.sharedInstance().registerComponent(tree)
        EditSourceOnDoubleClickHandler.install(tree)
        EditSourceOnEnterKeyHandler.install(tree)
        tree.isRootVisible = true
        val defaultExpander = DefaultTreeExpander(tree)
        addDataProvider(RemoteEditDataProvider(project, tree, fs))
        setContent(ScrollPaneFactory.createScrollPane(tree))
    }

    private class NewConnectionAction : AnAction(
            FileViewerBundle.messagePointer("add.new.connection.action.name"),
            FileViewerBundle.messagePointer("add.new.connection.action.description"),
            IconUtil.getAddIcon()
    ) {

        override fun actionPerformed(e: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    override fun dispose() {

    }
}
