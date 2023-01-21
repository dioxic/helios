package uk.dioxic.mgenerate.ksp.commons

import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver

@OptIn(KotlinPoetKspPreview::class)
fun KSTypeReference.toLambaTypeName(alwaysLambda: Boolean = false) = let {
    val element = it.element
    if (element is KSCallableReference) {
        val resolver = element.returnType.resolve().declaration.typeParameters.toTypeParameterResolver()
        LambdaTypeName.get(returnType = element.returnType.toTypeName(resolver))
    } else {
        if (alwaysLambda) {
            LambdaTypeName.get(returnType = toTypeName())
        }
        else {
            toTypeName()
        }
    }
}