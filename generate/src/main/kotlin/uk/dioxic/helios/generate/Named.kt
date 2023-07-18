package uk.dioxic.helios.generate

interface Named {
    val name: String

    companion object {
        fun create(name: String) =
            object : Named {
                override val name
                    get() = name
            }
    }

}