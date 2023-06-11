package uk.dioxic.mgenerate.worker

class Benchmark(
    override val name: String,
    val stages: List<Stage>
): Named