package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val defaultWorkspaceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Workspace(
    @DocumentId val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val currency: String = "BDT",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class MemberRole {
    OWNER,
    EDITOR,
    CONTRIBUTOR,
    VIEWER
}

@IgnoreExtraProperties
data class WorkspaceMember(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val displayName: String = "",
    val role: String = MemberRole.VIEWER.name,
    val dailySpendingLimit: Double = 0.0,
    val monthlySpendingLimit: Double = 0.0,
    val maxSingleTxAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AccountType {
    BANK,
    MOBILE_WALLET,
    CASH,
    SAVINGS,
    OTHER
}

@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val name: String = "",
    val type: String = AccountType.BANK.name,
    val providerName: String = "",
    val accountNumberMasked: String = "",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val color: String = "#0F766E",
    val icon: String = "account_balance",
    val status: String = "ACTIVE",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    ADJUSTMENT
}

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val toAccountId: String = "",
    val toAccountName: String = "",
    val date: String = "",
    val description: String = "",
    val receiptUrl: String = "",
    val createdBy: String = "",
    val creatorName: String = "",
    val isReversed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val name: String = "",
    val nameBn: String = "",
    val type: String = "EXPENSE",
    val icon: String = "category",
    val color: String = "#0F766E",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Budget(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val period: String = "MONTHLY",
    val categoryId: String = "",
    val categoryName: String = "",
    val month: Long = 0L,
    val year: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDate: String = "",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Debt(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val personName: String = "",
    val type: String = "RECEIVABLE", // RECEIVABLE (পাওনা) or PAYABLE (দেনা)
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: String = "",
    val status: String = "PENDING", // PENDING, PARTIAL, COMPLETED
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RecurringTransaction(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val frequency: String = "MONTHLY",
    val accountId: String = "",
    val categoryId: String = "",
    val nextExecutionDate: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val userId: String = "",
    val userName: String = "",
    val actionType: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val description: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
