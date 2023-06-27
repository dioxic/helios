package uk.dioxic.helios.generate

import org.bson.Document
import org.bson.Transformer

class OperatorTransformer: Transformer {
    override fun transform(objectToTransform: Any?): Any {
        if (objectToTransform is Document) {
            if (objectToTransform.size == 1) {
                val (key, value) = objectToTransform.entries.iterator().next()
                if (OperatorFactory.canHandle(key)) {
                    return OperatorFactory.create(key, value)
                }
            }
        } else if (objectToTransform is String) {
            if (OperatorFactory.canHandle(objectToTransform)) {
                return OperatorFactory.create(objectToTransform)
            }
        }

        return objectToTransform!!
    }
}