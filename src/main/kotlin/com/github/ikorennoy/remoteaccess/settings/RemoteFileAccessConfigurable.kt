package com.github.ikorennoy.remoteaccess.settings

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.settings.ui.RemoteFileAccessSettingsComponent
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class RemoteFileAccessConfigurable(private val project: Project) : Configurable {

    private var settingsComponent: RemoteFileAccessSettingsComponent? = null

    override fun createComponent(): JComponent {
        if (settingsComponent == null) {
            settingsComponent = RemoteFileAccessSettingsComponent(project)
        }
        return settingsComponent?.panel ?: throw IllegalStateException()
    }

    override fun isModified(): Boolean {
        return settingsComponent?.isModified() ?: throw IllegalStateException()
    }

    override fun apply() {
        settingsComponent?.saveState()
        val remoteOperations = RemoteOperations.getInstance(project)
        remoteOperations.initWithModalDialogue()
        notifyRebuildTree()
    }

    override fun reset() {
        settingsComponent?.reset()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent?.getPreferredFocusedComponent() ?: throw IllegalStateException()
    }

    override fun getDisplayName(): String {
        return RemoteFileAccessBundle.message("settings.RemoteFileAccess.configurable.displayName")
    }
}
