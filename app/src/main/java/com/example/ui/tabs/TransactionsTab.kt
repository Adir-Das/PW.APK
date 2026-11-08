package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Transaction
import com.example.ui.WalletViewModel

@Composable
fun TransactionsTab(
    viewModel: WalletViewModel,
    onSelectTransaction: (Transaction) -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val typeFilter by viewModel.transactionTypeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("লেনদেন খুঁজুন (শিরোনাম, ক্যাটাগরি, একাউন্ট)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        Pair("ALL", "সব"),
                        Pair("INCOME", "আয় (+)") ,
                        Pair("EXPENSE", "ব্যয় (-)"),
                        Pair("TRANSFER", "ট্রান্সফার (⇄)")
                    )
                    filterOptions.forEach { opt ->
                        FilterChip(
                            selected = typeFilter == opt.first,
                            onClick = { viewModel.transactionTypeFilter.value = opt.first },
                            label = { Text(opt.second) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Transactions list
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        Text(
                            text = "কোনো লেনদেন পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                        )
                    }
                }
            } else {
                items(transactions) { tx ->
                    TransactionCard(transaction = tx, onClick = { onSelectTransaction(tx) })
                }
            }
        }
    }
}
