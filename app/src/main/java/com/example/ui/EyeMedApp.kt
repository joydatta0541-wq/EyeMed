package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyeMedApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    // Back handling: pop back to TODAY tab if user is on HISTORY or SETTINGS
    BackHandler(enabled = currentTab != ScreenTab.TODAY) {
        viewModel.selectTab(ScreenTab.TODAY)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TODAY,
                    onClick = { viewModel.selectTab(ScreenTab.TODAY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.TODAY) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                            contentDescription = "Today Doses"
                        )
                    },
                    label = { Text("Today", fontWeight = if (currentTab == ScreenTab.TODAY) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_today")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.HISTORY,
                    onClick = { viewModel.selectTab(ScreenTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History Log"
                        )
                    },
                    label = { Text("History", fontWeight = if (currentTab == ScreenTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_history")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.SETTINGS,
                    onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", fontWeight = if (currentTab == ScreenTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.TODAY -> TodayScreen(viewModel = viewModel)
                ScreenTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
