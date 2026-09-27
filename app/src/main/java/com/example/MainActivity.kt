package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.KifinViewModel
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: KifinViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                KifinMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KifinMainApp(viewModel: KifinViewModel) {
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()

    if (isAppLocked) {
        PinLockScreen(
            errorMessage = pinError,
            onPinSubmit = { pin ->
                viewModel.unlockWithPin(pin)
            },
            onClearError = {
                viewModel.clearPinError()
            }
        )
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    // Home screen states
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val allTimeBalance by viewModel.allTimeBalance.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val monthIncome by viewModel.selectedMonthIncome.collectAsStateWithLifecycle()
    val monthExpense by viewModel.selectedMonthExpense.collectAsStateWithLifecycle()
    val monthlyChartPoints by viewModel.monthlyChartPoints.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()

    // Add / Edit Sheet states
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val inputTransactionType by viewModel.inputTransactionType.collectAsStateWithLifecycle()
    val inputAmount by viewModel.inputAmount.collectAsStateWithLifecycle()
    val inputNote by viewModel.inputNote.collectAsStateWithLifecycle()
    val noteSuggestions by viewModel.noteSuggestions.collectAsStateWithLifecycle()
    val transactionToDelete by viewModel.transactionToDelete.collectAsStateWithLifecycle()

    // History screen states
    val historyStartDate by viewModel.historyStartDate.collectAsStateWithLifecycle()
    val historyEndDate by viewModel.historyEndDate.collectAsStateWithLifecycle()
    val historyPreset by viewModel.historyPreset.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historyTypeFilter by viewModel.historyTypeFilter.collectAsStateWithLifecycle()
    val historyNoteGroups by viewModel.historyNoteGroups.collectAsStateWithLifecycle()
    val selectedNoteGroup by viewModel.selectedNoteGroup.collectAsStateWithLifecycle()
    val historyChartPoints by viewModel.historyChartPoints.collectAsStateWithLifecycle()
    val filteredHistoryTransactions by viewModel.filteredHistoryTransactions.collectAsStateWithLifecycle()

    // Settings screen states
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.preferences.isPinEnabled.collectAsStateWithLifecycle()
    val isPinConfigured = viewModel.preferences.isPinConfigured()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_kifin_logo),
                            contentDescription = "Logo KIFIN",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KIFIN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                // Tab 1: Utama
                NavigationBarItem(
                    selected = currentTab == AppTab.HOME,
                    onClick = { viewModel.selectTab(AppTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Utama"
                        )
                    },
                    label = { Text("Utama", fontWeight = if (currentTab == AppTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 2: Riwayat
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { viewModel.selectTab(AppTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.HISTORY) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat", fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Tab 3: Pengaturan
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Pengaturan"
                        )
                    },
                    label = { Text("Pengaturan", fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.HOME) {
                FloatingActionButton(
                    onClick = { viewModel.openAddTransactionSheet() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    AppTab.HOME -> {
                        HomeScreen(
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth,
                            allTimeBalance = allTimeBalance,
                            monthIncome = monthIncome,
                            monthExpense = monthExpense,
                            isBalanceHidden = isBalanceHidden,
                            chartPoints = monthlyChartPoints,
                            recentTransactions = recentTransactions,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onToggleBalanceHidden = { viewModel.toggleBalanceVisibility() },
                            onOpenAddModal = { viewModel.openAddTransactionSheet() },
                            onEditTransaction = { tx -> viewModel.openEditTransactionSheet(tx) },
                            onDeleteTransaction = { tx -> viewModel.confirmDelete(tx) },
                            onNavigateToHistory = { viewModel.selectTab(AppTab.HISTORY) }
                        )
                    }

                    AppTab.HISTORY -> {
                        HistoryScreen(
                            startDate = historyStartDate,
                            endDate = historyEndDate,
                            activePreset = historyPreset,
                            searchQuery = historySearchQuery,
                            typeFilter = historyTypeFilter,
                            noteGroups = historyNoteGroups,
                            selectedNoteGroup = selectedNoteGroup,
                            chartPoints = historyChartPoints,
                            transactions = filteredHistoryTransactions,
                            onSelectPreset = { preset -> viewModel.setHistoryPreset(preset) },
                            onSetCustomRange = { start, end -> viewModel.setCustomDateRange(start, end) },
                            onSearchQueryChange = { query -> viewModel.setHistorySearchQuery(query) },
                            onTypeFilterChange = { type -> viewModel.setHistoryTypeFilter(type) },
                            onSelectNoteGroup = { group -> viewModel.selectNoteGroup(group) },
                            onEditTransaction = { tx -> viewModel.openEditTransactionSheet(tx) },
                            onDeleteTransaction = { tx -> viewModel.confirmDelete(tx) }
                        )
                    }

                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            themeMode = themeMode,
                            isPinEnabled = isPinEnabled,
                            isPinConfigured = isPinConfigured,
                            onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
                            onPinToggle = { enabled -> viewModel.preferences.setPinEnabled(enabled) },
                            onSetNewPin = { pin -> viewModel.setPin(pin) },
                            onLockApp = { viewModel.lockApp() },
                            onExport = { format -> viewModel.exportTransactions(format) },
                            onImportUri = { uri -> viewModel.importData(uri) }
                        )
                    }
                }
            }
        }

        // Add / Edit Transaction Modal BottomSheet
        AddEditTransactionSheet(
            isOpen = isAddEditOpen,
            editingTransaction = editingTransaction,
            transactionType = inputTransactionType,
            amount = inputAmount,
            note = inputNote,
            noteSuggestions = noteSuggestions,
            onTypeChange = { type -> viewModel.setInputType(type) },
            onAddAmount = { delta -> viewModel.addToNominal(delta) },
            onResetAmount = { viewModel.resetNominal() },
            onSetAmount = { amt -> viewModel.setNominal(amt) },
            onNoteChange = { n -> viewModel.setInputNote(n) },
            onSave = { viewModel.saveTransaction() },
            onDismiss = { viewModel.closeAddEditSheet() }
        )

        // Delete Confirmation Dialog
        DeleteConfirmationDialog(
            transaction = transactionToDelete,
            onConfirm = { viewModel.executeDelete() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}
