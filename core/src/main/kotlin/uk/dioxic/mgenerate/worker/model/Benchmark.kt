package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.Named
import kotlin.time.Duration

@Serializable
data class Benchmark(
    override val name: String,
    val stages: List<Stage>
) : Named

@Serializable
sealed interface Stage : Named {
    val workloads: List<Workload>
}

@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val workloads: List<Workload>
) : Stage

@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    val timeout: Duration = Duration.INFINITE,
    override val workloads: List<Workload>
) : Stage

@Serializable
data class Workload(
    override val name: String,
    val executor: Executor,
    val weight: Double = 1.0,
    val count: Long = 1,
    val rate: Rate = Unlimited,
) : Named

@Serializable
sealed interface Executor

@Serializable
@SerialName("insertOne")
data class InsertOneExecutor(
    val database: String,
    val collection: String,
    val template: Template
) : Executor

@Serializable
sealed interface Rate

@Serializable
object Unlimited : PeriodicRate {
    override val tps = null
    override val every = Duration.ZERO
    override val fuzzy = 0.0
}

@Serializable
sealed interface PeriodicRate : Rate {
    val tps: Int?
    val every: Duration?
    val fuzzy: Double
}

@Serializable
@SerialName("fixed")
data class FixedRate(
    override val tps: Int? = null,
    override val every: Duration? = null,
    override val fuzzy: Double = 0.0
) : PeriodicRate {
    init {
        require((tps != null) xor (every != null)) {
            "'tps' or 'every' needs to be specified but not both!"
        }
    }
}

@Serializable
@SerialName("ramped")
data class RampedRate(
    val delay: Duration = Duration.ZERO,
    val from: PeriodicRate,
    val to: PeriodicRate = Unlimited,
    val rampDuration: Duration
) : Rate