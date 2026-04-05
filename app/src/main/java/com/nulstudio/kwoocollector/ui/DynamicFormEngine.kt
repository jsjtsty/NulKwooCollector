package com.nulstudio.kwoocollector.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nulstudio.kwoocollector.net.model.response.FormField
import kotlinx.coroutines.launch

@Composable
fun DynamicFormEngine(
    fields: List<FormField>,
    initialValues: Map<String, Any> = emptyMap(),
    isSubmitting: Boolean = false,
    submitLabel: String = "提交",
    onUploadImage: suspend (Uri) -> String?,
    onValidationError: (String) -> Unit,
    onSubmit: (Map<String, Any>) -> Unit
) {
    val formState = remember(fields, initialValues) {
        mutableStateMapOf<String, Any>().apply { putAll(initialValues) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "字段录入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                fields.forEach { field ->
                    when (field) {
                        is FormField.Text -> StringInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.Number -> NumberInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.Bool -> BoolInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.Select -> SelectInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.Image -> ImageInputItem(
                            field = field,
                            state = formState,
                            enabled = !isSubmitting,
                            onUploadImage = onUploadImage
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                validateForm(fields, formState.toMap())?.let(onValidationError) ?: onSubmit(formState.toMap())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isSubmitting,
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(submitLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun validateForm(fields: List<FormField>, values: Map<String, Any>): String? {
    fields.forEach { field ->
        if (!field.required) return@forEach

        val value = values[field.key]
        val invalid = when (value) {
            null -> true
            is String -> value.isBlank()
            is List<*> -> value.isEmpty()
            else -> false
        }

        if (invalid) {
            return "请填写${field.label}"
        }
    }
    return null
}

@Composable
private fun StringInputItem(field: FormField.Text, state: MutableMap<String, Any>, enabled: Boolean) {
    val textValue = state[field.key] as? String ?: ""

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (field.maxLength == null || newValue.length <= field.maxLength) {
                state[field.key] = newValue
            }
        },
        label = { Text(if (field.required) "* ${field.label}" else field.label) },
        supportingText = {
            field.maxLength?.let { max ->
                Text("${textValue.length}/$max")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled
    )
}

@Composable
private fun BoolInputItem(field: FormField.Bool, state: MutableMap<String, Any>, enabled: Boolean) {
    val isChecked = state[field.key] as? Boolean ?: false

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (field.required) "* ${field.label}" else field.label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isChecked) "已开启" else "已关闭",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = { state[field.key] = it },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun NumberInputItem(field: FormField.Number, state: MutableMap<String, Any>, enabled: Boolean) {
    val numberValue = state[field.key]?.toString() ?: ""

    OutlinedTextField(
        value = numberValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty()) {
                state.remove(field.key)
            } else {
                newValue.toDoubleOrNull()?.let { num ->
                    val withinMin = field.min?.let { num >= it } ?: true
                    val withinMax = field.max?.let { num <= it } ?: true
                    if (withinMin && withinMax) {
                        state[field.key] = num
                    }
                }
            }
        },
        label = { Text(if (field.required) "* ${field.label}" else field.label) },
        supportingText = {
            val parts = buildList {
                field.min?.let { add("最小 $it") }
                field.max?.let { add("最大 $it") }
            }
            if (parts.isNotEmpty()) {
                Text(parts.joinToString(" · "))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectInputItem(field: FormField.Select, state: MutableMap<String, Any>, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val selectedId = state[field.key] as? Int
    val selectedOptionName = field.options.find { it.id == selectedId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOptionName,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (field.required) "* ${field.label}" else field.label) },
            placeholder = { Text("请选择") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                )
                .fillMaxWidth(),
            enabled = enabled
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            field.options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item.name) },
                    onClick = {
                        state[field.key] = item.id
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageInputItem(
    field: FormField.Image,
    state: MutableMap<String, Any>,
    enabled: Boolean,
    onUploadImage: suspend (Uri) -> String?
) {
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    val imageIds = (state[field.key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        uris.forEach { uri ->
            coroutineScope.launch {
                isUploading = true
                val remoteId = onUploadImage(uri)
                if (remoteId != null) {
                    val currentList = (state[field.key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    state[field.key] = currentList + remoteId
                }
                isUploading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (field.required) "* ${field.label}" else field.label,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            imageIds.forEach { id ->
                ImagePreviewItem(
                    imageUrl = id,
                    onDelete = {
                        state[field.key] = imageIds.filter { it != id }
                    },
                    enabled = enabled && !isUploading
                )
            }

            if (imageIds.size < 9 && enabled && !isUploading) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            launcher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加图片",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "添加",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewItem(imageUrl: String, onDelete: () -> Unit, enabled: Boolean) {
    Box(modifier = Modifier.size(88.dp)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )

        if (enabled) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
