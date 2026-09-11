package com.ironlog.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

private data class Lift(val id: Long, val exercise: String, val date: String, val weight: Float, val reps: Int)
private enum class UnitMode { KG, LB }
private const val PREFS = "ironlog"
private const val LIFTS = "lifts"
private const val ONBOARDING = "onboarding_done"
private const val DARK = "dark_mode"
private const val UNIT = "unit"

private object JsonBackup {
    fun encode(lifts: List<Lift>): String = "{\"version\":1,\"app\":\"IronLog\",\"lifts\":[" + lifts.joinToString(",") { "{\"id\":${it.id},\"exercise\":\"${it.exercise.replace("\\", "\\\\").replace("\"", "\\\"")}\",\"date\":\"${it.date}\",\"weight\":${it.weight},\"reps\":${it.reps}}" } + "]}"
    fun decode(raw: String): List<Lift> = Regex("\\{\\\"id\\\":(\\d+),\\\"exercise\\\":\\\"(.*?)\\\",\\\"date\\\":\\\"(.*?)\\\",\\\"weight\\\":([0-9.]+),\\\"reps\\\":(\\d+)\\}").findAll(raw).map { m -> Lift(m.groupValues[1].toLong(), m.groupValues[2].replace("\\\"", "\""), m.groupValues[3], m.groupValues[4].toFloat(), m.groupValues[5].toInt()) }.toList()
}

private class LiftViewModel(private val context: Context) : ViewModel() {
    private val prefs get() = context.getSharedPreferences(PREFS, 0)
    var lifts by mutableStateOf(load()); private set
    var darkMode by mutableStateOf(prefs.getBoolean(DARK, false)); private set
    var unit by mutableStateOf(UnitMode.valueOf(prefs.getString(UNIT, UnitMode.KG.name) ?: UnitMode.KG.name)); private set
    private fun load() = prefs.getString(LIFTS, "")!!.split("\n").filter { it.isNotBlank() }.mapNotNull { p -> p.split("|").takeIf { it.size == 5 }?.let { runCatching { Lift(it[0].toLong(), it[1], it[2], it[3].toFloat(), it[4].toInt()) }.getOrNull() } }.sortedByDescending { it.id }
    private fun save() { prefs.edit().putString(LIFTS, lifts.joinToString("\n") { "${it.id}|${it.exercise}|${it.date}|${it.weight}|${it.reps}" }).apply() }
    fun add(e: String, d: String, w: Float, r: Int) { lifts = listOf(Lift(System.currentTimeMillis(), e, d, w, r)) + lifts; save() }
    fun remove(id: Long) { lifts = lifts.filterNot { it.id == id }; save() }
    fun restore(data: List<Lift>) { lifts = data.sortedByDescending { it.id }; save() }
    fun toggleDark() { darkMode = !darkMode; prefs.edit().putBoolean(DARK, darkMode).apply() }
    fun setUnit(u: UnitMode) { unit = u; prefs.edit().putString(UNIT, u.name).apply() }
    fun displayWeight(kg: Float) = if (unit == UnitMode.KG) kg else kg * 2.20462f
    val totalVolume get() = lifts.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
    val uniqueDays get() = lifts.map { it.date }.distinct()
    val streak get() = run { var n = 0; val cal = Calendar.getInstance(); while (uniqueDays.contains(fmt.format(cal.time))) { n++; cal.add(Calendar.DAY_OF_YEAR, -1) }; n }
    val bestWeight get() = lifts.maxOfOrNull { it.weight } ?: 0f
    companion object { val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { val vm: LiftViewModel = viewModel(factory = object : ViewModelProvider.Factory { override fun <T : ViewModel> create(c: Class<T>) = LiftViewModel(applicationContext) as T }); IronLogTheme(vm.darkMode) { IronLogApp(vm) } } }
}

