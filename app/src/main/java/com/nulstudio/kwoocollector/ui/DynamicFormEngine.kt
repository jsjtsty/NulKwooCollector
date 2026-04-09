package com.nulstudio.kwoocollector.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nulstudio.kwoocollector.net.model.response.FormField
import com.nulstudio.kwoocollector.util.flattenFormFields
import com.nulstudio.kwoocollector.util.resolveVisibleFields
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

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
    val currentValues = formState.toMap()
    val visibleFields = remember(fields, currentValues) {
        fields.resolveVisibleFields(currentValues)
    }

    LaunchedEffect(fields, visibleFields) {
        val visibleKeys = visibleFields.mapTo(mutableSetOf()) { it.key }
        fields.flattenFormFields()
            .map { it.key }
            .filterNot(visibleKeys::contains)
            .forEach { hiddenKey ->
                formState.remove(hiddenKey)
            }
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

                visibleFields.forEach { field ->
                    when (field) {
                        is FormField.Text -> StringInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.Number -> NumberInputItem(field, formState, enabled = !isSubmitting)
                        is FormField.DateTime -> DateTimeInputItem(field, formState, enabled = !isSubmitting)
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
                val visibleValues = formState.toMap()
                    .filterKeys { key -> visibleFields.any { it.key == key } }
                validateForm(visibleFields, visibleValues)?.let(onValidationError) ?: onSubmit(visibleValues)
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
            return "请填写 ${field.label}"
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
            Text(
                buildFieldSupportText(
                    field = field,
                    extra = field.maxLength?.let { max -> "${textValue.length}/$max" }
                )
            )
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
                    text = buildFieldSupportText(
                        field = field,
                        extra = if (isChecked) "当前为开启" else "当前为关闭"
                    ),
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
    var inputValue by remember(field.key) {
        mutableStateOf(formatNumber(state[field.key]))
    }

    val parsedNumber = inputValue.toDoubleOrNull()
    val isIncompleteNumber = inputValue == "-" || inputValue == "." || inputValue == "-."
    val isOutOfRange = parsedNumber != null && !isNumberWithinRange(parsedNumber, field)
    val hasInputError = inputValue.isNotEmpty() && !isIncompleteNumber && (parsedNumber == null || isOutOfRange)

    LaunchedEffect(state[field.key]) {
        if (state[field.key] != null || inputValue.isBlank()) {
            val formattedValue = formatNumber(state[field.key])
            if (formattedValue != inputValue) {
                inputValue = formattedValue
            }
        }
    }

    OutlinedTextField(
        value = inputValue,
        onValueChange = { newValue ->
            if (!NUMBER_INPUT_PATTERN.matches(newValue)) {
                return@OutlinedTextField
            }

            inputValue = newValue
            if (newValue.isEmpty() || newValue == "-" || newValue == "." || newValue == "-.") {
                state.remove(field.key)
                return@OutlinedTextField
            }

            val number = newValue.toDoubleOrNull()
            if (number != null && isNumberWithinRange(number, field)) {
                state[field.key] = number
            } else {
                state.remove(field.key)
            }
        },
        label = { Text(if (field.required) "* ${field.label}" else field.label) },
        supportingText = {
            Text(
                buildFieldSupportText(
                    field = field,
                    extra = when {
                        hasInputError && isOutOfRange -> buildRangeText(field)
                        hasInputError -> "请输入有效数字"
                        else -> buildRangeText(field)
                    }
                )
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        isError = hasInputError,
        singleLine = true
    )
}

@Composable
private fun DateTimeInputItem(field: FormField.DateTime, state: MutableMap<String, Any>, enabled: Boolean) {
    val context = LocalContext.current
    val dateTimeValue = state[field.key] as? String ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                showDateTimePicker(
                    context = context,
                    initialValue = dateTimeValue,
                    onDateTimeSelected = { state[field.key] = it }
                )
            }
    ) {
        OutlinedTextField(
            value = dateTimeValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (field.required) "* ${field.label}" else field.label) },
            supportingText = {
                Text(buildFieldSupportText(field, "格式：YYYY-MM-DD HH:mm"))
            },
            placeholder = { Text("请选择日期时间") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "选择日期时间"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

private fun showDateTimePicker(
    context: Context,
    initialValue: String,
    onDateTimeSelected: (String) -> Unit
) {
    val initialCalendar = parseDateTimeValue(initialValue) ?: Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = (initialCalendar.clone() as Calendar).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    pickedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    pickedCalendar.set(Calendar.MINUTE, minute)
                    pickedCalendar.set(Calendar.SECOND, 0)
                    pickedCalendar.set(Calendar.MILLISECOND, 0)
                    onDateTimeSelected(formatDateTimeValue(pickedCalendar))
                },
                pickedCalendar.get(Calendar.HOUR_OF_DAY),
                pickedCalendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context)
            ).show()
        },
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun parseDateTimeValue(value: String): Calendar? {
    val matchResult = DATETIME_VALUE_PATTERN.matchEntire(value) ?: return null
    val (year, month, day, hour, minute) = matchResult.destructured
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year.toInt())
        set(Calendar.MONTH, month.toInt() - 1)
        set(Calendar.DAY_OF_MONTH, day.toInt())
        set(Calendar.HOUR_OF_DAY, hour.toInt())
        set(Calendar.MINUTE, minute.toInt())
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun formatDateTimeValue(calendar: Calendar): String {
    val year = calendar.get(Calendar.YEAR)
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$year-$month-$day $hour:$minute"
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
            supportingText = { Text(buildFieldSupportText(field)) },
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
        Text(
            text = buildFieldSupportText(field),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun buildFieldSupportText(field: FormField, extra: String? = null): String {
    return listOfNotNull(fieldTypeLabel(field), extra).joinToString(" · ")
}

private fun fieldTypeLabel(field: FormField): String {
    return when (field) {
        is FormField.Text -> "类型：文本"
        is FormField.Number -> "类型：数字"
        is FormField.DateTime -> "类型：日期时间"
        is FormField.Bool -> "类型：开关"
        is FormField.Select -> "类型：单选"
        is FormField.Image -> "类型：图片"
    }
}

private fun buildRangeText(field: FormField.Number): String? {
    val parts = buildList {
        field.min?.let { add("最小值 $it") }
        field.max?.let { add("最大值 $it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun formatNumber(value: Any?): String {
    val number = value as? Number ?: return ""
    return NUMBER_DISPLAY_FORMAT.format(number)
}

private fun isNumberWithinRange(value: Double, field: FormField.Number): Boolean {
    val withinMin = field.min?.let { value >= it } ?: true
    val withinMax = field.max?.let { value <= it } ?: true
    return withinMin && withinMax
}

private val NUMBER_INPUT_PATTERN = Regex("^-?(\\d+)?(\\.\\d*)?$")
private val DATETIME_VALUE_PATTERN = Regex("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2})$")
private val NUMBER_DISPLAY_FORMAT = DecimalFormat("0.###############")
