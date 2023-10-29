package com.github.ikorennoy.remotefileviewer.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.impl.FileTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.ui.tree.LeafState
import com.intellij.util.io.isFile
import kotlin.io.path.isDirectory

class RemoteFileTreeStructure(
        project: Project,
        fileChooserDescriptor: FileChooserDescriptor,
) : FileTreeStructure(project, fileChooserDescriptor) {

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return true
    }

    override fun getLeafState(element: Any): LeafState {
        return if (element is FileElement) {
            if (element.file.isDirectory) {
                LeafState.NEVER
            } else {
                LeafState.ALWAYS
            }
        } else {
            LeafState.DEFAULT
        }
    }

    override fun isAlwaysLeaf(element: Any): Boolean {
        return if (element is FileElement) {
            element.file.toNioPath().isFile()
        } else {
            false
        }
    }
}
