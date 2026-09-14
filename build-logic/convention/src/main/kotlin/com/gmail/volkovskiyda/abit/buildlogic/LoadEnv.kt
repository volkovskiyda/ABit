package com.gmail.volkovskiyda.abit.buildlogic

import java.io.File

/**
 * Reads `KEY=VALUE` lines from a repo-root config file, ignoring blanks and `#` comments. A missing
 * file yields an empty map rather than an error, which is what lets every caller treat "no
 * credentials" as a supported state instead of a failure.
 *
 * Each caller reads a git-ignored file that has a committed `.example.*` template beside it.
 */
internal fun loadEnv(file: File): Map<String, String> =
    file.takeIf { it.exists() }
        ?.readLines()
        ?.mapNotNull { line ->
            line.trim()
                .takeUnless { it.isEmpty() || it.startsWith("#") }
                ?.split("=", limit = 2)
                ?.takeIf { it.size == 2 }
                ?.let { (key, value) -> key.trim() to value.trim() }
        }
        ?.toMap()
        .orEmpty()
