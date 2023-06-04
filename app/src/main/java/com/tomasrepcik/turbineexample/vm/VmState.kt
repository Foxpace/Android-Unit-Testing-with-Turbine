package com.tomasrepcik.turbineexample.vm

sealed class VmState {
    object Waiting: VmState()
    object Running: VmState()
    data class Finished(val data: String): VmState()
}
