package com.gmail.volkovskiyda.abit.buildlogic

import com.android.build.api.dsl.Lint

/**
 * One lint policy for every Android target in the build, applied from the convention plugins.
 *
 * No baseline, deliberately and everywhere: a baseline lets suppressed debt accumulate unseen, and
 * it pins each finding to a path that carries a dependency's version, so it needs regenerating on
 * every bump. A genuine third-party false positive is scoped in the module's `lint.xml` by artifact
 * regexp instead, which survives version bumps and leaves the check live for everything else.
 */
internal fun configureAbitLint(lint: Lint) = lint.apply {
    checkAllWarnings = true
    // The one check `checkAllWarnings` leaves off (experimental interprocedural analysis).
    enable += "WrongThreadInterprocedural"
    // Findings in the console as well as the file reports under build/reports/.
    printTextReport = true
    abortOnError = true
    warningsAsErrors = false
}
