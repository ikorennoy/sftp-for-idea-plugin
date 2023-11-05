package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteFileInformation
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.ui.IconManager
import com.intellij.ui.LayeredIcon
import com.intellij.ui.tree.LeafState
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class RemoteFileTreeStructure(
    private val project: Project,
) : AbstractTreeStructure() {

    private val dummyRoot = DummyNode(project)

    override fun getRootElement(): Any {
        val ops = project.service<RemoteOperations>()
        return if (!ops.isInitializedAndConnected()) {
            dummyRoot
        } else {
            val conf = service<RemoteFileViewerSettingsState>()
            ops.findFileByPath(conf.root) ?: IllegalStateException("root path can't be null")
        }
    }

    override fun getChildElements(element: Any): Array<RemoteFileInformation> {
        return if (element is RemoteFileInformation) {
            element.getChildren()
        } else {
            emptyArray()
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
        val icon = getIcon(element)
        val name = element.getPresentableName()
        return RemoteFileNodeDescriptor(project, parentDescriptor, element, icon, name)
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
            if (element.isDirectory()) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is RemoteFileInformation) {
            element.isDirectory()
        } else {
            false
        }
    }

    private fun getIcon(file: RemoteFileInformation): Icon {
        val icon = if (file.isDirectory()) {
            IconManager.getInstance().tooltipOnlyIfComposite(PlatformIcons.FOLDER_ICON);
        } else {
            FileTypeRegistry.getInstance().getFileTypeByFileName(file.getName()).icon
        }
        return dressIcon(file, icon)
    }

    private fun dressIcon(file: RemoteFileInformation, baseIcon: Icon): Icon {
        return if (file.isValid() && file.`is`(VFileProperty.SYMLINK)) {
            LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON)
        } else {
            baseIcon
        }
    }
}
