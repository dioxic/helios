package uk.dioxic.mgenerate.app

import uk.dioxic.mgenerate.utils.StringUtils


/**
 * My main function.
 * Check out the [StringUtils.join] util.
 */
fun main() {
    val tokens = StringUtils.split(MessageUtils.getMessage())
    println(StringUtils.join(tokens))
}
