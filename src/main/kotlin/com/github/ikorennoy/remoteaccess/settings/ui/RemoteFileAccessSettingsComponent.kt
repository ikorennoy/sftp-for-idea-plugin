package com.github.ikorennoy.remoteaccess.settings.ui

import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
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

class RemoteFileAccessSettingsComponent(private val project: Project) {
    private val state = service<RemoteFileAccessSettingsState>()

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
                label(RemoteFileAccessBundle.message("connection.configuration.dialog.host"))
                    .widthGroup("CredentialsLabel")
                cell(hostField)

                label(RemoteFileAccessBundle.message("connection.configuration.dialog.port"))
                cell(portField)
            }
            row {
                label(RemoteFileAccessBundle.message("connection.configuration.dialog.root"))
                    .widthGroup("CredentialsLabel")
                cell(rootField)
            }
            row {
                label(RemoteFileAccessBundle.message("connection.configuration.dialog.username"))
                    .widthGroup("CredentialsLabel")
                cell(usernameField)
            }
            row {
                label(RemoteFileAccessBundle.message("connection.configuration.dialog.password"))
                    .widthGroup("CredentialsLabel")
                cell(passwordField)
            }
            row {
                var errorIcon: Cell<JLabel>? = null
                var okIcon: Cell<JLabel>? = null
                var loadingIcon: Cell<JLabel>? = null
                var errorLink: Cell<ActionLink>? = null
                var possibleError: Exception? = null


                button("Test Connection") {
                    errorLink?.visible(false)
                    errorIcon?.visible(false)
                    okIcon?.visible(false)
                    loadingIcon?.visible(true)
                    saveState()

                    val clientService = RemoteOperations.getInstance(project)
                    // try connect

                    ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
                        clientService.initSilently()
                    }.handleOnEdt(ModalityState.defaultModalityState()) { possibleConnectionError, _ ->
                        loadingIcon?.visible(false)
                        if (possibleConnectionError == null) {
                            okIcon?.visible(true)
                        } else {
                            possibleError = possibleConnectionError
                            errorIcon?.visible(true)
                            errorLink?.visible(true)
                        }
                    }
                }
                loadingIcon = icon(AnimatedIcon.Default.INSTANCE).visible(false)
                errorIcon = icon(AllIcons.CodeWithMe.CwmTerminate).visible(false)
                okIcon = icon(AllIcons.Actions.Commit).visible(false)
                errorLink = link("Details") {
                    val thisErrorLink = errorLink ?: return@link
                    showErrorDetailsBalloon(possibleError, thisErrorLink.component)
                }.visible(false)
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

    private fun showErrorDetailsBalloon(possibleError: Exception?, component: JComponent) {
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
            possibleError?.message ?: "Unknown Error",
            MessageType.ERROR,
            null
        ).setShowCallout(false)
            .setHideOnClickOutside(true)
            .setHideOnAction(true)
            .setHideOnFrameResize(true)
            .setHideOnKeyOutside(true)
            .createBalloon()
            .show(RelativePoint.getSouthEastOf(component), Balloon.Position.above)
    }
}
