package uk.dioxic.helios.execute.format

import kotlinx.coroutines.flow.Flow
import uk.dioxic.helios.execute.FrameworkMessage

fun Flow<FrameworkMessage>.format(formatter: ReportFormatter) = formatter.format(this)

enum class ReportFormat {
    TEXT,
    JSON
}

sealed class ReportFormatter {
    abstract fun format(results: Flow<FrameworkMessage>): Flow<String>

    companion object {
        fun create(format: ReportFormat): ReportFormatter = when (format) {
            ReportFormat.TEXT -> ConsoleReportFormatter
            ReportFormat.JSON -> JsonReportFormatter
        }
    }
}

