package uk.dioxic.helios.generate.fixture

import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.KeyedOperator
import uk.dioxic.helios.generate.operators.Operator

typealias StringFn = () -> String

sealed class AbstractTestOperator : Operator<String> {
    override fun invoke(): String = "hello world!"
}

sealed class AbstractKeyedTestOperator : KeyedOperator<String>() {
    abstract override val key: String
    override fun invoke(): String = key
}

@Alias
class OperatorWithSingleMandatoryArg(val arg: StringFn, val arg2: StringFn = { "fake" }) : AbstractTestOperator()

@Alias
class OperatorWithSingleOptionalArg(val arg: StringFn = { "fake" }) : AbstractTestOperator()

class OperatorWithMultiOptionalArg(val arg: StringFn = { "fake" }, val arg2: StringFn = { "fake" }) : AbstractTestOperator()

class OperatorWithMultiMandatoryArg(val arg: StringFn, val arg2: StringFn) : AbstractTestOperator()

class KeyedOperatorWithNoArg(override val key: String) : AbstractKeyedTestOperator()
class KeyedOperatorWithSingleMandatoryArg(override val key: String, val arg: StringFn) : AbstractKeyedTestOperator()
class KeyedOperatorWithSingleOptionalArg(override val key: String, val arg: StringFn = { "fake" }) : AbstractKeyedTestOperator()
class KeyedOperatorWithMultiOptionalArg(override val key: String, val arg: StringFn = { "fake" }, val arg2: StringFn = { "fake" }) : AbstractKeyedTestOperator()
class KeyedOperatorWithMultiMandatoryArg(override val key: String, val arg: StringFn, val arg2: StringFn) : AbstractKeyedTestOperator()