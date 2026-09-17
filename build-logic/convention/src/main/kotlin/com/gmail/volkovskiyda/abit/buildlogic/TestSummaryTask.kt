package com.gmail.volkovskiyda.abit.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDateTime

/**
 * One HTML page summarising every test layer and all three static-analysis tools, written to
 * `build/reports/test-summary/index.html`.
 *
 * It reads XML that is already on disk and runs nothing itself, so it is safe to attach to any
 * pipeline; `scripts/run-tests.sh` calls it once the layers it ran have finished. That also gives
 * the page its one unusual property: a layer nobody ran reads "not run" rather than "0 passed",
 * because a layer skipped for want of a device is not the same as a layer with nothing in it.
 *
 * A real task class in build-logic rather than a `doLast` in a build script: the configuration cache
 * cannot serialize references to build-script-level functions, which is what forces the whole
 * parser into one lambda when a task like this is written in Kotlin DSL.
 */
abstract class TestSummaryTask : DefaultTask() {
    /** Module path (`:core:sync`) to that module's build directory. */
    @get:Internal
    abstract val moduleBuildDirectories: MapProperty<String, File>

    @get:Internal
    abstract val rootBuildDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "verification"
        description =
            "Aggregate the results of every test layer and all three static-analysis tools into one HTML report."
        // The inputs are other tasks' outputs, which this task deliberately does not declare — it
        // reports on whatever ran, and declaring them would make it demand that they run.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun summarise() {
        val modules = moduleBuildDirectories.get()
        val page = outputFile.get().asFile
        val summaryDir = page.parentFile

        val layers = TEST_LAYERS.map { layer -> layer to readLayer(layer.rootsIn(modules)) }
        val checks = analysisRows(modules)

        if (layers.all { it.second == null } && checks.all { it.second == null }) {
            // `quiet` rather than `lifecycle` throughout this task: `scripts/run-tests.sh` calls it
            // with `-q`, which hides everything above QUIET — and the summary it just asked for is
            // the one thing that must survive that.
            logger.quiet("testSummary: no results found — run scripts/run-tests.sh first.")
            return
        }

