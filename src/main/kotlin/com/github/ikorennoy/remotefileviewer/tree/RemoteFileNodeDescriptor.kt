package com.github.ikorennoy.remotefileviewer.tree

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class RemoteFileNodeDescriptor(
    project: Project,
    parentDescriptor: NodeDescriptor<*>?,
    private val element: VirtualFile,
    icon: Icon,
    name: String,
) : NodeDescriptor<VirtualFile>(project, parentDescriptor) {

    init {
        myName = name
        setIcon(icon)
    }

    override fun update(): Boolean {
        var updated = false

        val newName = element.presentableName
        if (myName != newName) {
            myName = newName
            updated = true
        }

        return updated
    }

    override fun getElement(): VirtualFile {
        return element
    }
}
