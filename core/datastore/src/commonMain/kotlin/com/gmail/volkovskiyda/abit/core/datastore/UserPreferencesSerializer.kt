package com.gmail.volkovskiyda.abit.core.datastore

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * JSON rather than protobuf: the file is small, the schema is this project's own, and a readable
 * file is worth more during development than a few bytes.
 *
 * `ignoreUnknownKeys` and the defaults on every field are what make adding or removing a preference
 * a non-event — a file written by an older build still reads.
 */
object UserPreferencesSerializer : OkioSerializer<UserPreferences> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(source: BufferedSource): UserPreferences =
        json.decodeFromString(UserPreferences.serializer(), source.readUtf8())

    override suspend fun writeTo(
        t: UserPreferences,
        sink: BufferedSink,
    ) {
        sink.writeUtf8(json.encodeToString(UserPreferences.serializer(), t))
    }
}

/** The file name every platform stores these under, wherever that platform puts app data. */
const val USER_PREFERENCES_FILE_NAME: String = "user_preferences.json"