        summaryDir.mkdirs()
        page.writeText(render(layers, checks, summaryDir))
        // A file:// URL rather than a bare path: terminals linkify it, matching how Gradle prints
        // its own report locations.
        logger.quiet("Test summary: file://${page.absolutePath}")
        layers.forEach { (layer, result) -> logLayer(layer, result) }
        checks.forEach { (label, findings) ->
            val line =
                findings?.let { "${it.total} findings, ${it.errors} errors, ${it.warnings} warnings" }
                    ?: "not run"
            logger.quiet("  %-28s %s".format(label, line))
        }
    }

    private fun logLayer(
        layer: TestLayer,
        result: Pair<Totals, List<Breakdown>>?,
    ) {
        val totals = result?.first
        val line =
            totals?.let { "${it.tests} tests, ${it.failures} failed, ${it.skipped} skipped" } ?: "not run"
        logger.quiet("  %-28s %s".format(layer.label, line))
        // Printed here rather than left to the HTML: "instrumented tests FAILED" on a multi-module
        // run does not say which module, and that is the first thing anyone asks.
        if (totals == null || totals.failures == 0) return
        result.second.filter { it.failed.isNotEmpty() }.forEach { breakdown ->
            logger.quiet("    %-30s %d failed".format(breakdown.label, breakdown.failed.size))
            breakdown.failed.take(LOGGED_FAILURES).forEach { logger.quiet("      - $it") }
            (breakdown.failed.size - LOGGED_FAILURES)
                .takeIf { it > 0 }
                ?.let { logger.quiet("      …and $it more") }
        }
    }

    private fun analysisRows(modules: Map<String, File>): List<Pair<String, Findings?>> {
        val rootBuild = rootBuildDirectory.get().asFile
        // detekt is applied to the root project alone — it scans every module's `src` from there —
        // so its one report lives in the root build directory rather than in each module's.
        val detekt = readFindings(listOf(rootBuild.resolve("reports/detekt/detekt.xml")))
        // ktlint runs per project *and* per source set, so :core:sync alone writes one report for
        // commonMain, one for commonTest and one for its build script. Summing every module's is the
        // only way to get one number.
        val ktlint =
            (modules.values + rootBuild)
                .flatMap { ktlintReports(it) }
                .let(::readFindings)
        // lintDebug only, matching scripts/run-tests.sh: the release variant reports the same
        // findings a second time.
        val lint =
            modules.values
                .map { it.resolve("reports/lint-results-debug.xml") }
                .let(::readFindings)
        return listOf("detekt" to detekt, "ktlint" to ktlint, "Android lint (debug)" to lint)
    }

    private fun render(
        layers: List<Pair<TestLayer, Pair<Totals, List<Breakdown>>?>>,
        checks: List<Pair<String, Findings?>>,
        summaryDir: File,
    ): String {
        val ran = layers.mapNotNull { it.second?.first }
        val analysed = checks.mapNotNull { it.second }
        val tests = ran.fold(Totals.EMPTY) { acc, layer -> acc + layer }
        val findings = analysed.fold(Findings.EMPTY) { acc, tool -> acc + tool }

        val rows = layers.joinToString("\n") { (layer, result) -> layerRows(layer, result, summaryDir) }
        val failures = failureSections(layers)
        val analysisRows = checks.joinToString("\n") { (label, found) -> analysisRow(label, found) }

        val verdict =
            when {
                tests.failures > 0 && findings.errors > 0 -> "${tests.failures} failed, ${findings.errors} analysis errors"
                tests.failures > 0 -> "${tests.failures} failed"
                findings.errors > 0 -> "tests passed, ${findings.errors} analysis errors"
                else -> "all passed"
            }
        // Stamped because this page is written after failures too: a run that stopped at static
        // analysis leaves the previous run's XML in place, and without a time there is no way to
        // tell a fresh layer from a stale one.
        val generatedAt = LocalDateTime.now().withNano(0).toString().replace("T", " ")

        // trimMargin rather than trimIndent: every interpolated block below (the style sheet, the
        // rows) carries its own indentation, and trimIndent would take the smallest of those as the
        // page's margin and leave the rest of the document indented behind it.
        return """
            |<!doctype html>
            |<html lang="en"><head><meta charset="utf-8">
            |<title>ABit test summary</title>
            |<style>
$STYLE
            |</style></head><body>
            |<h1>ABit test summary — $verdict</h1>
            |<div class="meta">${tests.tests} tests across ${ran.size} of ${layers.size} layers &middot;
            |${findings.total} static-analysis findings from ${analysed.size} of ${checks.size} tools
            |&middot; generated $generatedAt.<br>
            |A row reads "not run" when the layer was skipped — no device attached, or the Firebase
            |emulators not started. One that did not re-run in the latest pass still shows its
            |previous results.</div>
            |<h2>Tests</h2>
            |<table>
            |  <thead><tr><th>Layer</th><th>Tests</th><th>Passed</th><th>Failed</th><th>Skipped</th></tr></thead>
            |  <tbody>
$rows
            |  </tbody>
            |  <tfoot><tr><td>Total</td><td>${tests.tests}</td><td>${tests.passed}</td>
            |  <td>${tests.failures}</td><td>${tests.skipped}</td></tr></tfoot>
            |</table>
${screenshotHint(layers)}
$failures
            |<h2>Static analysis</h2>
            |<table>
            |  <thead><tr><th>Tool</th><th>Findings</th><th>Errors</th><th>Warnings</th><th>Other</th></tr></thead>
            |  <tbody>
$analysisRows
            |  </tbody>
            |  <tfoot><tr><td>Total</td><td>${findings.total}</td><td>${findings.errors}</td>
            |  <td>${findings.warnings}</td><td>${findings.other}</td></tr></tfoot>
            |</table>
            |</body></html>
            """.trimMargin()
    }

    private fun layerRows(
        layer: TestLayer,
        result: Pair<Totals, List<Breakdown>>?,
        summaryDir: File,
    ): String {
        if (result == null) {
            return """      <tr class="notrun"><td>${layer.label}</td><td colspan="4">not run</td></tr>"""
        }
        val modules = moduleBuildDirectories.get()
        val (totals, breakdowns) = result
        val name = link(layer.soleReportIn(modules), layer.label, summaryDir)
        val row = row("", name, totals)
        // The per-module rows only earn their space when the layer's own row cannot answer "where",
        // and then only for the modules that failed: a layer spans twenty projects here, and
        // nineteen green rows under one red one is the noise this section exists to cut through.
        if (totals.failures == 0 || breakdowns.size < 2) return row
        val detail =
            breakdowns.filter { it.totals.failures > 0 }.joinToString("\n") { breakdown ->
                val label =
                    link(modules[breakdown.label]?.let(layer::reportIn), breakdown.label, summaryDir)
                row("detail ", label, breakdown.totals)
            }
        return "$row\n$detail"
    }

    private fun row(
        prefix: String,
        label: String,
        totals: Totals,
    ): String =
        """      <tr class="$prefix${if (totals.failures > 0) "fail" else "pass"}"><td>$label</td>""" +
            """<td>${totals.tests}</td><td>${totals.passed}</td>""" +
            """<td>${totals.failures}</td><td>${totals.skipped}</td></tr>"""

    /** A path relative to the page, so the whole build directory stays movable. */
    private fun link(
        report: File?,
        label: String,
        summaryDir: File,
    ): String =
        report
            ?.relativeToOrNull(summaryDir)
            ?.invariantSeparatorsPath
            ?.let { """<a href="$it">$label</a>""" }
            ?: label

    /**
     * Every failed case, named, grouped by the module that failed it — the list a terminal scroll
     * cannot hold. Capped per group: one broken emulator produces hundreds of identical lines, and
     * the point is to identify where, not to reprint the suite.
     */
    private fun failureSections(layers: List<Pair<TestLayer, Pair<Totals, List<Breakdown>>?>>): String {
        val sections =
            layers.flatMap { (layer, result) ->
                result?.second.orEmpty().filter { it.failed.isNotEmpty() }.map { layer to it }
            }
        if (sections.isEmpty()) return ""
        val body =
            sections.joinToString("\n") { (layer, breakdown) ->
                val items =
                    breakdown.failed.take(LISTED_FAILURES).map { "  <li>$it</li>" } +
                        listOfNotNull(
                            (breakdown.failed.size - LISTED_FAILURES)
                                .takeIf { it > 0 }
                                ?.let { """  <li class="more">…and $it more</li>""" },
                        )
                val heading =
                    "<h3>${breakdown.label} &middot; ${layer.label} &middot; ${breakdown.failed.size} failed</h3>"
                (listOf(heading, """<ul class="failures">""") + items + "</ul>").joinToString("\n")
            }
        return "<h2>What failed, and where</h2>\n$body"
    }

    /**
     * A failed golden is usually an intentional UI change rather than a bug, so the page carries the
     * command that re-bakes it — the same hint `scripts/run-tests.sh` prints in the terminal.
     */
    private fun screenshotHint(layers: List<Pair<TestLayer, Pair<Totals, List<Breakdown>>?>>): String {
        val failed =
            layers.any { (layer, result) ->
                layer.label == SCREENSHOT_LAYER && (result?.first?.failures ?: 0) > 0
            }
        if (!failed) return ""
        return """<p class="hint">Screenshot goldens differ. If the change is intentional """ +
            """(a new screen, a fixed label), re-bake the baselines and re-run:<br>""" +
            """<code>./gradlew :app:android:updateDebugScreenshotTest</code></p>"""
    }

    private fun analysisRow(
        label: String,
        findings: Findings?,
    ): String {
        if (findings == null) {
            return """      <tr class="notrun"><td>$label</td><td colspan="4">not run</td></tr>"""
        }
        // Warnings get a state of their own: detekt fails the build on any finding while Android
        // lint fails only on errors, so "issues but no errors" is neither a pass nor a failure.
        val cls =
            when {
                findings.errors > 0 -> "fail"
                findings.total > 0 -> "warn"
                else -> "pass"
            }
        return """      <tr class="$cls"><td>$label</td><td>${findings.total}</td>""" +
            """<td>${findings.errors}</td><td>${findings.warnings}</td><td>${findings.other}</td></tr>"""
    }

    private companion object {
        const val LOGGED_FAILURES = 5
        const val LISTED_FAILURES = 20
    }
}
