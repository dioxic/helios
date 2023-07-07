package uk.dioxic.helios.generate.fixture

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.Wrapped
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.KeyedOperator


sealed class AbstractTestOperator : Operator<String> {
    override fun invoke(): String = "hello world!"
}

sealed class AbstractKeyedTestOperator : KeyedOperator<String>() {
    abstract override val key: String
    override fun invoke(): String = key
}

@Alias
class OperatorWithSingleMandatoryArg(
    val arg: Wrapped<String>,
    val arg2: Wrapped<String> = Wrapped { "fake" }
) : AbstractTestOperator()

@Alias
class OperatorWithSingleOptionalArg(
    val arg: Wrapped<String> = Wrapped { "fake" }
) : AbstractTestOperator()

class OperatorWithMultiOptionalArg(
    val arg: Wrapped<String> = Wrapped { "fake" },
    val arg2: Wrapped<String> = Wrapped { "fake" }
) :
    AbstractTestOperator()

class OperatorWithMultiMandatoryArg(
    val arg: Wrapped<String>,
    val arg2: Wrapped<String>
) : AbstractTestOperator()

class KeyedOperatorWithNoArg(override val key: String) : AbstractKeyedTestOperator()

class KeyedOperatorWithSingleMandatoryArg(
    override val key: String,
    val arg: Wrapped<String>
) : AbstractKeyedTestOperator()

class KeyedOperatorWithSingleOptionalArg(
    override val key: String,
    val arg: Wrapped<String> = Wrapped { "fake" }
) : AbstractKeyedTestOperator()

class KeyedOperatorWithMultiOptionalArg(
    override val key: String,
    val arg: Wrapped<String> = Wrapped { "fake" },
    val arg2: Wrapped<String> = Wrapped { "fake" }
) : AbstractKeyedTestOperator()

class KeyedOperatorWithMultiMandatoryArg(
    override val key: String,
    val arg: Wrapped<String>,
    val arg2: Wrapped<String>
) : AbstractKeyedTestOperator()