package uk.dioxic.helios.generate

import arrow.core.Either
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptyListOf
import arrow.core.raise.*
import uk.dioxic.helios.generate.exceptions.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

object OperatorBuilder {

    fun <T : Operator<*>> fromMap(
        clazz: KClass<out T>,
        map: Map<*, *> = emptyMap<Any, Any>(),
        subKey: String = ""
    ): Either<OperatorBuildError, T> = either {
        val operatorName = clazz.simpleName.orEmpty()
        ensure(subKey.isEmpty() || clazz.isSubclassOf(KeyedOperator::class)) {
            raise(ExpectingKeyedOperator(operatorName))
        }

        val constructor = ensureNotNull(clazz.primaryConstructor) {
            raise(NoPrimaryConstructor(operatorName))
        }

        val either = constructor.valueParameters
            .map { it to map[it.name] }
            .map {
                if (it.first.name == KeyedOperator.KEY_NAME && subKey.isNotEmpty()) {
                    it.first to subKey
                } else {
                    it
                }
            }
            .filterNot { (p, v) -> p.isOptional && v == null }
            .mapOrAccumulate { (p, v) ->
                ensureNotNull(v) {
                    raise(ParameterNotOptional(p.name.orEmpty()))
                }
                p to convert(p, v)
            }.map {
                constructor.callBy(it.toMap())
            }.mapLeft {
                OperatorConversionError(operatorName, it)
            }

        ensureNotNull(either.getOrNull()) {
            raise(either.leftOrNull()!!)
        }
    }

    fun <T : Wrapped<*>> fromValue(
        clazz: KClass<out T>,
        value: Any,
        subKey: String = ""
    ): Either<OperatorBuildError, T> = either {
        val operatorName = clazz.simpleName.orEmpty()
        ensure(subKey.isEmpty() || clazz.isSubclassOf(KeyedOperator::class)) {
            raise(ExpectingKeyedOperator(operatorName))
        }

        val constructor = ensureNotNull(clazz.primaryConstructor) {
            raise(NoPrimaryConstructor(operatorName))
        }

        val parameters = constructor.valueParameters.let { p ->
            if (subKey.isNotEmpty()) {
                p.filterNot { it.name == KeyedOperator.KEY_NAME }
            } else {
                p
            }
        }

        val primaryParameter = if (parameters.size == 1) {
            parameters.first()
        } else {
            ensure(parameters.countMandatory == 1) {
                raise(NoSingleValueParameter(operatorName))
            }
            parameters.first { !it.isOptional }
        }

        val primaryValue = recover({ convert(primaryParameter, value) }) {
            raise(OperatorConversionError(operatorName, nonEmptyListOf(it)))
        }

        val args = mutableMapOf(primaryParameter to primaryValue)
        if (subKey.isNotEmpty()) {
            val keyParam = ensureNotNull(constructor.valueParameters.first { it.name == KeyedOperator.KEY_NAME }) {
                raise(NoKeyParameter(operatorName))
            }
            args[keyParam] = subKey
        }

        constructor.callBy(args)
    }

    fun <T : Operator<*>> build(clazz: KClass<T>): Either<OperatorBuildError, T> = either {
        val mandatoryParameters = ensureNotNull(clazz.primaryConstructor) {
            raise(NoPrimaryConstructor(clazz.simpleName.orEmpty()))
        }.valueParameters.countMandatory

        ensure(mandatoryParameters == 0) {
            raise(NoDefaultConfiguration(clazz.simpleName.orEmpty()))
        }

        clazz.createInstance()
    }

    private val List<KParameter>.countMandatory: Int
        get() = count { !it.isOptional }


    private fun KClass<out Wrapped<*>>.getKeyParameter(): KParameter =
        primaryConstructor.let { constructor ->
            require(constructor != null) {
                "$simpleName must have a primary constructor"
            }
            constructor.valueParameters.first { it.name == KeyedOperator.KEY_NAME }
        }

    context(Raise<ParameterConversionError>)
    private fun convert(parameter: KParameter, value: Any): Any =
        recover({
            if (parameter.type.jvmErasure.isSubclassOf(Wrapped::class)) {
                val desiredType = parameter.type.arguments.first().type
                wrap(value, desiredType)
            } else {
                convert(value, parameter.type)
            }
        }) {
            raise(ParameterConversionError(parameter.name.orEmpty(), it))
        }

    context(Raise<ConversionError>)
    private fun wrap(obj: Any, type: KType?): Wrapped<*> =
        when (obj) {
            is Wrapped<*> -> obj
            else -> {
                val tObj = type?.let { convert(obj, type) } ?: obj
                Wrapped { tObj }
            }
        }

    context(Raise<ConversionError>)
    private fun convert(obj: Any, type: KType): Any =
        obj::class.starProjectedType.run {
            when {
                isSubtypeOf(type) -> obj
                isSubtypeOf(Long::class.starProjectedType) && obj is Number -> obj.toLong()
                isSubtypeOf(Int::class.starProjectedType) && obj is Number -> obj.toInt()
                isSubtypeOf(Double::class.starProjectedType) && obj is Number -> obj.toDouble()
                isSubtypeOf(Float::class.starProjectedType) && obj is Number -> obj.toFloat()
                else -> raise(ConversionError(obj, type.jvmErasure))
            }
        }


}