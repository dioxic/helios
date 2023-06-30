package uk.dioxic.helios.generate.exceptions

import arrow.core.Nel
import kotlin.reflect.KClass

class OperatorTransformationException(msg: String): Exception(msg)

sealed interface OperatorError

data class NoOperatorFound(val key: String): OperatorError

sealed interface OperatorBuildError: OperatorError {
    val name: String
}

data class OperatorConversionError(
    override val name: String,
    val errors: Nel<ParameterError>
) : OperatorBuildError

data class NoDefaultConfiguration(override val name: String) : OperatorBuildError
data class NoPrimaryConstructor(override val name: String) : OperatorBuildError
data class NoSingleValueParameter(override val name: String) : OperatorBuildError
data class ExpectingKeyedOperator(override val name: String) : OperatorBuildError
data class NoKeyParameter(override val name: String) : OperatorBuildError

sealed interface ParameterError {
    val parameter: String
}

data class ParameterConversionError(
    override val parameter: String,
    val conversionError: ConversionError
) : ParameterError

data class ParameterNotOptional(override val parameter: String) : ParameterError

data class ConversionError(val value: Any?, val target: KClass<*>)
