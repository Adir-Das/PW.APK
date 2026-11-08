package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.WalletViewModel
import com.example.ui.dialogs.*
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.util.FormatUtils

@Composable
fun BudgetsSavingsTab(
    viewModel: WalletViewModel
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Budgets, 1: Savings, 2: Debts, 3: Recurring

    val budgets by viewModel.budgets.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val recurring by viewModel.recurring.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }

    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var selectedDebtForRepayment by remember { mutableStateOf<Debt?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("বাজেট") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("সঞ্চয় লক্ষ্য") })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("দেনা-পাওনা") })
            Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, text = { Text("পুনরাবৃত্ত") })
        }

        when (selectedSubTab) {
            0 -> BudgetsView(
                budgets = budgets,
                onAddClick = { showAddBudgetDialog = true }
            )
            1 -> SavingsView(
                goals = savingsGoals,
                onAddClick = { showAddSavingsDialog = true },
                onDepositClick = { selectedGoalForDeposit = it }
            )
            2 -> DebtsView(
                debts = debts,
                onAddClick = { showAddDebtDialog = true },
                onRepayClick = { selectedDebtForRepayment = it }
            )
            3 -> RecurringView(recurring = recurring)
        }
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = categories,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { name, amount, catId, catName, period ->
                viewModel.addBudget(name, amount, catId, catName, period)
                showAddBudgetDialog = false
            }
        )
    }

    if (showAddSavingsDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddSavingsDialog = false },
            onConfirm = { title, targetAmount, targetDate ->
                viewModel.addSavingsGoal(title, targetAmount, targetDate)
                showAddSavingsDialog = false
            }
        )
    }

    if (selectedGoalForDeposit != null) {
        AddContributionDialog(
            goalTitle = selectedGoalForDeposit?.title ?: "",
            onDismiss = { selectedGoalForDeposit = null },
            onConfirm = { amount ->
                selectedGoalForDeposit?.let { viewModel.addSavingsContribution(it.id, amount) }
                selectedGoalForDeposit = null
            }
        )
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { person, type, amount, dueDate, desc ->
                viewModel.addDebt(person, type, amount, dueDate, desc)
                showAddDebtDialog = false
            }
        )
    }

    if (selectedDebtForRepayment != null) {
        val debt = selectedDebtForRepayment!!
        RecordRepaymentDialog(
            personName = debt.personName,
            remainingAmount = debt.amount - debt.paidAmount,
            onDismiss = { selectedDebtForRepayment = null },
            onConfirm = { amount ->
                viewModel.recordDebtRepayment(debt.id, amount)
                selectedDebtForRepayment = null
            }
        )
    }
}

@Composable
private fun BudgetsView(
    budgets: List<Budget>,
    onAddClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "বাজেট নিয়ন্ত্রণ ও পর্যবেক্ষণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("বাজেট যোগ")
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("এখনো কোনো বাজেট নির্ধারণ করা হয়নি", style = MaterialTheme.typography.bodyLarge)
                        Text("মাসিক খরচ নিয়ন্ত্রণে রাখতে উপরের বাটনে ট্যাপ করে বাজেট যোগ করুন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(budgets) { budget ->
                BudgetCard(budget = budget)
            }
        }
    }
}

@Composable
fun BudgetCard(budget: Budget) {
    val progress = if (budget.amount > 0) (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1.5f) else 0f
    val pct = (progress * 100).toInt()
    val isOver = budget.spentAmount > budget.amount
    val progressColor = if (isOver) ExpenseRed else if (progress > 0.8f) AmberTertiary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = budget.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (budget.categoryName.isNotBlank()) {
                        Text(text = "ক্যাটাগরি: ${budget.categoryName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = "$pct% ব্যবহৃত",
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ব্যয়: ${FormatUtils.formatBdt(budget.spentAmount)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "বাজেট: ${FormatUtils.formatBdt(budget.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            if (isOver) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সতর্কতা: বাজেট অতিক্রম করেছে!", color = ExpenseRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SavingsView(
    goals: List<SavingsGoal>,
    onAddClick: () -> Unit,
    onDepositClick: (SavingsGoal) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "সঞ্চয় লক্ষ্যমাত্রা ও অগ্রগতি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("লক্ষ্য যোগ")
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("কোনো সঞ্চয় লক্ষ্য নির্ধারিত নেই", style = MaterialTheme.typography.bodyLarge)
                        Text("ভবিষ্যতের জন্য সঞ্চয়ের লক্ষ্যমাত্রা ঠিক করতে উপরে ট্যাপ করুন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(goals) { goal ->
                SavingsGoalCard(goal = goal, onDepositClick = { onDepositClick(goal) })
            }
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    onDepositClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).toInt()
    val isComplete = goal.savedAmount >= goal.targetAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (goal.targetDate.isNotBlank()) {
                        Text(text = "লক্ষ্য তারিখ: ${FormatUtils.formatDateDisplay(goal.targetDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = if (isComplete) "সম্পূর্ণ! ($pct%)" else "$pct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isComplete) IncomeGreen else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) IncomeGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "জমা: ${FormatUtils.formatBdt(goal.savedAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    Text(text = "লক্ষ্য: ${FormatUtils.formatBdt(goal.targetAmount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onDepositClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Text("+ টাকা জমা")
                }
            }
        }
    }
}

@Composable
private fun DebtsView(
    debts: List<Debt>,
    onAddClick: () -> Unit,
    onRepayClick: (Debt) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "দেনা-পাওনা ট্র্যাকার (Loans & Debts)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("যুক্ত করুন")
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("কোনো দেনা বা পাওনা রেকর্ড নেই", style = MaterialTheme.typography.bodyLarge)
                        Text("কাউকে টাকা ধার দিলে বা ধার নিলে এখানে লিখে রাখুন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(debts) { debt ->
                val isReceivable = debt.type == "RECEIVABLE"
                val remaining = debt.amount - debt.paidAmount
                val isComplete = debt.status == "COMPLETED" || remaining <= 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = debt.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isReceivable) "পাওনা (আমি পাবো)" else "দেনা (আমাকে দিতে হবে)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isReceivable) TransferBlue else AmberTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (isComplete) "পরিশোধিত" else "বাকি: ${FormatUtils.formatBdt(remaining)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) IncomeGreen else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "মোট: ${FormatUtils.formatBdt(debt.amount)} | পরিশোধিত: ${FormatUtils.formatBdt(debt.paidAmount)}", style = MaterialTheme.typography.labelSmall)
                            if (!isComplete) {
                                TextButton(onClick = { onRepayClick(debt) }) {
                                    Text("পরিশোধ রেকর্ড")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringView(
    recurring: List<RecurringTransaction>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "পুনরাবৃত্ত মাসিক/সাপ্তাহিক বিল ও খরচ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (recurring.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("পুনরাবৃত্ত কোনো সূচি নেই", style = MaterialTheme.typography.bodyLarge)
                        Text("যেমন: মাসিক ইন্টারনেট বিল, বাড়ি ভাড়া, বা মাসিক সাবস্ক্রিপশন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recurring) { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "পুনরাবৃত্তি: ${item.frequency} | পরবর্তী তারিখ: ${item.nextExecutionDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(text = FormatUtils.formatBdt(item.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
