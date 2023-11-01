package com.github.ikorennoy.remotefileviewer.settings.ui

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.sftp.SftpClientService
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
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
                    .focused()

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
                    val clientService = service<SftpClientService>()
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
}
