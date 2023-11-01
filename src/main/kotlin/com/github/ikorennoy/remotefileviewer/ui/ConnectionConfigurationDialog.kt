package com.github.ikorennoy.remotefileviewer.ui

import com.github.ikorennoy.remotefileviewer.filesystem.*
import com.github.ikorennoy.remotefileviewer.template.FileViewerBundle
import com.intellij.ide.util.PropertiesComponent
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
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

private const val HOST_PROPERTY = "REMOTE_FILE_SYSTEM_PLUGIN_HOST"
private const val PORT_PROPERTY = "REMOTE_FILE_SYSTEM_PLUGIN_PORT"
private const val USERNAME_PROPERTY = "REMOTE_FILE_SYSTEM_PLUGIN_USERNAME"
private const val ROOT_PROPERTY = "REMOTE_FILE_SYSTEM_PLUGIN_ROOT"

class ConnectionConfigurationDialog(project: Project, private val remoteFs: SftpFileSystem) : DialogWrapper(project) {

    private val persistentConfiguration = PropertiesComponent.getInstance()

    private val hostField = ExtendableTextField(COLUMNS_SHORT)
    private val usernameField = JBTextField(COLUMNS_SHORT)
    private val passwordField = JBPasswordField()
    private val portField: JBTextField = JBTextField(COLUMNS_TINY)
    private val rootField: JBTextField = JBTextField(COLUMNS_SHORT)

    init {
        hostField.text = persistentConfiguration.getValue(HOST_PROPERTY, "")
        portField.text = persistentConfiguration.getValue(PORT_PROPERTY, "22")
        usernameField.text = persistentConfiguration.getValue(USERNAME_PROPERTY, "")
        rootField.text = persistentConfiguration.getValue(ROOT_PROPERTY, "")
    }


    private val loadingExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    private val uiDispatcher get() = Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()
    private val scope = CoroutineScope(SupervisorJob()).also { Disposer.register(disposable) { it.cancel() } }

    private var accessError: ValidationInfo? = null

    init {
        passwordField.columns = COLUMNS_SHORT
        title = FileViewerBundle.message("connection.configuration.dialog.name")
        init()
    }

    var port: Int = portField.text.toInt()
    val host: String get() = hostField.text
    val root: String get() = rootField.text
    val username: String get() = usernameField.text
    val password: CharArray get() = passwordField.password


    override fun getPreferredFocusedComponent(): JComponent {
        return if (hostField.text.isEmpty()) {
            hostField
        } else if (usernameField.text.isEmpty()) {
            usernameField
        } else {
            passwordField
        }
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            label(FileViewerBundle.message("connection.configuration.dialog.host"))
                .widthGroup("CredentialsLabel")
            cell(hostField)
                .validationOnApply { checkHostNotBlank() ?: accessError }
                .applyToComponent { clearUrlAccessErrorOnTextChanged() }
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
                .validationOnApply { checkUsernameNotBlank() }
                .applyToComponent { clearUrlAccessErrorOnTextChanged() }
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
            saveState()
            try {
                accessError = checkAccess()
            } finally {
                setLoading(false)
            }
            if (accessError == null) {
                super.doOKAction()
            } else {
                if (hostField == accessError?.component) {
                    IdeFocusManager.getGlobalInstance().requestFocus(hostField, true)
                } else if (usernameField == accessError?.component) {
                    IdeFocusManager.getGlobalInstance().requestFocus(usernameField, true)
                }
                startTrackingValidation()
            }
        }
    }

    private fun saveState() {
        persistentConfiguration.setValue(HOST_PROPERTY, hostField.text.trim())
        persistentConfiguration.setValue(PORT_PROPERTY, portField.text)
        persistentConfiguration.setValue(ROOT_PROPERTY, rootField.text.trim())
        persistentConfiguration.setValue(USERNAME_PROPERTY, usernameField.text.trim())
    }

    private fun setLoading(isLoading: Boolean) {
        rootField.isEnabled = !isLoading
        portField.isEnabled = !isLoading
        hostField.isEnabled = !isLoading
        usernameField.isEnabled = !isLoading
        passwordField.isEnabled = !isLoading

        hostField.apply { if (isLoading) addExtension(loadingExtension) else removeExtension(loadingExtension) }
        hostField.isEnabled = !isLoading

        isOKActionEnabled = !isLoading
    }

    private fun checkHostNotBlank(): ValidationInfo? =
        if (hostField.text.isNotEmpty()) null
        else ValidationInfo(
            FileViewerBundle.message("connection.configuration.dialog.host.empty.validation"),
            hostField
        )

    private fun checkUsernameNotBlank(): ValidationInfo? =
        if (usernameField.text.isNotEmpty()) null
        else ValidationInfo(
            FileViewerBundle.message("connection.configuration.dialog.username.empty.validation"),
            usernameField
        )

    private suspend fun checkAccess(): ValidationInfo? {
        return when (val result = testConnection()) {
            is Ok -> null
            is CannotFindHost -> ValidationInfo(
                result.message,
                hostField
            ).withOKEnabled()

            is UsernameOrPassword -> ValidationInfo(
                result.message,
                usernameField
            )

            is UnknownRequestError -> ValidationInfo(
                result.message,
                hostField
            )
        }
    }

    private suspend fun testConnection(): RequestResult {
        return withContext(Dispatchers.IO) {
            runBlocking {
//                remoteFs.init(host, port, root, username, password)
                Ok()
            }
        }
    }

    private fun JBTextField.clearUrlAccessErrorOnTextChanged() =
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                accessError = null
            }
        })
}
