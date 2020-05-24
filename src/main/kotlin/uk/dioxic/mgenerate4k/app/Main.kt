package uk.dioxic.mgenerate4k.app

import uk.dioxic.mgenerate4k.utilities.StringUtils


/**
 * My main function
 */
fun main() {
    val tokens = StringUtils.split(MessageUtils.getMessage())
    println(StringUtils.join(tokens))
}
