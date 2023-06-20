package uk.dioxic.mgenerate.execute

import uk.dioxic.mgenerate.execute.model.State
import uk.dioxic.mgenerate.template.Template

interface Stateful {
    val state: Template
    val hydratedState: State
}