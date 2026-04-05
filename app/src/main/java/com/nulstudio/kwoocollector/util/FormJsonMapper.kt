package com.nulstudio.kwoocollector.util

import com.nulstudio.kwoocollector.net.model.response.FormField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun formStateToJsonObject(values: Map<String, Any>): JsonObject = buildJsonObject {
    values.forEach { (key, value) ->
        when (value) {
            is String -> put(key, JsonPrimitive(value))
            is Int -> put(key, JsonPrimitive(value))
            is Long -> put(key, JsonPrimitive(value))
            is Float -> put(key, JsonPrimitive(value))
            is Double -> put(key, JsonPrimitive(value))
            is Boolean -> put(key, JsonPrimitive(value))
            is List<*> -> {
                val items = value.filterIsInstance<String>().map { JsonPrimitive(it) }
                put(key, JsonArray(items))
            }
        }
    }
}

fun jsonObjectToFormState(
    fields: List<FormField>,
    content: JsonObject
): Map<String, Any> = buildMap {
    fields.forEach { field ->
        val value = content[field.key] ?: return@forEach
        parseFieldValue(field, value)?.let { put(field.key, it) }
    }
}

private fun parseFieldValue(field: FormField, value: JsonElement): Any? {
    return when (field) {
        is FormField.Text -> value.jsonPrimitive.contentOrNull
        is FormField.Number -> value.jsonPrimitive.doubleOrNull
        is FormField.Bool -> value.jsonPrimitive.booleanOrNull
        is FormField.Select -> value.jsonPrimitive.intOrNull
        is FormField.Image -> value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
    }
}
