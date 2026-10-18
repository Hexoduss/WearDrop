package com.weardrop.ui.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weardrop.BuildConfig
import com.weardrop.R
import com.weardrop.WearDropApplication
import com.weardrop.data.AppSettings
import com.weardrop.data.MAX_PREFERRED_STYLES
import com.weardrop.data.Style
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as WearDropApplication).container
    val settings: StateFlow<AppSettings> = container.settings.state

    fun setCity(value: String) = container.settings.update { it.copy(city = value) }

    /** Adds or removes a favorite style (max [MAX_PREFERRED_STYLES]). */
    fun toggleStyle(style: Style) = container.settings.update { current ->
        val styles = current.preferredStyles
        val next = if (style in styles) styles - style else (styles + style).take(MAX_PREFERRED_STYLES)
        current.copy(preferredStyles = next)
    }
}

/** "" = follow the system language. */
private val LANGUAGES = listOf("" to R.string.lang_system, "en" to R.string.lang_en, "it" to R.string.lang_it)

private fun currentLanguageTag(): String {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return LANGUAGES.map { it.first }.firstOrNull { it.isNotEmpty() && tag.startsWith(it) } ?: ""
}

private fun applyLanguage(tag: String) {
    val locales = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(locales)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selectedLanguage = remember { currentLanguageTag() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(stringResource(R.string.settings_style)) {
                Text(
                    stringResource(R.string.settings_style_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val selected = settings.preferredStyles
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Style.entries.forEach { style ->
                        val isSelected = style in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.toggleStyle(style) },
                            label = { Text(stringResource(style.label)) },
                            enabled = isSelected || selected.size < MAX_PREFERRED_STYLES,
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_language)) {
                LANGUAGES.forEach { (tag, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedLanguage == tag,
                                onClick = { if (selectedLanguage != tag) applyLanguage(tag) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedLanguage == tag, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_weather)) {
                OutlinedTextField(
                    value = settings.city,
                    onValueChange = vm::setCity,
                    label = { Text(stringResource(R.string.settings_city)) },
                    placeholder = { Text(stringResource(R.string.settings_city_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.settings_weather_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.settings_about_text), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}
