package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteFileInformation
import com.github.ikorennoy.remotefileviewer.utils.Er
import com.github.ikorennoy.remotefileviewer.utils.Ok

class FileNode(val file: RemoteFileInformation) {

    fun getChildren(): Array<FileNode> {
        return when (val children = file.getChildren()) {
            is Ok -> children.value.map { FileNode(it) }.toTypedArray()
            is Er -> emptyArray()
        }
    }

    fun getParent(): FileNode? {
        return file.getParent()?.toFileNode()
    }

    private fun RemoteFileInformation.toFileNode(): FileNode {
        return FileNode(this)
    }

    fun getPresentableName(): String {
        return file.getPresentableName()
    }
}