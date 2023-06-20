package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.model.State

interface Stateful {
    val state: Template
    val hydratedState: State
}