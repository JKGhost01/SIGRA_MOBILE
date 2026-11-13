package com.sigra.app.ui

import android.Manifest
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.sigra.app.ui.screens.MapScreen
import com.sigra.app.ui.screens.QuakesScreen
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sigra.app.data.SeismicAlert
import com.sigra.app.ui.screens.AlertFullScreen
import com.sigra.app.ui.screens.AlertsScreen
import com.sigra.app.ui.screens.HomeScreen
import com.sigra.app.ui.screens.OnboardingScreen
import com.sigra.app.ui.screens.SettingsScreen
import com.sigra.app.viewmodel.MainViewModel
import com.sigra.app.service.NotificationHelper

@Composable
fun SigraRoot(
    alertJson: String?,
    onDismissAlert: () -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val startupError by vm.startupError.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val openedAlert: SeismicAlert? = remember(alertJson) { vm.parsearAlerta(alertJson) }
    val alerta = settings.alertInbox.entries.firstOrNull { it.alert.uuid == openedAlert?.uuid }?.alert ?: openedAlert
    LaunchedEffect(openedAlert?.uuid) {
        openedAlert?.let { vm.openAlert(it) }
    }

    val permisos = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.completarOnboarding()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            if (vm.error.value == it) vm.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !ready -> {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            startupError != null -> {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No pudimos iniciar SIGRA", style = MaterialTheme.typography.titleLarge)
                        Text(startupError.orEmpty())
                        TextButton(onClick = vm::initialize) { Text("Reintentar") }
                    }
                }
            }

            !settings.onboardingCompleted -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    OnboardingScreen(
                        onActivate = {
                            lanzadorPermisos.launch(permisos)
                        },
                        onExplore = { vm.completarOnboarding(activateMonitoring = false) },
                    )
                }
                SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing))
            }

            else -> {
                MainShell(vm = vm, snackbar = snackbar)
            }
        }

        if (alerta != null) {
            AlertFullScreen(alerta = alerta, onDismiss = {
                NotificationHelper.cancelAlert(context, alerta.uuid)
                onDismissAlert()
            })
        }
    }
}

@Composable
private fun MainShell(vm: MainViewModel, snackbar: SnackbarHostState) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val unreadCount = settings.alertInbox.unreadCount
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: AppDestination.HOME.route
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, vm) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                vm.awaitRefresh()
                delay(20_000)
            }
        }
    }
    fun navigate(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp || (maxWidth >= 600.dp && maxHeight < 480.dp)
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                NavigationRail(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
                    containerColor = MaterialTheme.colorScheme.surface) {
                    Text("SIGRA", modifier = Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    AppDestination.entries.forEach { destination ->
                        NavigationRailItem(selected = route == destination.route, onClick = { navigate(destination) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                            icon = { DestinationIcon(destination, unreadCount) }, label = { Text(destination.label) })
                    }
                }
            }
            Scaffold(modifier = Modifier.weight(1f).fillMaxHeight(), contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!wide) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                            AppDestination.entries.forEach { destination ->
                                NavigationBarItem(selected = route == destination.route, onClick = { navigate(destination) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                    icon = { DestinationIcon(destination, unreadCount) }, label = { Text(destination.label) })
                            }
                        }
                    }
                }) { padding ->
                NavHost(navController = navController, startDestination = AppDestination.HOME.route,
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    composable(AppDestination.HOME.route) {
                        PageFrame {
                            HomeScreen(vm = vm, onOpenAlerts = { navigate(AppDestination.ALERTS) },
                                onOpenMap = { navigate(AppDestination.MAP) }, onOpenQuakes = { navigate(AppDestination.QUAKES) })
                        }
                    }
                    composable(AppDestination.MAP.route) {
                        MapScreen(vm, onOpenEvents = { navigate(AppDestination.QUAKES) })
                    }
                    composable(AppDestination.QUAKES.route) {
                        PageFrame { QuakesScreen(vm, onOpenMap = { navigate(AppDestination.MAP) }) }
                    }
                    composable(AppDestination.ALERTS.route) { PageFrame { AlertsScreen(vm) } }
                    composable(AppDestination.SETTINGS.route) { PageFrame { SettingsScreen(vm) } }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: AppDestination, unreadCount: Int) {
    if (destination == AppDestination.ALERTS && unreadCount > 0) {
        BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
            Icon(destination.icon, contentDescription = "Alertas, $unreadCount sin leer")
        }
    } else {
        Icon(destination.icon, contentDescription = destination.label)
    }
}

@Composable
private fun PageFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) { content() }
    }
}
