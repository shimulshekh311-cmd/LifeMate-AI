package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.ui.components.FloatingAssistantAvatarButton
import com.example.ui.components.LifeMateHeader
import com.example.ui.screens.*
import com.example.ui.theme.LifeMateTheme
import com.example.ui.theme.PrimaryViolet
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPrefs by viewModel.userPreferences.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val isAssistantOpen by viewModel.isAssistantOpen.collectAsState()

            LaunchedEffect(userPrefs?.appLanguage) {
                userPrefs?.appLanguage?.let { I18n.currentLanguage = it }
            }

            LifeMateTheme(
                themeMode = userPrefs?.themeMode ?: "system"
            ) {
                // BackHandler: Return to Home if on another tab
                if (currentTab != ScreenTab.HOME) {
                    BackHandler {
                        viewModel.setTab(ScreenTab.HOME)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val title = when (currentTab) {
                            ScreenTab.HOME -> I18n.t("app_title")
                            ScreenTab.ROUTINE -> I18n.t("nav_routine")
                            ScreenTab.EXERCISE -> I18n.t("nav_exercise")
                            ScreenTab.STUDY -> I18n.t("nav_study")
                            ScreenTab.RELIGION -> I18n.t("nav_religion")
                            ScreenTab.JOBS -> I18n.t("nav_jobs")
                            ScreenTab.FINANCE -> I18n.t("nav_finance")
                            ScreenTab.STATS -> I18n.t("nav_stats")
                            ScreenTab.SETTINGS -> I18n.t("nav_settings")
                            ScreenTab.ASSISTANT -> userPrefs?.assistantName ?: "LifeMate"
                        }
                        LifeMateHeader(
                            title = title,
                            subtitle = if (currentTab == ScreenTab.HOME) I18n.t("app_subtitle") else null,
                            onAvatarClick = { viewModel.toggleAssistant(true) },
                            onLangToggle = {
                                val nextLang = if (I18n.currentLanguage == "bn") "en" else "bn"
                                viewModel.setLanguage(nextLang)
                            }
                        )
                    },
                    bottomBar = {
                        LifeMateBottomNavigation(
                            currentTab = currentTab,
                            isReligionEnabled = userPrefs?.isReligionModuleEnabled ?: true,
                            onTabSelected = { viewModel.setTab(it) }
                        )
                    },
                    floatingActionButton = {
                        if (!isAssistantOpen && currentTab != ScreenTab.SETTINGS) {
                            FloatingAssistantAvatarButton(
                                onClick = { viewModel.toggleAssistant(true) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            ScreenTab.HOME -> HomeScreen(viewModel)
                            ScreenTab.ROUTINE -> RoutineScreen(viewModel)
                            ScreenTab.EXERCISE -> ExerciseScreen(viewModel)
                            ScreenTab.STUDY -> StudyScreen(viewModel)
                            ScreenTab.RELIGION -> ReligionScreen(viewModel)
                            ScreenTab.JOBS -> JobApplicationsScreen(viewModel)
                            ScreenTab.FINANCE -> FinanceScreen(viewModel)
                            ScreenTab.STATS -> StatisticsScreen(viewModel)
                            ScreenTab.SETTINGS -> SettingsScreen(viewModel)
                            ScreenTab.ASSISTANT -> HomeScreen(viewModel)
                        }

                        if (isAssistantOpen) {
                            AssistantDialog(
                                viewModel = viewModel,
                                onDismiss = { viewModel.toggleAssistant(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifeMateBottomNavigation(
    currentTab: ScreenTab,
    isReligionEnabled: Boolean,
    onTabSelected: (ScreenTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = mutableListOf(
            NavigationItem(ScreenTab.HOME, Icons.Default.Home, I18n.t("nav_home")),
            NavigationItem(ScreenTab.ROUTINE, Icons.Default.Checklist, I18n.t("nav_routine")),
            NavigationItem(ScreenTab.EXERCISE, Icons.Default.FitnessCenter, I18n.t("nav_exercise")),
            NavigationItem(ScreenTab.STUDY, Icons.Default.MenuBook, I18n.t("nav_study"))
        )
        if (isReligionEnabled) {
            items.add(NavigationItem(ScreenTab.RELIGION, Icons.Default.WbTwilight, I18n.t("nav_religion")))
        }
        items.add(NavigationItem(ScreenTab.JOBS, Icons.Default.Work, I18n.t("nav_jobs")))
        items.add(NavigationItem(ScreenTab.FINANCE, Icons.Default.AccountBalanceWallet, I18n.t("nav_finance")))
        items.add(NavigationItem(ScreenTab.STATS, Icons.Default.BarChart, I18n.t("nav_stats")))
        items.add(NavigationItem(ScreenTab.SETTINGS, Icons.Default.Settings, I18n.t("nav_settings")))

        items.forEach { item ->
            val selected = currentTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag("nav_item_${item.tab.name.lowercase()}")
            )
        }
    }
}

private data class NavigationItem(
    val tab: ScreenTab,
    val icon: ImageVector,
    val label: String
)

@Composable
fun AssistantDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var chatReply by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("LifeMate AI Assistant", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (I18n.currentLanguage == "bn")
                        "আপনার কাজ, পড়ার লক্ষ্য, ব্যায়াম বা খরচের ব্যাপারে যে কোনো প্রশ্ন করুন।"
                    else
                        "Ask anything regarding your tasks, study goals, workouts, or expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text(if (I18n.currentLanguage == "bn") "যেমন: আজকের কাজের তালিকা দেখাও" else "e.g., Show today's schedule") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assistant_input_field"),
                    singleLine = true
                )
                if (chatReply != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chatReply!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (queryText.isNotBlank()) {
                        coroutineScope.launch {
                            val replyMsg = viewModel.aiEngine.processUserMessage(queryText)
                            chatReply = replyMsg.text
                        }
                    }
                },
                modifier = Modifier.testTag("assistant_send_btn")
            ) {
                Text(if (I18n.currentLanguage == "bn") "পাঠান" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(I18n.t("btn_dismiss"))
            }
        }
    )
}
