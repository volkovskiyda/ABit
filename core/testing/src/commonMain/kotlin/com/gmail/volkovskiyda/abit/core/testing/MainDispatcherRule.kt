package com.gmail.volkovskiyda.abit.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Not a JUnit rule — `commonTest` has no JUnit. Call [setUp] from `@BeforeTest` and [tearDown] from
 * `@AfterTest`; anything that touches `viewModelScope` needs it, because that scope is hardcoded to
 * `Dispatchers.Main`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) {
    fun setUp() = Dispatchers.setMain(testDispatcher)

    fun tearDown() = Dispatchers.resetMain()
}
