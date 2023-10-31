package com.github.ikorennoy.remotefileviewer.settings.ui

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.swing.JPanel


class RemoteFilePluginSettingsComponent {
    private val state = service<RemoteFileViewerSettingsState>()

    private val hostField = ExtendableTextField(COLUMNS_MEDIUM)
    private val usernameField = JBTextField(COLUMNS_MEDIUM)
    private val passwordField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_MEDIUM)

    private val loadingExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    private val uiDispatcher get() = Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()
    private val scope = CoroutineScope(SupervisorJob()) //.also { Disposer.register(disposable) { it.cancel() } }

    val panel: JPanel

    init {
        hostField.text = state.host
        portField.text = state.port.toString()
        usernameField.text = state.username
        rootField.text = state.root
        panel = panel {
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.host"))
                    .widthGroup("CredentialsLabel")
                cell(hostField)
//                    .validationOnApply { checkHostNotBlank() ?: accessError }
//                    .applyToComponent { clearUrlAccessErrorOnTextChanged() }
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
//                    .validationOnApply { checkUsernameNotBlank() }
//                    .applyToComponent { clearUrlAccessErrorOnTextChanged() }
            }
            row {
                label(FileViewerBundle.message("connection.configuration.dialog.password"))
                    .widthGroup("CredentialsLabel")
                cell(passwordField)
            }
            row {
                button("Test Connection") {
                    println("test connection")
                }
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

    fun modified(): Boolean {
        return state.host != hostField.text ||
                state.port.toString() != portField.text ||
                state.root != rootField.text ||
                state.username != usernameField.text ||
                passwordField.password.isNotEmpty()
    }
}
