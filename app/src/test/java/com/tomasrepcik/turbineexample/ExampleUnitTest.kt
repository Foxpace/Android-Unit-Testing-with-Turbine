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
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub


class TurbineViewModelTest {


    @Mock
    private var heavyComputation: HeavyComputationTemplate = mock()

    private var sut: ExampleViewModel = ExampleViewModel(heavyComputation)


    @Before
    fun setUp() {
        sut = ExampleViewModel(heavyComputation)
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        reset(heavyComputation)
        Dispatchers.resetMain()
    }

    @Test
    fun `Given the sut is initialized, then it waits for event`() =
        assertTrue(sut.vmState.value == VmState.Waiting)

    @Test
    fun `Given the ViewModel waits - When the event OnLaunch comes, then execute heavy computation with result`() =
        runTest {
            // ARRANGE
            val expectedString = "Result"
            heavyComputation.stub {
                onBlocking { doComputation() } doAnswer { expectedString }
            }

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