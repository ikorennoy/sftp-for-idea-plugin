package com.github.ikorennoy.remote.settings.ui

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class RemoteFileAccessConfigurable : Configurable {

    private lateinit var settingsComponent: RemoteFileAccessSettingsComponent

    override fun createComponent(): JComponent {
        settingsComponent = RemoteFileAccessSettingsComponent()
        return settingsComponent.panel
    }

    override fun isModified(): Boolean {
        return settingsComponent.isModified()
    }

    override fun apply() {
        // todo draw a tree (check if init and connected and draw)
        // according to guidelines on ok and conf change I should perform an action and draw a tree
        settingsComponent.saveState()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent.getPreferredFocusedComponent()
    }

    override fun getDisplayName(): String {
        return "SSH Configuration"
    }
}
