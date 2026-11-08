package com.example.ui.tabs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLog
import com.example.data.model.MemberRole
import com.example.data.model.WorkspaceMember
import com.example.ui.WalletViewModel
import com.example.ui.dialogs.InviteMemberDialog
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditTeamTab(
    viewModel: WalletViewModel
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Audit Logs, 1: Members, 2: Reports

    val auditLogs by viewModel.auditLogs.collectAsState()
    val members by viewModel.members.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val totalBal by viewModel.totalBalance.collectAsState()
    val totalInc by viewModel.totalIncome.collectAsState()
    val totalExp by viewModel.totalExpense.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("অডিট লগ") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("টিম ও সদস্য") })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("রিপোর্ট ও এক্সপোর্ট") })
        }

        when (selectedSubTab) {
            0 -> AuditLogsView(auditLogs = auditLogs)
            1 -> MembersView(
                members = members,
                onInviteClick = { showInviteDialog = true }
            )
            2 -> ReportsView(
                totalBal = totalBal,
                totalIncome = totalInc,
                totalExpense = totalExp,
                accountsCount = accounts.size,
                transactionsCount = transactions.size
            )
        }
    }

    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onConfirm = { email, name, role, dailyLimit, monthlyLimit, maxTxLimit ->
                viewModel.addMember(email, name, role, dailyLimit, monthlyLimit, maxTxLimit)
                showInviteDialog = false
            }
        )
    }
}

@Composable
private fun AuditLogsView(
    auditLogs: List<AuditLog>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "নিরাপত্তা ও অপরিবর্তনীয় অডিট ট্রেইল (Immutable Audit Logs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ওয়ার্কস্পেসের প্রতিটি কার্যক্রম ও লেনদেন পরিবর্তনের অডিট রেকর্ড এখানে সংরক্ষিত হয়।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("কোনো অডিট লগ রেকর্ড নেই", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            items(auditLogs) { log ->
                AuditLogCard(log = log)
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLog) {
    val dateStr = try {
        log.timestamp?.toDate()?.let {
            SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault()).format(it)
        } ?: "এখনই"
    } catch (e: Exception) {
        "এখনই"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.actionType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (log.details.isNotBlank()) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "কর্তৃক: ${log.userName.ifBlank { "সদস্য" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MembersView(
    members: List<WorkspaceMember>,
    onInviteClick: () -> Unit
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
                Column {
                    Text(text = "ওয়ার্কস্পেস সদস্য ও পারমিশন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "রোল-ভিত্তিক এক্সেস ও খরচের সীমা নিয়ন্ত্রণ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onInviteClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("আমন্ত্রণ")
                }
            }
        }

        items(members) { member ->
            MemberCard(member = member)
        }
    }
}

@Composable
fun MemberCard(member: WorkspaceMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName.ifBlank { "সদস্য" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = member.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member.dailySpendingLimit > 0 || member.monthlySpendingLimit > 0) {
                    Text(
                        text = "খরচের সীমা: দৈনিক ${FormatUtils.formatBdt(member.dailySpendingLimit)}, মাসিক ${FormatUtils.formatBdt(member.monthlySpendingLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            SuggestionChip(
                onClick = {},
                label = { Text(member.role) }
            )
        }
    }
}

@Composable
private fun ReportsView(
    totalBal: Double,
    totalIncome: Double,
    totalExpense: Double,
    accountsCount: Int,
    transactionsCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "আর্থিক সারসংক্ষেপ ও রিপোর্ট (Financial Statement)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportRow(label = "বর্তমান নিট ব্যালেন্স", value = FormatUtils.formatBdt(totalBal), isBold = true)
                ReportRow(label = "নির্বাচিত সময়ের মোট আয়", value = "+${FormatUtils.formatBdt(totalIncome)}")
                ReportRow(label = "নির্বাচিত সময়ের মোট ব্যয়", value = "-${FormatUtils.formatBdt(totalExpense)}")
                val netSavings = totalIncome - totalExpense
                ReportRow(label = "নিট সঞ্চয় (আয় - ব্যয়)", value = FormatUtils.formatBdt(netSavings), isBold = true)
                HorizontalDivider()
                ReportRow(label = "সক্রিয় একাউন্ট সংখ্যা", value = "$accountsCount টি")
                ReportRow(label = "রেকর্ডকৃত লেনদেন সংখ্যা", value = "$transactionsCount টি")
            }
        }

        // Export Actions
        Button(
            onClick = {
                val reportText = buildString {
                    appendLine("=== PW – পার্সোনাল ওয়ালেট রিপোর্ট ===")
                    appendLine("তারিখ: ${SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())}")
                    appendLine("----------------------------------------")
                    appendLine("বর্তমান মোট ব্যালেন্স: ${FormatUtils.formatBdt(totalBal)}")
                    appendLine("মোট আয়: ${FormatUtils.formatBdt(totalIncome)}")
                    appendLine("মোট ব্যয়: ${FormatUtils.formatBdt(totalExpense)}")
                    appendLine("নিট সঞ্চয়: ${FormatUtils.formatBdt(totalIncome - totalExpense)}")
                    appendLine("সক্রিয় একাউন্ট: $accountsCount টি")
                    appendLine("লেনদেন সংখ্যা: $transactionsCount টি")
                    appendLine("----------------------------------------")
                    appendLine("PW Personal Wallet: Your Money. Your Control. Your Financial Clarity.")
                }

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, reportText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "PW আর্থিক রিপোর্ট শেয়ার করুন")
                context.startActivity(shareIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
            Spacer(modifier = Modifier.width(8.dp))
            Text("আর্থিক রিপোর্ট শেয়ার / এক্সপোর্ট করুন (Export Statement)")
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}
