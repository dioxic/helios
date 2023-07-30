package uk.dioxic.helios.execute.test

import com.mongodb.client.AggregateIterable
import com.mongodb.client.FindIterable
import io.mockk.every
import io.mockk.mockk

fun <T : Any> mockFindIterable(vararg results: T) =
    mockk<FindIterable<T>> {
        every { iterator() } returns mockk {
            every { hasNext() } returnsMany List(results.size) { true } andThen false
            every { next() } returnsMany results.toList()
        }
        every { projection(any()) } returns this
        every { sort(any()) } returns this
    }

fun <T : Any> mockAggregateIterable(vararg results: T) =
    mockk<AggregateIterable<T>> {
        every { iterator() } returns mockk {
            every { hasNext() } returnsMany List(results.size) { true } andThen false
            every { next() } returnsMany results.toList()
        }
    }