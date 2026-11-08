package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, accountId: String, accountName: String, categoryId: String, categoryName: String, date: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(categories.filter { it.type == "INCOME" }.firstOrNull()?.id ?: "") }
    var dateStr by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var description by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val incomeCategories = categories.filter { it.type == "INCOME" }
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    val selectedCategory = incomeCategories.find { it.id == selectedCategoryId } ?: incomeCategories.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন আয় যোগ করুন", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("আয়ের শিরোনাম / বিবরণ") },
                    placeholder = { Text("যেমন: বেতন, ফ্রিল্যান্সিং ইত্যাদি") },
                    modifier = Modifier.fillMaxWidth().testTag("income_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    placeholder = { Text("০.০০") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("income_amount_input"),
                    singleLine = true,
                    leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = IncomeGreen) }
                )

                // Destination Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "একাউন্ট নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("জমা একাউন্ট") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (বর্তমান: ${FormatUtils.formatBdt(acc.currentBalance)})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { it.nameBn.ifBlank { it.name } } ?: "ক্যাটাগরি নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("আয়ের ক্যাটাগরি") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        incomeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameBn.ifBlank { cat.name }) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0 && selectedAccount != null) {
                        onConfirm(
                            title,
                            amt,
                            selectedAccount.id,
                            selectedAccount.name,
                            selectedCategory?.id ?: "",
                            selectedCategory?.nameBn?.ifBlank { selectedCategory.name } ?: "আয়",
                            dateStr,
                            description
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                modifier = Modifier.testTag("confirm_add_income")
            ) {
                Text("আয় সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, accountId: String, accountName: String, categoryId: String, categoryName: String, date: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(categories.filter { it.type == "EXPENSE" }.firstOrNull()?.id ?: "") }
    var dateStr by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var description by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    val selectedCategory = expenseCategories.find { it.id == selectedCategoryId } ?: expenseCategories.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন ব্যয় যোগ করুন", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ব্যয়ের শিরোনাম") },
                    placeholder = { Text("যেমন: খাবার খরচ, বাড়ি ভাড়া ইত্যাদি") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    placeholder = { Text("০.০০") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                    singleLine = true,
                    leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = ExpenseRed) }
                )

                // Payment Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.let { "${it.name} (ব্যালেন্স: ${FormatUtils.formatBdt(it.currentBalance)})" } ?: "একাউন্ট নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("পরিশোধ একাউন্ট") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${FormatUtils.formatBdt(acc.currentBalance)})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { it.nameBn.ifBlank { it.name } } ?: "ক্যাটাগরি নির্বাচন করুন",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ব্যয়ের ক্যাটাগরি") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameBn.ifBlank { cat.name }) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0 && selectedAccount != null) {
                        onConfirm(
                            title,
                            amt,
                            selectedAccount.id,
                            selectedAccount.name,
                            selectedCategory?.id ?: "",
                            selectedCategory?.nameBn?.ifBlank { selectedCategory.name } ?: "ব্যয়",
                            dateStr,
                            description
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier.testTag("confirm_add_expense")
            ) {
                Text("ব্যয় সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMoneyDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (srcId: String, srcName: String, dstId: String, dstName: String, amount: Double, date: String, desc: String) -> Unit
) {
    var sourceId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var destId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var description by remember { mutableStateOf("") }

    var srcExpanded by remember { mutableStateOf(false) }
    var dstExpanded by remember { mutableStateOf(false) }

    val srcAccount = accounts.find { it.id == sourceId } ?: accounts.firstOrNull()
    val dstAccount = accounts.find { it.id == destId } ?: accounts.getOrNull(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TransferBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("টাকা ট্রান্সফার", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "নোট: ট্রান্সফার আয় বা ব্যয় হিসেবে গণ্য হয় না এবং সামগ্রিক নিট সম্পদে কোনো পরিবর্তন আনে না।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Source Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = srcExpanded,
                    onExpandedChange = { srcExpanded = !srcExpanded }
                ) {
                    OutlinedTextField(
                        value = srcAccount?.let { "${it.name} (ব্যালেন্স: ${FormatUtils.formatBdt(it.currentBalance)})" } ?: "উৎস একাউন্ট",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কোন একাউন্ট থেকে কর্তন হবে") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = srcExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = srcExpanded,
                        onDismissRequest = { srcExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${FormatUtils.formatBdt(acc.currentBalance)})") },
                                onClick = {
                                    sourceId = acc.id
                                    srcExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = dstExpanded,
                    onExpandedChange = { dstExpanded = !dstExpanded }
                ) {
                    OutlinedTextField(
                        value = dstAccount?.let { "${it.name} (ব্যালেন্স: ${FormatUtils.formatBdt(it.currentBalance)})" } ?: "গন্তব্য একাউন্ট",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কোন একাউন্টে জমা হবে") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dstExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dstExpanded,
                        onDismissRequest = { dstExpanded = false }
                    ) {
                        accounts.filter { it.id != sourceId }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${FormatUtils.formatBdt(acc.currentBalance)})") },
                                onClick = {
                                    destId = acc.id
                                    dstExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("ট্রান্সফার পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                    singleLine = true,
                    leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = TransferBlue) }
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("তারিখ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("রেফারেন্স / বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && srcAccount != null && dstAccount != null && srcAccount.id != dstAccount.id) {
                        onConfirm(
                            srcAccount.id,
                            srcAccount.name,
                            dstAccount.id,
                            dstAccount.name,
                            amt,
                            dateStr,
                            description
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TransferBlue),
                modifier = Modifier.testTag("confirm_transfer")
            ) {
                Text("ট্রান্সফার সম্পন্ন করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, provider: String, openingBal: Double, color: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("BANK") }
    var provider by remember { mutableStateOf("") }
    var openingBalStr by remember { mutableStateOf("0") }
    var selectedColor by remember { mutableStateOf("#1E3A8A") }
    var typeExpanded by remember { mutableStateOf(false) }

    val accountTypes = listOf(
        Pair("BANK", "ব্যাংক একাউন্ট"),
        Pair("MOBILE_WALLET", "মোবাইল ওয়ালেট (বিকাশ/নগদ)"),
        Pair("CASH", "ক্যাশ / নগদ টাকা"),
        Pair("SAVINGS", "সঞ্চয় একাউন্ট"),
        Pair("OTHER", "অন্যান্য")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন একাউন্ট যোগ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("একাউন্টের নাম") },
                    placeholder = { Text("যেমন: ব্র্যাক ব্যাংক, পার্সোনাল বিকাশ ইত্যাদি") },
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = accountTypes.find { it.first == selectedType }?.second ?: selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("একাউন্টের ধরন") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        accountTypes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    selectedType = item.first
                                    typeExpanded = false
                                    selectedColor = when (item.first) {
                                        "BANK" -> "#1E3A8A"
                                        "MOBILE_WALLET" -> "#E2136E"
                                        "CASH" -> "#059669"
                                        else -> "#0F766E"
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("ব্যাংক বা প্রোভাইডারের নাম") },
                    placeholder = { Text("যেমন: BRAC Bank, bKash, City Bank") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = openingBalStr,
                    onValueChange = { openingBalStr = it },
                    label = { Text("প্রারম্ভিক ব্যালেন্স (Opening Balance ৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("account_opening_balance_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = openingBalStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        val icon = when (selectedType) {
                            "BANK" -> "bank"
                            "MOBILE_WALLET" -> "wallet"
                            "CASH" -> "cash"
                            else -> "account_balance"
                        }
                        onConfirm(name, selectedType, provider.ifBlank { name }, bal, selectedColor, icon)
                    }
                },
                modifier = Modifier.testTag("confirm_add_account")
            ) {
                Text("একাউন্ট তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, currency: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BDT") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন ওয়ার্কস্পেস তৈরি করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ওয়ার্কস্পেসের নাম") },
                    placeholder = { Text("যেমন: পারিবারিক ফাইনান্স, ছোট ব্যবসা ইত্যাদি") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("মুদ্রা (Currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("সংক্ষিপ্ত বিবরণ (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onConfirm(name, currency, desc)
            }) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onReverse: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (transaction.type) {
                        "INCOME" -> Icons.Default.ArrowDownward
                        "EXPENSE" -> Icons.Default.ArrowUpward
                        else -> Icons.Default.SwapHoriz
                    },
                    contentDescription = null,
                    tint = when (transaction.type) {
                        "INCOME" -> IncomeGreen
                        "EXPENSE" -> ExpenseRed
                        else -> TransferBlue
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(transaction.title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("পরিমাণ: ${FormatUtils.formatBdt(transaction.amount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("ধরন: ${when(transaction.type) { "INCOME" -> "আয়"; "EXPENSE" -> "ব্যয়"; "TRANSFER" -> "ট্রান্সফার"; else -> transaction.type }}")
                Text("একাউন্ট: ${transaction.accountName}")
                if (transaction.toAccountName.isNotBlank()) {
                    Text("গন্তব্য একাউন্ট: ${transaction.toAccountName}")
                }
                if (transaction.categoryName.isNotBlank()) {
                    Text("ক্যাটাগরি: ${transaction.categoryName}")
                }
                Text("তারিখ: ${FormatUtils.formatDateDisplay(transaction.date)}")
                if (transaction.creatorName.isNotBlank()) {
                    Text("তৈরি করেছেন: ${transaction.creatorName}")
                }
                if (transaction.description.isNotBlank()) {
                    Text("বিবরণ: ${transaction.description}")
                }
                if (transaction.isReversed) {
                    Text("অবস্থা: এই লেনদেনটি রিভার্স (বাতিল) করা হয়েছে", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            if (!transaction.isReversed) {
                Button(
                    onClick = onReverse,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("লেনদেন রিভার্স করুন (Reversal)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}
