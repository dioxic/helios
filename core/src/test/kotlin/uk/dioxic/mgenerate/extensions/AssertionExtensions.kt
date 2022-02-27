package uk.dioxic.mgenerate.extensions

import assertk.Assert
import assertk.assertions.isEmpty
import assertk.assertions.support.expected
import assertk.assertions.support.show

/**
 * Asserts the char sequence is a subset of another char sequence.
 * @see [isEmpty]
 */
fun Assert<CharSequence>.isSubsetOf(charPool : CharSequence) = given { actual ->
    if (actual.all { charPool.contains(it) }) return

    expected("${show(actual)} to be be a subset of ${show(charPool)}")
}