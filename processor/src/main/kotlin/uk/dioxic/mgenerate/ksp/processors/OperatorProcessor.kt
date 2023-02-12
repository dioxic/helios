package uk.dioxic.mgenerate.ksp.processors

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import uk.dioxic.mgenerate.*
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.ksp.commons.getDefaultValue
import uk.dioxic.mgenerate.ksp.commons.toLambaTypeName
import uk.dioxic.mgenerate.operators.Operator
import kotlin.reflect.KClass

class OperatorProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("uk.dioxic.mgenerate.annotations.Operator")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSFunctionDeclaration && it.validate() }
            .map { it as KSFunctionDeclaration }
            .onEach { logger.info("Generating Operator class for [$it]") }
            .forEach {
                val operatorPoet = OperatorPoet(it, resolver)

                FileSpec.builder(operatorPoet.packageName, operatorPoet.operatorClassName)
                    .addType(operatorPoet.build())
                    .build()
                    .writeTo(codeGenerator, Dependencies(true, it.containingFile!!))

            }
        return ret
    }

    inner class OperatorPoet(
        private val funDeclaration: KSFunctionDeclaration,
        private val resolver: Resolver
    ) {
        private val functionName = funDeclaration.simpleName.asString()

        val operatorClassName = "${functionName.replaceFirstChar { it.uppercaseChar() }}Operator"
        val packageName = funDeclaration.packageName.asString()

        fun build() = with(TypeSpec.classBuilder(operatorClassName)) {
            val parameters = funDeclaration.parameters
            val returnType = funDeclaration.returnType!!.toTypeName()
            val operatorAnnotation = funDeclaration.annotations
                .first { it.shortName.asString() == "Operator" }

            addAnnotation(operatorAnnotationSpec(operatorAnnotation, functionName))
            addSuperinterface(functionClass(returnType))

            val constructorBuilder = FunSpec.constructorBuilder()

            parameters.forEach {
                val name = it.name!!.asString()
                val callableType = it.type.element is KSCallableReference
                val type = it.type.toLambaTypeName(alwaysLambda = true)

                val parameterSpec = with(ParameterSpec.builder(name, type)) {
                    if (it.hasDefault) {
                        val dvCode = it.getDefaultValue(resolver)!!.code
                        if (callableType) {
                            defaultValue("%L", dvCode)
                        } else {
                            defaultValue("{ %L }", dvCode)
                        }
                    }
                    build()
                }

                val propertySpec = with(PropertySpec.builder(name, type)) {
                    initializer(name)
                    build()
                }

                constructorBuilder.addParameter(parameterSpec)
                addProperty(propertySpec)
            }

            val invokeFunArguments = parameters
//                .map {param ->
//                    val callableType = param.type.element is KSCallableReference
//                    param.name!!.asString().let {
//                        if (callableType) {
//                            "$it()"
//                        }
//                        else {
//                            it
//                        }
//                    }
//                }
                .map { it.name!!.asString() to (it.type.element is KSCallableReference) }
                .joinToString { (name, callable) ->
                    if (callable) {
                        "$name = $name"
                    } else {
                        "$name = $name()"
                    }
                }

            val invokeFunSpec = FunSpec.builder("invoke")
                .addModifiers(KModifier.OVERRIDE)
                .returns(returnType)
                .addStatement("return %M(%L)", MemberName(packageName, functionName), invokeFunArguments)

            primaryConstructor(constructorBuilder.build())
            addFunction(invokeFunSpec.build())

            build()
        }

        private fun functionClass(returnType: TypeName): KClass<*> {
            return when (returnType) {
                is ClassName -> functionClass(returnType)
                is ParameterizedTypeName -> functionClass(returnType.rawType)
                else -> {
                    throw IllegalArgumentException("function return type [${returnType::class}] not supported")
                }
            }
        }

        private fun functionClass(returnType: ClassName) =
            Operator::class
//            when (returnType.simpleName) {
//                "Number", "Int", "Long", "Double", "Float" -> NumberFunction::class
//                "String" -> StringFunction::class
//                "List" -> ListFunction::class
//                "Map" -> MapFunction::class
//                "Boolean" -> BooleanFunction::class
//                "Any" -> AnyFunction::class
//                else -> throw IllegalArgumentException("function return type [$returnType] not supported")
//            }

        private fun operatorAnnotationSpec(annotation: KSAnnotation, funcName: String) =
            with(AnnotationSpec.builder(Alias::class)) {
                val existingAliases = annotation.arguments
                    .first { it.name?.asString() == "aliases" }
                    .value as List<*>

                val aliases = existingAliases.ifEmpty {
                    listOf(funcName.replaceFirstChar { it.lowercaseChar() })
                }

                addMember("%L", aliases.joinToString(",", transform = { "\"$it\"" }))

                build()
            }
    }
}

class OperatorProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OperatorProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
    }
}
