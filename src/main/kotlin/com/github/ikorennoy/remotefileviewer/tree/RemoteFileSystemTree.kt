package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteVirtualFile
import com.github.ikorennoy.remotefileviewer.remoteEdit.EditRemoteFileTask
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
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
import java.io.IOException
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class RemoteFileSystemTree(val project: Project) : Disposable {

    private val treeModel: StructureTreeModel<RemoteFileTreeStructure>
    private val asyncTreeModel: AsyncTreeModel

    val tree: JTree

    init {
        val remoteTreeStructure = RemoteFileTreeStructure(project)
        treeModel = StructureTreeModel(remoteTreeStructure, FileComparator.getInstance(), this)
        asyncTreeModel = AsyncTreeModel(treeModel, this)
        tree = Tree(asyncTreeModel)
        TreeSpeedSearch(tree)
        TreeUtil.installActions(tree)
        PopupHandler.installPopupMenu(tree, createActionGroup(), "RemoteFileSystemTreePopup")
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        val editAction = EditRemoteFileTask(project, this)

        tree.registerKeyboardAction(
            { performAction(editAction, true) },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                performAction(editAction, false)
                return true
            }
        }.installOn(tree)
    }

    fun getSelectedFile(): RemoteVirtualFile? {
        val treePath = tree.selectionPath ?: return null
        return getTargetPath(treePath)
    }

    fun getNewFileParent(): RemoteVirtualFile? {
        val selected = getSelectedFile()
        if (selected != null) return selected
        return null
    }

    fun createNewDirectory(parentDirectory: RemoteVirtualFile, newDirectoryName: String) {
        CommandProcessor.getInstance().executeCommand(project, {
            ProcessIOExecutorService.INSTANCE.execute {
                try {
                    val file = parentDirectory.createChildDirectory(newDirectoryName)
                    ApplicationManager.getApplication().invokeLater {
                        updateAndSelect(file)
                    }
                } catch (_: IOException) {
                }
            }
        }, UIBundle.message("file.chooser.create.new.folder.command.name"), null)
    }

    fun deleteFile(fileToDelete: RemoteVirtualFile) {
        CommandProcessor.getInstance().executeCommand(project, {
            ProcessIOExecutorService.INSTANCE.execute {
                try {
                    fileToDelete.delete()
                    ApplicationManager.getApplication().invokeLater {
                        update()
                    }
                } catch (_: IOException) {

                }
            }
        }, "Delete", null)
    }

    fun createNewFile(parentDirectory: RemoteVirtualFile, newFileName: String) {
        CommandProcessor.getInstance().executeCommand(
            project, {
                ProcessIOExecutorService.INSTANCE.execute {
                    try {
                        val file = parentDirectory.createChildData(newFileName)
                        ApplicationManager.getApplication().invokeLater {
                            updateAndSelect(file)
                        }
                    } catch (_: IOException) {
                    }
                }
            }, UIBundle.message("file.chooser.create.new.file.command.name"), null
        )
    }

    fun update() {
        treeModel.invalidateAsync()
    }

    override fun dispose() {
    }

    private fun updateAndSelect(file: RemoteVirtualFile) {
        treeModel.invalidateAsync().thenRun {
            treeModel.select(file, tree) {}
        }
    }

    private fun performAction(action: Runnable, toggle: Boolean) {
        val path = tree.selectionPath
        if (path != null) {
            if (asyncTreeModel.isLeaf(path.lastPathComponent)) {
                action.run()
            } else if (toggle) {
                if (tree.isExpanded(path)) {
                    tree.collapsePath(path)
                } else {
                    tree.expandPath(path)
                }
            }
        }
    }

    private fun getTargetPath(treePath: TreePath): RemoteVirtualFile? {
        val userObject = (treePath.lastPathComponent as DefaultMutableTreeNode).userObject
        return if (userObject is RemoteFileNodeDescriptor) {
            userObject.element
        } else {
            null
        }
    }

    private fun createActionGroup(): DefaultActionGroup {
        val showCreate = ActionManager.getInstance().getAction("RemoteFileSystem.ShowCreate")
        val delete = ActionManager.getInstance().getAction("RemoteFileSystem.Delete")
        val group = DefaultActionGroup()
        group.add(showCreate)
        group.addSeparator()
        group.add(delete)
        group.addSeparator()
        return group
    }

    companion object {
        val DATA_KEY: DataKey<RemoteFileSystemTree> = DataKey.create("RemoteFileSystemTree")
    }
}
