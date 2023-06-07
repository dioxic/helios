package uk.dioxic.mgenerate.extensions

import com.mongodb.MongoClientSettings
import uk.dioxic.mgenerate.Template

fun MongoClientSettings.Builder.applyTemplateCodecRegistry() = codecRegistry(Template.defaultRegistry)