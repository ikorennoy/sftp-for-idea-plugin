package com.github.ikorennoy.remotefileviewer.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Transient

@Service
@State(
    name = "com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState",
    storages = [Storage("RemoteFileViewerPlugin.xml")]
)
class RemoteFileViewerSettingsState :
    SimplePersistentStateComponent<RemoteFileViewerSettingsState.ConfigurationState>(ConfigurationState()) {

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
        get() = state.password.clone()
        set(value) {
            state.password = value
        }

    // valid means we have all fields not empty
    // in that case we just return true

    // but it's possible to have an empty password
    // in that case we return false
    // it's a responsibility of connection state manager service to process this situation
    // usual flow will be: show enter password dialogue
    // but write that password could be empty and in case user left password empty just attempt to connect
    // if connection is successful, then password is not necessary, but connection already exist, so the call to this
    // method is not required anymore
    // if connection is not established we keep to asking for password enter on every attempt, that's the limitation
    // it's avoidable by a checkbox, but I decided to omit it for now for simplicity

    // todo remove
    fun isNotValidWithEmptyPassword(): Boolean {
        return host.isEmpty() || username.isEmpty() || root.isEmpty()
    }

    fun isNotValid(): Boolean {
        return host.isEmpty() || username.isEmpty() || root.isEmpty()
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
