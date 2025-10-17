@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jun1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jun1.data.AlarmRepo
import com.example.jun1.model.AlarmSpec
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }

    @Composable
    fun App() {
        val nav = rememberNavController()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val dest by nav.currentBackStackEntryAsState()
                    val route = dest?.destination?.route ?: "list"

                    NavigationBarItem(
                        selected = route.startsWith("list"),
                        onClick = { nav.navigate("list") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("알람목록") }
                    )
                    NavigationBarItem(
                        selected = route.startsWith("edit"),
                        onClick = { nav.navigate("edit?alarmId=") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Alarm, contentDescription = null) },
                        label = { Text("알람수정") }
                    )
                }
            }
        ) { pad ->
            NavHost(
                navController = nav,
                startDestination = "list",
                modifier = Modifier.padding(pad)
            ) {
                composable("list") {
                    AlarmListScreen(
                        onAdd = { nav.navigate("edit?alarmId=") },
                        onEdit = { id -> nav.navigate("edit?alarmId=$id") }
                    )
                }
                composable(
                    route = "edit?alarmId={alarmId}",
                    arguments = listOf(navArgument("alarmId") {
                        type = NavType.StringType
                        nullable = true
                    })
                ) { back ->
                    val id = back.arguments?.getString("alarmId")
                    AlarmEditScreen(
                        alarmId = id,
                        onSaved = {
                            nav.navigate("list") {
                                popUpTo("list") { inclusive = true }
                            }
                        },
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

/* ----------------------- 목록 화면 ----------------------- */

@Composable
private fun AlarmListScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(AlarmRepo.loadAll(ctx)) }

    // 예시 데이터 하나 자동 추가
    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            AlarmRepo.upsert(ctx, AlarmSpec(name = "물마시기"))
            items = AlarmRepo.loadAll(ctx)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("알람목록") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, null) }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(items, key = { it.id }) { a ->
                AlarmCard(
                    alarm = a,
                    onToggle = { en ->
                        AlarmRepo.toggle(ctx, a.id, en)
                        items = AlarmRepo.loadAll(ctx)
                    },
