package com.gmail.volkovskiyda.abit.core.data

/**
 * Identifies which device wrote a session, for reading a divergence after the fact. It never decides
 * one — that is [SyncEngine]'s last-write-wins on `updatedAt` — so it does not have to be stable
 * across reinstalls and is deliberately not a hardware or advertising identifier.
 */
expect fun deviceId(): String
