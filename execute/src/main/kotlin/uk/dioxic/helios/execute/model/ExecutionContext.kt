package uk.dioxic.helios.execute.model

import com.mongodb.MongoException
import uk.dioxic.helios.execute.measureTimedResult
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.ErrorResult
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.generate.OperatorContext
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

data class ExecutionContext(
    val workload: Workload,
    val executor: Executor,
    val rate: Rate,
    override val constants: Lazy<Map<String, Any?>>,
    override val variables: Lazy<Map<String, Any?>>,
    override val executionCount: Long = 0,
    val startTime: ValueTimeMark = TimeSource.Monotonic.markNow(),
) : OperatorContext {
    override val identity
        get() = workload

    context(ResourceRegistry)
    suspend operator fun invoke(): TimedResult = measureTimedResult {
        try {
            executor.execute()
        } catch (e: MongoException) {
            ErrorResult(e)
        }
    }
}
