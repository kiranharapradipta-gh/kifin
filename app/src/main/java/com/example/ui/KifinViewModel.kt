package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.ExportHelper
import com.example.data.GoogleDriveBackupManager
import com.example.data.ImportHelper
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class AppTab {
    HOME,
    HISTORY,
    SETTINGS
}

enum class DateRangePreset {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH,
    LAST_30_DAYS,
    THIS_YEAR,
    ALL
}

data class DailyChartPoint(
    val day: Int,
    val dateLabel: String,
    val income: Long,
    val expense: Long
)

data class NoteGroupSummary(
    val note: String,
    val totalAmount: Long,
    val count: Int,
    val percentage: Float,
    val transactions: List<TransactionEntity>
)

class KifinViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = TransactionRepository(database.transactionDao())
    val preferences = AppPreferences(application)
    val driveManager = GoogleDriveBackupManager(application)

    // Google Account & Drive Backup states
    val isGoogleLoggedIn: StateFlow<Boolean> = driveManager.isLoggedIn
    val googleAccountName: StateFlow<String> = driveManager.accountName
    val googleAccountEmail: StateFlow<String> = driveManager.accountEmail
    val isAutoBackupEnabled: StateFlow<Boolean> = driveManager.isAutoBackupEnabled
    val lastBackupTime: StateFlow<Long> = driveManager.lastBackupTime

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // PIN lock state
    private val _isAppLocked = MutableStateFlow(preferences.isPinEnabled.value && preferences.isPinConfigured())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Theme mode
    val themeMode: StateFlow<String> = preferences.themeMode

    // Home Screen Month Selection (Year and 0-indexed month)
    private val nowCalendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(nowCalendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(nowCalendar.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    // All transactions from DB
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 10 Recent transactions
    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Distinct notes for suggestions
    val distinctNotes: StateFlow<List<String>> = repository.distinctNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All-time balance (total income - total expense)
    val allTimeBalance: StateFlow<Long> = allTransactions.combine(_selectedMonth) { list, _ ->
        var income = 0L
        var expense = 0L
        list.forEach {
            if (it.type == TransactionType.INCOME.name) income += it.amount
            else expense += it.amount
        }
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Monthly income for selected month
    val selectedMonthIncome: StateFlow<Long> = combine(allTransactions, _selectedYear, _selectedMonth) { list, year, month ->
        val start = DateUtils.getMonthStartMillis(year, month)
        val end = DateUtils.getMonthEndMillis(year, month)
        list.filter { it.timestamp in start..end && it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Monthly expense for selected month
    val selectedMonthExpense: StateFlow<Long> = combine(allTransactions, _selectedYear, _selectedMonth) { list, year, month ->
        val start = DateUtils.getMonthStartMillis(year, month)
        val end = DateUtils.getMonthEndMillis(year, month)
        list.filter { it.timestamp in start..end && it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Daily chart points for selected month
    val monthlyChartPoints: StateFlow<List<DailyChartPoint>> = combine(allTransactions, _selectedYear, _selectedMonth) { list, year, month ->
        val daysInMonth = DateUtils.getDaysInMonth(year, month)
        val start = DateUtils.getMonthStartMillis(year, month)
        val end = DateUtils.getMonthEndMillis(year, month)
        val monthTransactions = list.filter { it.timestamp in start..end }

        val points = mutableListOf<DailyChartPoint>()
        for (day in 1..daysInMonth) {
            val dayTransactions = monthTransactions.filter { DateUtils.getDayOfMonth(it.timestamp) == day }
            val inc = dayTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val exp = dayTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            points.add(DailyChartPoint(day = day, dateLabel = "$day", income = inc, expense = exp))
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Modal / BottomSheet Add & Edit State ---
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    private val _inputTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val inputTransactionType: StateFlow<TransactionType> = _inputTransactionType.asStateFlow()

    private val _inputAmount = MutableStateFlow(0L)
    val inputAmount: StateFlow<Long> = _inputAmount.asStateFlow()

    private val _inputNote = MutableStateFlow("")
    val inputNote: StateFlow<String> = _inputNote.asStateFlow()

    private val _inputTimestamp = MutableStateFlow(System.currentTimeMillis())
    val inputTimestamp: StateFlow<Long> = _inputTimestamp.asStateFlow()

    // Note suggestion filter
    val noteSuggestions: StateFlow<List<String>> = combine(_inputNote, distinctNotes) { input, notes ->
        if (input.isBlank()) {
            notes.take(6)
        } else {
            notes.filter { it.contains(input, ignoreCase = true) && !it.equals(input, ignoreCase = true) }.take(6)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending delete transaction for confirmation dialog
    private val _transactionToDelete = MutableStateFlow<TransactionEntity?>(null)
    val transactionToDelete: StateFlow<TransactionEntity?> = _transactionToDelete.asStateFlow()

    // --- History Screen Filter States ---
    private val _historyPreset = MutableStateFlow(DateRangePreset.THIS_MONTH)
    val historyPreset: StateFlow<DateRangePreset> = _historyPreset.asStateFlow()

    private val _historyStartDate = MutableStateFlow(DateUtils.getMonthStartMillis(nowCalendar.get(Calendar.YEAR), nowCalendar.get(Calendar.MONTH)))
    val historyStartDate: StateFlow<Long> = _historyStartDate.asStateFlow()

    private val _historyEndDate = MutableStateFlow(DateUtils.getMonthEndMillis(nowCalendar.get(Calendar.YEAR), nowCalendar.get(Calendar.MONTH)))
    val historyEndDate: StateFlow<Long> = _historyEndDate.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyTypeFilter = MutableStateFlow<TransactionType?>(null) // null means Semua
    val historyTypeFilter: StateFlow<TransactionType?> = _historyTypeFilter.asStateFlow()

    // Selected note for detail tooltip/dialog in Donut Chart
    private val _selectedNoteGroup = MutableStateFlow<NoteGroupSummary?>(null)
    val selectedNoteGroup: StateFlow<NoteGroupSummary?> = _selectedNoteGroup.asStateFlow()

    // Filtered History Transactions
    val filteredHistoryTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _historyStartDate,
        _historyEndDate,
        _historySearchQuery,
        _historyTypeFilter
    ) { list, start, end, query, typeFilter ->
        list.filter { tx ->
            val withinDate = tx.timestamp in start..end
            val matchesType = typeFilter == null || tx.type == typeFilter.name
            val matchesQuery = query.isBlank() ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            withinDate && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Note group summary for Pie/Donut Chart in History
    val historyNoteGroups: StateFlow<List<NoteGroupSummary>> = filteredHistoryTransactions.combine(_historyTypeFilter) { txList, _ ->
        val groups = txList.groupBy { if (it.note.isNotBlank()) it.note.trim() else "Lainnya" }
        val total = txList.sumOf { it.amount }
        groups.map { (note, items) ->
            val subTotal = items.sumOf { it.amount }
            val pct = if (total > 0) (subTotal.toFloat() / total) * 100f else 0f
            NoteGroupSummary(
                note = note,
                totalAmount = subTotal,
                count = items.size,
                percentage = pct,
                transactions = items.sortedByDescending { it.timestamp }
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chart points for filtered history range
    val historyChartPoints: StateFlow<List<DailyChartPoint>> = combine(
        filteredHistoryTransactions,
        _historyStartDate,
        _historyEndDate
    ) { txList, start, end ->
        // Aggregate by date
        val daysMap = mutableMapOf<String, Pair<Long, Long>>() // dateStr -> (inc, exp)
        val sortedList = txList.sortedBy { it.timestamp }

        sortedList.forEach { tx ->
            val dateLabel = DateUtils.formatDateShort(tx.timestamp)
            val curr = daysMap.getOrDefault(dateLabel, 0L to 0L)
            if (tx.type == TransactionType.INCOME.name) {
                daysMap[dateLabel] = (curr.first + tx.amount) to curr.second
            } else {
                daysMap[dateLabel] = curr.first to (curr.second + tx.amount)
            }
        }

        var idx = 1
        daysMap.map { (label, pair) ->
            DailyChartPoint(day = idx++, dateLabel = label, income = pair.first, expense = pair.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed initial sample data if database is empty
        viewModelScope.launch {
            val existing = repository.getAllSnapshot()
            if (existing.isEmpty()) {
                seedInitialData()
            }
        }
    }

    private suspend fun seedInitialData() {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Sample transactions in the current month
        val sampleEntries = listOf(
            Triple(8500000L, TransactionType.INCOME, "Gaji Bulanan"),
            Triple(45000L, TransactionType.EXPENSE, "Kopi & Sarapan"),
            Triple(250000L, TransactionType.EXPENSE, "Belanja Mingguan"),
            Triple(1200000L, TransactionType.INCOME, "Proyek Sampingan"),
            Triple(65000L, TransactionType.EXPENSE, "Makan Siang"),
            Triple(150000L, TransactionType.EXPENSE, "Bensin Kendaraan"),
            Triple(80000L, TransactionType.EXPENSE, "Langganan Internet"),
            Triple(120000L, TransactionType.EXPENSE, "Makan Malam Bersama"),
            Triple(35000L, TransactionType.EXPENSE, "Transportasi Online"),
            Triple(500000L, TransactionType.EXPENSE, "Tagihan Listrik")
        )

        sampleEntries.forEachIndexed { i, (amount, type, note) ->
            val offsetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, - (sampleEntries.size - 1 - i))
            }
            repository.insert(
                TransactionEntity(
                    amount = amount,
                    type = type.name,
                    note = note,
                    timestamp = offsetCal.timeInMillis
                )
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- PIN Management ---
    fun unlockWithPin(input: String): Boolean {
        if (preferences.verifyPin(input)) {
            _isAppLocked.value = false
            _pinError.value = null
            return true
        } else {
            _pinError.value = "PIN yang dimasukkan salah"
            return false
        }
    }

    fun lockApp() {
        if (preferences.isPinEnabled.value && preferences.isPinConfigured()) {
            _isAppLocked.value = true
        }
    }

    fun setPin(pin: String) {
        preferences.setPin(pin)
        _isAppLocked.value = false
    }

    fun disablePin() {
        preferences.disablePin()
        _isAppLocked.value = false
    }

    fun clearPinError() {
        _pinError.value = null
    }

    // --- Month Navigation ---
    fun previousMonth() {
        var year = _selectedYear.value
        var month = _selectedMonth.value - 1
        if (month < 0) {
            month = 11
            year -= 1
        }
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun nextMonth() {
        var year = _selectedYear.value
        var month = _selectedMonth.value + 1
        if (month > 11) {
            month = 0
            year += 1
        }
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun goToCurrentMonth() {
        val cal = Calendar.getInstance()
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    // --- Quick Add & Edit Dialog ---
    fun openAddTransactionSheet() {
        _editingTransaction.value = null
        _inputTransactionType.value = TransactionType.EXPENSE
        _inputAmount.value = 0L
        _inputNote.value = ""
        _inputTimestamp.value = System.currentTimeMillis()
        _isAddEditOpen.value = true
    }

    fun openEditTransactionSheet(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _inputTransactionType.value = if (transaction.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE
        _inputAmount.value = transaction.amount
        _inputNote.value = transaction.note
        _inputTimestamp.value = transaction.timestamp
        _isAddEditOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditOpen.value = false
        _editingTransaction.value = null
    }

    fun setInputType(type: TransactionType) {
        _inputTransactionType.value = type
    }

    fun addToNominal(delta: Long) {
        _inputAmount.value = (_inputAmount.value + delta).coerceAtLeast(0L)
    }

    fun setNominal(amount: Long) {
        _inputAmount.value = amount.coerceAtLeast(0L)
    }

    fun resetNominal() {
        _inputAmount.value = 0L
    }

    fun setInputNote(note: String) {
        _inputNote.value = note
    }

    fun setInputTimestamp(timestamp: Long) {
        _inputTimestamp.value = timestamp
    }

    fun saveTransaction() {
        val amount = _inputAmount.value
        if (amount <= 0) return

        val noteText = _inputNote.value.trim().ifEmpty {
            if (_inputTransactionType.value == TransactionType.INCOME) "Pemasukan Lainnya" else "Pengeluaran Lainnya"
        }
        val type = _inputTransactionType.value.name
        val timestamp = _inputTimestamp.value

        viewModelScope.launch {
            val currentEditing = _editingTransaction.value
            if (currentEditing != null) {
                repository.update(
                    currentEditing.copy(
                        amount = amount,
                        type = type,
                        note = noteText,
                        timestamp = timestamp
                    )
                )
            } else {
                repository.insert(
                    TransactionEntity(
                        amount = amount,
                        type = type,
                        note = noteText,
                        timestamp = timestamp
                    )
                )
            }
            closeAddEditSheet()
            triggerAutoBackup()
        }
    }

    // --- Delete confirmation ---
    fun confirmDelete(transaction: TransactionEntity) {
        _transactionToDelete.value = transaction
    }

    fun dismissDeleteDialog() {
        _transactionToDelete.value = null
    }

    fun executeDelete() {
        val tx = _transactionToDelete.value ?: return
        viewModelScope.launch {
            repository.delete(tx)
            _transactionToDelete.value = null
            triggerAutoBackup()
        }
    }

    // --- History Screen Actions ---
    fun setHistoryPreset(preset: DateRangePreset) {
        _historyPreset.value = preset
        when (preset) {
            DateRangePreset.TODAY -> {
                _historyStartDate.value = DateUtils.getTodayStart()
                _historyEndDate.value = DateUtils.getTodayEnd()
            }
            DateRangePreset.LAST_7_DAYS -> {
                _historyStartDate.value = DateUtils.getLast7DaysStart()
                _historyEndDate.value = DateUtils.getTodayEnd()
            }
            DateRangePreset.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                _historyStartDate.value = DateUtils.getMonthStartMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                _historyEndDate.value = DateUtils.getMonthEndMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            }
            DateRangePreset.LAST_30_DAYS -> {
                _historyStartDate.value = DateUtils.getLast30DaysStart()
                _historyEndDate.value = DateUtils.getTodayEnd()
            }
            DateRangePreset.THIS_YEAR -> {
                _historyStartDate.value = DateUtils.getThisYearStart()
                _historyEndDate.value = DateUtils.getTodayEnd()
            }
            DateRangePreset.ALL -> {
                _historyStartDate.value = 0L
                _historyEndDate.value = Long.MAX_VALUE
            }
        }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _historyPreset.value = DateRangePreset.ALL
        _historyStartDate.value = DateUtils.getStartOfDay(start)
        _historyEndDate.value = DateUtils.getEndOfDay(end)
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryTypeFilter(type: TransactionType?) {
        _historyTypeFilter.value = type
    }

    fun selectNoteGroup(summary: NoteGroupSummary?) {
        _selectedNoteGroup.value = summary
    }

    // Balance hidden state
    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    fun toggleBalanceVisibility() {
        _isBalanceHidden.value = !_isBalanceHidden.value
    }

    // --- Settings & Export / Import ---
    fun setThemeMode(mode: String) {
        preferences.setThemeMode(mode)
    }

    fun exportTransactions(format: ExportHelper.ExportFormat): File? {
        val list = filteredHistoryTransactions.value.ifEmpty { allTransactions.value }
        return ExportHelper.exportAndShare(getApplication(), format, list)
    }

    suspend fun importData(uri: Uri): ImportHelper.ImportResult {
        val result = ImportHelper.importFromUri(getApplication(), uri, repository)
        if (result.success) {
            triggerAutoBackup()
        }
        return result
    }

    // --- Google Account & Google Drive Auto Backup & Restore ---
    fun getGoogleSignInIntent(): android.content.Intent {
        return driveManager.getSignInIntent()
    }

    fun hasDrivePermission(): Boolean {
        return driveManager.hasDrivePermission()
    }

    fun loginGoogle(name: String, email: String, photo: String? = null) {
        driveManager.setGoogleAccount(name, email, photo)
    }

    fun logoutGoogle() {
        driveManager.signOut()
    }

    fun setAutoBackup(enabled: Boolean) {
        driveManager.setAutoBackup(enabled)
        if (enabled) {
            triggerAutoBackup()
        }
    }

    suspend fun backupToGoogleDrive(): GoogleDriveBackupManager.DriveSyncResult {
        val list = repository.getAllSnapshot()
        return driveManager.backupToGoogleDrive(list)
    }

    suspend fun restoreFromGoogleDrive(): GoogleDriveBackupManager.DriveSyncResult {
        return driveManager.restoreFromGoogleDrive(repository)
    }

    fun triggerAutoBackup() {
        if (driveManager.isAutoBackupEnabled.value && driveManager.isLoggedIn.value) {
            viewModelScope.launch {
                val list = repository.getAllSnapshot()
                driveManager.backupToGoogleDrive(list)
            }
        }
    }
}
