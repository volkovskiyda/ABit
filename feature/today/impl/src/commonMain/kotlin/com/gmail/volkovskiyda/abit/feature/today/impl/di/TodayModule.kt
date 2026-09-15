package com.gmail.volkovskiyda.abit.feature.today.impl.di

import com.gmail.volkovskiyda.abit.feature.today.impl.TodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val todayModule =
    module {
        viewModelOf(::TodayViewModel)
    }
