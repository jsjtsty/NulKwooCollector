package com.nulstudio.kwoocollector.util

import com.nulstudio.kwoocollector.net.model.response.FormField

fun List<FormField>.flattenFormFields(): List<FormField> = buildList {
    this@flattenFormFields.forEach { field ->
        add(field)
        if (field is FormField.Select) {
            field.branches.forEach { branch ->
                addAll(branch.fields.flattenFormFields())
            }
        }
    }
}

fun List<FormField>.resolveVisibleFields(values: Map<String, Any>): List<FormField> = buildList {
    this@resolveVisibleFields.forEach { field ->
        add(field)
        if (field is FormField.Select) {
            val selectedId = (values[field.key] as? Number)?.toInt()
            val branchFields = field.branches
                .firstOrNull { it.optionId == selectedId }
                ?.fields
                .orEmpty()
            addAll(branchFields.resolveVisibleFields(values))
        }
    }
}
