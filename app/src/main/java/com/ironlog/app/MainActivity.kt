package com.ironlog.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

private data class Lift(val id: Long, val exercise: String, val date: String, val weight: Float, val reps: Int)

private class LiftViewModel(private val context: Context) : ViewModel() {
    var lifts by mutableStateOf(load())
        private set
    private fun load(): List<Lift> {
        val raw = context.getSharedPreferences("ironlog", 0).getString("lifts", "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { p -> p.split("|").takeIf { it.size == 5 }?.let { Lift(it[0].toLong(), it[1], it[2], it[3].toFloat(), it[4].toInt()) } }.sortedByDescending { it.id }
    }
    private fun save() { context.getSharedPreferences("ironlog", 0).edit().putString("lifts", lifts.joinToString("\n") { "${it.id}|${it.exercise}|${it.date}|${it.weight}|${it.reps}" }).apply() }
    fun add(exercise: String, date: String, weight: Float, reps: Int) { lifts = listOf(Lift(System.currentTimeMillis(), exercise, date, weight, reps)) + lifts; save() }
    fun remove(id: Long) { lifts = lifts.filterNot { it.id == id }; save() }
    val totalVolume get() = lifts.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
    val uniqueDays get() = lifts.map { it.date }.distinct()
    val streak get() = run { var n = 0; val cal = Calendar.getInstance(); while (uniqueDays.contains(fmt.format(cal.time))) { n++; cal.add(Calendar.DAY_OF_YEAR, -1) }; n }
    fun inLastDays(days: Int): List<Lift> { val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(days - 1)) }.timeInMillis; return lifts.filter { runCatching { fmt.parse(it.date)?.time ?: 0L }.getOrDefault(0L) >= start } }
    fun bestWeight(): Float = lifts.maxOfOrNull { it.weight } ?: 0f
    companion object { val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { IronLogTheme { val vm: LiftViewModel = viewModel(factory = object : ViewModelProvider.Factory { override fun <T : ViewModel> create(c: Class<T>) = LiftViewModel(applicationContext) as T }); IronLogApp(vm) } } }
}

@Composable private fun IronLogApp(vm: LiftViewModel) {
    var tab by remember { mutableIntStateOf(0) }; var showAdd by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Default.Add, "إضافة") } }) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(Modifier.width(10.dp)); Text("IronLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.weight(1f)); Text("سجّل. تطوّر. كرّر.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (tab == 0) Summary(vm) else History(vm)
                Spacer(Modifier.weight(1f)); NavigationBar { NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Insights, null) }, label = { Text("ملخص") }); NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("السجل") }) }
            }
        }
        if (showAdd) AddDialog({ showAdd = false }) { e, d, w, r -> vm.add(e, d, w, r); showAdd = false }
    }
}

@Composable private fun Summary(vm: LiftViewModel) {
    var range by remember { mutableIntStateOf(7) }; val scoped = vm.inLastDays(range); val scopedVolume = scoped.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("صباح القوة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("جاهز تكسر رقمك؟", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { SegmentedButton(range == 7, { range = 7 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("أسبوع") }; SegmentedButton(range == 30, { range = 30 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("شهر") }; SegmentedButton(range == 365, { range = 365 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("سنة") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("${vm.streak}", "يوم streak", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primaryContainer); StatCard("${scoped.size}", "تسجيل الفترة", Icons.Default.BarChart, MaterialTheme.colorScheme.tertiaryContainer) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallMetric("${"%.1f".format(scopedVolume)}", "حجم الفترة"); SmallMetric("${vm.bestWeight()} kg", "أعلى وزن") } }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("نشاط آخر 7 أيام", fontWeight = FontWeight.Bold); Text("${"%.1f".format(vm.totalVolume)} كجم × تكرار إجمالي", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); MiniChart(vm.lifts) } } }
        item { Text("آخر التمرينات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts.take(4), key = { it.id }) { LiftRow(it, {}) }
    }
}

@Composable private fun SmallMetric(value: String, label: String) { Card(Modifier.width(160.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun StatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(Modifier.width(160.dp), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp)) { Icon(icon, null, Modifier.size(24.dp)); Spacer(Modifier.height(10.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelLarge) } } }

@Composable private fun MiniChart(lifts: List<Lift>) { val values = (0..6).map { day -> lifts.filter { it.date == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - (6 - day) * 86400000L)) }.sumOf { it.weight.toDouble() }.toFloat() }; val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f); Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) { values.forEachIndexed { i, v -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.fillMaxWidth().height((70 * (v / max)).dp.coerceAtLeast(6.dp)).clip(RoundedCornerShape(8.dp)).background(if (i == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .35f))); Text(listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")[i], style = MaterialTheme.typography.labelSmall) } } } }

@Composable private fun History(vm: LiftViewModel) { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("سجل التمارين", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("كل إنجاز يبدأ بتسجيل واحد", color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts, key = { it.id }) { lift -> LiftRow(lift) { vm.remove(lift.id) } } } }

@Composable private fun LiftRow(lift: Lift, onDelete: () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(lift.exercise, fontWeight = FontWeight.Bold); Text("${lift.date}  •  ${lift.reps} تكرار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${lift.weight} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error) } } } }

@Composable private fun EmptyState() { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text("لسه مفيش تسجيلات", fontWeight = FontWeight.Bold); Text("اضغط + وسجّل أول تمرين", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun AddDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Int) -> Unit) { var exercise by remember { mutableStateOf("") }; var date by remember { mutableStateOf(LiftViewModel.fmt.format(Date())) }; var weightText by remember { mutableStateOf("") }; var reps by remember { mutableStateOf("8") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل تمرين", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(exercise, { exercise = it }, label = { Text("اسم التمرين") }, singleLine = true); OutlinedTextField(date, { date = it }, label = { Text("اليوم (YYYY-MM-DD)") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(weightText, { weightText = it }, Modifier.width(130.dp), label = { Text("الوزن kg") }, singleLine = true); OutlinedTextField(reps, { reps = it }, Modifier.width(130.dp), label = { Text("التكرارات") }, singleLine = true) } } }, confirmButton = { Button(enabled = exercise.isNotBlank() && weightText.toFloatOrNull() != null, onClick = { onSave(exercise, date, weightText.toFloat(), reps.toIntOrNull() ?: 1) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }) }

@Composable private fun IronLogTheme(content: @Composable () -> Unit) { val scheme = lightColorScheme(primary = Color(0xFF245B4A), onPrimary = Color.White, primaryContainer = Color(0xFFC8E9D9), secondaryContainer = Color(0xFFFFDDB5), tertiaryContainer = Color(0xFFD9E2FF), background = Color(0xFFF9FAF7)); MaterialTheme(colorScheme = scheme, typography = Typography(), content = content) }
