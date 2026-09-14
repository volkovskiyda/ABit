package com.gmail.volkovskiyda.abit.core.observability

/**
 * Crash reporting, as the app sees it. The only implementation that reports anywhere is the Android
 * one (Crashlytics); the desktop and web builds bind [NoopCrashReporter], because neither platform
 * has a crash reporter this project is willing to add.
 */
interface CrashReporter {
    fun log(message: String)

    fun recordException(throwable: Throwable)

    fun setUserId(id: String?)

    fun setCustomKey(
        key: String,
        value: String,
    )
}

/**
 * One name, three sinks. A [Span] is written once and read by the platform tracer (Perfetto on
 * Android), by Firebase Performance and by Kotzilla, so a measurement means the same thing wherever
 * it is read. Adding a span means adding it to [Spans], not spelling a string at the call site.
 */
data class Span(
    val id: String,
)

object Spans {
    val AppStart = Span("abit.app.start")
    val KoinStart = Span("abit.koin.start")
    val SyncCycle = Span("abit.sync.cycle")
}

interface Tracer {
    suspend fun <T> trace(
        span: Span,
        block: suspend () -> T,
    ): T

    fun mark(label: String)
}

object NoopCrashReporter : CrashReporter {
    override fun log(message: String) = Unit

    override fun recordException(throwable: Throwable) = Unit

    override fun setUserId(id: String?) = Unit

    override fun setCustomKey(
        key: String,
        value: String,
    ) = Unit
}

object NoopTracer : Tracer {
    override suspend fun <T> trace(
        span: Span,
        block: suspend () -> T,
    ): T = block()

    override fun mark(label: String) = Unit
}
