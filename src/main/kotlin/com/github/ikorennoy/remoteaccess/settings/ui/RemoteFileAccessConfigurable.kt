package com.github.ikorennoy.remoteaccess.settings.ui

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class RemoteFileAccessConfigurable(private val project: Project) : Configurable {

    private lateinit var settingsComponent: RemoteFileAccessSettingsComponent

    override fun createComponent(): JComponent {
        settingsComponent = RemoteFileAccessSettingsComponent(project)
        return settingsComponent.panel
    }

    override fun isModified(): Boolean {
        return settingsComponent.isModified()
    }

    override fun apply() {
        // todo draw a tree (check if init and connected and draw)
        // according to guidelines on ok and conf change I should perform an action and draw a tree
        settingsComponent.saveState()
        val remoteOperations = RemoteOperations.getInstance(project)
        remoteOperations.initWithModalDialogue(project)
        notifyRebuildTree()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent.getPreferredFocusedComponent()
    }

    override fun getDisplayName(): String {
        return "SSH Configuration"
    }
}
