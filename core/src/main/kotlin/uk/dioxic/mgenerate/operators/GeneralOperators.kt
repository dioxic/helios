package uk.dioxic.mgenerate.operators

//fun array(of: () -> Any, number: () -> Int = { 5 }): List<Any> {
//    return generateSequence(of).take(number.invoke()).toList();
//}
//
//fun boolean(): Boolean = Random.nextBoolean()
//
//fun choose(from: List<() -> Any>, weights: List<Int>) {
//    require(from.size == weights.size) { "length of from and weights arguments must match!" }
//
//
//}
//
////fun arrayProcess(array: Array): List<Any> =
////    generateSequence(array.of)
////        .take(array.number.invoke())
////        .toList()
//
//@Serializable
//data class Array(
//    val of: Resolvable<@Contextual Any>,
//    val number: Resolvable<Int> = Resolvable { 5 }
//) : Resolvable<List<Any>> {
//
//    override fun resolve(): List<Any> =
//        generateSequence { of.resolve() }
//            .take(number.resolve())
//            .toList()
//}
//
//@Serializable
//data class Choose(
//    val from: Resolvable<List<@Contextual Any>>,
//    val weights: Resolvable<List<Double>>
//) : Resolvable<Any> {
//
//    @Transient
//    val weightedCollection = RandomCollection<Any>()
//
//    init {
//        val fromResolved = from.resolve()
//        val weightsResolved = weights.resolve()
//        require(fromResolved.size == weightsResolved.size) { "length of from and weights arguments must match!" }
//        fromResolved.forEachIndexed { index, fn ->
//            weightedCollection.add(weightsResolved[index], fn)
//        }
//    }
//
//    override fun resolve(): Any = weightedCollection.next()
//}
//
//fun interface Resolvable<T> {
//    fun resolve(): T
//}
//
//fun main() {
//    val doc: Map<String, Any> = mapOf(
//        "field1" to Choose(from = { listOf("A", "B", "C") }, weights = { listOf(0.3, 0.6, 0.1) })
//    )
//
//    val newOp = Resolvable { 29 }
//    val array = Array(of = { Random.nextInt(10) }, number = { 3 })
//    val choose = Choose(from = array, weights = { listOf(0.3, 0.6, 0.1) })
//
//    println(choose.resolve())
//
//}