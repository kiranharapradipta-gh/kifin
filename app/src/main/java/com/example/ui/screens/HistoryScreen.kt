package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.DailyChartPoint
import com.example.ui.DateRangePreset
import com.example.ui.NoteGroupSummary
import com.example.ui.components.MonthlyLineChart
import com.example.ui.components.NoteGroupDetailDialog
import com.example.ui.components.NoteGroupDonutChart
import com.example.ui.components.SwipeableTransactionCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    startDate: Long,
    endDate: Long,
    activePreset: DateRangePreset,
    searchQuery: String,
    typeFilter: TransactionType?,
    noteGroups: List<NoteGroupSummary>,
    selectedNoteGroup: NoteGroupSummary?,
    chartPoints: List<DailyChartPoint>,
    transactions: List<TransactionEntity>,
    onSelectPreset: (DateRangePreset) -> Unit,
    onSetCustomRange: (Long, Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onSelectNoteGroup: (NoteGroupSummary?) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pagination: default 20, load more +10
    var visibleCount by remember(transactions.size, searchQuery, typeFilter, startDate, endDate) {
        mutableStateOf(20)
    }
    val displayedTransactions = transactions.take(visibleCount)

    // DatePicker helpers
    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = if (startDate > 0) startDate else System.currentTimeMillis() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newStartCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }
                onSetCustomRange(newStartCal.timeInMillis, endDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = if (endDate < Long.MAX_VALUE) endDate else System.currentTimeMillis() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newEndCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                }
                onSetCustomRange(startDate, newEndCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen_container")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Title
            item {
                Text(
                    text = "Riwayat & Analisis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Filter rentang tanggal, lihat ringkasan kategori, dan cari data transaksi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. Input Penentu Rentang Tanggal
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_range_picker_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rentang Tanggal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Range Presets
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = activePreset == DateRangePreset.THIS_MONTH,
                                onClick = { onSelectPreset(DateRangePreset.THIS_MONTH) },
                                label = { Text("Bulan Ini", fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("preset_this_month")
                            )
                            FilterChip(
                                selected = activePreset == DateRangePreset.LAST_7_DAYS,
                                onClick = { onSelectPreset(DateRangePreset.LAST_7_DAYS) },
                                label = { Text("7 Hari Terakhir", fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("preset_7_days")
                            )
                            FilterChip(
                                selected = activePreset == DateRangePreset.LAST_30_DAYS,
                                onClick = { onSelectPreset(DateRangePreset.LAST_30_DAYS) },
                                label = { Text("30 Hari Terakhir", fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("preset_30_days")
                            )
                            FilterChip(
                                selected = activePreset == DateRangePreset.THIS_YEAR,
                                onClick = { onSelectPreset(DateRangePreset.THIS_YEAR) },
                                label = { Text("Tahun Ini", fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("preset_this_year")
                            )
                            FilterChip(
                                selected = activePreset == DateRangePreset.ALL,
                                onClick = { onSelectPreset(DateRangePreset.ALL) },
                                label = { Text("Semua", fontSize = 11.5.sp) },
                                modifier = Modifier.testTag("preset_all")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Date Pickers (Dari - Sampai)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Date
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker() }
                                    .testTag("start_date_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Dari",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (startDate > 0) DateUtils.formatDateShort(startDate) else "Awal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // End Date
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker() }
                                    .testTag("end_date_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Sampai",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (endDate < Long.MAX_VALUE) DateUtils.formatDateShort(endDate) else "Sekarang",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Diagram Lingkaran (Donut) Ringkasan Berdasarkan Catatan Transaksi Sama
            item {
                NoteGroupDonutChart(
                    groups = noteGroups,
                    selectedGroup = selectedNoteGroup,
                    onSelectGroup = onSelectNoteGroup
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Diagram Garis Berdasarkan Rentang Tanggal Terpilih
            item {
                MonthlyLineChart(
                    points = chartPoints,
                    title = "Rentang Tanggal",
                    modifier = Modifier.testTag("history_line_chart")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Input Pencarian Realtime & Filter Jenis Transaksi
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = { Text("Cari...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Tombol Jenis Transaksi (Semua, Pemasukan, Pengeluaran)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_type_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { onTypeFilterChange(null) },
                            label = { Text("Semua") },
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterChip(
                            selected = typeFilter == TransactionType.INCOME,
                            onClick = { onTypeFilterChange(TransactionType.INCOME) },
                            label = { Text("Pemasukan") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                                selectedLabelColor = IncomeGreen
                            ),
                            modifier = Modifier.testTag("filter_income")
                        )
                        FilterChip(
                            selected = typeFilter == TransactionType.EXPENSE,
                            onClick = { onTypeFilterChange(TransactionType.EXPENSE) },
                            label = { Text("Pengeluaran") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                                selectedLabelColor = ExpenseRed
                            ),
                            modifier = Modifier.testTag("filter_expense")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Menampilkan Riwayat Transaksi Sesuai Filter & Pagination
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Riwayat (${displayedTransactions.size}/${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada transaksi yang cocok dengan kriteria filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { tx ->
                    SwipeableTransactionCard(
                        transaction = tx,
                        onEdit = onEditTransaction,
                        onDelete = onDeleteTransaction,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Load More button (+10 items)
                if (transactions.size > displayedTransactions.size) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { visibleCount += 10 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("load_more_history_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Muat Lebih Banyak (+10) • Tersisa ${transactions.size - displayedTransactions.size}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // Tooltip / Detail Dialog for selected note group
        if (selectedNoteGroup != null) {
            NoteGroupDetailDialog(
                summary = selectedNoteGroup,
                onDismiss = { onSelectNoteGroup(null) }
            )
        }
    }
}
