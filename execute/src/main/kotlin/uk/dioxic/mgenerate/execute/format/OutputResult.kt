package uk.dioxic.mgenerate.execute.format

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.execute.results.SummarizedLatencies
import kotlin.time.Duration

typealias Percent = Int

@Serializable
data class OutputResult (
    @SerialName("stage") val stageName: String = "",
    @SerialName("workload") val workloadName: String,
    @SerialName("operations") val operationCount: Int,
    @SerialName("inserted") val insertedCount: Long = 0L,
    @SerialName("matched") val matchedCount: Long = 0L,
    @SerialName("modified") val modifiedCount: Long = 0L,
    @SerialName("deleted") val deletedCount: Long = 0L,
    @SerialName("upserted") val upsertedCount: Long = 0L,
    @SerialName("docsReturned") val docsReturned: Int = 0,
    @SerialName("successes") val successCount: Int = 0,
    @SerialName("failures") val failureCount: Int = 0,
    @SerialName("elapsed") @Contextual val elapsed: Duration,
    @SerialName("progress") @Contextual val progress: Percent,
    val latencies: SummarizedLatencies? = null
)

