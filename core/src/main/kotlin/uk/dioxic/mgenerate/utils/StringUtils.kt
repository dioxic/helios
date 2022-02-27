package uk.dioxic.mgenerate.utils

import uk.dioxic.mgenerate.list.LinkedList

class StringUtils {
    companion object {
        /**
         * Joins a bunch of things in [source]
         * @param[source] the things to join
         * @see Math
         * @return the joined things
         */
        fun join(source: LinkedList): String {
            return JoinUtils.join(source)
        }

        fun split(source: String): LinkedList {
            return SplitUtils.split(source)
        }
    }
}
