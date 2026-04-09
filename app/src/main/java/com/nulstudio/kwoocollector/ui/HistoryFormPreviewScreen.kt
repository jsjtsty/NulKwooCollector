package com.nulstudio.kwoocollector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.model.response.FormField
import com.nulstudio.kwoocollector.util.jsonObjectToFormState
import com.nulstudio.kwoocollector.util.resolveVisibleFields
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFormPreviewScreen(
    formId: Int,
    onBack: () -> Unit
) {
    val apiService = koinInject<ApiService>()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("历史表单") }
    var description by remember { mutableStateOf("查看历史提交内容。") }
    var schema by remember { mutableStateOf<List<FormField>>(emptyList()) }
    var values by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    val visibleSchema = remember(schema, values) { schema.resolveVisibleFields(values) }

    LaunchedEffect(formId) {
        isLoading = true
        errorMessage = null
        runCatching { apiService.fetchFormHistoryDetail(formId) }
            .onSuccess { response ->
                if (response.code == 0 && response.result != null) {
                    title = response.result.title
                    description = response.result.description
                    schema = response.result.fields
                    values = jsonObjectToFormState(response.result.fields, response.result.content)
                } else {
                    errorMessage = response.message ?: "加载历史表单失败"
                }
            }
            .onFailure { error ->
                errorMessage = error.localizedMessage ?: "加载历史表单失败"
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("历史表单", fontWeight = FontWeight.Bold) },
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
            schema.isEmpty() -> ErrorState(message = "当前历史表单无字段可展示", modifier = Modifier.padding(paddingValues))
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
                            description = description
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
