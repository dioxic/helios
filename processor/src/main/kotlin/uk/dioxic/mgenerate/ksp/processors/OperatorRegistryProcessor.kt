package uk.dioxic.mgenerate.ksp.processors

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo

@OptIn(KotlinPoetKspPreview::class)
class OperatorRegistryProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("uk.dioxic.mgenerate.annotations.Operator")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .forEach {
                val operatorPoet = OperatorRegistryPoet(it)

                FileSpec.builder(operatorPoet.packageName, operatorPoet.registryClassName)
                    .addType(operatorPoet.build())
                    .build()
                    .writeTo(codeGenerator, Dependencies(true, it.containingFile!!))

            }
        return ret
    }

    inner class OperatorRegistryPoet(private val classDeclaration: KSClassDeclaration) {
        private val functionName = classDeclaration.simpleName.asString()

        val registryClassName = "${functionName.replaceFirstChar { it.uppercaseChar() }}Operator"
        val packageName = classDeclaration.packageName.asString()

        fun build() = with(TypeSpec.classBuilder(registryClassName)) {
            build()
        }


    }

}

class OperatorRegistryProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OperatorRegistryProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
    }
}
