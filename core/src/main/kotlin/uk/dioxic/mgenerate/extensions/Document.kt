package uk.dioxic.mgenerate.extensions

import org.bson.Document

fun Document.of(vararg pairs: Pair<String, Any>): Document =
    pairs.toMap(this)
