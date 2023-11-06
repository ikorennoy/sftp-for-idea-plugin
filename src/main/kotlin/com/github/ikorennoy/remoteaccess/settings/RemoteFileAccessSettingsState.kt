package com.github.ikorennoy.remoteaccess.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Transient

@Service(Service.Level.PROJECT)
@State(
    name = "com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState",
    storages = [Storage("RemoteFileAccessPlugin.xml", roamingType = RoamingType.DISABLED)]
)
class RemoteFileAccessSettingsState :
    SimplePersistentStateComponent<RemoteFileAccessSettingsState.ConfigurationState>(ConfigurationState()) {

    var host: String
        get() = state.host ?: ""
        set(value) {
            state.host = value
        }

    var port: Int
        get() = state.port
        set(value) {
            state.port = value
        }

    var root: String
        get() = state.root ?: ""
        set(value) {
            state.root = value
        }

    var username: String
        get() = state.username ?: ""
        set(value) {
            state.username = value
        }

    var password: CharArray
        get() = state.password
        set(value) {
            state.password = value
        }

    fun isNotValid(): Boolean {
        return host.isEmpty() || username.isEmpty() || root.isEmpty()
    }

    companion object {
        fun getInstance(project: Project): RemoteFileAccessSettingsState = project.service()
    }

    class ConfigurationState : BaseState() {
        var host by string()
        var port: Int by property(22)
        var username by string()
        var root by string()

        @Transient
        var password: CharArray = CharArray(0)
    }
}
