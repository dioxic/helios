package uk.dioxic.helios.generate.test

import io.kotest.matchers.types.shouldBeInstanceOf
import uk.dioxic.helios.generate.Wrapped

inline fun <reified T : Any> Any?.shouldBeWrapped(matcher: (T) -> Unit) {
    val wrapped = this.shouldBeInstanceOf<Wrapped<T>>()
    return matcher(withEmptyContext { wrapped.invoke() })
}

inline fun <reified T : Any> Any?.shouldBeWrapped(): T {
    val wrapped = this.shouldBeInstanceOf<Wrapped<T>>()
    return withEmptyContext { wrapped.invoke() }
}