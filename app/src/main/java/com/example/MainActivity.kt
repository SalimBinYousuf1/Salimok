package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SalimViewModel
import com.example.ui.screens.DeviceDetailsScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HostsListScreen
import com.example.ui.screens.PairingWizardScreen
import com.example.ui.screens.RemoteScreenViewerScreen
import com.example.ui.theme.MyApplicationTheme

enum class ControllerScreen {
    HOSTS,
    VIEWER,
    PAIRING,
    DETAILS,
    DIAGNOSTICS
}

enum class ControllerTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOSTS("Hosts", Icons.Filled.Devices, Icons.Outlined.Devices),
    DIAGNOSTICS("Diagnostics", Icons.Filled.BugReport, Icons.Outlined.BugReport)
}

class MainActivity : ComponentActivity() {

    private val viewModel: SalimViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(ControllerScreen.HOSTS) }
                var selectedTab by remember { mutableStateOf(ControllerTab.HOSTS) }
                var selectedHostId by remember { mutableStateOf("") }

                // BackHandler to return to HOSTS screen
                BackHandler(enabled = currentScreen != ControllerScreen.HOSTS) {
                    currentScreen = ControllerScreen.HOSTS
                    selectedTab = ControllerTab.HOSTS
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Hide bottom navigation when in remote screen viewer
                        if (currentScreen != ControllerScreen.VIEWER) {
                            NavigationBar(
                                containerColor = Color.White,
                                tonalElevation = 6.dp
                            ) {
                                ControllerTab.values().forEach { tab ->
                                    val isSelected = selectedTab == tab && (currentScreen == ControllerScreen.HOSTS || currentScreen == ControllerScreen.DIAGNOSTICS)
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTab = tab
                                            currentScreen = when (tab) {
                                                ControllerTab.HOSTS -> ControllerScreen.HOSTS
                                                ControllerTab.DIAGNOSTICS -> ControllerScreen.DIAGNOSTICS
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1E40AF),
                                            selectedTextColor = Color(0xFF1E40AF),
                                            indicatorColor = Color(0xFFEFF6FF),
                                            unselectedIconColor = Color(0xFF64748B),
                                            unselectedTextColor = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        ControllerScreen.HOSTS -> HostsListScreen(
                            viewModel = viewModel,
                            onNavigateToViewer = { currentScreen = ControllerScreen.VIEWER },
                            onNavigateToPairing = { currentScreen = ControllerScreen.PAIRING },
                            onNavigateToDetails = { hostId ->
                                selectedHostId = hostId
                                currentScreen = ControllerScreen.DETAILS
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        ControllerScreen.VIEWER -> RemoteScreenViewerScreen(
                            viewModel = viewModel,
                            onCloseViewer = { currentScreen = ControllerScreen.HOSTS },
                            modifier = Modifier.fillMaxSize()
                        )
                        ControllerScreen.PAIRING -> PairingWizardScreen(
                            viewModel = viewModel,
                            onPairingSuccess = { currentScreen = ControllerScreen.HOSTS },
                            onBack = { currentScreen = ControllerScreen.HOSTS },
                            modifier = Modifier.padding(innerPadding)
                        )
                        ControllerScreen.DETAILS -> DeviceDetailsScreen(
                            hostId = selectedHostId,
                            viewModel = viewModel,
                            onBack = { currentScreen = ControllerScreen.HOSTS },
                            modifier = Modifier.padding(innerPadding)
                        )
                        ControllerScreen.DIAGNOSTICS -> DiagnosticsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
