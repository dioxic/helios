package uk.dioxic.mgenerate

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

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
            function: KSFunctionDeclaration
        ): List<PropertySpec> =
            function.parameters
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
            function: KSFunctionDeclaration,
            packageName: String,
            parentClassName: String,
        ): List<FunSpec> {

            val operatorClass = ClassName(packageName, parentClassName)
            val function0Class = ClassName("kotlin", "Function0")
            val setterSpecs = mutableListOf<FunSpec>()

            // build function
            val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor")
            val buildFun =
                FunSpec.builder("build")
                    .returns(operatorClass)
                    .addStatement("val cons = %T::class.%M!!", operatorClass, primaryConstructor)
                    .addCode(valueMapBlock(function.parameters))
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
                        .parameterizedBy(String::class.asClassName(), Any::class.asClassName())
                )

            function.parameters.forEach { parameter ->
                val propertyName = parameter.name!!.asString()
                val parameterType = parameter.type.resolve()
                val typeName = parameter.type.toTypeName()

                mapFunSpecBuilder.beginControlFlow("map[%S]?.let", propertyName)
                mapFunSpecBuilder.beginControlFlow("$propertyName = when (it)")

                if (parameterType.isFunctionType) {

                    val argumentTypeName = parameterType.arguments.first().toTypeName()
                    val lambdaTypeName = LambdaTypeName.get(returnType = argumentTypeName)
                    setterSpecs.add(lambdaSetterSpec(propertyName, argumentTypeName))
                    setterSpecs.add(scalarSetterSpec(propertyName, lambdaTypeName))
                    mapFunSpecBuilder.addStatement(
                        "is Function0<*> -> it as %T",
                        function0Class.parameterizedBy(argumentTypeName)
                    )
                    mapFunSpecBuilder.addStatement("is %T -> {{ it }}", argumentTypeName)
                } else {
                    mapFunSpecBuilder.addStatement("is %T -> it", typeName)
                    setterSpecs.add(scalarSetterSpec(propertyName, typeName))
                }

                mapFunSpecBuilder.addStatement(
                    "else -> throw IllegalArgumentException(%P)",
                    "\${it::class.java} not valid for $propertyName attribute"
                )
                mapFunSpecBuilder.endControlFlow()
                mapFunSpecBuilder.endControlFlow()
            }
            mapFunSpecBuilder.addStatement("return %N()", buildFun)
            return setterSpecs + mapFunSpecBuilder.build() + buildFun
        }

        private fun scalarSetterSpec(propertyName: String, typeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, typeName)
                .beginControlFlow("return apply")
                .addStatement("this.$propertyName = $propertyName")
                .endControlFlow()
                .build()

        private fun lambdaSetterSpec(propertyName: String, typeName: TypeName) =
            FunSpec.builder(propertyName)
                .addParameter(propertyName, typeName)
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
    }
}