package com.gmail.volkovskiyda.abit.feature.pomodoro.impl.di

import com.gmail.volkovskiyda.abit.feature.pomodoro.impl.PomodoroViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pomodoroModule =
    module {
        viewModelOf(::PomodoroViewModel)
    }
