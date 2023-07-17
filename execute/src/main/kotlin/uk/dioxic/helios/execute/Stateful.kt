package uk.dioxic.helios.execute

import uk.dioxic.helios.generate.Named
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten

interface Stateful: Named {
    val constantsDefinition: Template
    val variablesDefinition: Template
    val constants : Lazy<Map<String, Any?>>
    val variables
        get() = variablesDefinition.hydrateAndFlatten()
}