package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.extensions.measureTimedResult

class WorkloadContext<T : ExecutionContext>(
    val workload: Workload,
    val executionContext: T
) {
    fun invoke(workerId: Int) = measureTimedResult(workload.name) {
        executionContext.invoke(workerId)
    }
}