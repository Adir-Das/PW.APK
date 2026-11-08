package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.Transaction
import com.example.ui.auth.signOut
import com.example.ui.dialogs.*
import com.example.ui.tabs.*
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WalletViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val workspaces by viewModel.workspaces.collectAsState()
    val currentWorkspaceId by viewModel.currentWorkspaceId.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val currentWorkspace = workspaces.find { it.id == currentWorkspaceId }

    // Dialog states
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showWorkspaceMenu = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PW",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentWorkspace?.name ?: "ওয়ার্কস্পেস",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch workspace"
                                )
                            }
                            Text(
                                text = "PW – Personal Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Workspace Switcher Dropdown Menu
                        DropdownMenu(
                            expanded = showWorkspaceMenu,
                            onDismissRequest = { showWorkspaceMenu = false }
                        ) {
                            Text(
                                text = "ওয়ার্কস্পেস নির্বাচন করুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            workspaces.forEach { ws ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (ws.id == currentWorkspaceId) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(ws.name, fontWeight = if (ws.id == currentWorkspaceId) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectWorkspace(ws.id)
                                        showWorkspaceMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("নতুন ওয়ার্কস্পেস তৈরি করুন")
                                    }
                                },
                                onClick = {
                                    showWorkspaceMenu = false
                                    showCreateWorkspaceDialog = true
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Firebase.auth.currentUser?.displayName?.take(1)?.uppercase() ?: "U",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectedTab.value = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("ড্যাশবোর্ড") },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectedTab.value = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("লেনদেন") },
                    modifier = Modifier.testTag("tab_transactions")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectedTab.value = 2 },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                    label = { Text("একাউন্ট") },
                    modifier = Modifier.testTag("tab_accounts")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectedTab.value = 3 },
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Budgets") },
                    label = { Text("বাজেট") },
                    modifier = Modifier.testTag("tab_budgets")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectedTab.value = 4 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Team & Audit") },
                    label = { Text("টিম ও অডিট") },
                    modifier = Modifier.testTag("tab_audit")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickActionSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_quick_action")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Actions")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    onOpenAddIncome = { showAddIncomeDialog = true },
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onOpenTransfer = { showTransferDialog = true },
                    onOpenAddAccount = { showAddAccountDialog = true },
                    onSelectTransaction = { selectedTransactionForDetail = it }
                )
                1 -> TransactionsTab(
                    viewModel = viewModel,
                    onSelectTransaction = { selectedTransactionForDetail = it }
                )
                2 -> AccountsTab(
                    viewModel = viewModel,
                    onOpenAddAccount = { showAddAccountDialog = true }
                )
                3 -> BudgetsSavingsTab(
                    viewModel = viewModel
                )
                4 -> AuditTeamTab(
                    viewModel = viewModel
                )
            }
        }
    }

    // Quick Action BottomSheet
    if (showQuickActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "দ্রুত লেনদেন ও একশন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ListItem(
                    headlineContent = { Text("নতুন আয় যোগ করুন (Income)", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("বেতন, ব্যবসা বা অন্য কোনো উৎস থেকে প্রাপ্ত টাকা") },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen)
                        }
                    },
                    modifier = Modifier.clickable {
                        showQuickActionSheet = false
                        showAddIncomeDialog = true
                    }
                )

                ListItem(
                    headlineContent = { Text("নতুন ব্যয় যোগ করুন (Expense)", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("খাবার, যাতায়াত বা অন্যান্য বিল পরিশোধ") },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(ExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed)
                        }
                    },
                    modifier = Modifier.clickable {
                        showQuickActionSheet = false
                        showAddExpenseDialog = true
                    }
                )

                ListItem(
                    headlineContent = { Text("টাকা ট্রান্সফার করুন (Transfer)", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("ব্যাংক থেকে বিকাশ বা নগদ ক্যাশে অর্থ স্থানান্তর") },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(TransferBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TransferBlue)
                        }
                    },
                    modifier = Modifier.clickable {
                        showQuickActionSheet = false
                        showTransferDialog = true
                    }
                )

                ListItem(
                    headlineContent = { Text("নতুন ব্যাংক বা ওয়ালেট একাউন্ট যোগ করুন", fontWeight = FontWeight.SemiBold) },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        showQuickActionSheet = false
                        showAddAccountDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialogs
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { title, amt, accId, accName, catId, catName, date, desc ->
                viewModel.addIncome(title, amt, accId, accName, catId, catName, date, desc)
                showAddIncomeDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amt, accId, accName, catId, catName, date, desc ->
                viewModel.addExpense(title, amt, accId, accName, catId, catName, date, desc)
                showAddExpenseDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferMoneyDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { srcId, srcName, dstId, dstName, amt, date, desc ->
                viewModel.transferMoney(srcId, srcName, dstId, dstName, amt, date, desc)
                showTransferDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, provider, openingBal, color, icon ->
                viewModel.addAccount(name, type, provider, openingBal, color, icon)
                showAddAccountDialog = false
            }
        )
    }

    if (showCreateWorkspaceDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateWorkspaceDialog = false },
            onConfirm = { name, curr, desc ->
                viewModel.createWorkspace(name, curr, desc)
                showCreateWorkspaceDialog = false
            }
        )
    }

    if (selectedTransactionForDetail != null) {
        TransactionDetailDialog(
            transaction = selectedTransactionForDetail!!,
            onDismiss = { selectedTransactionForDetail = null },
            onReverse = {
                selectedTransactionForDetail?.let { viewModel.reverseTransaction(it) }
                selectedTransactionForDetail = null
            }
        )
    }

    // Profile & Sign Out Dialog
    if (showProfileDialog) {
        val user = Firebase.auth.currentUser
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("ব্যবহারকারী প্রোফাইল", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("নাম: ${user?.displayName ?: "ব্যবহারকারী"}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("ইমেইল: ${user?.email ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    Text("ইউজার আইডি: ${user?.uid?.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("ডিফল্ট মুদ্রা: BDT (৳)", style = MaterialTheme.typography.bodySmall)
                    Text("টাইমজোন: Asia/Dhaka", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        signOut(credentialManager, coroutineScope) {
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("লগ আউট করুন (Sign Out)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}
