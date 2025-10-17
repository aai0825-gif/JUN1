@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.jun1

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
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
import com.example.jun1.alarm.AlarmPlanner
import com.example.jun1.data.AlarmRepo
import com.example.jun1.model.AlarmSpec
import com.example.jun1.model.ScheduleMode
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot() } } // 이름 충돌 방지: AppRoot로 변경
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val dest by nav.currentBackStackEntryAsState()
                val route = dest?.destination?.route ?: "list"
                NavigationBarItem(
                    selected = route.startsWith("list"),
                    onClick = { nav.navigate("list") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.List, null) },
                    label = { Text("알람목록") }
                )
                NavigationBarItem(
                    selected = route.startsWith("edit"),
                    onClick = { nav.navigate("edit?alarmId=") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Alarm, null) },
                    label = { Text("알람수정") }
                )
            }
        }
    ) { pad ->
        NavHost(navController = nav, startDestination = "list", modifier = Modifier.padding(pad)) {
            composable("list") {
                AlarmListScreen(
                    onAdd = { nav.navigate("edit?alarmId=") },
                    onEdit = { id -> nav.navigate("edit?alarmId=$id") }
                )
            }
            composable(
                "edit?alarmId={alarmId}",
                arguments = listOf(navArgument("alarmId"){ type = NavType.StringType; nullable = true })
            ) { back ->
                val id = back.arguments?.getString("alarmId")
                AlarmEditScreen(
                    alarmId = id,
                    onSaved = {
                        // 저장 후 전체 스케줄 재설정
                        AlarmPlanner.scheduleAll(LocalContext.current)
                        nav.navigate("list"){ popUpTo("list"){ inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

/* ---------------- 리스트 ---------------- */

@Composable
private fun AlarmListScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(AlarmRepo.loadAll(ctx)) }
    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            AlarmRepo.upsert(ctx, AlarmSpec(name = "물마시기"))
            items = AlarmRepo.loadAll(ctx)
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("알람목록") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, null) } }
    ){ pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(items, key={it.id}){ a ->
                ElevatedCard(
                    onClick = { onEdit(a.id) },
                    modifier = Modifier.fillMaxWidth()
                ){
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(a.name, style=MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            val range = "%02d:%02d ~ %02d:%02d".format(a.startHour,a.startMinute,a.endHour,a.endMinute)
                            val extra = if (a.scheduleMode==ScheduleMode.RANGE)
                                "간격: ${a.intervalMinutes}분"
                            else "시각: ${a.times.joinToString { "%02d:%02d".format(it/60,it%60) }}"
                            Text(range, style=MaterialTheme.typography.bodyMedium)
                            Text(extra, style=MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally){
                            Icon(Icons.Filled.Alarm, null); Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically){
                                Text(if(a.enabled)"ON" else "OFF"); Spacer(Modifier.width(6.dp))
                                Switch(
                                    checked=a.enabled,
                                    onCheckedChange={
                                        AlarmRepo.toggle(ctx, a.id, it)
                                        items = AlarmRepo.loadAll(ctx)
                                        AlarmPlanner.scheduleAll(ctx)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ---------------- 편집 ---------------- */

@Composable
private fun AlarmEditScreen(alarmId:String?, onSaved:()->Unit, onBack:()->Unit){
    val ctx = LocalContext.current
    var spec by remember { mutableStateOf(alarmId?.let { AlarmRepo.find(ctx, it) } ?: AlarmSpec()) }

    // 벨소리 선택
    val ringPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = res.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            spec = spec.copy(ringtoneUri = uri?.toString())
        }
    }

    Scaffold(
        topBar={ TopAppBar(
            title={Text("알람수정")},
            navigationIcon={ IconButton(onClick=onBack){ Icon(Icons.Filled.List, "back") } },
            actions={
                IconButton(onClick={ AlarmRepo.delete(ctx, spec.id); AlarmPlanner.cancel(ctx, spec.id); onBack() }){
                    Icon(Icons.Filled.Delete,"delete")
                }
            }
        ) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)){
            OutlinedTextField(value=spec.name, onValueChange={ spec = spec.copy(name=it) }, label={ Text("알람이름") }, modifier=Modifier.fillMaxWidth())

            // 요일 선택
            Text("반복요일", fontWeight = FontWeight.Bold)
            val labels = listOf("월","화","수","목","금","토","일")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)){
                labels.forEachIndexed { idx, label ->
                    val day = idx + 1
                    FilterChip(
                        selected = day in spec.daysEnabled,
                        onClick = {
                            val s = spec.daysEnabled.toMutableSet()
                            if (day in s) s.remove(day) else s.add(day)
                            spec = spec.copy(daysEnabled = s)
                        },
                        label = { Text(label) }
                    )
                }
            }

            // 반복 방식: FilterChip 2개로 토글
            Text("반복 방식", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = spec.scheduleMode == ScheduleMode.RANGE,
                    onClick = { spec = spec.copy(scheduleMode = ScheduleMode.RANGE) },
                    label = { Text("구간 반복") }
                )
                FilterChip(
                    selected = spec.scheduleMode == ScheduleMode.TIMES,
                    onClick = { spec = spec.copy(scheduleMode = ScheduleMode.TIMES) },
                    label = { Text("특정 시각") }
                )
            }

            if (spec.scheduleMode == ScheduleMode.RANGE) {
                TimeRangeEditor(
                    startH = spec.startHour, startM = spec.startMinute,
                    endH = spec.endHour, endM = spec.endMinute,
                    onChange = { sh, sm, eh, em -> spec = spec.copy(startHour = sh, startMinute = sm, endHour = eh, endMinute = em) }
                )
                Row(verticalAlignment = Alignment.CenterVertically){
                    Text("간격"); Spacer(Modifier.width(12.dp))
                    IntervalDropdown(spec.intervalMinutes){ v -> spec = spec.copy(intervalMinutes=v) }
                }
            } else {
                TimesEditor(
                    times = spec.times,
                    onAdd = { h, m ->
                        val mins = h*60+m
                        spec = spec.copy(times = (spec.times + mins).distinct().sorted())
                    },
                    onRemove = { mins -> spec = spec.copy(times = spec.times.filterNot { it == mins }) }
                )
            }

            // 벨소리
            Text("벨소리", fontWeight = FontWeight.Bold)
            val ringName = remember(spec.ringtoneUri){
                spec.ringtoneUri?.let { u -> RingtoneManager.getRingtone(ctx, Uri.parse(u))?.getTitle(ctx) } ?: "시스템 기본"
            }
            OutlinedButton(onClick={
                val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, spec.ringtoneUri?.let(Uri::parse))
                }
                ringPicker.launch(i)
            }){ Text(ringName) }

            // 볼륨/지속
            Text("볼륨", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically){
                Slider(value=spec.volumePercent/100f, onValueChange={ spec = spec.copy(volumePercent=(it*100).toInt()) }, modifier=Modifier.weight(1f))
                Spacer(Modifier.width(8.dp)); Text("${spec.volumePercent}%")
            }
            Row(verticalAlignment = Alignment.CenterVertically){ Text("지속"); Spacer(Modifier.width(12.dp)); RingDropdown(spec.ringSeconds){ v -> spec = spec.copy(ringSeconds=v)} }

            Spacer(Modifier.weight(1f))
            Button(onClick={ AlarmRepo.upsert(ctx, spec); onSaved() }, modifier=Modifier.fillMaxWidth()){ Text("저장") }
        }
    }
}

/* ---- 소품들 ---- */

@Composable
private fun TimeRangeEditor(
    startH: Int, startM: Int,
    endH: Int, endM: Int,
    onChange: (Int, Int, Int, Int) -> Unit
) {
    val ctx = LocalContext.current
    fun pick(h: Int, m: Int, cb:(Int,Int)->Unit) {
        TimePickerDialog(ctx, { _, hh, mm -> cb(hh, mm) }, h, m, true).show()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { pick(startH,startM){hh,mm-> onChange(hh,mm,endH,endM)} }) {
            Text("%02d:%02d".format(startH,startM))
        }
        Spacer(Modifier.width(12.dp)); Text("~"); Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = { pick(endH,endM){hh,mm-> onChange(startH,startM,hh,mm)} }) {
            Text("%02d:%02d".format(endH,endM))
        }
    }
}

