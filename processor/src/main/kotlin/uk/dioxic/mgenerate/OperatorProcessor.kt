package uk.dioxic.mgenerate

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import uk.dioxic.mgenerate.exceptions.IncorrectTypeException
import uk.dioxic.mgenerate.exceptions.MissingArgumentException

class OperatorProcessor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("uk.dioxic.mgenerate.annotations.Operator")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(OperatorVistitor(), Unit) }
        return ret
    }

    @OptIn(KotlinPoetKspPreview::class)
    inner class OperatorVistitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.primaryConstructor!!.accept(this, data)
        }

        @OptIn(KotlinPoetKspPreview::class)
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration
            val parentClassName = parent.simpleName.asString()
            val packageName = parent.containingFile!!.packageName.asString()
            val className = "${parent.simpleName.asString()}Builder"

            val propertySpecList = getPropertySpecList(function)
            val funSpecList = getFunctionSpecList(function, packageName, parentClassName)

            val classSpec = TypeSpec.classBuilder(className)
                .addProperties(propertySpecList)
                .addFunctions(funSpecList)
                .build()

            val fileSpec = FileSpec.builder(packageName, className)
                .addType(classSpec)
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, function.containingFile!!))

        }

        private fun getPropertySpecList(
            constructor: KSFunctionDeclaration
        ): List<PropertySpec> =
            constructor.parameters
                .map { parameter ->
                    val propertyName = parameter.name!!.asString()
                    val typeName = parameter.type.toTypeName()
                    val parameterType = parameter.type.resolve()

                    val propertyType = if (parameterType.isFunctionType) {
                        val argumentTypeName = parameterType.arguments.first().toTypeName()
                        LambdaTypeName.get(returnType = argumentTypeName)
                    } else {
                        typeName
                    }.copy(nullable = true)


                    PropertySpec.builder(propertyName, propertyType)
                        .addModifiers(KModifier.PRIVATE)
                        .mutable(true)
                        .initializer("null")
                        .build()
                }

        private fun getFunctionSpecList(
            constructor: KSFunctionDeclaration,
            packageName: String,
            parentClassName: String,
        ): List<FunSpec> {

            val operatorClass = ClassName(packageName, parentClassName)
            val function0Class = ClassName("kotlin", "Function0")
            val setterSpecs = mutableListOf<FunSpec>()

            // build function
            val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor")

            val validationFun =
                FunSpec.builder("validate")
                    .addModifiers(KModifier.PRIVATE)
                    .addCode(missingArgumentsBlock(constructor.parameters))
                    .beginControlFlow("if (missingArgs.isNotEmpty())")
                    .addStatement("throw %T(%L)", MissingArgumentException::class, "missingArgs")
                    .endControlFlow()
                    .build()

            val buildFun =
                FunSpec.builder("build")
                    .returns(operatorClass)
                    .addStatement("val cons = %T::class.%M!!", operatorClass, primaryConstructor)
                    .addCode(valueMapBlock(constructor.parameters))
                    .addStatement("%N()", validationFun)
                    .addCode(
                        """
                        return cons.parameters
                            .associateBy({ it }, { valMap[it.name] })
                            .filterNot { it.value == null }
                            .let {
                                cons.callBy(it)
                            }
                    """.trimIndent()
                    )
                    .build()

            // map property builder
            val mapFunSpecBuilder = FunSpec.builder("from")
                .returns(operatorClass)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
                .addParameter(
                    "map", Map::class.asClassName()
                        .parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true))
                )

            constructor.parameters.forEach { parameter ->
                val propertyName = parameter.name!!.asString()
                val parameterType = parameter.type.resolve()
                val typeName = parameter.type.toTypeName()

                mapFunSpecBuilder.beginControlFlow("map[%S]?.let", propertyName)
                mapFunSpecBuilder.beginControlFlow("$propertyName = when (it)")

                if (parameterType.isFunctionType) {
                    val argumentTypeName = parameterType.arguments.first().toTypeName() as ClassName
                    val lambdaTypeName = LambdaTypeName.get(returnType = argumentTypeName)
                    setterSpecs.add(scalarSetterSpec(propertyName, argumentTypeName))
                    setterSpecs.add(lambdaSetterSpec(propertyName, lambdaTypeName))

                    if (argumentTypeName.simpleName == "Any") {
                        mapFunSpecBuilder.addStatement("is Function0<*> -> it as %T", lambdaTypeName)
                        mapFunSpecBuilder.addStatement("else -> {{ it }}")
                    } else {
                        mapFunSpecBuilder.addStatement(
                            "is %T -> it",
                            ClassName("uk.dioxic.mgenerate", "${argumentTypeName.simpleName}Function")
                        )
                        mapFunSpecBuilder.addStatement("is %T -> {{ it }}", argumentTypeName)
                        mapFunSpecBuilder.addCode(invalidArgumentExceptionBlock(propertyName))
                    }

                } else {
                    mapFunSpecBuilder.addStatement("is %T -> it", typeName)
                    setterSpecs.add(lambdaSetterSpec(propertyName, typeName))
                    mapFunSpecBuilder.addCode(invalidArgumentExceptionBlock(propertyName))
                }

                mapFunSpecBuilder.endControlFlow()
                mapFunSpecBuilder.endControlFlow()
            }
            mapFunSpecBuilder.addStatement("return %N()", buildFun)
            return setterSpecs + mapFunSpecBuilder.build() + buildFun + validationFun
        }

        private fun lambdaSetterSpec(propertyName: String, lambdaTypeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, lambdaTypeName)
                .beginControlFlow("return apply")
                .addStatement("this.$propertyName = $propertyName")
                .endControlFlow()
                .build()

        private fun scalarSetterSpec(propertyName: String, scalarTypeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, scalarTypeName)
                .beginControlFlow("return apply")
                .addStatement("this.$propertyName = { $propertyName }")
                .endControlFlow()
                .build()

        private fun valueMapBlock(parameters: List<KSValueParameter>) =
            with(CodeBlock.builder()) {
                addStatement("val valMap = mapOf(")
                indent()
                parameters.forEach {
                    val propertyName = it.name!!.asString()
                    addStatement("%S to %L,", propertyName, propertyName)
                }
                unindent()
                addStatement(")")
                build()
            }

        private fun missingArgumentsBlock(parameters: List<KSValueParameter>) =
            with(CodeBlock.builder()) {
                beginControlFlow(
                    "val missingArgs = %T().apply",
                    ArrayList::class.asClassName().parameterizedBy(String::class.asClassName())
                )
                parameters
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
    }
}