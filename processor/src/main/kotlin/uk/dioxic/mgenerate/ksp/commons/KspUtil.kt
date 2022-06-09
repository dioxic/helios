package uk.dioxic.mgenerate.ksp.commons

import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName

@OptIn(KotlinPoetKspPreview::class)
fun KSTypeReference.toLambaTypeName() = let {
    val element = it.element
    if (element is KSCallableReference) {
        LambdaTypeName.get(returnType = element.returnType.toTypeName())
    } else {
        toTypeName()
    }
}