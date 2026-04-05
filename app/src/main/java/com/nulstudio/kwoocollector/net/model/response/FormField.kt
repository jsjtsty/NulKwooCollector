package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class FormField {
    abstract val key: String
    abstract val label: String
    abstract val required: Boolean

    @Serializable
    @SerialName("text")
    data class Text(
        override val key: String,
        override val label: String,
        override val required: Boolean,
        val maxLength: Int? = null
    ) : FormField()

    @Serializable
    @SerialName("number")
    data class Number(
        override val key: String,
        override val label: String,
        override val required: Boolean,
        val min: Double? = null,
        val max: Double? = null
    ) : FormField()

    @Serializable
    @SerialName("select")
    data class Select(
        override val key: String,
        override val label: String,
        override val required: Boolean,
        val options: List<SelectItem>
    ) : FormField() {
        @Serializable
        data class SelectItem (
            val id: Int,
            val name: String
        )
    }


    @Serializable
    @SerialName("image")
    data class Image(
        override val key: String,
        override val label: String,
        override val required: Boolean,
    ): FormField()

    @Serializable
    @SerialName("bool")
    data class Bool(
        override val key: String,
        override val label: String,
        override val required: Boolean,
    ): FormField()
}