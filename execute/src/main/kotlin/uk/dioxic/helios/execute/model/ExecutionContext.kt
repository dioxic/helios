package uk.dioxic.helios.execute.model

import arrow.optics.optics
import com.mongodb.MongoException
import uk.dioxic.helios.execute.measureTimedResult
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.ErrorResult
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.generate.OperatorContext
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@optics
data class ExecutionContext(
    val workload: Workload,
    val executor: Executor,
    val rate: Rate,
    override val constants: Lazy<Map<String, Any?>>,
    override val variables: Lazy<Map<String, Any?>>,
    override val count: Long = 0,
    val startTime: ValueTimeMark = TimeSource.Monotonic.markNow(),
) : OperatorContext {

    override fun withConstants(constants: Lazy<Map<String, Any?>>) =
        copy(constants = constants)

    override fun withVariables(variables: Lazy<Map<String, Any?>>) =
        copy(variables = variables)

    context(ResourceRegistry)
    suspend operator fun invoke(): TimedResult = measureTimedResult {
        try {
            OperatorContext.threadLocal.set(this)
            workload.executor.execute()
        } catch (e: MongoException) {
            ErrorResult(e)
        }
    }

    companion object
}
