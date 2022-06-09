package uk.dioxic.mgenerate.ksp.commons

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

fun String.removeFromTo(from: String, to: String): String {
    val startIndex = indexOf(from)
    val endIndex = indexOf(to) + to.length

    return kotlin.runCatching { removeRange(startIndex, endIndex) }.getOrNull() ?: this
}

fun KSAnnotated.findAnnotation(name: String): KSAnnotation {
    return annotations.find { it.shortName.asString() == name }!!
}

inline fun <reified T> KSAnnotation.findArgumentValue(name: String): T? {
    return arguments.find { it.name?.asString() == name }?.value as T?
}

fun File.readLineAndImports(lineNumber: Int): Pair<String, List<String>> {
    val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(this), Charsets.UTF_8))
    return bufferedReader
        .useLines { lines: Sequence<String> ->
            val firstNLines = lines.take(lineNumber)

            val iterator = firstNLines.iterator()
            var line = iterator.next()
            val importsList = mutableListOf<String>()
            while (iterator.hasNext()) {
                line = iterator.next()
                if (line.startsWith("import")) {
                    importsList.add(line.removePrefix("import "))
                }
            }

            line to importsList
        }
}

fun File.readLine(lineNumber: Int): String {
    val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(this), Charsets.UTF_8))
    return bufferedReader
        .useLines { lines: Sequence<String> ->
            lines
                .take(lineNumber)
                .last()
        }
}

// this comes from a file, so we expect the full quote escape char stuff (e.g. "something \" "
/**
 * Removes all the double-quoted text from a string.
 * Ignores escaped quotes.
 * Example:
 *    aaa"bbb\"bbb"ccc --> aaaccc
 *
 */
fun unquotedText(s: String) =
    s.split(Regex("""(?<!\\)""""))
        .filterIndexed { idx, _ -> idx.isEven() }
        .joinToString("")

inline fun CharSequence.indexOfFirstIgnoreQuoted(predicate: (Char) -> Boolean): Int {
    var q = false
    for (index in indices) {
        if (this[index] == '"' && this[index-1] != '\\'){
            q = !q
        }
        if (!q && predicate(this[index])) {
            return index
        }
    }
    return -1
}

fun KSType.isRequireOptInAnnotation(): Boolean {
    return declaration.annotations.any { annotation ->
        annotation.shortName.asString() == "RequiresOptIn"
                || annotation.annotationType.annotations.any {
            annotation.annotationType.resolve().isRequireOptInAnnotation()
        }
    }
}

fun Int.isOdd() =
    this % 2 != 0

fun Int.isEven() =
    !this.isOdd()