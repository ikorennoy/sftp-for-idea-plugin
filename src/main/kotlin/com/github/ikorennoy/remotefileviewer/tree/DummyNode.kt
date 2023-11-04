package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.components.service

class DummyNode: ValidateableNode {

    /**
     * Valid until the service is initialized
     */
    override fun isValid(): Boolean {
        return !service<RemoteOperations>().isInitializedAndConnected()
    }
}
