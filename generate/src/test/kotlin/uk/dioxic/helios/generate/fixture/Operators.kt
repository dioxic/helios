package uk.dioxic.helios.generate.fixture

import uk.dioxic.helios.generate.KeyedOperator
import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.annotations.Alias


sealed class AbstractTestOperator : Operator<String> {
    override fun invoke(): String = "hello world!"
}

sealed class AbstractKeyedTestOperator : KeyedOperator<String>() {
    abstract override val key: String
    override fun invoke(): String = key
}

@Alias
class OperatorWithSingleMandatoryArg(
    val arg: Operator<String>,
    val arg2: Operator<String> = Operator { "fake" }
) : AbstractTestOperator()

@Alias
class OperatorWithSingleOptionalArg(
    val arg: Operator<String> = Operator { "fake" }
) : AbstractTestOperator()

class OperatorWithMultiOptionalArg(
    val arg: Operator<String> = Operator { "fake" },
    val arg2: Operator<String> = Operator { "fake" }
) :
    AbstractTestOperator()

class OperatorWithMultiMandatoryArg(
    val arg: Operator<String>,
    val arg2: Operator<String>
) : AbstractTestOperator()

class KeyedOperatorWithNoArg(override val key: String) : AbstractKeyedTestOperator()

class KeyedOperatorWithSingleMandatoryArg(
    override val key: String,
    val arg: Operator<String>
) : AbstractKeyedTestOperator()

class KeyedOperatorWithSingleOptionalArg(
    override val key: String,
    val arg: Operator<String> = Operator { "fake" }
) : AbstractKeyedTestOperator()

class KeyedOperatorWithMultiOptionalArg(
    override val key: String,
    val arg: Operator<String> = Operator { "fake" },
    val arg2: Operator<String> = Operator { "fake" }
) : AbstractKeyedTestOperator()

class KeyedOperatorWithMultiMandatoryArg(
    override val key: String,
    val arg: Operator<String>,
    val arg2: Operator<String>
) : AbstractKeyedTestOperator()