package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteVirtualFile
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import javax.swing.Icon

class RemoteFileNodeDescriptor(
    project: Project,
    parentDescriptor: NodeDescriptor<*>?,
    private val element: RemoteVirtualFile,
    icon: Icon,
    name: String,
) : NodeDescriptor<RemoteVirtualFile>(project, parentDescriptor) {

    init {
        myName = name
        setIcon(icon)
    }

    override fun update(): Boolean {
        var updated = false

        val newName = element.getPresentableName()
        if (myName != newName) {
            myName = newName
            updated = true
        }

        return updated
    }

    override fun getElement(): RemoteVirtualFile {
        return element
    }
}
