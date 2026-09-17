package com.gmail.volkovskiyda.abit.buildlogic

import java.io.File

internal const val SCREENSHOT_LAYER = "Screenshot goldens"

/**
 * A test layer, as the summary page reports it: a label, where each module writes that layer's JUnit
 * XML, and which HTML report a reader should open.
 *
 * Paths rather than tasks. The summary reports on what ran, so it has to find results without asking
 * whether the task that would have produced them exists in this module — most of these layers only
 * exist in two or three of the twenty-odd projects.
 */
internal data class TestLayer(
    val label: String,
    /** Relative to a module's build directory. A directory, walked for `*.xml`. */
    private val results: String,
    /** Relative to a module's build directory. Linked from the row when it exists. */
    private val report: String,
) {
    fun rootsIn(modules: Map<String, File>): Map<String, File> =
        modules
            .mapValues { (_, buildDir) -> buildDir.resolve(results) }
            .filterValues { it.isDirectory }

    /** The HTML report one module wrote for this layer, when it wrote one. */
    fun reportIn(buildDirectory: File): File? = buildDirectory.resolve(report).takeIf { it.isFile }

    /**
     * The one report to link the layer's own row to — and null when several modules wrote one.
     *
     * Most layers here span twenty projects, and linking a row to whichever of them happens to sort
     * first is worse than not linking it: the reader follows it expecting the layer and lands in one
     * arbitrary module. The per-module rows below carry their own links instead.
     */
    fun soleReportIn(modules: Map<String, File>): File? =
        modules.values.mapNotNull { reportIn(it) }.singleOrNull()
}

/**
 * The five layers `scripts/run-tests.sh` can produce, in the order that script runs them.
 *
 * The Firestore rules tests (`scripts/emulator-tests.sh`) deliberately have no row of their own:
 * they *are* `:core:sync:desktopTest`, so they already sit inside the desktop unit-test row, and a
 * second row reading the same directory would double-count them.
 *
 * What that costs is worth stating: those five cases early-return when `FIRESTORE_EMULATOR_HOST` is
 * unset, so with no emulators running they report as *passed* rather than as skipped, and no row
 * here can tell the two apart. Giving them a row of their own only becomes worth doing once they
 * report a real skip.
 */
internal val TEST_LAYERS =
    listOf(
        TestLayer(
            label = "Unit tests (desktop JVM)",
            results = "test-results/desktopTest",
            report = "reports/tests/desktopTest/index.html",
        ),
        TestLayer(
            label = "Unit tests (Android host JVM)",
            results = "test-results/testAndroidHostTest",
            report = "reports/tests/testAndroidHostTest/index.html",
        ),
        TestLayer(
            label = "Desktop UI tests",
            results = "test-results/test",
            report = "reports/tests/test/index.html",
        ),
        TestLayer(
            label = SCREENSHOT_LAYER,
            results = "test-results/validateDebugScreenshotTest",
            // The screenshot plugin writes an index plus one page per class, with the reference,
            // actual and diff images on a failure. The index is the entry point.
            report = "reports/screenshotTest/preview/debug/index.html",
        ),
        // One row per device path, because they are two runs and not one. Pointed at the shared
        // parent, the walk found both `managedDevice/` and `connected/` and added them together:
        // nine tests read as eighteen, and a stale failing connected run sat in the same total as a
        // green managed one with nothing to say which was which. A path nobody ran reads "not run",
        // which is exactly the right thing for whichever of these the caller did not ask for.
        TestLayer(
            label = "Instrumented (managed emulator)",
            results = "outputs/androidTest-results/managedDevice",
            report = "reports/androidTests/managedDevice/debug/allDevices/index.html",
        ),
        TestLayer(
            label = "Instrumented (connected device)",
            results = "outputs/androidTest-results/connected",
            report = "reports/androidTests/connected/debug/index.html",
        ),
    )

/** The page's styles. Kept out of the task so the HTML it builds stays readable. */
internal val STYLE = """
    body { font: 15px/1.5 system-ui, sans-serif; margin: 2rem; color: #222; }
    h1 { font-size: 1.3rem; margin: 0 0 .25rem; }
    h2 { font-size: 1rem; margin: 2rem 0 .5rem; }
    .meta { color: #666; font-size: .85rem; margin-bottom: 1.5rem; }
    table { border-collapse: collapse; min-width: 34rem; }
    th, td { padding: .5rem .9rem; border-bottom: 1px solid #e3e3e3; text-align: right; }
    th:first-child, td:first-child { text-align: left; }
    thead th { border-bottom: 2px solid #ccc; font-size: .8rem; text-transform: uppercase; color: #555; }
    tr.fail td:first-child::before { content: "\2717 "; color: #c0392b; }
    tr.pass td:first-child::before { content: "\2713 "; color: #17803d; }
    tr.warn td:first-child::before { content: "\26A0 "; color: #b7791f; }
    tr.notrun td { color: #999; font-style: italic; }
    tr.detail td { color: #555; font-size: .9rem; }
    tr.detail td:first-child { padding-left: 2.2rem; }
    h3 { font-size: .95rem; margin: 1.2rem 0 .3rem; }
    ul.failures { margin: 0; padding-left: 1.4rem; font-size: .9rem; color: #555; }
    ul.failures li { font-family: ui-monospace, monospace; font-size: .82rem; }
    ul.failures li.more { font-family: inherit; font-style: italic; }
    tfoot td { font-weight: 600; border-top: 2px solid #ccc; border-bottom: none; }
    a { color: #1a4f9c; }
    .hint { margin: .75rem 0 0; color: #b7791f; }
    .hint code { background: #f2f2f2; padding: .1rem .4rem; border-radius: 4px; color: #222; }
    @media (prefers-color-scheme: dark) {
      body { background: #16181c; color: #e6e6e6; }
      tr.detail td, ul.failures { color: #9aa4b2; }
      th, td { border-color: #303540; } thead th { color: #9aa4b2; border-color: #454b57; }
      tfoot td { border-color: #454b57; } a { color: #7aa7ff; } .meta { color: #9aa4b2; }
      .hint code { background: #232833; color: #e6e6e6; }
    }
""".trimIndent()

/**
 * ktlint's XML reports, excluding the ones its *Format* tasks write.
 *
 * Those list what `ktlintFormat` already fixed rather than what is still wrong, so counting them
 * would report a clean tree as dirty for as long as the last format run's output sat in the build
 * directory.
 */
internal fun ktlintReports(buildDirectory: File): List<File> =
    buildDirectory
        .resolve("reports/ktlint")
        .xmlFiles()
        .filterNot { it.parentFile.name.endsWith("Format") }
