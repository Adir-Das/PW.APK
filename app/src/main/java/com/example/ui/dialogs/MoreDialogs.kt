package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.MemberRole
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, categoryId: String, categoryName: String, period: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("MONTHLY") }
    var catExpanded by remember { mutableStateOf(false) }

    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val selectedCat = expenseCategories.find { it.id == selectedCategoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন বাজেট নির্ধারণ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("বাজেটের নাম") },
                    placeholder = { Text("যেমন: খাবার খরচ বাজেট, মাসিক সামগ্রিক বাজেট") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("বাজেট পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCat?.let { it.nameBn.ifBlank { it.name } } ?: "সামগ্রিক বাজেট (সব ক্যাটাগরি)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("নির্দিষ্ট ক্যাটাগরি (ঐচ্ছিক)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সামগ্রিক বাজেট (সব ক্যাটাগরি)") },
                            onClick = {
                                selectedCategoryId = ""
                                catExpanded = false
                            }
                        )
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameBn.ifBlank { cat.name }) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt > 0) {
                    onConfirm(name, amt, selectedCategoryId, selectedCat?.nameBn?.ifBlank { selectedCat.name } ?: "", selectedPeriod)
                }
            }) {
                Text("বাজেট সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, targetDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2026-12-31") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন সঞ্চয় লক্ষ্য", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("সঞ্চয়ের উদ্দেশ্য / নাম") },
                    placeholder = { Text("যেমন: জরুরি তহবিল, ল্যাপটপ ক্রয়, ভ্রমণ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("লক্ষ্যমাত্রা পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("অর্জনের সম্ভাব্য তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = targetStr.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && amt > 0) {
                    onConfirm(title, amt, targetDate)
                }
            }) {
                Text("লক্ষ্য সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun AddContributionDialog(
    goalTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$goalTitle - এ টাকা জমা দিন", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("জমার পরিমাণ (৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) onConfirm(amt)
            }) {
                Text("জমা করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (person: String, type: String, amount: Double, dueDate: String, desc: String) -> Unit
) {
    var person by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("RECEIVABLE") } // RECEIVABLE or PAYABLE
    var amountStr by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন দেনা-পাওনা রেকর্ড", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "RECEIVABLE",
                        onClick = { type = "RECEIVABLE" },
                        label = { Text("আমি পাবো (পাওনা)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYABLE",
                        onClick = { type = "PAYABLE" },
                        label = { Text("আমাকে দিতে হবে (দেনা)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("ব্যক্তি বা প্রতিষ্ঠানের নাম") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("পরিশোধের শেষ তারিখ (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (person.isNotBlank() && amt > 0) {
                    onConfirm(person, type, amt, dueDate, desc)
                }
            }) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun RecordRepaymentDialog(
    personName: String,
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$personName - এর পরিশোধ রেকর্ড", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("অবশিষ্ট পরিমাণ: ${FormatUtils.formatBdt(remainingAmount)}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("পরিশোধের পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) onConfirm(amt)
            }) {
                Text("পরিশোধ সম্পন্ন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String, name: String, role: MemberRole, dailyLimit: Double, monthlyLimit: Double, maxTxLimit: Double) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(MemberRole.VIEWER) }
    var dailyLimitStr by remember { mutableStateOf("0") }
    var monthlyLimitStr by remember { mutableStateOf("0") }
    var maxTxLimitStr by remember { mutableStateOf("0") }
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন সদস্য যুক্ত করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল অ্যাড্রেস") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("সদস্যের নাম") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = when(role) {
                            MemberRole.OWNER -> "মালিক (Owner)"
                            MemberRole.EDITOR -> "এডিটর (Editor - লেনদেন যোগ ও সম্পাদনা)"
                            MemberRole.CONTRIBUTOR -> "কন্ট্রিবিউটর (Contributor - কেবল যোগ)"
                            MemberRole.VIEWER -> "ভিউয়ার (Viewer - কেবল প্রদর্শন)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ভূমিকা ও পারমিশন (Role)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        MemberRole.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.name} - ${when(r) { MemberRole.OWNER -> "সব অধিকার"; MemberRole.EDITOR -> "যোগ ও পরিবর্তন"; MemberRole.CONTRIBUTOR -> "শুধুমাত্র যোগ"; MemberRole.VIEWER -> "শুধুমাত্র দেখা" }}") },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("খরচের সীমা (Spending Limits ৳ - ০ হলে সীমাহীন):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = dailyLimitStr,
                    onValueChange = { dailyLimitStr = it },
                    label = { Text("দৈনিক সীমা (Daily Limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyLimitStr,
                    onValueChange = { monthlyLimitStr = it },
                    label = { Text("মাসিক সীমা (Monthly Limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isNotBlank()) {
                    onConfirm(
                        email,
                        name.ifBlank { email },
                        role,
                        dailyLimitStr.toDoubleOrNull() ?: 0.0,
                        monthlyLimitStr.toDoubleOrNull() ?: 0.0,
                        maxTxLimitStr.toDoubleOrNull() ?: 0.0
                    )
                }
            }) {
                Text("আমন্ত্রণ জানান")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
