package uk.dioxic.mgenerate.execute.resources

import kotlin.reflect.KClass

class ResourceRegistry(vararg resource: Resource) {
    private val resourceMap = HashMap<KClass<out Resource>, Resource>(resource.associateBy { it::class })

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Resource> get(resourceClass: KClass<T>): T {
        require(resourceMap.contains(resourceClass)) {
            "Resource $resourceClass not found!"
        }
        return resourceMap[resourceClass] as T
    }
}

