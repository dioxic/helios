package uk.dioxic.mgenerate.ksp.processors

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*
import uk.dioxic.mgenerate.exceptions.IncorrectTypeException
import uk.dioxic.mgenerate.exceptions.MissingArgumentException
import uk.dioxic.mgenerate.operators.OperatorBuilder

@OptIn(KotlinPoetKspPreview::class)
class OperatorBuilderProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("uk.dioxic.mgenerate.annotations.Operator")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .onEach { logger.info("Generating builder for [$it]") }
            .forEach {
                val operatorPoet = OperatorBuilderPoet(it)

                FileSpec.builder(operatorPoet.packageName, operatorPoet.builderClassName)
                    .addType(operatorPoet.build())
                    .build()
                    .writeTo(codeGenerator, Dependencies(true, it.containingFile!!))

            }
        return ret
    }

    inner class OperatorBuilderPoet(private val classDeclaration: KSClassDeclaration) {
        private val uncheckedCastAnnotationSpec = AnnotationSpec.builder(Suppress::class.asClassName())
            .addMember("%S", "UNCHECKED_CAST")
            .build()

        private val operatorClassName = classDeclaration.toClassName()
        private val valueParameters = classDeclaration.primaryConstructor?.parameters ?: emptyList()

        val packageName = classDeclaration.packageName.asString()
        val builderClassName = "${classDeclaration.simpleName.asString()}Builder"

        private fun TypeSpec.Builder.addProperty(valueParameter: KSValueParameter) = apply {
            val propertyType = valueParameter.type.toTypeName(true).copy(nullable = true)

            this.addProperty(
                PropertySpec.builder(valueParameter.name!!.asString(), propertyType)
                    .addModifiers(KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("null")
                    .build()
            )
        }

        private fun TypeSpec.Builder.addSetters(valueParameter: KSValueParameter) {
            val propertyName = valueParameter.name!!.asString()
            val typeRef = valueParameter.type
            val element = typeRef.element
            val type = typeRef.resolve()
            val typeName = type.toClassName()

            if (type.isFunctionType && element is KSCallableReference) {
                val returnTypeName = element.returnType.toTypeName() as ClassName
                val lambdaTypeName = LambdaTypeName.get(returnType = returnTypeName)

                if (returnTypeName.simpleName == "Any") {
                    addFunction(anySetterSpec(propertyName))
                } else {
                    addFunction(wrappedSetterSpec(propertyName, returnTypeName))
                    addFunction(passthroughSetterSpec(propertyName, lambdaTypeName))
                }
            } else {
                addFunction(passthroughSetterSpec(propertyName, typeName))
            }
        }

        private fun KSTypeReference.toTypeName(preferLambda: Boolean) = let {
            val element = it.element
            if (preferLambda && element is KSCallableReference) {
                LambdaTypeName.get(returnType = element.returnType.toTypeName())
            } else {
                toTypeName()
            }
        }

        private fun anySetterSpec(propertyName: String) =
            with(FunSpec.builder(propertyName)) {
                val anyType = ClassName("kotlin", "Any")
                addParameter(propertyName, anyType)
                addAnnotation(uncheckedCastAnnotationSpec)
                beginControlFlow("return apply")
                addCode(
                    """
                    if (%L is Function0<*>) {
                        this.$propertyName = $propertyName as () -> %T
                    }
                    else {
                        this.$propertyName = { $propertyName }
                    }
                """.trimIndent(), propertyName, anyType
                )
                endControlFlow()
                build()
            }

        private fun passthroughSetterSpec(propertyName: String, typeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, typeName)
                .beginControlFlow("return apply")
                .addStatement("this.$propertyName = $propertyName")
                .endControlFlow()
                .build()

        private fun wrappedSetterSpec(propertyName: String, typeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, typeName)
                .beginControlFlow("return apply")
                .addStatement("this.$propertyName = { $propertyName }")
                .endControlFlow()
                .build()

        private fun valueMapBlock() =
            with(CodeBlock.builder()) {
                addStatement("val valMap = mapOf(")
                indent()
                valueParameters.forEach {
                    val propertyName = it.name!!.asString()
                    addStatement("%S to %L,", propertyName, propertyName)
                }
                unindent()
                addStatement(")")
                build()
            }

        private fun missingArgumentsBlock() =
            with(CodeBlock.builder()) {
                beginControlFlow(
                    "val missingArgs = %T().apply",
                    ArrayList::class.asClassName().parameterizedBy(String::class.asClassName())
                )
                valueParameters
                    .filterNot { it.hasDefault }
                    .forEach {
                        val propertyName = it.name!!.asString()
                        beginControlFlow("if (%L == null)", propertyName)
                        add("add(%S)", propertyName)
                        endControlFlow()
                    }
                endControlFlow()
                build()
            }

        private fun invalidArgumentExceptionBlock(propertyName: String) =
            CodeBlock.builder()
                .addStatement(
                    "else -> throw %T(it::class, %S)",
                    IncorrectTypeException::class,
                    propertyName
                )
                .build()

        private fun fromFunSpec(buildFun: FunSpec) =
            with(FunSpec.builder("from")) {

                returns(operatorClassName)
                addModifiers(KModifier.OVERRIDE)
                addAnnotation(uncheckedCastAnnotationSpec)
                addParameter(
                    "map", Map::class.asClassName()
                        .parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true))
                )

                valueParameters.forEach {
                    val propertyName = it.name!!.asString()
                    val parameterType = it.type.resolve()
                    val typeName = it.type.toTypeName()
                    val element = it.type.element

                    beginControlFlow("map[%S]?.let", propertyName)
                    beginControlFlow("$propertyName = when (it)")
                    if (parameterType.isFunctionType && element is KSCallableReference) {
                        val returnTypeName = element.returnType.toTypeName() as ClassName
                        val lambdaTypeName = LambdaTypeName.get(returnType = returnTypeName)

                        if (returnTypeName.simpleName == "Any") {
                            addStatement("is Function0<*> -> it as %T", lambdaTypeName)
                            addStatement("else -> {{ it }}")
                        } else {
                            addStatement(
                                "is %T -> it",
                                ClassName("uk.dioxic.mgenerate", "${returnTypeName.simpleName}Function")
                            )
                            addStatement("is %T -> {{ it }}", returnTypeName)
                            addCode(invalidArgumentExceptionBlock(propertyName))
                        }

                    } else {
                        addStatement("is %T -> it", typeName)
                        addCode(invalidArgumentExceptionBlock(propertyName))
                    }

                    endControlFlow()
                    endControlFlow()
                }
                addStatement("return %N()", buildFun)

                build()
            }

        private fun validationFunSpec() =
            if (valueParameters.isNotEmpty()) {
                FunSpec.builder("validate")
                    .addModifiers(KModifier.PRIVATE)
                    .addCode(missingArgumentsBlock())
                    .beginControlFlow("if (missingArgs.isNotEmpty())")
                    .addStatement("throw %T(%L)", MissingArgumentException::class, "missingArgs")
                    .endControlFlow()
                    .build()
            } else {
                null
            }

        private fun hasDefaultFunSpec() =
            with(FunSpec.builder("hasDefault")) {
                addModifiers(KModifier.OVERRIDE)
                val hasDefault = when (valueParameters.count { !it.hasDefault }) {
                    0 -> "true"
                    else -> "false"
                }
                addStatement("return $hasDefault")
                returns(Boolean::class.asClassName())
                build()
            }

        private fun buildFunSpec(validationFunSpec: FunSpec?) =
            with(FunSpec.builder("build")) {
                val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor")

                returns(operatorClassName)
                addStatement("val cons = %T::class.%M!!", operatorClassName, primaryConstructor)
                addModifiers(KModifier.OVERRIDE)

                if (valueParameters.isEmpty()) {
                    addCode(
                        """
                        return cons.call()
                        """.trimIndent()
                    )
                } else {
                    addCode(valueMapBlock())
                    addStatement("%N()", validationFunSpec!!)
                    addCode(
                        """
                        return cons.parameters
                            .associateBy({ it }, { valMap[it.name] })
                            .filterNot { it.value == null }
                            .let {
                                cons.callBy(it)
                            }
                    """.trimIndent()
                    )
                }

                build()
            }

        fun build() = with(TypeSpec.classBuilder(builderClassName)) {

            addSuperinterface(
                OperatorBuilder::class.asClassName()
                    .parameterizedBy(operatorClassName)
            )

            valueParameters.forEach {
                addProperty(it)
                addSetters(it)
            }

            val validationFunSpec = validationFunSpec()?.apply {
                addFunction(this)
            }

            buildFunSpec(validationFunSpec).apply {
                addFunction(fromFunSpec(this))
                addFunction(this)
            }

            addFunction(hasDefaultFunSpec())

            build()
        }
    }
}

class OperatorBuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OperatorBuilderProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
    }
}
