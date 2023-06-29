package uk.dioxic.helios.generate

import arrow.core.getOrElse
import org.bson.Document
import org.bson.Transformer
import uk.dioxic.helios.generate.exceptions.OperatorTransformationException

class OperatorTransformer : Transformer {
    override fun transform(objectToTransform: Any?): Any? {
        when (objectToTransform) {
            is Document -> {
                if (objectToTransform.size == 1) {
                    val (key, value) = objectToTransform.entries.first()
                    if (OperatorFactory.canHandle(key)) {
                        return OperatorFactory.create(key, value).getOrElse {
                            throw OperatorTransformationException(it.toString())
                        }
                    }
                }
            }

            is String -> {
                if (OperatorFactory.canHandle(objectToTransform)) {
                    return OperatorFactory.create(objectToTransform).getOrElse {
                        throw OperatorTransformationException(it.toString())
                    }
                }
            }
        }
        return objectToTransform
    }
}