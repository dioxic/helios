package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.State
import uk.dioxic.helios.generate.Template

interface Stateful {
    val state: Template
    val hydratedState: State
}