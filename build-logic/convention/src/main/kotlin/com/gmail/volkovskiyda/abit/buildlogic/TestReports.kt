package com.gmail.volkovskiyda.abit.buildlogic

import org.w3c.dom.Element
import java.io.File

/**
 * Reading the XML that every test task and every analysis tool already writes.
 *
 * Nothing here runs a test or a check. `:testSummary` only reports what is on disk, which is what
 * makes it safe to attach to any pipeline and what lets it say "not run" for a layer nobody ran —
 * a distinction a task that ran the layers itself could not make.
 */

private fun Element.suiteHeaderTotals() =
    Totals(
        tests = intAttribute("tests"),
        failures = intAttribute("failures") + intAttribute("errors"),
        skipped = intAttribute("skipped"),
    )

/**
 * A `<failure>` that is really a skip.
 *
 * AGP's connected-test engine records two kinds of skip as a failure and counts them in the suite
 * header, while the task that produced them still passes: an `assumeTrue` violation, whose text
 * names `AssumptionViolatedException`, and a class-level `@Ignore`, which becomes one empty
 * `<failure></failure>`. The macrobenchmarks in `:baselineprofile` are the case in this repository —
 * `MacrobenchmarkRule` refuses to measure on an emulator — so without this correction a run that
 * measured nothing reports two red tests.
 */
private fun Element.isRecordedSkip(): Boolean {
    if (tagName != "failure" && tagName != "error") return false
    val text = textContent.trim()
    return text.isEmpty() || text.startsWith("org.junit.AssumptionViolatedException")
}

private fun Element.failureChildren(): List<Element> {
    val children = childNodes
    return (0 until children.length)
        .mapNotNull { children.item(it) as? Element }
        .filter { it.tagName == "failure" || it.tagName == "error" }
}

/**
 * A JUnit XML file's totals, with the recorded skips above moved out of the failed column.
 *
 * Both shapes turn up: Gradle writes one `<testsuite>` per class, while the instrumentation runner
 * wraps several in a `<testsuites>` whose header would double-count if it were summed alongside its
 * children. The wrapper wins when it is there, and the suites are summed when it is not.
 */
private fun File.suiteTotals(): Totals {
    val root = parse(this) ?: return Totals.EMPTY
    val assumed = root.childElements("testcase").count { case -> case.failureChildren().any { it.isRecordedSkip() } }
    val wrapper = if (root.tagName == "testsuites") root else root.childElements("testsuites").firstOrNull()
    val header =
        wrapper?.suiteHeaderTotals()
            ?: (if (root.tagName == "testsuite") listOf(root) else root.childElements("testsuite"))
                .fold(Totals.EMPTY) { acc, suite -> acc + suite.suiteHeaderTotals() }
    return header.copy(failures = header.failures - assumed, skipped = header.skipped + assumed)
}

/** Every genuinely failed case in one file, as `com.example.SomeTest.theCaseName`. */
private fun File.failedCases(): List<String> {
    val root = parse(this) ?: return emptyList()
    return root
        .childElements("testcase")
        .filter { case -> case.failureChildren().any { !it.isRecordedSkip() } }
        .map { "${it.getAttribute("classname")}.${it.getAttribute("name")}" }
}

/**
 * Totals for a layer, plus one breakdown entry per directory that contributed.
 *
 * [roots] is a label-to-directory map — module path for a host layer, module path and device for an
 * instrumented one. Returns null when not one of them holds an XML file, which is the "not run"
 * case: a layer skipped for want of a device is not a layer with nothing in it.
 */
internal fun readLayer(roots: Map<String, File>): Pair<Totals, List<Breakdown>>? {
    val breakdowns =
        roots.mapNotNull { (label, dir) ->
            val files = dir.xmlFiles()
            if (files.isEmpty()) {
                null
            } else {
                Breakdown(
                    label = label,
                    totals = files.fold(Totals.EMPTY) { acc, file -> acc + file.suiteTotals() },
                    failed = files.flatMap { it.failedCases() },
                )
            }
        }.sortedBy { it.label }
    if (breakdowns.isEmpty()) return null
    return breakdowns.fold(Totals.EMPTY) { acc, breakdown -> acc + breakdown.totals } to breakdowns
}

/**
 * Findings by severity across a tool's reports.
 *
 * detekt and ktlint write checkstyle (`<error severity=…>`); Android lint writes its own
 * (`<issue severity=…>`). A real XML parse rather than a line scan, because lint embeds multi-line
 * rule explanations in its attributes and a regex reads those wrong. Null when there is nothing to
 * read, which is the tool's "not run".
 */
internal fun readFindings(reports: List<File>): Findings? {
    val files = reports.filter { it.isFile }
    if (files.isEmpty()) return null
    val severities =
        files.flatMap { file ->
            val root = parse(file) ?: return@flatMap emptyList()
            listOf("error", "issue")
                .flatMap { root.childElements(it) }
                .map { it.getAttribute("severity").lowercase() }
        }
    return Findings(
        errors = severities.count { it == "error" || it == "fatal" },
        warnings = severities.count { it == "warning" },
        // Everything below a warning — lint's "information" and "hint", detekt's "info".
        other = severities.count { it != "error" && it != "fatal" && it != "warning" },
    )
}
