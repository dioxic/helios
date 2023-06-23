package uk.dioxic.mgenerate.execute.report

import kotlinx.coroutines.flow.Flow
import uk.dioxic.mgenerate.execute.FrameworkMessage

fun Flow<FrameworkMessage>.format(formatter: ReportFormatter) = formatter.format(this)

enum class ReportFormat {
    TEXT
}

sealed class ReportFormatter {
    abstract fun format(results: Flow<FrameworkMessage>): Flow<String>

    companion object {
        fun create(format: ReportFormat): ReportFormatter = when (format) {
            ReportFormat.TEXT -> ConsoleReportFormatter
        }
    }
}

