package com.gmail.volkovskiyda.abit.app.shared.di

import com.gmail.volkovskiyda.abit.app.shared.ABIT_VERSION_NAME
import com.gmail.volkovskiyda.abit.core.common.AppVersion
import org.koin.dsl.module

/**
 * The generated version constant, as a binding. It lives here rather than in `commonModule` because
 * `core:common` cannot know the number — only `app:shared` has the generated source, and only the
 * composition root should.
 */
val appVersionModule =
    module {
        single { AppVersion(ABIT_VERSION_NAME) }
    }
