package com.github.ikorennoy.remotefileviewer.settings.ui

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

// todo add validation
class RemoteFilePluginSettingsComponent {
    private val state = service<RemoteFileViewerSettingsState>()

    private val hostField = ExtendableTextField(COLUMNS_MEDIUM)
    private val usernameField = JBTextField(COLUMNS_MEDIUM)
    private val passwordField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_MEDIUM)

    val panel: JPanel

    init {
        hostField.text = state.host
        portField.text = state.port.toString()
        usernameField.text = state.username
        rootField.text = state.root
        passwordField.setPasswordIsStored(state.password.isNotEmpty())

        panel = panel {
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.host"))
                    .widthGroup("CredentialsLabel")
                cell(hostField)

                label(FileViewerBundle.message("connection.configuration.dialog.port"))
                cell(portField)
            }
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.root"))
                    .widthGroup("CredentialsLabel")
                cell(rootField)
            }
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.username"))
                    .widthGroup("CredentialsLabel")
                cell(usernameField)
            }
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.password"))
                    .widthGroup("CredentialsLabel")
                cell(passwordField)
            }
            row {
                var icon: Cell<JLabel>? = null
                button("Test Connection") {
                    saveState()
                    val clientService = service<RemoteOperations>()
                    if (clientService.init()) {
                        icon?.visible(true)
                    }
                }
                icon = icon(AllIcons.Actions.Commit)
                icon.visible(false)
            }
        }
    }

    fun saveState() {
        if (hostField.text.isEmpty()) {
            throw ConfigurationException("Host can't be blank")
        }
        val portVal = portField.text.toIntOrNull() ?: throw ConfigurationException("Port can't be blank")
        if (portVal !in 0..65535) {
            throw ConfigurationException("Port must be between 0 and 65535")
        }
        if (rootField.text.isEmpty()) {
            throw ConfigurationException("Root path can't be blank")
        }
        if (usernameField.text.isEmpty()) {
            throw ConfigurationException("Username can't be blank")
        }
        state.host = hostField.text.trim()
        state.port = portField.text.toInt()
        state.root = rootField.text.trim()
        state.username = usernameField.text.trim()
        state.password = passwordField.password
    }

    fun isModified(): Boolean {
        return state.host != hostField.text ||
                state.port.toString() != portField.text ||
                state.root != rootField.text ||
                state.username != usernameField.text ||
                passwordField.password.isNotEmpty()
    }

    fun getPreferredFocusedComponent(): JComponent {
        return if (hostField.text.isEmpty()) {
            hostField
        } else if (portField.text.isEmpty()) {
            portField
        } else if (usernameField.text.isEmpty()) {
            usernameField
        } else {
            passwordField
        }
    }

    private fun checkHostIsNotBlank(): ValidationInfo? {
        return if (hostField.text.isNotEmpty()) {
            null
        } else {
            ValidationInfo(
                FileViewerBundle.message("connection.configuration.dialog.host.empty.validation"),
                hostField
            )
        }
    }

}
