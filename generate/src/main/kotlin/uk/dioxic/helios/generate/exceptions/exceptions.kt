package uk.dioxic.helios.generate.exceptions

import kotlin.reflect.KClass

sealed class OperatorArgumentException(msg: String) : Exception(msg)

class IncorrectTypeException(
    type: KClass<*>,
    argument: String
) : OperatorArgumentException("$type not valid for [$argument] argument")

class MissingArgumentException(
    argument: List<String>
): OperatorArgumentException("Mandatory argument(s) [$argument] not provided")

class UnsupportedOperatorAliasException(
    alias: String
): Exception("Operator alias \"$alias\" not supported")