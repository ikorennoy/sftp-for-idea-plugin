package com.github.ikorennoy.remoteaccess.tree

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.edit.EditRemoteFileTask
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.UIBundle
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class RemoteFileSystemTree(val project: Project, parent: Disposable) : Disposable {

    private val treeModel: StructureTreeModel<RemoteFileSystemTreeStructure>
    private val asyncTreeModel: AsyncTreeModel
    private val remoteTreeStructure: RemoteFileSystemTreeStructure = RemoteFileSystemTreeStructure(project)

    val tree: JTree

    init {
        treeModel = StructureTreeModel(remoteTreeStructure, FileComparator.getInstance(), this)
        remoteTreeStructure.setTreeMode(treeModel)
        asyncTreeModel = AsyncTreeModel(treeModel, this)
        tree = Tree(asyncTreeModel)
        TreeSpeedSearch.installOn(tree)
        TreeUtil.installActions(tree)
        PopupHandler.installPopupMenu(tree, createActionGroup(), "RemoteFileSystemTreePopup")
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        val editTask = EditRemoteFileTask(project, this)

        tree.registerKeyboardAction(
            { performAction(editTask, true) },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                performAction(editTask, false)
                return true
            }
        }.installOn(tree)
        Disposer.register(parent, this)
    }

    fun getSelectedFile(): RemoteFileInformation? {
        val treePath = tree.selectionPath ?: return null
        return getTargetPath(treePath)
    }

    fun getSelectedFileParent(): RemoteFileInformation? {
        val treePath = tree.selectionPath ?: return null
        val parentParent = treePath.parentPath ?: return null
        return getTargetPath(parentParent)
    }

    fun createNewDirectory(parentDirectory: RemoteFileInformation, newDirectoryName: String) {
        CommandProcessor.getInstance().executeCommand(project, {
            ProcessIOExecutorService.INSTANCE.execute {
                val remoteOperations = RemoteOperations.getInstance(project)
                when (val res = remoteOperations.createChildDirectory(parentDirectory, newDirectoryName)) {
                    is Ok -> {
                        ApplicationManager.getApplication().invokeLater {
                            updateNodeAndSelect(parentDirectory, res.value)
                        }
                    }

                    is Er -> {
                        RemoteOperationsNotifier.getInstance(project)
                            .cannotCreateChildDirectory(newDirectoryName, res.error)
                    }
                }
            }
        }, UIBundle.message("file.chooser.create.new.folder.command.name"), null)
    }

    fun deleteFile(toDelete: RemoteFileInformation) {
        CommandProcessor.getInstance().executeCommand(project, {
            ProcessIOExecutorService.INSTANCE.execute {
                when (val res = RemoteOperations.getInstance(project).remove(toDelete)) {
                    is Ok -> {
                        ApplicationManager.getApplication().invokeLater {
                            val parentPath = getSelectedFileParent()
                            if (parentPath != null) {
                                treeModel.invalidateAsync(parentPath, true)
                            } else {
                                rebuildFullTree()
                            }
                        }
                    }

                    is Er -> {
                        val entity = if (toDelete.isDirectory()) {
                            RemoteFileAccessBundle.message("action.RemoteFileAccess.delete.folder.entity")
                        } else {
                            RemoteFileAccessBundle.message("action.RemoteFileAccess.delete.file.entity")
                        }
                        RemoteOperationsNotifier.getInstance(project).cannotDelete(toDelete, res.error, entity)
                    }
                }
            }
        }, UIBundle.message("delete.dialog.title"), null)
    }

    fun createNewFile(parentDirectory: RemoteFileInformation, newFileName: String) {
        CommandProcessor.getInstance().executeCommand(
            project, {
                ProcessIOExecutorService.INSTANCE.execute {
                    val remoteOperations = RemoteOperations.getInstance(project)
                    when (val res = remoteOperations.createChildFile(parentDirectory, newFileName)) {
                        is Ok -> {
                            ApplicationManager.getApplication().invokeLater {
                                updateNodeAndSelect(parentDirectory, res.value)
                            }
                        }
                        is Er -> {
                            RemoteOperationsNotifier.getInstance(project)
                                .cannotCreateChildFile(newFileName, res.error)
                        }
                    }
                }
            }, UIBundle.message("file.chooser.create.new.file.command.name"), null
        )
    }

    /**
     * Invalidate the tree and clear the state of the tree structure
     * Used when there are changes in the connection state
     */
    fun cleanAndRebuildFullTree() {
        treeModel.invalidateAsync()
        remoteTreeStructure.clear()
    }

    override fun dispose() {
        val myTree = tree
        ToolTipManager.sharedInstance().unregisterComponent(myTree)
        for (keyStroke in tree.registeredKeyStrokes) {
            tree.unregisterKeyboardAction(keyStroke)
        }
    }

    fun select(file: RemoteFileInformation) {
        treeModel.select(file, tree) {}
    }

    fun rebuildFullTree() {
        treeModel.invalidateAsync()
    }

    fun rebuildTreeNode(parentNode: RemoteFileInformation) {
        treeModel.invalidateAsync(parentNode, true)
    }

    private fun updateNodeAndSelect(nodeToUpdate: RemoteFileInformation, nodeToSelect: RemoteFileInformation) {
        treeModel.invalidateAsync(nodeToUpdate, true).thenRun {
            treeModel.select(nodeToSelect, tree) {}
        }
    }

    private fun performAction(action: Runnable, toggle: Boolean) {
        val file = getSelectedFile()
        if (file != null) {
            if (file.isPlainFile()) {
                action.run()
            } else if (toggle) {
                val path = tree.selectionPath
                if (path != null) {
                    if (tree.isExpanded(path)) {
                        tree.collapsePath(path)
                    } else {
                        tree.expandPath(path)
                    }
                }
            }
        }
    }

    private fun getTargetPath(treePath: TreePath): RemoteFileInformation? {
        val userObject = (treePath.lastPathComponent as DefaultMutableTreeNode).userObject
        return if (userObject is RemoteFileSystemTreeNodeDescriptor) {
            userObject.element
        } else {
            null
        }
    }

    private fun createActionGroup(): DefaultActionGroup {
        val createFile = ActionManager.getInstance().getAction("RemoteFileAccess.NewFileAction")
        val createDirectory = ActionManager.getInstance().getAction("RemoteFileAccess.NewDirectoryAction")
        val delete = ActionManager.getInstance().getAction("RemoteFileAccess.Delete")
        val editAttributes = ActionManager.getInstance().getAction("RemoteFileAccess.EditAttributes")
        val group = DefaultActionGroup()
        group.add(createFile)
        group.add(createDirectory)
        group.add(delete)
        group.addSeparator()
        group.add(editAttributes)
        return group
    }

    companion object {
        val DATA_KEY: DataKey<RemoteFileSystemTree> = DataKey.create("RemoteFileSystemTree")
    }
}
