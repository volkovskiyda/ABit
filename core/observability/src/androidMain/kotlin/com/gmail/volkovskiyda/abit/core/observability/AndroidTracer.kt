package com.gmail.volkovskiyda.abit.core.observability

import androidx.tracing.trace
import com.google.firebase.perf.FirebasePerformance

/**
 * One [Span], two sinks. `androidx.tracing` puts a section on the Perfetto timeline, which is what a
 * macrobenchmark's `TraceSectionMetric` measures; Firebase Performance records the same span as a
 * custom trace, which is what shows up in the console for real installs. Naming them from the same
 * [Span] is the point — a measurement means the same thing wherever it is read.
 */
internal class AndroidTracer : Tracer {
    override suspend fun <T> trace(
        span: Span,
        block: suspend () -> T,
    ): T {
        val firebaseTrace = FirebasePerformance.getInstance().newTrace(span.id)
        firebaseTrace.start()
        return try {
            // The platform section is synchronous by nature: it brackets the suspend call, so the
            // slice on the timeline is wall-clock elapsed time rather than time on a thread.
            trace(span.id) { block() }
        } finally {
            firebaseTrace.stop()
        }
    }

    override fun mark(label: String) {
        // A zero-width instant. Perfetto has no first-class marker, so this is a section that opens
        // and closes immediately, which is how the trace viewer shows a point in time.
        trace(label) { }
    }
}
