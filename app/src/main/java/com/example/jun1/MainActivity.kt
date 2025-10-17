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
import com.example.jun1.data.AlarmSpec
import com.example.jun1.data.AlarmRepo
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
private fun AlarmListScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(emptyList<AlarmSpec>()) }

    LaunchedEffect(Unit) {
        items = AlarmRepo.loadAll(ctx)  // 예외는 Repo에서 처리
        if (items.isEmpty()) {
            // 최초 실행시 샘플 하나 생성
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
                    onClick = { onEdit(a.id) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmSpec,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    alarm.name.ifBlank { "알람" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                val timeRange = "%02d:%02d ~ %02d:%02d".format(
                    alarm.startHour, alarm.startMinute, alarm.endHour, alarm.endMinute
                )
                Text(timeRange, style = MaterialTheme.typography.bodyMedium)
                Text("간격: ${alarm.intervalMinutes}분", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Alarm, null)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (alarm.enabled) "ON" else "OFF")
                    Spacer(Modifier.width(6.dp))
                    Switch(checked = alarm.enabled, onCheckedChange = onToggle)
                }
            }
        }
    }
}

/* ----------------------- 수정 화면 ----------------------- */

@Composable
private fun AlarmEditScreen(
    alarmId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var spec by remember {
        mutableStateOf(
            if (alarmId.isNullOrBlank())
                AlarmSpec()
            else
                AlarmRepo.loadAll(ctx).firstOrNull { it.id == alarmId } ?: AlarmSpec()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알람수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.List, "back") }
                },
                actions = {
                    IconButton(onClick = {
                        AlarmRepo.delete(ctx, spec.id)
                        onBack()
                    }) { Icon(Icons.Filled.Add, "delete") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = spec.name,
                onValueChange = { spec = spec.copy(name = it) },
                label = { Text("알람이름") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("간격")
                Spacer(Modifier.width(12.dp))
                IntervalDropdown(spec.intervalMinutes) { v ->
                    spec = spec.copy(intervalMinutes = v)
                }
            }

            Text("볼륨", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = spec.volumePercent / 100f,
                    onValueChange = { spec = spec.copy(volumePercent = (it * 100).toInt()) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text("${spec.volumePercent}%")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("지속")
                Spacer(Modifier.width(12.dp))
                RingDropdown(spec.ringSeconds) { v -> spec = spec.copy(ringSeconds = v) }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { AlarmRepo.upsert(ctx, spec); onSaved() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("저장") }
        }
    }
}

/* 필요 시 사용하던 TimeField 구현은 나중에 추가하세요 */
@Composable
private fun TimeField(h: Int, m: Int, onChange: (Int, Int) -> Unit) { /* ... */ }

/* ----------------------- 드롭다운 ----------------------- */

@Composable
private fun IntervalDropdown(cur: Int, onSelect: (Int) -> Unit) {
    val opts = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    var exp by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(
            value = "$cur",
            onValueChange = {},
            readOnly = true,
            label = { Text("분") },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            opts.forEach { v ->
                DropdownMenuItem(
                    text = { Text("$v") },
                    onClick = { onSelect(v); exp = false }
                )
            }
        }
    }
}

@Composable
private fun RingDropdown(cur: Int, onSelect: (Int) -> Unit) {
    val opts = listOf(5, 10, 30, 60, 180)
    var exp by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(
            value = "$cur",
            onValueChange = {},
            readOnly = true,
            label = { Text("초") },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            opts.forEach { v ->
                DropdownMenuItem(
                    text = { Text("$v") },
                    onClick = { onSelect(v); exp = false }
                )
            }
        }
    }
}
