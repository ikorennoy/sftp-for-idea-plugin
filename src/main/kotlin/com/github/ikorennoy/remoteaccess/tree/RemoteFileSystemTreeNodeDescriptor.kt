package com.github.ikorennoy.remoteaccess.tree

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.ui.IconManager
import com.intellij.ui.LayeredIcon
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class RemoteFileSystemTreeNodeDescriptor(
    project: Project,
    parentDescriptor: NodeDescriptor<*>?,
    private val element: RemoteFileInformation,
    private val dirsWithNoReadPermission: Map<RemoteFileInformation, Unit>,
) : NodeDescriptor<RemoteFileInformation>(project, parentDescriptor) {

    init {
        myName = element.getPresentableName()
        icon = computeIcon()
    }

    override fun update(): Boolean {
        var updated = false

        val newName = element.getPresentableName()
        if (myName != newName) {
            myName = newName
            updated = true
        }

        val newIcon = computeIcon()

        if (icon != newIcon) {
            icon = newIcon
            updated = true
        }

        return updated
    }

    override fun getElement(): RemoteFileInformation {
        return element
    }

    private fun computeIcon(): Icon {
        if (dirsWithNoReadPermission.containsKey(element)) {
            return AllIcons.General.BalloonError
        }

        val icon = if (element.isDirectory()) {
            IconManager.getInstance().tooltipOnlyIfComposite(PlatformIcons.FOLDER_ICON);
        } else {
            FileTypeRegistry.getInstance().getFileTypeByFileName(element.getName()).icon
        }

        return dressIcon(icon)
    }

    private fun dressIcon(baseIcon: Icon): Icon {
        return if (element.isSymlink()) {
            LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON)
        } else {
            baseIcon
        }
    }
}