@Composable private fun IronLogApp(vm: LiftViewModel) {
    val context = LocalContext.current; var page by remember { mutableIntStateOf(0) }; var showAdd by remember { mutableStateOf(false) }; var intro by remember { mutableStateOf(!context.getSharedPreferences(PREFS, 0).getBoolean(ONBOARDING, false)) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(JsonBackup.encode(vm.lifts).toByteArray()) } } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> vm.restore(JsonBackup.decode(reader.readText())) } } }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (intro) Onboarding { context.getSharedPreferences(PREFS, 0).edit().putBoolean(ONBOARDING, true).apply(); intro = false }
        else Scaffold(containerColor = MaterialTheme.colorScheme.background, floatingActionButton = { if (page == 0) FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "إضافة") } }) { pad -> Column(Modifier.padding(pad).fillMaxSize()) { Header(); AnimatedContent(page, label = "navigation") { when (it) { 0 -> Summary(vm); 1 -> History(vm); else -> Settings(vm, { export.launch("ironlog-backup.json") }, { restore.launch(arrayOf("application/json")) }) } }; Spacer(Modifier.weight(1f)); NavigationBar { NavigationBarItem(page == 0, { page = 0 }, icon = { Icon(Icons.Default.Insights, null) }, label = { Text("ملخص") }); NavigationBarItem(page == 1, { page = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("السجل") }); NavigationBarItem(page == 2, { page = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("الإعدادات") }) } } }
        if (showAdd) AddDialog({ showAdd = false }) { e, d, w, r -> vm.add(e, d, w, r); showAdd = false }
    }
}

