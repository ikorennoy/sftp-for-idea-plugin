package com.github.ikorennoy.remoteaccess.settings.ui

import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
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

    private val hostField = ExtendableTextField(COLUMNS_MEDIUM)
    private val usernameField = JBTextField(COLUMNS_MEDIUM)
    private val passwordField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_MEDIUM)

    private var connectionTested = false

    val panel: JPanel

    init {
        panel = panel {
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.host"))
                    .widthGroup("CredentialsLabel")
                cell(hostField)

                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.port"))
                cell(portField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.root"))
                    .widthGroup("CredentialsLabel")
                cell(rootField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.username"))
                    .widthGroup("CredentialsLabel")
                cell(usernameField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.password"))
                    .widthGroup("CredentialsLabel")
                cell(passwordField)
            }
            row {
                var errorIcon: Cell<JLabel>? = null
                var okIcon: Cell<JLabel>? = null
                var loadingIcon: Cell<JLabel>? = null
                var errorLink: Cell<ActionLink>? = null
                var possibleError: Exception? = null
                button(RemoteFileAccessBundle.message("settings.RemoteFileAccess.testConnectionButton.text")) {
                    errorLink?.visible(false)
                    errorIcon?.visible(false)
                    okIcon?.visible(false)
                    loadingIcon?.visible(true)
                    connectionTested = true

                    if (isModified()) {
                        saveState()
                    }

                    val clientService = RemoteOperations.getInstance(project)
                    // try to connect
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
                errorIcon = icon(AllIcons.General.BalloonError).visible(false)
                okIcon = icon(AllIcons.Actions.Commit).visible(false)
                errorLink = link(RemoteFileAccessBundle.message("settings.RemoteFileAccess.errorDetails.link.text")) {
                    val thisErrorLink = errorLink ?: return@link
                    showErrorDetailsBalloon(possibleError, thisErrorLink.component)
                }.visible(false)
            }
        }
    }

    fun saveState() {
        val state = RemoteFileAccessSettingsState.getInstance(project)
        state.host = hostField.text.trim()
        state.port = portField.text.toInt()
        state.root = rootField.text.trim()
        state.username = usernameField.text.trim()
        state.password = passwordField.password
    }

    fun reset() {
        val state = RemoteFileAccessSettingsState.getInstance(project)
        hostField.text = state.host
        portField.text = state.port.toString()
        usernameField.text = state.username
        rootField.text = state.root
        passwordField.setPasswordIsStored(state.password.isNotEmpty())
    }

    fun isModified(): Boolean {
        val state = RemoteFileAccessSettingsState.getInstance(project)
        return connectionTested ||
                state.host != hostField.text ||
                state.port.toString() != portField.text ||
                state.root != rootField.text ||
                state.username != usernameField.text ||
                !state.password.contentEquals(passwordField.password)
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
