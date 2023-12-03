package com.github.ikorennoy.remoteaccess.tree

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class RemoteFileSystemTreeNodeDescriptor(
    project: Project,
    parentDescriptor: NodeDescriptor<*>?,
    private val element: RemoteFileInformation,
    private val dirsWithNoReadPermission: Set<String>,
) : PresentableNodeDescriptor<RemoteFileInformation>(project, parentDescriptor) {

    override fun update(presentation: PresentationData) {
        presentation.setIcon(computeIcon())
        presentation.presentableText = element.getPresentableName()
        if (element.isPlainFile()) {
            presentation.tooltip = element.getPresentableLength()
        }
    }

    override fun getElement(): RemoteFileInformation {
        return element
    }

    override fun toString(): String {
        return element.getPath()
    }

    private fun computeIcon(): Icon {
        if (dirsWithNoReadPermission.contains(element.getUri())) {
            return AllIcons.General.BalloonError
        }

        val icon = if (element.isDirectory()) {
            PlatformIcons.FOLDER_ICON
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
