@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tomasrepcik.turbineexample

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.tomasrepcik.turbineexample.repo.HeavyComputationTemplate
import com.tomasrepcik.turbineexample.vm.ExampleViewModel
import com.tomasrepcik.turbineexample.vm.VmEvents
import com.tomasrepcik.turbineexample.vm.VmState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.stub


@RunWith(JUnit4::class)
class TurbineViewModelTest {

    @Mock
    lateinit var heavyComputation: HeavyComputationTemplate

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `Given the sut is initialized, then it waits for event`() {
        val sut = ExampleViewModel(heavyComputation)
        assertTrue(sut.vmState.value == VmState.Waiting)
    }

    @Test
    fun `Given the ViewModel waits - When the event OnLaunch comes, then execute heavy computation with result`() =
        runTest {
            // ARRANGE
            val expectedString = "Result"
            heavyComputation.stub {
                onBlocking { doComputation() } doAnswer { expectedString }
            }

            val sut = ExampleViewModel(heavyComputation)


            sut.vmState.test {

                // ACTION
                sut.onEvent(VmEvents.OnLaunch)

                // CHECK
                assertEquals(VmState.Waiting, awaitItem())
                assertEquals(VmState.Running, awaitItem())
                assertEquals(VmState.Finished(expectedString), awaitItem())
                assertEquals(VmState.Waiting, awaitItem())
            }
        }

    @Test
    fun `Given the ViewModel waits - When the event OnLaunch comes, then both computations runs successfully`() =
        runTest {
            // ARRANGE
            val expectedString = "Result"
            heavyComputation.stub {
                onBlocking { doComputation() } doAnswer { expectedString }
            }

            val sut = ExampleViewModel(heavyComputation)

            val firstStateReceiver = sut.vmState.testIn(backgroundScope)
            val secondStateReceiver = sut.secondVmState.testIn(backgroundScope)

            // ACTION
            sut.onEvent(VmEvents.OnLaunch)

            // CHECK
            assertEquals(VmState.Waiting, firstStateReceiver.awaitItem())
            assertEquals(VmState.Waiting, secondStateReceiver.awaitItem())

            assertEquals(VmState.Running, firstStateReceiver.awaitItem())
            assertEquals(VmState.Running, secondStateReceiver.awaitItem())

            assertEquals(VmState.Finished(expectedString), firstStateReceiver.awaitItem())
            assertEquals(VmState.Finished(expectedString), secondStateReceiver.awaitItem())

            assertEquals(VmState.Waiting, firstStateReceiver.awaitItem())
            assertEquals(VmState.Waiting, secondStateReceiver.awaitItem())

            firstStateReceiver.cancel()
            secondStateReceiver.cancel()
        }
}