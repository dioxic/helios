@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.helios.execute.model

import com.mongodb.client.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.DeleteOptionsSerializer
import uk.dioxic.helios.execute.serialization.UpdateOptionsSerializer
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Template

typealias VariablesCache = List<Lazy<Map<String, Any?>>>

@Serializable
sealed interface WriteOperation {
    val count: Int

    context(ExecutionContext)
    fun getWriteModels(): List<WriteModel<EncodeContext>> = List(count) {
        toWriteModel(stateContext[it])
    }

    fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext>
}

@Serializable
@SerialName("insert")
data class InsertOneOperation(
    override val count: Int,
    val template: Template,
) : WriteOperation {

    override fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext> =
        InsertOneModel(EncodeContext(template, context))
}

@Serializable
@SerialName("updateOne")
data class UpdateOneOperation(
    override val count: Int,
    val filter: Template,
    val update: Template,
    @Serializable(with = UpdateOptionsSerializer::class) val options: UpdateOptions,
) : WriteOperation {

    override fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext> =
        UpdateOneModel(EncodeContext(filter, context), EncodeContext(update, context), options)
}

@Serializable
@SerialName("updateMany")
data class UpdateManyOperation(
    override val count: Int,
    val filter: Template,
    val update: Template,
    @Serializable(with = UpdateOptionsSerializer::class) val options: UpdateOptions,
) : WriteOperation {

    override fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext> =
        UpdateManyModel(EncodeContext(filter, context), EncodeContext(update, context), options)
}

@Serializable
@SerialName("deleteOne")
data class DeleteOneOperation(
    override val count: Int,
    val filter: Template,
    @Serializable(with = DeleteOptionsSerializer::class) val options: DeleteOptions,
) : WriteOperation {

    override fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext> =
        DeleteOneModel(EncodeContext(filter, context), options)
}

@Serializable
@SerialName("deleteMany")
data class DeleteManyOperation(
    override val count: Int,
    val filter: Template,
    @Serializable(with = DeleteOptionsSerializer::class) val options: DeleteOptions,
) : WriteOperation {

    override fun toWriteModel(context: OperatorContext): WriteModel<EncodeContext> =
        DeleteManyModel(EncodeContext(filter, context), options)
}