package com.gmail.volkovskiyda.abit.feature.schedules.impl.di

import com.gmail.volkovskiyda.abit.feature.schedules.impl.ScheduleEditorViewModel
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val schedulesModule =
    module {
        viewModelOf(::SchedulesViewModel)
        // A parameterised ViewModel: the editor is opened for an existing schedule or for a new one,
        // and `null` is a legitimate argument rather than a missing dependency.
        factory { (id: String?) -> ScheduleEditorViewModel(id = id, repository = get(), clock = get()) }
    }
