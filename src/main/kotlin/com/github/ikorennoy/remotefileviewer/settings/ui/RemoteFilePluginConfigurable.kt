package com.github.ikorennoy.remotefileviewer.settings.ui

import com.github.ikorennoy.remotefileviewer.sftp.SftpClientService
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
        settingsComponent.saveState()
        val sftpService = service<SftpClientService>()
        sftpService.init() // ensures connection
    }

    override fun getDisplayName(): String {
        return "SSH Configuration"
    }


}
