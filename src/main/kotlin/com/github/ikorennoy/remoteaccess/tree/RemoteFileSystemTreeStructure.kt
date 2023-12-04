package com.github.ikorennoy.remoteaccess.tree

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.tree.LeafState
import com.intellij.ui.tree.StructureTreeModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RemoteFileSystemTreeStructure(
    private val project: Project,
) : AbstractTreeStructure() {

    private val dummyRoot = DummyNode()

    private val dirsWithoutReadPermission = Collections.newSetFromMap<String>(ConcurrentHashMap())

    @Volatile
    lateinit var myTreeModel: StructureTreeModel<RemoteFileSystemTreeStructure>

    override fun getRootElement(): Any {
        val remoteOperations = RemoteOperations.getInstance(project)
        return if (!remoteOperations.isInitializedAndConnected()) {
            dummyRoot
        } else {
            val conf = RemoteFileAccessSettingsState.getInstance(project)
            when (val res = remoteOperations.findFileByPath(conf.root)) {
                is Ok -> res.value
                is Er -> {
                    RemoteOperationsNotifier.getInstance(project).cannotFindRoot(conf.root, res.error)
                    dummyRoot
                }
            }
        }
    }

    override fun getChildElements(element: Any): Array<RemoteFileInformation> {
        return if (element is RemoteFileInformation) {
            when (val res = element.getChildren()) {
                is Ok -> {
                    if (dirsWithoutReadPermission.remove(element.getUri())) {
                        rebuildNode(element)
                    }
                    res.value
                }

                is Er -> {
                    // if we add the element for the first time then notify user
                    if (dirsWithoutReadPermission.add(element.getUri())) {
                        RemoteOperationsNotifier.getInstance(project).cannotLoadChildren(element.getName(), res.error)
                        rebuildNode(element)
                    }
                    emptyArray()
                }
            }
        } else {
            emptyArray()
        }
    }

    private fun rebuildNode(element: RemoteFileInformation) {
        val parent = element.getParent()
        if (parent != null) {
            myTreeModel.invalidateAsync(parent, true)
        }
    }

    override fun getParentElement(element: Any): RemoteFileInformation? {
        return if (element is RemoteFileInformation) {
            element.getParent()
        } else {
            null
        }
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        if (element is DummyNode) {
            return element.getNodeDescriptor(project, parentDescriptor)
        }

        if (element !is RemoteFileInformation) throw IllegalArgumentException("element is not file")
        return RemoteFileSystemTreeNodeDescriptor(project, parentDescriptor, element, dirsWithoutReadPermission)
    }

    override fun commit() {
    }

    override fun hasSomethingToCommit(): Boolean {
        return false
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return false
    }

    override fun getLeafState(element: Any): LeafState {
        return if (element is RemoteFileInformation) {
            if (element.isDirectory() && !dirsWithoutReadPermission.contains(element.getUri())) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    internal fun setTreeMode(treeMode: StructureTreeModel<RemoteFileSystemTreeStructure>) {
        myTreeModel = treeMode
    }

    internal fun clear() {
        dirsWithoutReadPermission.clear()
    }
}
