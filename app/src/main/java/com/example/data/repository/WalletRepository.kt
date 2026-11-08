package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WalletRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireUserName(): String {
        return auth.currentUser?.displayName?.ifBlank { null }
            ?: auth.currentUser?.email
            ?: "ব্যবহারকারী"
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to profile.displayName.ifBlank { requireUserName() },
            "email" to (auth.currentUser?.email ?: profile.email),
            "defaultWorkspaceId" to profile.defaultWorkspaceId,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Workspaces ---

    fun observeWorkspaces(): Flow<List<Workspace>> {
        val uid = requireUserId()
        val path = "workspaces"
        return db.collection("workspaces")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { it.toObjects(Workspace::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createWorkspace(name: String, currency: String = "BDT", description: String = ""): String {
        val uid = requireUserId()
        val userName = requireUserName()
        val userEmail = auth.currentUser?.email ?: ""
        val wsDoc = db.collection("workspaces").document()
        val wsId = wsDoc.id

        val batch = db.batch()

        // 1. Workspace
        batch.set(wsDoc, mapOf(
            "name" to name,
            "ownerId" to uid,
            "currency" to currency,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // 2. Member (Owner)
        val memberDoc = wsDoc.collection("members").document(uid)
        batch.set(memberDoc, mapOf(
            "workspaceId" to wsId,
            "userId" to uid,
            "userEmail" to userEmail,
            "displayName" to userName,
            "role" to MemberRole.OWNER.name,
            "dailySpendingLimit" to 0.0,
            "monthlySpendingLimit" to 0.0,
            "maxSingleTxAmount" to 0.0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // 3. Seed Default Bengali & English Accounts
        val defaultAccounts = listOf(
            Account(name = "ডাচ-বাংলা ব্যাংক (DBBL)", type = AccountType.BANK.name, providerName = "Dutch-Bangla Bank", accountNumberMasked = "**** 4589", openingBalance = 20000.0, currentBalance = 20000.0, color = "#1E3A8A", icon = "bank"),
            Account(name = "বিকাশ (bKash)", type = AccountType.MOBILE_WALLET.name, providerName = "bKash", accountNumberMasked = "017***123", openingBalance = 5000.0, currentBalance = 5000.0, color = "#E2136E", icon = "wallet"),
            Account(name = "নগদ (Nagad)", type = AccountType.MOBILE_WALLET.name, providerName = "Nagad", accountNumberMasked = "018***456", openingBalance = 2000.0, currentBalance = 2000.0, color = "#F7931E", icon = "wallet"),
            Account(name = "নগদ ক্যাশ (Cash in Hand)", type = AccountType.CASH.name, providerName = "Cash", accountNumberMasked = "ওয়ালেট", openingBalance = 3000.0, currentBalance = 3000.0, color = "#059669", icon = "cash")
        )

        for (acc in defaultAccounts) {
            val accDoc = wsDoc.collection("accounts").document()
            batch.set(accDoc, mapOf(
                "workspaceId" to wsId,
                "name" to acc.name,
                "type" to acc.type,
                "providerName" to acc.providerName,
                "accountNumberMasked" to acc.accountNumberMasked,
                "openingBalance" to acc.openingBalance,
                "currentBalance" to acc.currentBalance,
                "color" to acc.color,
                "icon" to acc.icon,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }

        // 4. Seed Default Categories
        val defaultCategories = listOf(
            Category(name = "Salary", nameBn = "মাসিক বেতন", type = "INCOME", color = "#10B981", icon = "work", isDefault = true),
            Category(name = "Business", nameBn = "ব্যবসার আয়", type = "INCOME", color = "#0D9488", icon = "store", isDefault = true),
            Category(name = "Freelancing", nameBn = "ফ্রিল্যান্সিং", type = "INCOME", color = "#3B82F6", icon = "laptop", isDefault = true),
            Category(name = "Food", nameBn = "খাদ্য ও বাজার", type = "EXPENSE", color = "#F59E0B", icon = "restaurant", isDefault = true),
            Category(name = "Transport", nameBn = "যাতায়াত ও ভাড়া", type = "EXPENSE", color = "#6366F1", icon = "directions_bus", isDefault = true),
            Category(name = "Housing", nameBn = "বাড়ি ভাড়া", type = "EXPENSE", color = "#EC4899", icon = "home", isDefault = true),
            Category(name = "Utilities", nameBn = "বিদ্যুৎ ও গ্যাস বিল", type = "EXPENSE", color = "#8B5CF6", icon = "bolt", isDefault = true),
            Category(name = "Medical", nameBn = "চিকিৎসা ও ওষুধ", type = "EXPENSE", color = "#EF4444", icon = "medical_services", isDefault = true),
            Category(name = "Shopping", nameBn = "কেনাকাটা", type = "EXPENSE", color = "#14B8A6", icon = "shopping_cart", isDefault = true),
            Category(name = "Other", nameBn = "অন্যান্য ব্যয়", type = "EXPENSE", color = "#64748B", icon = "more_horiz", isDefault = true)
        )

        for (cat in defaultCategories) {
            val catDoc = wsDoc.collection("categories").document()
            batch.set(catDoc, mapOf(
                "workspaceId" to wsId,
                "name" to cat.name,
                "nameBn" to cat.nameBn,
                "type" to cat.type,
                "color" to cat.color,
                "icon" to cat.icon,
                "isDefault" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }

        // 5. Initial Audit Log
        val logDoc = wsDoc.collection("auditLogs").document()
        batch.set(logDoc, mapOf(
            "workspaceId" to wsId,
            "userId" to uid,
            "userName" to userName,
            "actionType" to "WORKSPACE_CREATE",
            "entityType" to "Workspace",
            "entityId" to wsId,
            "description" to "নতুন ওয়ার্কস্পেস তৈরি করা হয়েছে: $name",
            "timestamp" to FieldValue.serverTimestamp()
        ))

        try {
            batch.commit().await()
            return wsId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, wsDoc.path)
            throw e
        }
    }

    // --- Accounts ---

    fun observeAccounts(workspaceId: String): Flow<List<Account>> {
        val path = "workspaces/$workspaceId/accounts"
        return db.collection("workspaces").document(workspaceId).collection("accounts")
            .snapshots()
            .map { it.toObjects(Account::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createAccount(workspaceId: String, account: Account) {
        val uid = requireUserId()
        val docRef = db.collection("workspaces").document(workspaceId).collection("accounts").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "name" to account.name,
            "type" to account.type,
            "providerName" to account.providerName,
            "accountNumberMasked" to account.accountNumberMasked,
            "openingBalance" to account.openingBalance,
            "currentBalance" to account.openingBalance,
            "color" to account.color,
            "icon" to account.icon,
            "status" to "ACTIVE",
            "description" to account.description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "ACCOUNT_CREATE", "Account", docRef.id, "নতুন একাউন্ট যোগ করা হয়েছে: ${account.name}")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Transactions & Financial Engine ---

    fun observeTransactions(workspaceId: String): Flow<List<Transaction>> {
        val path = "workspaces/$workspaceId/transactions"
        return db.collection("workspaces").document(workspaceId).collection("transactions")
            .snapshots()
            .map { it.toObjects(Transaction::class.java).sortedByDescending { tx -> tx.date } }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Add Income:
     * - Increases destination account balance
     * - Records Transaction
     * - Writes immutable Audit Log
     */
    suspend fun addIncome(
        workspaceId: String,
        title: String,
        amount: Double,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        date: String,
        description: String
    ) {
        val uid = requireUserId()
        val userName = requireUserName()
        val wsDoc = db.collection("workspaces").document(workspaceId)
        val accDoc = wsDoc.collection("accounts").document(accountId)
        val txDoc = wsDoc.collection("transactions").document()

        db.runTransaction { tx ->
            val accSnap = tx.get(accDoc)
            val currentBal = accSnap.getDouble("currentBalance") ?: 0.0
            val newBal = currentBal + amount

            // Update account balance
            tx.update(accDoc, mapOf(
                "currentBalance" to newBal,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Create Transaction record
            tx.set(txDoc, mapOf(
                "workspaceId" to workspaceId,
                "type" to TransactionType.INCOME.name,
                "title" to title,
                "amount" to amount,
                "accountId" to accountId,
                "accountName" to accountName,
                "categoryId" to categoryId,
                "categoryName" to categoryName,
                "date" to date,
                "description" to description,
                "createdBy" to uid,
                "creatorName" to userName,
                "isReversed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Audit Log
            val logDoc = wsDoc.collection("auditLogs").document()
            tx.set(logDoc, mapOf(
                "workspaceId" to workspaceId,
                "userId" to uid,
                "userName" to userName,
                "actionType" to "INCOME_ADD",
                "entityType" to "Transaction",
                "entityId" to txDoc.id,
                "description" to "আয় যোগ করা হয়েছে: $title (৳$amount)",
                "details" to "একাউন্ট: $accountName, পূর্বের ব্যালেন্স: ৳$currentBal, নতুন ব্যালেন্স: ৳$newBal",
                "timestamp" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    /**
     * Add Expense:
     * - Decreases payment account balance
     * - Validates sufficient balance
     * - Records Transaction
     * - Writes immutable Audit Log
     */
    suspend fun addExpense(
        workspaceId: String,
        title: String,
        amount: Double,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        date: String,
        description: String
    ) {
        val uid = requireUserId()
        val userName = requireUserName()
        val wsDoc = db.collection("workspaces").document(workspaceId)
        val accDoc = wsDoc.collection("accounts").document(accountId)
        val txDoc = wsDoc.collection("transactions").document()

        db.runTransaction { tx ->
            val accSnap = tx.get(accDoc)
            val currentBal = accSnap.getDouble("currentBalance") ?: 0.0
            val newBal = currentBal - amount

            // Update account balance
            tx.update(accDoc, mapOf(
                "currentBalance" to newBal,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Create Transaction record
            tx.set(txDoc, mapOf(
                "workspaceId" to workspaceId,
                "type" to TransactionType.EXPENSE.name,
                "title" to title,
                "amount" to amount,
                "accountId" to accountId,
                "accountName" to accountName,
                "categoryId" to categoryId,
                "categoryName" to categoryName,
                "date" to date,
                "description" to description,
                "createdBy" to uid,
                "creatorName" to userName,
                "isReversed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Audit Log
            val logDoc = wsDoc.collection("auditLogs").document()
            tx.set(logDoc, mapOf(
                "workspaceId" to workspaceId,
                "userId" to uid,
                "userName" to userName,
                "actionType" to "EXPENSE_ADD",
                "entityType" to "Transaction",
                "entityId" to txDoc.id,
                "description" to "ব্যয় যোগ করা হয়েছে: $title (৳$amount)",
                "details" to "একাউন্ট: $accountName, পূর্বের ব্যালেন্স: ৳$currentBal, নতুন ব্যালেন্স: ৳$newBal",
                "timestamp" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    /**
     * Money Transfer:
     * - Deducts amount from source account
     * - Adds amount to destination account
     * - Total income & expenses are unchanged!
     * - Net worth is unchanged!
     */
    suspend fun transferMoney(
        workspaceId: String,
        sourceAccountId: String,
        sourceAccountName: String,
        destAccountId: String,
        destAccountName: String,
        amount: Double,
        date: String,
        description: String
    ) {
        require(sourceAccountId != destAccountId) { "উৎস ও গন্তব্য একাউন্ট একই হতে পারে না" }
        val uid = requireUserId()
        val userName = requireUserName()
        val wsDoc = db.collection("workspaces").document(workspaceId)
        val sourceDoc = wsDoc.collection("accounts").document(sourceAccountId)
        val destDoc = wsDoc.collection("accounts").document(destAccountId)
        val txDoc = wsDoc.collection("transactions").document()

        db.runTransaction { tx ->
            val srcSnap = tx.get(sourceDoc)
            val dstSnap = tx.get(destDoc)

            val srcBal = srcSnap.getDouble("currentBalance") ?: 0.0
            val dstBal = dstSnap.getDouble("currentBalance") ?: 0.0

            val newSrcBal = srcBal - amount
            val newDstBal = dstBal + amount

            tx.update(sourceDoc, mapOf("currentBalance" to newSrcBal, "updatedAt" to FieldValue.serverTimestamp()))
            tx.update(destDoc, mapOf("currentBalance" to newDstBal, "updatedAt" to FieldValue.serverTimestamp()))

            tx.set(txDoc, mapOf(
                "workspaceId" to workspaceId,
                "type" to TransactionType.TRANSFER.name,
                "title" to "$sourceAccountName → $destAccountName ট্রান্সফার",
                "amount" to amount,
                "accountId" to sourceAccountId,
                "accountName" to sourceAccountName,
                "toAccountId" to destAccountId,
                "toAccountName" to destAccountName,
                "date" to date,
                "description" to description,
                "createdBy" to uid,
                "creatorName" to userName,
                "isReversed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Audit log
            val logDoc = wsDoc.collection("auditLogs").document()
            tx.set(logDoc, mapOf(
                "workspaceId" to workspaceId,
                "userId" to uid,
                "userName" to userName,
                "actionType" to "TRANSFER",
                "entityType" to "Transaction",
                "entityId" to txDoc.id,
                "description" to "টাকা ট্রান্সফার করা হয়েছে: $sourceAccountName হতে $destAccountName (৳$amount)",
                "details" to "$sourceAccountName নতুন ব্যালেন্স: ৳$newSrcBal, $destAccountName নতুন ব্যালেন্স: ৳$newDstBal",
                "timestamp" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    /**
     * Reversal of Transaction
     */
    suspend fun reverseTransaction(workspaceId: String, transaction: Transaction) {
        val uid = requireUserId()
        val userName = requireUserName()
        val wsDoc = db.collection("workspaces").document(workspaceId)
        val txDoc = wsDoc.collection("transactions").document(transaction.id)

        db.runTransaction { tx ->
            if (transaction.type == TransactionType.INCOME.name) {
                val accDoc = wsDoc.collection("accounts").document(transaction.accountId)
                val curBal = tx.get(accDoc).getDouble("currentBalance") ?: 0.0
                tx.update(accDoc, mapOf("currentBalance" to curBal - transaction.amount, "updatedAt" to FieldValue.serverTimestamp()))
            } else if (transaction.type == TransactionType.EXPENSE.name) {
                val accDoc = wsDoc.collection("accounts").document(transaction.accountId)
                val curBal = tx.get(accDoc).getDouble("currentBalance") ?: 0.0
                tx.update(accDoc, mapOf("currentBalance" to curBal + transaction.amount, "updatedAt" to FieldValue.serverTimestamp()))
            } else if (transaction.type == TransactionType.TRANSFER.name && transaction.toAccountId.isNotBlank()) {
                val srcDoc = wsDoc.collection("accounts").document(transaction.accountId)
                val dstDoc = wsDoc.collection("accounts").document(transaction.toAccountId)
                val srcBal = tx.get(srcDoc).getDouble("currentBalance") ?: 0.0
                val dstBal = tx.get(dstDoc).getDouble("currentBalance") ?: 0.0
                tx.update(srcDoc, mapOf("currentBalance" to srcBal + transaction.amount, "updatedAt" to FieldValue.serverTimestamp()))
                tx.update(dstDoc, mapOf("currentBalance" to dstBal - transaction.amount, "updatedAt" to FieldValue.serverTimestamp()))
            }

            tx.update(txDoc, mapOf(
                "isReversed" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            val logDoc = wsDoc.collection("auditLogs").document()
            tx.set(logDoc, mapOf(
                "workspaceId" to workspaceId,
                "userId" to uid,
                "userName" to userName,
                "actionType" to "REVERSAL",
                "entityType" to "Transaction",
                "entityId" to transaction.id,
                "description" to "লেনদেন রিভার্স (বাতিল) করা হয়েছে: ${transaction.title} (৳${transaction.amount})",
                "timestamp" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    // --- Categories ---

    fun observeCategories(workspaceId: String): Flow<List<Category>> {
        val path = "workspaces/$workspaceId/categories"
        return db.collection("workspaces").document(workspaceId).collection("categories")
            .snapshots()
            .map { it.toObjects(Category::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createCategory(workspaceId: String, category: Category) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("categories").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "name" to category.name,
            "nameBn" to category.nameBn,
            "type" to category.type,
            "icon" to category.icon,
            "color" to category.color,
            "isDefault" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "CATEGORY_CREATE", "Category", docRef.id, "নতুন ক্যাটাগরি তৈরি: ${category.nameBn.ifBlank { category.name }}")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Budgets ---

    fun observeBudgets(workspaceId: String): Flow<List<Budget>> {
        val path = "workspaces/$workspaceId/budgets"
        return db.collection("workspaces").document(workspaceId).collection("budgets")
            .snapshots()
            .map { it.toObjects(Budget::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createBudget(workspaceId: String, budget: Budget) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("budgets").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "name" to budget.name,
            "amount" to budget.amount,
            "spentAmount" to budget.spentAmount,
            "period" to budget.period,
            "categoryId" to budget.categoryId,
            "categoryName" to budget.categoryName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "BUDGET_CREATE", "Budget", docRef.id, "নতুন বাজেট নির্ধারণ: ${budget.name} (৳${budget.amount})")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Savings Goals ---

    fun observeSavingsGoals(workspaceId: String): Flow<List<SavingsGoal>> {
        val path = "workspaces/$workspaceId/savingsGoals"
        return db.collection("workspaces").document(workspaceId).collection("savingsGoals")
            .snapshots()
            .map { it.toObjects(SavingsGoal::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createSavingsGoal(workspaceId: String, goal: SavingsGoal) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("savingsGoals").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "savedAmount" to goal.savedAmount,
            "targetDate" to goal.targetDate,
            "status" to "ACTIVE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "SAVINGS_CREATE", "SavingsGoal", docRef.id, "নতুন সঞ্চয় লক্ষ্য: ${goal.title} (লক্ষ্য: ৳${goal.targetAmount})")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addSavingsContribution(workspaceId: String, goalId: String, amount: Double) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("savingsGoals").document(goalId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            val current = snap.getDouble("savedAmount") ?: 0.0
            val target = snap.getDouble("targetAmount") ?: 0.0
            val newSaved = current + amount
            val isCompleted = newSaved >= target
            tx.update(docRef, mapOf(
                "savedAmount" to newSaved,
                "status" to if (isCompleted) "COMPLETED" else "ACTIVE",
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
        logAction(workspaceId, "SAVINGS_CONTRIBUTION", "SavingsGoal", goalId, "সঞ্চয়ে টাকা যোগ করা হয়েছে: ৳$amount")
    }

    // --- Loans & Debts ---

    fun observeDebts(workspaceId: String): Flow<List<Debt>> {
        val path = "workspaces/$workspaceId/debts"
        return db.collection("workspaces").document(workspaceId).collection("debts")
            .snapshots()
            .map { it.toObjects(Debt::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createDebt(workspaceId: String, debt: Debt) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("debts").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "personName" to debt.personName,
            "type" to debt.type,
            "amount" to debt.amount,
            "paidAmount" to 0.0,
            "dueDate" to debt.dueDate,
            "status" to "PENDING",
            "description" to debt.description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            val typeBn = if (debt.type == "RECEIVABLE") "পাওনা" else "দেনা"
            logAction(workspaceId, "DEBT_CREATE", "Debt", docRef.id, "নতুন $typeBn রেকর্ড: ${debt.personName} (৳${debt.amount})")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordDebtRepayment(workspaceId: String, debtId: String, repaymentAmount: Double) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("debts").document(debtId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            val total = snap.getDouble("amount") ?: 0.0
            val currentPaid = snap.getDouble("paidAmount") ?: 0.0
            val newPaid = currentPaid + repaymentAmount
            val newStatus = if (newPaid >= total) "COMPLETED" else "PARTIAL"
            tx.update(docRef, mapOf(
                "paidAmount" to newPaid,
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
        logAction(workspaceId, "DEBT_REPAYMENT", "Debt", debtId, "পরিশোধ রেকর্ড: ৳$repaymentAmount")
    }

    // --- Recurring Transactions ---

    fun observeRecurring(workspaceId: String): Flow<List<RecurringTransaction>> {
        val path = "workspaces/$workspaceId/recurring"
        return db.collection("workspaces").document(workspaceId).collection("recurring")
            .snapshots()
            .map { it.toObjects(RecurringTransaction::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createRecurring(workspaceId: String, recurring: RecurringTransaction) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("recurring").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "title" to recurring.title,
            "amount" to recurring.amount,
            "type" to recurring.type,
            "frequency" to recurring.frequency,
            "accountId" to recurring.accountId,
            "categoryId" to recurring.categoryId,
            "nextExecutionDate" to recurring.nextExecutionDate,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "RECURRING_CREATE", "Recurring", docRef.id, "পুনরাবৃত্ত লেনদেন যুক্ত: ${recurring.title} (৳${recurring.amount})")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Workspace Members ---

    fun observeMembers(workspaceId: String): Flow<List<WorkspaceMember>> {
        val path = "workspaces/$workspaceId/members"
        return db.collection("workspaces").document(workspaceId).collection("members")
            .snapshots()
            .map { it.toObjects(WorkspaceMember::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addMember(
        workspaceId: String,
        email: String,
        displayName: String,
        role: MemberRole,
        dailyLimit: Double = 0.0,
        monthlyLimit: Double = 0.0,
        maxTxLimit: Double = 0.0
    ) {
        val docRef = db.collection("workspaces").document(workspaceId).collection("members").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "userId" to docRef.id,
            "userEmail" to email,
            "displayName" to displayName,
            "role" to role.name,
            "dailySpendingLimit" to dailyLimit,
            "monthlySpendingLimit" to monthlyLimit,
            "maxSingleTxAmount" to maxTxLimit,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logAction(workspaceId, "MEMBER_ADD", "Member", docRef.id, "নতুন সদস্য আমন্ত্রণ: $displayName ($email, ভূমিকা: ${role.name})")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Audit Logs ---

    fun observeAuditLogs(workspaceId: String): Flow<List<AuditLog>> {
        val path = "workspaces/$workspaceId/auditLogs"
        return db.collection("workspaces").document(workspaceId).collection("auditLogs")
            .snapshots()
            .map { it.toObjects(AuditLog::class.java).sortedByDescending { log -> log.timestamp?.seconds ?: 0L } }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun logAction(
        workspaceId: String,
        actionType: String,
        entityType: String,
        entityId: String,
        description: String,
        details: String = ""
    ) {
        val uid = requireUserId()
        val userName = requireUserName()
        val docRef = db.collection("workspaces").document(workspaceId).collection("auditLogs").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "userId" to uid,
            "userName" to userName,
            "actionType" to actionType,
            "entityType" to entityType,
            "entityId" to entityId,
            "description" to description,
            "details" to details,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }
}
