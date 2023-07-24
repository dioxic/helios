package uk.dioxic.helios.execute.model

import arrow.optics.optics

@optics
data class WorkloadContext(
    val workload: Workload,
    val executor: Executor = workload.executor,
    val rate: Rate,
    val executionId: Long = 0,
) {
    companion object
}