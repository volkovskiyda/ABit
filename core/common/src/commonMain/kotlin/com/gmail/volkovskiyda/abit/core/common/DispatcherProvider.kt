package com.gmail.volkovskiyda.abit.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The dispatchers the app runs on, injected rather than referenced directly, so a test can replace
 * all three at once with a single [kotlinx.coroutines.test.TestDispatcher].
 *
 * There is no `Dispatchers.IO` on Kotlin/Wasm — the browser has one thread — which is the other
 * reason this is an interface: [DefaultDispatcherProvider] resolves [io] per platform.
 */
interface DispatcherProvider {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = ioDispatcher()
    override val main: CoroutineDispatcher get() = Dispatchers.Main
}

/** `Dispatchers.IO` where the platform has one, `Dispatchers.Default` where it does not. */
internal expect fun ioDispatcher(): CoroutineDispatcher
