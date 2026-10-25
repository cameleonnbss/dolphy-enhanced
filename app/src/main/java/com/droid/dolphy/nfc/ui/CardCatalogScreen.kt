package com.droid.dolphy.nfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droid.dolphy.MaterialBackground
import com.droid.dolphy.MaterialCard
import com.droid.dolphy.R
import com.droid.dolphy.SectionTopBar
import com.droid.dolphy.nfc.CardCatalog
import com.droid.dolphy.nfc.NfcCard

/**
 * Browsable reference of NFC / RFID card families.
 *
 * Purpose: when you scan a card and get "SAK 0x08, ATQA 0x0004", you can look up
 * what that means and whether it is expected to be clonable. Data lives in
 * `assets/nfc/cards.json` — add an entry there and it shows up here, in every
 * language, with no code change.
 */
@Composable
fun CardCatalogScreen(navController: NavController) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    val categories = remember { CardCatalog.categories(context) }
    val results = remember(query, category) { CardCatalog.search(context, query, category) }

    MaterialBackground(accentColor = accent) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionTopBar(
                title = stringResource(R.string.card_db_title),
                onBack = { navController.popBackStack() },
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.card_db_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )

            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = category == null,
                        onClick = { category = null },
                        label = { Text(stringResource(R.string.card_db_all)) },
                    )
                }
                items(categories) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = if (category == item) null else item },
                        label = { Text(item.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.card_db_count, results.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                items(results, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        accent = accent,
                        isOpen = expanded == card.id,
                        onToggle = { expanded = if (expanded == card.id) null else card.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardRow(
    card: NfcCard,
    accent: androidx.compose.ui.graphics.Color,
    isOpen: Boolean,
    onToggle: () -> Unit,
) {
    MaterialCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        accentColor = accent,
        contentPadding = 14.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        card.standard.ifBlank { card.category },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (isOpen) 180f else 0f),
                )
            }

            AnimatedVisibility(visible = isOpen) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    card.technical.forEach { (label, value) ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text(
                                "$label  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                value,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = if (label in setOf("ATQA", "SAK", "AID")) FontFamily.Monospace
                                else FontFamily.Default,
                            )
                        }
                    }
                    if (card.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            card.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
