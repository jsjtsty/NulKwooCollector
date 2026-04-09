package com.nulstudio.kwoocollector.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.FormDetailResponse
import com.nulstudio.kwoocollector.net.model.response.FormField
import com.nulstudio.kwoocollector.util.formStateToJsonObject
import com.nulstudio.kwoocollector.util.jsonObjectToFormState
import com.nulstudio.kwoocollector.util.resolveVisibleFields
import com.nulstudio.kwoocollector.util.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTaskScreen(
    formId: Int,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val apiService = koinInject<ApiService>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<FormDetailResponse?>(null) }

    LaunchedEffect(formId) {
        isLoading = true
        errorMessage = null
        runCatching { apiService.fetchForm(formId) }
            .onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    form = response.result
                } else {
                    errorMessage = response.message ?: "加载表单失败"
                }
            }
            .onFailure { error ->
                errorMessage = error.localizedMessage ?: "网络连接异常"
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(form?.title ?: "表单填写", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            errorMessage != null -> ErrorState(message = errorMessage!!, modifier = Modifier.padding(paddingValues))
            form != null -> FormEditorLayout(
                paddingValues = paddingValues,
                description = form!!.description,
                fields = form!!.fields,
                initialValues = emptyMap(),
                isSubmitting = isSubmitting,
                submitLabel = "提交表单",
                onValidationError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                onUploadImage = { uri -> uploadImage(apiService, context, uri) },
                onSubmit = { values ->
                    scope.launch {
                        isSubmitting = true
                        runCatching {
                            apiService.fillForm(formId, formStateToJsonObject(values))
                        }.onSuccess { response ->
                            if (response.code == 0) {
                                Toast.makeText(context, "表单提交成功", Toast.LENGTH_SHORT).show()
                                onSubmitted()
                            } else {
                                Toast.makeText(
                                    context,
                                    response.message ?: "表单提交失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "网络连接异常",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isSubmitting = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    tableId: Int,
    entryId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val apiService = koinInject<ApiService>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = entryId > 0

    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf(if (isEditMode) "编辑记录" else "新增记录") }
    var description by remember {
        mutableStateOf(if (isEditMode) "修改后保存，列表页将显示最新结果。" else "填写业务字段并提交到数据中心。")
    }
    var schema by remember { mutableStateOf<List<FormField>>(emptyList()) }
    var initialValues by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    LaunchedEffect(tableId, entryId) {
        isLoading = true
        errorMessage = null

        runCatching { apiService.fetchTables() }
            .onSuccess { tableResponse ->
                val table = tableResponse.result?.find { it.id == tableId }
                if (tableResponse.code == 0 && table != null) {
                    title = if (isEditMode) "编辑${table.name}" else "新增${table.name}"
                    description = if (isEditMode) {
                        "修改后保存，列表页将显示最新结果。"
                    } else {
                        "填写业务字段并提交到数据中心。"
                    }
                    schema = table.schema
                } else {
                    errorMessage = tableResponse.message ?: "未找到业务表"
                }
            }
            .onFailure { error ->
                errorMessage = error.localizedMessage ?: "加载业务表失败"
            }

        if (errorMessage == null && isEditMode) {
            runCatching { apiService.fetchEntry(tableId, entryId) }
                .onSuccess { entryResponse ->
                    if (entryResponse.code == 0 && entryResponse.result != null) {
                        initialValues = jsonObjectToFormState(
                            fields = schema,
                            content = entryResponse.result
                        )
                    } else {
                        errorMessage = entryResponse.message ?: "加载记录失败"
                    }
                }
                .onFailure { error ->
                    errorMessage = error.localizedMessage ?: "加载记录失败"
                }
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            errorMessage != null -> ErrorState(message = errorMessage!!, modifier = Modifier.padding(paddingValues))
            schema.isEmpty() -> ErrorState(message = "当前业务表未配置字段", modifier = Modifier.padding(paddingValues))
            else -> FormEditorLayout(
                paddingValues = paddingValues,
                description = description,
                fields = schema,
                initialValues = initialValues,
                isSubmitting = isSubmitting,
                submitLabel = if (isEditMode) "保存修改" else "新增记录",
                onValidationError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                onUploadImage = { uri -> uploadImage(apiService, context, uri) },
                onSubmit = { values ->
                    scope.launch {
                        isSubmitting = true
                        val payload = formStateToJsonObject(values)
                        runCatching {
                            if (isEditMode) {
                                apiService.updateEntry(tableId, entryId, payload)
                            } else {
                                apiService.createEntry(tableId, payload)
                            }
                        }.onSuccess { response ->
                            if (response.code == 0) {
                                Toast.makeText(
                                    context,
                                    if (isEditMode) "记录已更新" else "记录已创建",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaved()
                            } else {
                                Toast.makeText(
                                    context,
                                    response.message ?: "保存失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "网络连接异常",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isSubmitting = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EntryPreviewScreen(
    tableId: Int,
    entryId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val apiService = koinInject<ApiService>()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("记录详情") }
    var description by remember { mutableStateOf("查看当前记录的详细内容。") }
    var schema by remember { mutableStateOf<List<FormField>>(emptyList()) }
    var values by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    val visibleSchema = remember(schema, values) { schema.resolveVisibleFields(values) }

    LaunchedEffect(tableId, entryId) {
        isLoading = true
        errorMessage = null

        runCatching { apiService.fetchTables() }
            .onSuccess { tableResponse ->
                val table = tableResponse.result?.find { it.id == tableId }
                if (tableResponse.code == 0 && table != null) {
                    title = table.name
                    description = "记录 ID: $entryId"
                    schema = table.schema
                } else {
                    errorMessage = tableResponse.message ?: "未找到业务表"
                }
            }
            .onFailure { error ->
                errorMessage = error.localizedMessage ?: "加载业务表失败"
            }

        if (errorMessage == null) {
            runCatching { apiService.fetchEntry(tableId, entryId) }
                .onSuccess { response ->
                    if (response.code == 0 && response.result != null) {
                        values = jsonObjectToFormState(schema, response.result)
                    } else {
                        errorMessage = response.message ?: "加载记录失败"
                    }
                }
                .onFailure { error ->
                    errorMessage = error.localizedMessage ?: "加载记录失败"
                }
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("预览记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            errorMessage != null -> ErrorState(message = errorMessage!!, modifier = Modifier.padding(paddingValues))
            schema.isEmpty() -> ErrorState(message = "当前业务表未配置字段", modifier = Modifier.padding(paddingValues))
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PreviewInfoHeroCard(
                            title = title,
                            description = description,
                            trailingAction = {
                                FilledTonalButton(
                                    onClick = onEdit,
                                    modifier = Modifier.padding(top = 18.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Text("编辑此记录", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        )
                    }

                    items(visibleSchema, key = { it.key }) { field ->
                        when (field) {
                            is FormField.Image -> {
                                val images = (values[field.key] as? List<*>)?.filterIsInstance<String>().orEmpty()
                                PreviewImageSection(
                                    label = field.label,
                                    images = images
                                )
                            }

                            else -> {
                                PreviewInfoCard(
                                    label = field.label,
                                    value = previewValueText(field, values[field.key])
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewInfoHeroCard(
    title: String,
    description: String,
    trailingAction: (@Composable () -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                .padding(22.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            trailingAction?.invoke()
        }
    }
}

@Composable
fun PreviewInfoCard(label: String, value: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewImageSection(label: String, images: List<String>) {
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (images.isEmpty()) {
                Text(
                    text = "未上传图片",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    images.forEach { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { previewImageUrl = imageUrl },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    if (previewImageUrl != null) {
        Dialog(
            onDismissRequest = { previewImageUrl = null }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = previewImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )

                    IconButton(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭预览"
                        )
                    }
                }
            }
        }
    }
}

fun previewValueText(field: FormField, value: Any?): String {
    return when (field) {
        is FormField.Text -> value as? String ?: "-"
        is FormField.Number -> value?.toString() ?: "-"
        is FormField.DateTime -> value as? String ?: "-"
        is FormField.Bool -> when (value as? Boolean) {
            true -> "是"
            false -> "否"
            null -> "-"
        }

        is FormField.Select -> {
            val selectedId = value as? Int
            field.options.find { it.id == selectedId }?.name ?: "-"
        }

        is FormField.Image -> {
            val images = (value as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (images.isEmpty()) "未上传图片" else "已上传 ${images.size} 张图片"
        }
    }
}

@Composable
private fun FormEditorLayout(
    paddingValues: PaddingValues,
    description: String,
    fields: List<FormField>,
    initialValues: Map<String, Any>,
    isSubmitting: Boolean,
    submitLabel: String,
    onValidationError: (String) -> Unit,
    onUploadImage: suspend (Uri) -> String?,
    onSubmit: (Map<String, Any>) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            DynamicFormEngine(
                fields = fields,
                initialValues = initialValues,
                isSubmitting = isSubmitting,
                submitLabel = submitLabel,
                onUploadImage = onUploadImage,
                onValidationError = onValidationError,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            ElevatedCard(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private suspend fun uploadImage(
    apiService: ApiService,
    context: Context,
    uri: Uri
): String? = withContext(Dispatchers.IO) {
    val tempFile = uri.toTempFile(context) ?: return@withContext null
    try {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = tempFile.name,
            body = tempFile.asRequestBody("image/*".toMediaType())
        )
        val response = apiService.uploadImage(part)
        if (response.code == 0) response.result else null
    } finally {
        tempFile.delete()
    }
}
