package com.gmail.volkovskiyda.abit.core.database.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Empty until Room arrives (plan item 07). It exists now so `app:shared` can list it and the module
 * list does not have to change when the database lands.
 */
val databaseModule: Module = module { }
