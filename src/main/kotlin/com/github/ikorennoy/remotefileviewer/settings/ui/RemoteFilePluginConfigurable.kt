package com.github.ikorennoy.remotefileviewer.settings.ui

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
        settingsComponent.saveState()
    }



    override fun getDisplayName(): String {
        return "SSH Configuration"
    }
}
