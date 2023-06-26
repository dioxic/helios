package uk.dioxic.mgenerate.execute.resources

import com.mongodb.client.MongoClient
import kotlin.reflect.KClass

class ResourceRegistry(vararg resource: Any) {
    private val resourceMap = HashMap<KClass<out Any>, Any>(resource.associateBy { getClassKey(it) })

    private fun getClassKey(resource: Any) =
        when (resource) {
            is MongoClient -> MongoClient::class
            else -> resource::class
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getResource(resourceClass: KClass<T>): T {
        require(resourceMap.contains(resourceClass)) {
            "Resource $resourceClass not found!"
        }
        return resourceMap[resourceClass] as T
    }

    inline fun <reified T : Any> getResource(): T =
        getResource(T::class)
}