package uk.dioxic.mgenerate.ksp.commons

class IllegalDestinationsSetup(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class MissingRequiredDependency(message: String) : RuntimeException(message)

class UnexpectedException(message: String) : RuntimeException(message)