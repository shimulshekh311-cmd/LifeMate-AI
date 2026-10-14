package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.local.entities.TransactionEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun FinanceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Decimal-safe currency arithmetic
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
    }
    val netBalance = remember(totalIncome, totalExpense) {
        totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_EVEN)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldSuccess,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = I18n.t("add_transaction"))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Balance Big Hero Card
            item {
                GlassCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = I18n.t("net_balance"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳${String.format(Locale.getDefault(), "%,.2f", netBalance.toDouble())}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance >= BigDecimal.ZERO) EmeraldSuccess else RoseError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "🟢 " + I18n.t("total_income"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${String.format(Locale.getDefault(), "%,.2f", totalIncome.toDouble())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                        Column {
                            Text(
                                text = "🔴 " + I18n.t("total_expense"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${String.format(Locale.getDefault(), "%,.2f", totalExpense.toDouble())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoseError
                            )
                        }
                    }
                }
            }

            // Accounts Breakdown
            item {
                Text(
                    text = "অ্যাকাউন্টসমূহ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { acc ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳${String.format(Locale.getDefault(), "%,.2f", acc.currentBalance)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions List
            item {
                Text(
                    text = I18n.t("recent_transactions"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        message = "এখনো কোনো লেনদেন রেকর্ড করা হয়নি।",
                        actionButton = {
                            Button(onClick = { showAddDialog = true }) {
                                Text(I18n.t("add_transaction"))
                            }
                        }
                    )
                }
            } else {
                items(transactions) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onDelete = { txToDelete = tx }
                    )
                }
            }
        }
    }

    if (txToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "৳${txToDelete?.amount} (${txToDelete?.category}) লেনদেনটি মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteTransaction(txToDelete!!.id)
                txToDelete = null
            },
            onDismiss = { txToDelete = null }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, amount, category, desc, accountId ->
                viewModel.addTransaction(type, amount, category, desc, accountId)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (transaction.description.isNotEmpty()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"}৳${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isIncome) EmeraldSuccess else RoseError
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, Long) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("Food", "Education", "Salary", "Freelance", "Health", "Transport", "Utilities", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_transaction"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text(I18n.t("type_expense")) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoseError.copy(alpha = 0.2f))
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text(I18n.t("type_income")) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldSuccess.copy(alpha = 0.2f))
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(I18n.t("amount_bdt")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("ক্যাটেগরি নির্বাচন:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSave(type, amount, category, desc, 1L)
                    }
                }
            ) {
                Text(I18n.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}
