package com.gmail.volkovskiyda.abit.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer

/**
 * A [DataStore] over the browser's `localStorage`.
 *
 * Hand-rolled because DataStore 1.2.1 has no working wasm implementation — its `DataStoreFactory`
 * actual for that target is `TODO()`. The stored bytes still go through [UserPreferencesSerializer],
 * so what the browser writes is byte-for-byte what the phone and the desktop write, and a future
 * real implementation can read it.
 *
 * `localStorage` is synchronous and per-origin. The [Mutex] therefore guards against interleaved
 * read-modify-write from concurrent coroutines rather than against threads, of which the browser
 * has one.
 */
internal class LocalStorageDataStore(
    private val key: String,
    private val serializer: UserPreferencesSerializer,
) : DataStore<UserPreferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(serializer.defaultValue)

    override val data: Flow<UserPreferences> = state.asStateFlow()

    suspend fun load() {
        mutex.withLock {
            val stored = runCatching { localStorage.getItem(key) }.getOrNull() ?: return
            // A value that will not parse is treated as absent rather than fatal: a half-written or
            // older entry must not stop the app from starting.
            state.value =
                runCatching {
                    serializer.readFrom(Buffer().writeUtf8(stored))
                }.getOrDefault(serializer.defaultValue)
        }
    }

    override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
        mutex.withLock {
            val updated = transform(state.value)
            val buffer = Buffer()
            serializer.writeTo(updated, buffer)
            // A quota error is the realistic failure and it must not take the app down; the in-memory
            // value still updates, so the session behaves correctly and only persistence is lost.
            runCatching { localStorage.setItem(key, buffer.readUtf8()) }
            state.value = updated
            updated
        }
}
