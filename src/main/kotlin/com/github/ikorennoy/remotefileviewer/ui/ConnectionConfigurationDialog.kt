package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.*
import java.util.concurrent.ThreadLocalRandom
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class ConnectionConfigurationDialog(project: Project) : DialogWrapper(project) {
    private val DEFAULT_PORT = 22

    private val hostField = ExtendableTextField(COLUMNS_SHORT)
    private val usernameField = JBTextField(COLUMNS_SHORT)
    private val passwordField = JBPasswordField()


    private val loadingExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    private val uiDispatcher get() = Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()
    private val scope = CoroutineScope(SupervisorJob()).also { Disposer.register(disposable) { it.cancel() } }

    private var accessError: ValidationInfo? = null

    init {
        passwordField.columns = COLUMNS_SHORT
        title = FileViewerBundle.message("connection.configuration.dialog.name")
        init()
    }

    var port: Int = DEFAULT_PORT
    val host: String get() = hostField.text.orEmpty().trim()
    val username: String get() = usernameField.text.orEmpty().trim()
    val password: CharArray get() = passwordField.password
    lateinit var portField: JBTextField


    override fun createCenterPanel(): JComponent = panel {
        row {
            label(FileViewerBundle.message("connection.configuration.dialog.host"))
                .widthGroup("CredentialsLabel")
            cell(hostField)
                .validationOnApply { checkHostNotBlank() ?: accessError }
                .applyToComponent { clearUrlAccessErrorOnTextChanged() }
                .focused()

            label("Port:")
            portField = intTextField(0..65536)
                .bindIntText(::port)
                .text("22").component
        }
        row {
            label(FileViewerBundle.message("connection.configuration.dialog.username"))
                .widthGroup("CredentialsLabel")
            cell(usernameField)
                .validationOnApply { checkUsernameNotBlank() }
        }
        row {
            label(FileViewerBundle.message("connection.configuration.dialog.password"))
                .widthGroup("CredentialsLabel")
            cell(passwordField)
        }
    }

    override fun doOKAction() {
        scope.launch(uiDispatcher) {
            setLoading(true)
            try {
                accessError = checkAccess()
            } finally {
                setLoading(false)
            }
            if (accessError == null) {
                super.doOKAction()
            } else {
                IdeFocusManager.getGlobalInstance().requestFocus(hostField, true)
                startTrackingValidation()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        portField.isEnabled = !isLoading
        hostField.isEnabled = !isLoading
        usernameField.isEnabled = !isLoading
        passwordField.isEnabled = !isLoading

        hostField.apply { if (isLoading) addExtension(loadingExtension) else removeExtension(loadingExtension) }
        hostField.isEnabled = !isLoading

        isOKActionEnabled = !isLoading
    }

    private fun checkHostNotBlank(): ValidationInfo? =
        if (host.isNotEmpty()) null
        else ValidationInfo(
            FileViewerBundle.message("connection.configuration.dialog.host.empty.validation"),
            hostField
        )

    private fun checkUsernameNotBlank(): ValidationInfo? =
        if (username.isNotEmpty()) null
        else ValidationInfo(
            FileViewerBundle.message("connection.configuration.dialog.username.empty.validation"),
            usernameField
        )

    private suspend fun checkAccess(): ValidationInfo? {
        val result = test()

        if (result) return null

        return ValidationInfo(
            FileViewerBundle.message("connection.configuration.dialog.cannot.connect.validation"),
            hostField
        ).withOKEnabled()
    }

    private suspend fun test(): Boolean {
        withContext(Dispatchers.IO) {
            runBlocking { delay(3000) }
        }
        return ThreadLocalRandom.current().nextBoolean()
    }

    private fun JBTextField.clearUrlAccessErrorOnTextChanged() =
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                accessError = null
            }
        })
}
