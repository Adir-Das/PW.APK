package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilter(val labelBn: String, val labelEn: String) {
    THIS_MONTH("এই মাস", "This Month"),
    TODAY("আজ", "Today"),
    THIS_WEEK("এই সপ্তাহ", "This Week"),
    LAST_MONTH("বিগত মাস", "Last Month"),
    THIS_YEAR("এই বছর", "This Year"),
    ALL("সব সময়", "All Time")
}

class WalletViewModel(
    application: Application,
    private val repository: WalletRepository = WalletRepository(application)
) : AndroidViewModel(application) {

    val currentUserId = repository.currentUserId()

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _currentWorkspaceId = MutableStateFlow<String?>(null)
    val currentWorkspaceId: StateFlow<String?> = _currentWorkspaceId.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoal>> = _savingsGoals.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    private val _recurring = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val recurring: StateFlow<List<RecurringTransaction>> = _recurring.asStateFlow()

    private val _members = MutableStateFlow<List<WorkspaceMember>>(emptyList())
    val members: StateFlow<List<WorkspaceMember>> = _members.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    // Filters and UI states
    val dateFilter = MutableStateFlow(DateFilter.THIS_MONTH)
    val transactionTypeFilter = MutableStateFlow("ALL")
    val searchQuery = MutableStateFlow("")
    val selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Transactions, 2: Accounts, 3: Budget/Savings, 4: Audit/Team

    val errorMessage = MutableStateFlow<String?>(null)
    val successMessage = MutableStateFlow<String?>(null)
    val isLoading = MutableStateFlow(false)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        if (currentUserId == null) return

        viewModelScope.launch {
            repository.observeWorkspaces().collect { wsList ->
                _workspaces.value = wsList
                if (wsList.isNotEmpty()) {
                    if (_currentWorkspaceId.value == null || wsList.none { it.id == _currentWorkspaceId.value }) {
                        selectWorkspace(wsList.first().id)
                    }
                } else {
                    // Create default personal workspace automatically if none exists
                    createWorkspace("ব্যক্তিগত তহবিল (Personal)", "BDT", "ডিফল্ট ব্যক্তিগত ফাইনান্স স্পেস")
                }
            }
        }
    }

    fun selectWorkspace(workspaceId: String) {
        _currentWorkspaceId.value = workspaceId

        // Observe accounts
        viewModelScope.launch {
            repository.observeAccounts(workspaceId).collect {
                _accounts.value = it
            }
        }

        // Observe transactions
        viewModelScope.launch {
            repository.observeTransactions(workspaceId).collect {
                _transactions.value = it
            }
        }

        // Observe categories
        viewModelScope.launch {
            repository.observeCategories(workspaceId).collect {
                _categories.value = it
            }
        }

        // Observe budgets
        viewModelScope.launch {
            repository.observeBudgets(workspaceId).collect {
                _budgets.value = it
            }
        }

        // Observe savings
        viewModelScope.launch {
            repository.observeSavingsGoals(workspaceId).collect {
                _savingsGoals.value = it
            }
        }

        // Observe debts
        viewModelScope.launch {
            repository.observeDebts(workspaceId).collect {
                _debts.value = it
            }
        }

        // Observe recurring
        viewModelScope.launch {
            repository.observeRecurring(workspaceId).collect {
                _recurring.value = it
            }
        }

        // Observe members
        viewModelScope.launch {
            repository.observeMembers(workspaceId).collect {
                _members.value = it
            }
        }

        // Observe audit logs
        viewModelScope.launch {
            repository.observeAuditLogs(workspaceId).collect {
                _auditLogs.value = it
            }
        }
    }

    fun createWorkspace(name: String, currency: String = "BDT", description: String = "") {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val newId = repository.createWorkspace(name, currency, description)
                selectWorkspace(newId)
                successMessage.value = "ওয়ার্কস্পেস সফলভাবে তৈরি হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "ওয়ার্কস্পেস তৈরি করা সম্ভব হয়নি: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // --- Filtered Transactions ---
    val filteredTransactions = combine(
        _transactions,
        dateFilter,
        transactionTypeFilter,
        searchQuery
    ) { txs, dateF, typeF, query ->
        txs.filter { tx ->
            val matchesType = when (typeF) {
                "INCOME" -> tx.type == TransactionType.INCOME.name
                "EXPENSE" -> tx.type == TransactionType.EXPENSE.name
                "TRANSFER" -> tx.type == TransactionType.TRANSFER.name
                else -> true
            }

            val matchesSearch = if (query.isBlank()) true else {
                tx.title.contains(query, ignoreCase = true) ||
                tx.categoryName.contains(query, ignoreCase = true) ||
                tx.accountName.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)
            }

            val matchesDate = isDateMatchingFilter(tx.date, dateF)

            matchesType && matchesSearch && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Computed Financial Summaries ---
    val totalBalance: StateFlow<Double> = _accounts.map { accs ->
        accs.filter { it.status == "ACTIVE" }.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalIncome: StateFlow<Double> = filteredTransactions.map { txs ->
        txs.filter { it.type == TransactionType.INCOME.name && !it.isReversed }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions.map { txs ->
        txs.filter { it.type == TransactionType.EXPENSE.name && !it.isReversed }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalSavings: StateFlow<Double> = _savingsGoals.map { goals ->
        goals.sumOf { it.savedAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalReceivable: StateFlow<Double> = _debts.map { debtList ->
        debtList.filter { it.type == "RECEIVABLE" && it.status != "COMPLETED" }.sumOf { it.amount - it.paidAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalPayable: StateFlow<Double> = _debts.map { debtList ->
        debtList.filter { it.type == "PAYABLE" && it.status != "COMPLETED" }.sumOf { it.amount - it.paidAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Category Expense Breakdown
    val categoryExpenseBreakdown = filteredTransactions.map { txs ->
        val expenses = txs.filter { it.type == TransactionType.EXPENSE.name && !it.isReversed }
        val total = expenses.sumOf { it.amount }
        expenses.groupBy { it.categoryName.ifBlank { "অন্যান্য" } }
            .mapValues { entry ->
                val sum = entry.value.sumOf { it.amount }
                val pct = if (total > 0) (sum / total * 100).toInt() else 0
                Pair(sum, pct)
            }.toList().sortedByDescending { it.second.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Actions ---

    fun addIncome(
        title: String,
        amount: Double,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        date: String,
        description: String
    ) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.addIncome(wsId, title, amount, accountId, accountName, categoryId, categoryName, date, description)
                successMessage.value = "আয় সফলভাবে যোগ করা হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "আয় যোগ করতে সমস্যা: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        date: String,
        description: String
    ) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                // Check balance
                val acc = _accounts.value.find { it.id == accountId }
                if (acc != null && acc.currentBalance < amount) {
                    errorMessage.value = "সতর্কতা: নির্বাচিত একাউন্টে পর্যাপ্ত ব্যালেন্স নেই (বর্তমান: ৳${acc.currentBalance})"
                    return@launch
                }
                repository.addExpense(wsId, title, amount, accountId, accountName, categoryId, categoryName, date, description)
                successMessage.value = "ব্যয় সফলভাবে যোগ করা হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "ব্যয় যোগ করতে সমস্যা: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun transferMoney(
        sourceAccountId: String,
        sourceAccountName: String,
        destAccountId: String,
        destAccountName: String,
        amount: Double,
        date: String,
        description: String
    ) {
        val wsId = _currentWorkspaceId.value ?: return
        if (sourceAccountId == destAccountId) {
            errorMessage.value = "উৎস এবং গন্তব্য একাউন্ট একই হতে পারে না!"
            return
        }
        val src = _accounts.value.find { it.id == sourceAccountId }
        if (src != null && src.currentBalance < amount) {
            errorMessage.value = "সতর্কতা: উৎস একাউন্টে পর্যাপ্ত ব্যালেন্স নেই (বর্তমান: ৳${src.currentBalance})"
            return
        }

        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.transferMoney(wsId, sourceAccountId, sourceAccountName, destAccountId, destAccountName, amount, date, description)
                successMessage.value = "টাকা ট্রান্সফার সফল হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "ট্রান্সফার ব্যর্থ হয়েছে: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun reverseTransaction(transaction: Transaction) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.reverseTransaction(wsId, transaction)
                successMessage.value = "লেনদেনটি সফলভাবে রিভার্স (বাতিল) করা হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "রিভার্স ব্যর্থ হয়েছে: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addAccount(name: String, type: String, provider: String, openingBal: Double, color: String, icon: String) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                val newAcc = Account(
                    workspaceId = wsId,
                    name = name,
                    type = type,
                    providerName = provider,
                    accountNumberMasked = if (type == "BANK") "**** " + (1000..9999).random() else "01" + (10000000..99999999).random(),
                    openingBalance = openingBal,
                    currentBalance = openingBal,
                    color = color,
                    icon = icon,
                    status = "ACTIVE"
                )
                repository.createAccount(wsId, newAcc)
                successMessage.value = "একাউন্ট সফলভাবে তৈরি হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "একাউন্ট তৈরি করতে সমস্যা: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addCategory(name: String, nameBn: String, type: String, color: String, icon: String) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.createCategory(wsId, Category(workspaceId = wsId, name = name, nameBn = nameBn, type = type, color = color, icon = icon))
                successMessage.value = "ক্যাটাগরি সফলভাবে যোগ হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "ক্যাটাগরি তৈরি ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addBudget(name: String, amount: Double, categoryId: String, categoryName: String, period: String) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.createBudget(wsId, Budget(workspaceId = wsId, name = name, amount = amount, categoryId = categoryId, categoryName = categoryName, period = period))
                successMessage.value = "বাজেট সফলভাবে যোগ হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "বাজেট যোগ করতে সমস্যা: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addSavingsGoal(title: String, targetAmount: Double, targetDate: String) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.createSavingsGoal(wsId, SavingsGoal(workspaceId = wsId, title = title, targetAmount = targetAmount, targetDate = targetDate))
                successMessage.value = "সঞ্চয় লক্ষ্য সফলভাবে নির্ধারিত হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "সঞ্চয় লক্ষ্য ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addSavingsContribution(goalId: String, amount: Double) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.addSavingsContribution(wsId, goalId, amount)
                successMessage.value = "সঞ্চয়ে ৳$amount যোগ করা হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "সঞ্চয় জমা ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addDebt(personName: String, type: String, amount: Double, dueDate: String, description: String) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.createDebt(wsId, Debt(workspaceId = wsId, personName = personName, type = type, amount = amount, dueDate = dueDate, description = description))
                val label = if (type == "RECEIVABLE") "পাওনা" else "দেনা"
                successMessage.value = "$label সফলভাবে যুক্ত হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "যুক্ত করতে ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun recordDebtRepayment(debtId: String, repaymentAmount: Double) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.recordDebtRepayment(wsId, debtId, repaymentAmount)
                successMessage.value = "পরিশোধ ৳$repaymentAmount রেকর্ড হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "পরিশোধ রেকর্ড ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addMember(email: String, displayName: String, role: MemberRole, dailyLimit: Double, monthlyLimit: Double, maxTxLimit: Double) {
        val wsId = _currentWorkspaceId.value ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repository.addMember(wsId, email, displayName, role, dailyLimit, monthlyLimit, maxTxLimit)
                successMessage.value = "সদস্য সফলভাবে আমন্ত্রণ জানানো হয়েছে!"
            } catch (e: Exception) {
                errorMessage.value = "সদস্য যোগ করতে ব্যর্থ: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    private fun isDateMatchingFilter(txDateStr: String, filter: DateFilter): Boolean {
        if (filter == DateFilter.ALL) return true
        if (txDateStr.isBlank()) return true

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val txDate = sdf.parse(txDateStr) ?: return true
            val calTx = Calendar.getInstance().apply { time = txDate }
            val calNow = Calendar.getInstance()

            when (filter) {
                DateFilter.TODAY -> {
                    calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calTx.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                }
                DateFilter.THIS_WEEK -> {
                    calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calTx.get(Calendar.WEEK_OF_YEAR) == calNow.get(Calendar.WEEK_OF_YEAR)
                }
                DateFilter.THIS_MONTH -> {
                    calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calTx.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
                }
                DateFilter.LAST_MONTH -> {
                    val lastMonth = if (calNow.get(Calendar.MONTH) == 0) 11 else calNow.get(Calendar.MONTH) - 1
                    val year = if (calNow.get(Calendar.MONTH) == 0) calNow.get(Calendar.YEAR) - 1 else calNow.get(Calendar.YEAR)
                    calTx.get(Calendar.YEAR) == year && calTx.get(Calendar.MONTH) == lastMonth
                }
                DateFilter.THIS_YEAR -> {
                    calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
                }
                DateFilter.ALL -> true
            }
        } catch (e: Exception) {
            true
        }
    }
}
