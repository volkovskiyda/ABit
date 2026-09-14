package com.gmail.volkovskiyda.abit.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The browser runs one thread, so there is no `Dispatchers.IO` to offload to and blocking work has
 * to be handed to a Web Worker instead (which is what the SQLite driver does — see `core:database`).
 */
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