@Composable private fun Header() { Row(Modifier.padding(horizontal = 22.dp, vertical = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("IronLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.weight(1f)); Text("سجّل. تطوّر. كرّر.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun Onboarding(done: () -> Unit) { var step by remember { mutableIntStateOf(0) }; val titles = listOf("قوتك في مكان واحد", "سجّل كل إنجاز", "شوف تقدمك بوضوح"); val bodies = listOf("IronLog يساعدك تتابع أوزانك وتحافظ على الـ streak بدون تعقيد.", "أضف التمرين والوزن والتكرارات في ثواني، وخلّي بياناتك محفوظة على جهازك.", "ملخصات أسبوعية وشهرية، نسخ احتياطي JSON، وثيم يناسبك."); Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(88.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(32.dp)); AnimatedContent(step, label = "intro") { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(titles[it], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); Text(bodies[it], style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(40.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Box(Modifier.size(if (it == step) 24.dp else 8.dp, 8.dp).clip(RoundedCornerShape(8.dp)).background(if (it == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) } }; Spacer(Modifier.height(32.dp)); Button(onClick = { if (step == 2) done() else step++ }, modifier = Modifier.fillMaxWidth()) { Text(if (step == 2) "ابدأ الآن" else "التالي") } } } }

@Composable private fun Summary(vm: LiftViewModel) { var range by remember { mutableIntStateOf(7) }; val scoped = vm.lifts.filter { it.date >= LiftViewModel.fmt.format(Date(System.currentTimeMillis() - (range - 1) * 86400000L)) }; val pulse by animateFloatAsState(if (vm.lifts.isNotEmpty()) 1.04f else 1f, label = "pulse"); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("صباح القوة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("جاهز تكسر رقمك؟", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf(7 to "أسبوع", 30 to "شهر", 365 to "سنة").forEachIndexed { i, pair -> SegmentedButton(range == pair.first, { range = pair.first }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(pair.second) } } } }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("${vm.streak}", "يوم streak", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primaryContainer); StatCard("${scoped.size}", "تسجيل الفترة", Icons.Default.BarChart, MaterialTheme.colorScheme.tertiaryContainer) } }; item { Card(Modifier.scale(pulse).animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("حجم التدريب", fontWeight = FontWeight.Bold); Text("${"%.1f".format(vm.displayWeight(vm.totalVolume))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"} × تكرار", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary); Text("أعلى وزن: ${"%.1f".format(vm.displayWeight(vm.bestWeight))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; item { Text("آخر التمرينات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts.take(4), key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun StatCard(v: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(Modifier.width(160.dp), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp)) { Icon(icon, null); Spacer(Modifier.height(8.dp)); Text(v, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label) } } }

@Composable private fun History(vm: LiftViewModel) { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("سجل التمارين", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("كل إنجاز يبدأ بتسجيل واحد", color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts, key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun LiftRow(lift: Lift, vm: LiftViewModel) { var visible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { visible = true }; AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(lift.exercise, fontWeight = FontWeight.Bold); Text("${lift.date} • ${lift.reps} تكرار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${"%.1f".format(vm.displayWeight(lift.weight))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", fontWeight = FontWeight.Bold); IconButton(onClick = { vm.remove(lift.id) }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error) } } } } }

@Composable private fun Settings(vm: LiftViewModel, export: () -> Unit, restore: () -> Unit) { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("تحكم في تجربتك وبياناتك", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { SettingsCard(Icons.Default.DarkMode, "المظهر", "الوضع ${if (vm.darkMode) "الليلي" else "النهاري"}") { Switch(vm.darkMode, { vm.toggleDark() }) } }; item { Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Straighten, null); Spacer(Modifier.width(12.dp)); Column { Text("وحدة الوزن", fontWeight = FontWeight.Bold); Text("اختر الوحدة المفضلة", style = MaterialTheme.typography.bodySmall) } }; Spacer(Modifier.height(12.dp)); SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { SegmentedButton(vm.unit == UnitMode.KG, { vm.setUnit(UnitMode.KG) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("كيلو kg") }; SegmentedButton(vm.unit == UnitMode.LB, { vm.setUnit(UnitMode.LB) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("باوند lb") } } } } }; item { Text("البيانات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; item { SettingsCard(Icons.Default.Upload, "تصدير نسخة احتياطية", "ملف JSON قابل للحفظ والمشاركة") { TextButton(onClick = export) { Text("تصدير") } } }; item { SettingsCard(Icons.Default.Download, "استيراد نسخة احتياطية", "استرجاع تمارينك من ملف JSON") { TextButton(onClick = restore) { Text("استيراد") } } } } }

@Composable private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: @Composable () -> Unit) { Card(shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; action() } } }

@Composable private fun EmptyState() { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Text("لسه مفيش تسجيلات", fontWeight = FontWeight.Bold); Text("اضغط + وسجّل أول تمرين", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun AddDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Int) -> Unit) { var e by remember { mutableStateOf("") }; var d by remember { mutableStateOf(LiftViewModel.fmt.format(Date())) }; var w by remember { mutableStateOf("") }; var r by remember { mutableStateOf("8") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل تمرين", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(e, { e = it }, label = { Text("اسم التمرين") }, singleLine = true); OutlinedTextField(d, { d = it }, label = { Text("اليوم YYYY-MM-DD") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(w, { w = it }, Modifier.weight(1f), label = { Text("الوزن") }, singleLine = true); OutlinedTextField(r, { r = it }, Modifier.weight(1f), label = { Text("التكرارات") }, singleLine = true) } } }, confirmButton = { Button(enabled = e.isNotBlank() && w.toFloatOrNull() != null, onClick = { onSave(e, d, w.toFloat(), r.toIntOrNull() ?: 1) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }) }

@Composable private fun IronLogTheme(dark: Boolean, content: @Composable () -> Unit) { val light = lightColorScheme(primary = Color(0xFF0B5D46), onPrimary = Color.White, primaryContainer = Color(0xFFB8F0D8), secondaryContainer = Color(0xFFFFD9B8), tertiaryContainer = Color(0xFFD9E2FF), background = Color(0xFFF8FAF6)); val darkScheme = darkColorScheme(primary = Color(0xFF7BDCB8), onPrimary = Color(0xFF003829), primaryContainer = Color(0xFF00513E), secondaryContainer = Color(0xFF70451D)); val english = FontFamily(Font(com.ironlog.app.R.font.google_sans_flex)); val arabic = FontFamily(Font(com.ironlog.app.R.font.noto_sans_arabic)); MaterialTheme(colorScheme = if (dark) darkScheme else light, typography = Typography().run { copy(bodyLarge = bodyLarge.copy(fontFamily = arabic), bodyMedium = bodyMedium.copy(fontFamily = arabic), titleLarge = titleLarge.copy(fontFamily = english), headlineSmall = headlineSmall.copy(fontFamily = english), headlineMedium = headlineMedium.copy(fontFamily = english)) }, content = content) }
