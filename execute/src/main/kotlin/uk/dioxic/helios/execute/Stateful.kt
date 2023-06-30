package uk.dioxic.helios.execute

import uk.dioxic.helios.generate.Template

interface Stateful {
    val constantsDefinition: Template
    val variablesDefinition: Template
    val constants : Lazy<Map<String, Any?>>
    val variables : Lazy<Map<String, Any?>>
}