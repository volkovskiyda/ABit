package com.gmail.volkovskiyda.abit.buildlogic

/**
 * What `:testSummary` counts: a layer's totals, where those totals came from, and a tool's findings.
 *
 * Every one of them is absent rather than zeroed when nothing ran, which is the distinction the
 * whole page exists to make — a layer skipped for want of a device is not a layer that passed.
 */

/** One layer's totals. Absent — rather than zeroed — when the layer never ran. */
internal data class Totals(
    val tests: Int,
    val failures: Int,
    val skipped: Int,
) {
    val passed: Int get() = tests - failures - skipped

    operator fun plus(other: Totals) =
        Totals(tests + other.tests, failures + other.failures, skipped + other.skipped)

    internal companion object {
        val EMPTY = Totals(0, 0, 0)
    }
}

/** A layer's results for one module (or one device), so a failing row can name where it failed. */
internal data class Breakdown(
    val label: String,
    val totals: Totals,
    val failed: List<String>,
)

/** Findings by severity, for a static-analysis tool. Absent when the tool never ran. */
internal data class Findings(
    val errors: Int,
    val warnings: Int,
    val other: Int,
) {
    val total: Int get() = errors + warnings + other

    operator fun plus(other2: Findings) =
        Findings(errors + other2.errors, warnings + other2.warnings, other + other2.other)

    internal companion object {
        val EMPTY = Findings(0, 0, 0)
    }
}
