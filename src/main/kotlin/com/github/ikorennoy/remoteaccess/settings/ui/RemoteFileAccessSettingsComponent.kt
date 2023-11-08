package com.github.ikorennoy.remoteaccess.settings.ui

import com.github.ikorennoy.remoteaccess.notifyConnectionStatusChanged
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
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class RemoteFileAccessSettingsComponent(private val project: Project) {

    private val hostField = ExtendableTextField(COLUMNS_MEDIUM)
    private val usernameField = JBTextField(COLUMNS_MEDIUM)
    private val passwordField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_MEDIUM)
    private var testConnectionButton: JButton? = null
    private var disconnectLink: ActionLink? = null

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

                testConnectionButton = button(RemoteFileAccessBundle.message("settings.RemoteFileAccess.testConnectionButton.text")) {
                    errorLink?.visible(false)
                    errorIcon?.visible(false)
                    okIcon?.visible(false)
                    loadingIcon?.visible(true)
                    connectionTested = true

                    if (isModified()) {
                        saveState()
                    }

                    val remoteOperations = RemoteOperations.getInstance(project)
                    // try to connect
                    ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
                        remoteOperations.initSilently()
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
                }.component
                disconnectLink = link(RemoteFileAccessBundle.message("settings.RemoteFileAccess.disconnectLink.text")) {
                    disconnect()
                }.component

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

    fun reset() {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        val ops = RemoteOperations.getInstance(project)

        hostField.text = conf.host
        portField.text = conf.port.toString()
        usernameField.text = conf.username
        rootField.text = conf.root

        if (ops.isInitializedAndConnected()) {
            hostField.isEnabled = false
            portField.isEnabled = false
            usernameField.isEnabled = false
            rootField.isEnabled = false
            passwordField.setPasswordIsStored(true)
            passwordField.isEnabled = false
            testConnectionButton?.isEnabled = false
            disconnectLink?.isEnabled = true
        } else {
            disconnectLink?.isEnabled = false
        }
    }

    fun saveState() {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        conf.host = hostField.text.trim()
        conf.port = portField.text.toInt()
        conf.root = rootField.text.trim()
        conf.username = usernameField.text.trim()
        conf.password = passwordField.password
    }

    fun isModified(): Boolean {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        return connectionTested ||
                conf.host != hostField.text ||
                conf.port.toString() != portField.text ||
                conf.root != rootField.text ||
                conf.username != usernameField.text ||
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

    private fun disconnect() {
        ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
            RemoteOperations.getInstance(project).close()
        }.handleOnEdt(ModalityState.defaultModalityState()) { _,_ ->
            hostField.isEnabled = true
            portField.isEnabled = true
            usernameField.isEnabled = true
            rootField.isEnabled = true
            passwordField.setPasswordIsStored(false)
            passwordField.isEnabled = true
            testConnectionButton?.isEnabled = true
            disconnectLink?.isEnabled = false
            notifyConnectionStatusChanged()
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