@Composable
private fun TimesEditor(
    times: List<Int>,
    onAdd: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val ctx = LocalContext.current
    OutlinedButton(onClick = {
        val now = Calendar.getInstance()
        TimePickerDialog(ctx, { _, hh, mm -> onAdd(hh, mm) },
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true
        ).show()
    }) { Text("시각 추가") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (times.isEmpty()) {
            Text("선택된 시각 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            times.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("%02d:%02d".format(t/60, t%60), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onRemove(t) }) { Icon(Icons.Filled.Delete, contentDescription = "삭제") }
                }
            }
        }
    }
}

@Composable
private fun IntervalDropdown(cur:Int, onSelect:(Int)->Unit){
    val opts = listOf(5,10,15,30,45,60,90,120); var exp by remember{ mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded=exp, onExpandedChange={exp=it}){
        OutlinedTextField(value="$cur", onValueChange={}, readOnly=true, label={Text("분")}, modifier=Modifier.menuAnchor())
        ExposedDropdownMenu(expanded=exp, onDismissRequest={exp=false}){
            opts.forEach{ v -> DropdownMenuItem(text={Text("$v")}, onClick={ onSelect(v); exp=false }) }
        }
    }
}

@Composable
private fun RingDropdown(cur:Int, onSelect:(Int)->Unit){
    val opts = listOf(5,10,30,60,180); var exp by remember{ mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded=exp, onExpandedChange={exp=it}){
        OutlinedTextField(value="$cur", onValueChange={}, readOnly=true, label={Text("초")}, modifier=Modifier.menuAnchor())
        ExposedDropdownMenu(expanded=exp, onDismissRequest={exp=false}){
            opts.forEach{ v -> DropdownMenuItem(text={Text("$v")}, onClick={ onSelect(v); exp=false }) }
        }
    }
}
