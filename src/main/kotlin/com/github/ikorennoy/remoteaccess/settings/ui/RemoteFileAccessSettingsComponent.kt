package com.github.ikorennoy.remoteaccess.settings.ui

import com.github.ikorennoy.remoteaccess.notifyUpdateFullTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState.AuthenticationType.KEYPAIR
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState.AuthenticationType.PASSWORD
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val CONF_WIDTH_GROUP = "CredentialsLabel"
private const val PASSWORD_AUTH = "Password"
private const val KEY_PAIR_AUTH = "Key Pair"

class RemoteFileAccessSettingsComponent(private val project: Project) {

    private val hostField = JBTextField(COLUMNS_MEDIUM)
    private val usernameField = JBTextField(COLUMNS_MEDIUM)
    private val passwordField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val passphraseField = JBPasswordField().also { it.columns = COLUMNS_MEDIUM }
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_MEDIUM)

    private var testConnectionButton: JButton? = null
    private var disconnectLink: ActionLink? = null
    private var authenticationTypeComboBox: ComboBox<String>? = null
    private var certificateTextField: TextFieldWithBrowseButton? = null

    private var connectionTested = false

    val panel: JPanel

    init {
        panel = panel {
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.host"))
                    .widthGroup(CONF_WIDTH_GROUP)
                cell(hostField)

                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.port"))
                cell(portField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.root"))
                    .widthGroup(CONF_WIDTH_GROUP)
                cell(rootField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.username"))
                    .widthGroup(CONF_WIDTH_GROUP)
                cell(usernameField)
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.authType.text"))
                    .widthGroup(CONF_WIDTH_GROUP)
                authenticationTypeComboBox = comboBox(listOf(PASSWORD_AUTH, KEY_PAIR_AUTH)).component
            }
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.authType.pk.file"))
                    .widthGroup(CONF_WIDTH_GROUP)
                certificateTextField = textFieldWithBrowseButton().columns(COLUMNS_MEDIUM).component
            }.visibleIf(comboBoxPredicate(KEY_PAIR_AUTH))
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.authType.pk.passphrace"))
                    .widthGroup(CONF_WIDTH_GROUP)
                cell(passphraseField)
            }.visibleIf(comboBoxPredicate(KEY_PAIR_AUTH))
            row {
                label(RemoteFileAccessBundle.message("settings.RemoteFileAccess.label.password"))
                    .widthGroup(CONF_WIDTH_GROUP)
                cell(passwordField)
            }.visibleIf(comboBoxPredicate(PASSWORD_AUTH))

            row {
                var errorIcon: Cell<JLabel>? = null
                var okIcon: Cell<JLabel>? = null
                var loadingIcon: Cell<JLabel>? = null
                var errorLink: Cell<ActionLink>? = null
                var possibleError: Exception? = null

                testConnectionButton =
                    button(RemoteFileAccessBundle.message("settings.RemoteFileAccess.testConnectionButton.text")) {
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
        authenticationTypeComboBox?.item = if (conf.authenticationType == PASSWORD) {
            PASSWORD_AUTH
        } else {
            certificateTextField?.text = conf.certificateLocation
            KEY_PAIR_AUTH
        }

        if (ops.isInitializedAndConnected()) {
            hostField.isEnabled = false
            portField.isEnabled = false
            usernameField.isEnabled = false
            rootField.isEnabled = false
            passwordField.setPasswordIsStored(true)
            passphraseField.setPasswordIsStored(true)
            passwordField.isEnabled = false
            testConnectionButton?.isEnabled = false
            authenticationTypeComboBox?.isEnabled = false
            certificateTextField?.isEnabled = false
            passphraseField.isEnabled = false
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
        conf.authenticationType = if (authenticationTypeComboBox?.item == PASSWORD_AUTH) {
            conf.password = passwordField.password
            PASSWORD
        } else {
            conf.password = passphraseField.password
            conf.certificateLocation = certificateTextField?.text?.trim() ?: ""
            KEYPAIR
        }
    }

    fun isModified(): Boolean {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        return connectionTested ||
                conf.host != hostField.text ||
                conf.port.toString() != portField.text ||
                conf.root != rootField.text ||
                conf.username != usernameField.text ||
                conf.certificateLocation != certificateTextField?.text ||
                authenticationTypeComboBoxChanged(conf) ||
                passwordField.password.isNotEmpty()
    }

    private fun authenticationTypeComboBoxChanged(conf: RemoteFileAccessSettingsState): Boolean {
        return conf.authenticationType == KEYPAIR && authenticationTypeComboBox?.item == PASSWORD_AUTH ||
                conf.authenticationType == PASSWORD && authenticationTypeComboBox?.item == KEY_PAIR_AUTH
    }

    private fun comboBoxPredicate(value: String): ComponentPredicate {
        return object : ComponentPredicate() {
            override fun addListener(listener: (Boolean) -> Unit) {
                authenticationTypeComboBox?.addActionListener { listener(invoke()) }
            }

            override fun invoke(): Boolean {
                return authenticationTypeComboBox?.item == value
            }
        }
    }

    private fun disconnect() {
        ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
            RemoteOperations.getInstance(project).close()
        }.handleOnEdt(ModalityState.defaultModalityState()) { _, _ ->
            hostField.isEnabled = true
            portField.isEnabled = true
            usernameField.isEnabled = true
            rootField.isEnabled = true
            passwordField.setPasswordIsStored(false)
            passphraseField.setPasswordIsStored(false)
            passwordField.isEnabled = true
            testConnectionButton?.isEnabled = true
            authenticationTypeComboBox?.isEnabled = true
            certificateTextField?.isEnabled = true
            passphraseField.isEnabled = true
            disconnectLink?.isEnabled = false
            notifyUpdateFullTree()
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
