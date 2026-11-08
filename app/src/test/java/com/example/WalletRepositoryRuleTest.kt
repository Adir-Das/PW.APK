package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.WalletRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class WalletRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createWorkspace_validUser_succeedsAndSeedsDefaults() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = WalletRepository(firestore)

        val wsId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createWorkspace("Alice Personal Wallet", "BDT", "Testing workspace")
        }
        assertNotNull(wsId)
        assertTrue(wsId.isNotEmpty())

        val accounts = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeAccounts(wsId).first { it.isNotEmpty() }
        }
        assertTrue("Accounts should have seeded default items", accounts.isNotEmpty())
        assertTrue("DBBL should exist", accounts.any { it.name.contains("DBBL") || it.name.contains("ডাচ-বাংলা") })
    }

    @Test
    fun financialFlow_incomeExpenseAndTransfer_calculatesCorrectly() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = WalletRepository(firestore)

        // 1. Create workspace
        val wsId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createWorkspace("Scenario Wallet", "BDT", "Testing financial calculations")
        }

        // Wait for accounts
        val accounts = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeAccounts(wsId).first { it.size >= 2 }
        }
        val bankAcc = accounts.first { it.type == "BANK" }
        val walletAcc = accounts.first { it.type == "MOBILE_WALLET" }

        val initialBankBal = bankAcc.currentBalance
        val initialWalletBal = walletAcc.currentBalance

        // 2. Add Income 10,000 to Bank
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addIncome(
                workspaceId = wsId,
                title = "Client Payment",
                amount = 10000.0,
                accountId = bankAcc.id,
                accountName = bankAcc.name,
                categoryId = "cat_1",
                categoryName = "Business",
                date = "2026-11-08",
                description = "Freelance income"
            )
        }

        // 3. Add Expense 5,000 from Bank
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addExpense(
                workspaceId = wsId,
                title = "Office Supplies",
                amount = 5000.0,
                accountId = bankAcc.id,
                accountName = bankAcc.name,
                categoryId = "cat_2",
                categoryName = "Office",
                date = "2026-11-08",
                description = "Paper and stationery"
            )
        }

        // 4. Transfer 5,000 from Bank to Wallet
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.transferMoney(
                workspaceId = wsId,
                sourceAccountId = bankAcc.id,
                sourceAccountName = bankAcc.name,
                destAccountId = walletAcc.id,
                destAccountName = walletAcc.name,
                amount = 5000.0,
                date = "2026-11-08",
                description = "Bank to bKash refill"
            )
        }

        // Verify balances
        val updatedAccounts = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeAccounts(wsId).first { list ->
                val b = list.find { it.id == bankAcc.id }
                b != null && b.currentBalance == (initialBankBal + 10000.0 - 5000.0 - 5000.0)
            }
        }
        val finalBank = updatedAccounts.find { it.id == bankAcc.id }
        val finalWallet = updatedAccounts.find { it.id == walletAcc.id }

        assertEquals(initialBankBal, finalBank?.currentBalance ?: 0.0, 0.01)
        assertEquals(initialWalletBal + 5000.0, finalWallet?.currentBalance ?: 0.0, 0.01)
    }

    @Test
    fun unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = WalletRepository(firestore)

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                withTimeout(3000L) {
                    repo.observeAccounts("any_workspace_id").first()
                }
            }
        }
        val firestoreEx = exception.unwrapFirestoreException()
        // Either requireUserId() threw or Firestore PERMISSION_DENIED
        assertTrue(
            exception is IllegalStateException ||
            firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
