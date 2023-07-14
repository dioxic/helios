@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.helios.execute.model

import com.mongodb.client.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.DeleteOptionsSerializer
import uk.dioxic.helios.execute.serialization.UpdateOptionsSerializer
import uk.dioxic.helios.generate.Template

@Serializable
sealed interface WriteOperation<T> {
    val count: Int
    val writeModels: List<WriteModel<T>>
        get() = List(count) { toWriteModel() }

    fun toWriteModel(): WriteModel<T>
}

@Serializable
@SerialName("insert")
data class InsertOneOperation(
    override val count: Int,
    val template: Template,
) : WriteOperation<Template> {

    override fun toWriteModel(): WriteModel<Template> =
        InsertOneModel(template)
}

@Serializable
@SerialName("updateOne")
data class UpdateOneOperation(
    override val count: Int,
    val filter: Template,
    val update: Template,
    @Serializable(with = UpdateOptionsSerializer::class) val options: UpdateOptions,
) : WriteOperation<Template> {

    override fun toWriteModel(): WriteModel<Template> =
        UpdateOneModel(filter, update, options)
}

@Serializable
@SerialName("updateMany")
data class UpdateManyOperation(
    override val count: Int,
    val filter: Template,
    val update: Template,
    @Serializable(with = UpdateOptionsSerializer::class) val options: UpdateOptions,
) : WriteOperation<Template> {

    override fun toWriteModel(): WriteModel<Template> =
        UpdateManyModel(filter, update, options)
}

@Serializable
@SerialName("deleteOne")
data class DeleteOneOperation(
    override val count: Int,
    val filter: Template,
    @Serializable(with = DeleteOptionsSerializer::class) val options: DeleteOptions,
) : WriteOperation<Template> {

    override fun toWriteModel(): WriteModel<Template> =
        DeleteOneModel(filter, options)
}

@Serializable
@SerialName("deleteMany")
data class DeleteManyOperation(
    override val count: Int,
    val filter: Template,
    @Serializable(with = DeleteOptionsSerializer::class) val options: DeleteOptions,
) : WriteOperation<Template> {

    override fun toWriteModel(): WriteModel<Template> =
        DeleteManyModel(filter, options)
}