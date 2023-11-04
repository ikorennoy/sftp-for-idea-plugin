package com.github.ikorennoy.remotefileviewer.settings.ui

import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class RemoteFilePluginConfigurable : Configurable {

    private lateinit var settingsComponent: RemoteFilePluginSettingsComponent

    override fun createComponent(): JComponent {
        settingsComponent = RemoteFilePluginSettingsComponent()
        return settingsComponent.panel
    }

    override fun isModified(): Boolean {
        return settingsComponent.isModified()
    }

    override fun apply() {
        // todo draw a tree (check if init and connected and draw)
        // according to guidelines on ok and conf change I should perform an action and draw a tree
        settingsComponent.saveState()
        val clientService = service<RemoteOperations>()
        if (clientService.init()) {
            ApplicationManager.getApplication().messageBus.syncPublisher(RemoteConnectionListener.TOPIC).connectionStatusChanged()
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent.getPreferredFocusedComponent()
    }

    override fun getDisplayName(): String {
        return "SSH Configuration"
    }
}
