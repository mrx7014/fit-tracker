package com.ironlog.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.isSystemInDarkTheme
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
private data class Achievement(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val unlocked: Boolean)
private enum class UnitMode { KG, LB }
private enum class ThemeMode { SYSTEM, LIGHT, DARK }
private const val PREFS = "ironlog"
private const val LIFTS = "lifts"
private const val ONBOARDING = "onboarding_done"
private const val DARK = "dark_mode"
private const val UNIT = "unit"
private const val REMINDER = "reminder"
private const val REMINDER_CHANNEL = "ironlog_reminders"
private const val THEME = "theme_mode"
private const val DYNAMIC = "dynamic_color"
private const val ACCENT = "accent_color"

private object JsonBackup {
    fun encode(lifts: List<Lift>): String = "{\"version\":1,\"app\":\"IronLog\",\"lifts\":[" + lifts.joinToString(",") { "{\"id\":${it.id},\"exercise\":\"${it.exercise.replace("\\", "\\\\").replace("\"", "\\\"")}\",\"date\":\"${it.date}\",\"weight\":${it.weight},\"reps\":${it.reps}}" } + "]}"
    fun decode(raw: String): List<Lift> = Regex("\\{\\\"id\\\":(\\d+),\\\"exercise\\\":\\\"(.*?)\\\",\\\"date\\\":\\\"(.*?)\\\",\\\"weight\\\":([0-9.]+),\\\"reps\\\":(\\d+)\\}").findAll(raw).map { m -> Lift(m.groupValues[1].toLong(), m.groupValues[2].replace("\\\"", "\""), m.groupValues[3], m.groupValues[4].toFloat(), m.groupValues[5].toInt()) }.toList()
}

private class LiftViewModel(private val context: Context) : ViewModel() {
    private val prefs get() = context.getSharedPreferences(PREFS, 0)
    var lifts by mutableStateOf(load()); private set
    var darkMode by mutableStateOf(prefs.getBoolean(DARK, false)); private set
    var unit by mutableStateOf(UnitMode.valueOf(prefs.getString(UNIT, UnitMode.KG.name) ?: UnitMode.KG.name)); private set
    var reminders by mutableStateOf(prefs.getBoolean(REMINDER, false)); private set
    var themeMode by mutableStateOf(ThemeMode.valueOf(prefs.getString(THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)); private set
    var dynamicColors by mutableStateOf(prefs.getBoolean(DYNAMIC, true)); private set
    var accent by mutableStateOf(prefs.getInt(ACCENT, 0)); private set
    private fun load() = prefs.getString(LIFTS, "")!!.split("\n").filter { it.isNotBlank() }.mapNotNull { p -> p.split("|").takeIf { it.size == 5 }?.let { runCatching { Lift(it[0].toLong(), it[1], it[2], it[3].toFloat(), it[4].toInt()) }.getOrNull() } }.sortedByDescending { it.id }
    private fun save() { prefs.edit().putString(LIFTS, lifts.joinToString("\n") { "${it.id}|${it.exercise}|${it.date}|${it.weight}|${it.reps}" }).apply() }
    fun add(e: String, d: String, w: Float, r: Int) { lifts = listOf(Lift(System.currentTimeMillis(), e, d, w, r)) + lifts; save() }
    fun remove(id: Long) { lifts = lifts.filterNot { it.id == id }; save() }
    fun restore(data: List<Lift>) { lifts = data.sortedByDescending { it.id }; save() }
    fun toggleDark() { darkMode = !darkMode; prefs.edit().putBoolean(DARK, darkMode).apply() }
    fun updateUnit(u: UnitMode) { unit = u; prefs.edit().putString(UNIT, u.name).apply() }
    fun updateReminders(enabled: Boolean) { reminders = enabled; prefs.edit().putBoolean(REMINDER, enabled).apply(); if (enabled) ReminderManager.schedule(context) else ReminderManager.cancel(context) }
    fun updateTheme(mode: ThemeMode) { themeMode = mode; prefs.edit().putString(THEME, mode.name).apply() }
    fun updateDynamic(enabled: Boolean) { dynamicColors = enabled; prefs.edit().putBoolean(DYNAMIC, enabled).apply() }
    fun updateAccent(index: Int) { accent = index; prefs.edit().putInt(ACCENT, index).apply() }
    fun displayWeight(kg: Float) = if (unit == UnitMode.KG) kg else kg * 2.20462f
    val totalVolume get() = lifts.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
    val uniqueDays get() = lifts.map { it.date }.distinct()
    val streak get() = run { var n = 0; val cal = Calendar.getInstance(); while (uniqueDays.contains(fmt.format(cal.time))) { n++; cal.add(Calendar.DAY_OF_YEAR, -1) }; n }
    val bestWeight get() = lifts.maxOfOrNull { it.weight } ?: 0f
    val achievements get() = listOf(
        Achievement("البداية", "سجّل أول تمرين", Icons.Default.Flag, lifts.isNotEmpty()),
        Achievement("أسبوع كامل", "حافظت على streak لمدة 7 أيام", Icons.Default.LocalFireDepartment, streak >= 7),
        Achievement("عشر جلسات", "أكملت 10 تسجيلات", Icons.Default.EmojiEvents, lifts.size >= 10),
        Achievement("طن من الجهد", "تخطيت 1000 كجم × تكرار", Icons.Default.FitnessCenter, totalVolume >= 1000f),
        Achievement("متعدد المواهب", "جرّبت 5 تمارين مختلفة", Icons.Default.Stars, lifts.distinctBy { it.exercise }.size >= 5)
    )
    companion object { val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US) }
}

private object ReminderManager {
    fun schedule(context: Context) { val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "IronLog reminders", NotificationManager.IMPORTANCE_DEFAULT)); val intent = PendingIntent.getBroadcast(context, 77, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val first = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis; alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, intent) }
    fun cancel(context: Context) { val intent = PendingIntent.getBroadcast(context, 77, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(intent) }
}

class ReminderReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent?) { val notification = android.app.Notification.Builder(context, REMINDER_CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("IronLog").setContentText("حافظ على الاستريك وسجّل تمرينك اليوم").setAutoCancel(true).build(); (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(77, notification) } }

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { val vm: LiftViewModel = viewModel(factory = object : ViewModelProvider.Factory { override fun <T : ViewModel> create(c: Class<T>) = LiftViewModel(applicationContext) as T }); IronLogTheme(vm) { IronLogApp(vm) } } }
}

@Composable private fun IronLogApp(vm: LiftViewModel) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var intro by remember { mutableStateOf(!context.getSharedPreferences(PREFS, 0).getBoolean(ONBOARDING, false)) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(JsonBackup.encode(vm.lifts).toByteArray()) } } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> vm.restore(JsonBackup.decode(reader.readText())) } } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (intro) {
            Onboarding { context.getSharedPreferences(PREFS, 0).edit().putBoolean(ONBOARDING, true).apply(); intro = false }
        } else {
            BackHandler(enabled = page != 0) { page = if (page == 4) 3 else 0 }
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { AppNavigation(page) { page = it } },
                floatingActionButton = { if (page == 0) FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.padding(bottom = 8.dp), containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "إضافة") } },
                floatingActionButtonPosition = FabPosition.End
            ) { pad ->
                Column(Modifier.padding(pad).fillMaxSize()) {
                    Header()
                    AnimatedContent(page, label = "navigation") { current ->
                        when (current) {
                            0 -> Summary(vm)
                            1 -> History(vm)
                            2 -> StatsScreen(vm)
                            3 -> Settings(vm, { export.launch("ironlog-backup.json") }, { restore.launch(arrayOf("application/json")) }, { page = 4 }, { enabled -> if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch("android.permission.POST_NOTIFICATIONS"); vm.updateReminders(enabled) })
                            else -> AboutScreen()
                        }
                    }
                }
            }
        }
        if (showAdd) AddDialog({ showAdd = false }) { e, d, w, r -> vm.add(e, d, w, r); showAdd = false }
    }
}

@Composable private fun AppNavigation(page: Int, onPage: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(page == 0, { onPage(0) }, icon = { Icon(Icons.Default.Insights, null) }, label = { Text("ملخص") })
        NavigationBarItem(page == 1, { onPage(1) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("السجل") })
        NavigationBarItem(page == 2, { onPage(2) }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("إحصائيات") })
        NavigationBarItem(page == 3, { onPage(3) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("الإعدادات") })
    }
}

@Composable private fun Header() { Row(Modifier.padding(horizontal = 22.dp, vertical = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("IronLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.weight(1f)); Text("سجّل. تطوّر. كرّر.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun Onboarding(done: () -> Unit) { var step by remember { mutableIntStateOf(0) }; val titles = listOf("قوتك في مكان واحد", "سجّل كل إنجاز", "شوف تقدمك بوضوح"); val bodies = listOf("IronLog يساعدك تتابع أوزانك وتحافظ على الـ streak بدون تعقيد.", "أضف التمرين والوزن والتكرارات في ثواني، وخلّي بياناتك محفوظة على جهازك.", "ملخصات أسبوعية وشهرية، نسخ احتياطي JSON، وثيم يناسبك."); Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(88.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(32.dp)); AnimatedContent(step, label = "intro") { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(titles[it], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); Text(bodies[it], style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(40.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Box(Modifier.size(if (it == step) 24.dp else 8.dp, 8.dp).clip(RoundedCornerShape(8.dp)).background(if (it == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) } }; Spacer(Modifier.height(32.dp)); Button(onClick = { if (step == 2) done() else step++ }, modifier = Modifier.fillMaxWidth()) { Text(if (step == 2) "ابدأ الآن" else "التالي") } } } }

@Composable private fun Summary(vm: LiftViewModel) { var range by remember { mutableIntStateOf(7) }; val scoped = vm.lifts.filter { it.date >= LiftViewModel.fmt.format(Date(System.currentTimeMillis() - (range - 1) * 86400000L)) }; val pulse by animateFloatAsState(if (vm.lifts.isNotEmpty()) 1.04f else 1f, label = "pulse"); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("صباح القوة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("جاهز تكسر رقمك؟", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf(7 to "أسبوع", 30 to "شهر", 365 to "سنة").forEachIndexed { i, pair -> SegmentedButton(range == pair.first, { range = pair.first }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(pair.second) } } } }; item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { StatCard(Modifier.fillMaxWidth(), "${vm.streak}", "يوم streak", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primaryContainer) }; Box(Modifier.weight(1f)) { StatCard(Modifier.fillMaxWidth(), "${scoped.size}", "تسجيل الفترة", Icons.Default.BarChart, MaterialTheme.colorScheme.tertiaryContainer) } } }; item { Card(Modifier.scale(pulse).animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("حجم التدريب", fontWeight = FontWeight.Bold); Text("${"%.1f".format(vm.displayWeight(vm.totalVolume))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"} × تكرار", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary); Text("أعلى وزن: ${"%.1f".format(vm.displayWeight(vm.bestWeight))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; item { Text("آخر التمرينات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts.take(4), key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun StatCard(modifier: Modifier = Modifier, v: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(modifier, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(14.dp)) { Icon(icon, null); Spacer(Modifier.height(8.dp)); Text(v, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, maxLines = 1) } } }

@Composable private fun History(vm: LiftViewModel) { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("سجل التمارين", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("كل إنجاز يبدأ بتسجيل واحد", color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts, key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun StatsScreen(vm: LiftViewModel) { val points = vm.lifts.groupBy { it.date }.toSortedMap().entries.toList().takeLast(14); val max = (points.maxOfOrNull { it.value.maxOf { lift -> lift.weight } } ?: 1f).coerceAtLeast(1f); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text("إحصائيات متقدمة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("تطور أوزانك وإنجازاتك", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(18.dp)) { Text("منحنى الوزن — آخر ${points.size} يوم", fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); WeightChart(points.flatMap { it.value.map { lift -> lift.weight } }, max, vm) } } }; item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { StatCard(Modifier.fillMaxWidth(), "${vm.lifts.distinctBy { it.exercise }.size}", "تمارين مختلفة", Icons.Default.FitnessCenter, MaterialTheme.colorScheme.primaryContainer) }; Box(Modifier.weight(1f)) { StatCard(Modifier.fillMaxWidth(), "${"%.1f".format(vm.displayWeight(vm.bestWeight))}", "أعلى وزن", Icons.Default.TrendingUp, MaterialTheme.colorScheme.tertiaryContainer) } } }; item { Text("الإنجازات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(vm.achievements) { (title, description, icon, unlocked) -> AchievementCard(title, description, icon, unlocked) }; item { Text("أفضل أوزان حسب التمرين", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(vm.lifts.groupBy { it.exercise }.entries.sortedByDescending { it.value.maxOf { lift -> lift.weight } }.take(8)) { (exercise, lifts) -> ListItem(headlineContent = { Text(exercise, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${lifts.size} تسجيلات") }, trailingContent = { Text("${"%.1f".format(vm.displayWeight(lifts.maxOf { it.weight }))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", fontWeight = FontWeight.Bold) }) } } }

@Composable private fun AchievementCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unlocked: Boolean) { Card(colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(32.dp), tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (unlocked) "مفتوح" else "مقفل", color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold) } } }

@Composable private fun WeightChart(weights: List<Float>, max: Float, vm: LiftViewModel) { val lineColor = MaterialTheme.colorScheme.primary; val dotColor = MaterialTheme.colorScheme.tertiary; Canvas(Modifier.fillMaxWidth().height(180.dp)) { if (weights.size > 1) { val step = size.width / (weights.size - 1); val path = androidx.compose.ui.graphics.Path(); weights.forEachIndexed { i, value -> val x = i * step; val y = size.height - (value / max) * size.height; if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f, cap = StrokeCap.Round)); weights.forEachIndexed { i, value -> drawCircle(dotColor, 6f, androidx.compose.ui.geometry.Offset(i * step, size.height - (value / max) * size.height)) } } } }

@Composable private fun AboutScreen() { val arabic = Locale.getDefault().language == "ar"; val uri = "https://github.com/mrx7014/ironlog-android"; val uriHandler = LocalUriHandler.current; LazyColumn(contentPadding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Icon(Icons.Default.FitnessCenter, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary); Text(if (arabic) "عن IronLog" else "About IronLog", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(if (arabic) "IronLog هو تطبيق بسيط وخصوصي لتسجيل أوزان تمارين المقاومة ومتابعة التطور." else "IronLog is a simple, private strength-training log built to help you track progress.", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }; item { Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text(if (arabic) "المطور" else "Developer", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(if (arabic) "تم تطوير هذا التطبيق بواسطة صاحب المشروع لمساعدة الرياضيين على الاستمرارية وبناء عادات أفضل." else "Built by the project owner to help athletes stay consistent and build better habits."); Spacer(Modifier.height(10.dp)); Text(if (arabic) "الإصدار 1.0.0 • Material 3 Expressive" else "Version 1.0.0 • Material 3 Expressive", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; item { OutlinedButton(onClick = { uriHandler.openUri(uri) }) { Icon(Icons.Default.Code, null); Spacer(Modifier.width(8.dp)); Text(if (arabic) "مستودع GitHub" else "GitHub repository") } } } }

@Composable private fun LiftRow(lift: Lift, vm: LiftViewModel) { var visible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { visible = true }; AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(lift.exercise, fontWeight = FontWeight.Bold); Text("${lift.date} • ${lift.reps} تكرار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${"%.1f".format(vm.displayWeight(lift.weight))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", fontWeight = FontWeight.Bold); IconButton(onClick = { vm.remove(lift.id) }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error) } } } } }

@Composable private fun UpdateCard() {
    val uriHandler = LocalUriHandler.current
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("التحديثات", fontWeight = FontWeight.Bold); Text("الإصدار الحالي 1.0.0", style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { uriHandler.openUri("https://github.com/mrx7014/ironlog-android/releases/latest") }) { Text("فحص") }
            }
            Spacer(Modifier.height(8.dp))
            Text("آخر changelog: إنجازات، إحصائيات، تذكيرات، وثيمات جديدة.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { uriHandler.openUri("https://github.com/mrx7014/ironlog-android/releases") }) { Text("فتح صفحة الإصدارات") }
        }
    }
}

@Composable private fun Settings(vm: LiftViewModel, export: () -> Unit, restore: () -> Unit, about: () -> Unit, reminder: (Boolean) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("تحكم في تجربتك وبياناتك", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("المظهر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(ThemeMode.SYSTEM to "النظام", ThemeMode.LIGHT to "فاتح", ThemeMode.DARK to "داكن").forEachIndexed { i, pair ->
                            SegmentedButton(vm.themeMode == pair.first, { vm.updateTheme(pair.first) }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(pair.second) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SettingsCard(Icons.Default.Palette, "Dynamic Color", "استخدم ألوان النظام أو اختر لونًا مخصصًا") { Switch(vm.dynamicColors, { vm.updateDynamic(it) }) }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(Color(0xFF0B5D46), Color(0xFF6750A4), Color(0xFF9C4146), Color(0xFF006874)).forEachIndexed { i, color ->
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(color).clickable { vm.updateAccent(i) }, contentAlignment = Alignment.Center) { if (vm.accent == i) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
        item { SettingsCard(Icons.Default.Notifications, "تذكير يومي", "تنبيه الساعة 8 مساءً للحفاظ على الاستريك") { Switch(vm.reminders, reminder) } }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Straighten, null); Spacer(Modifier.width(12.dp)); Column { Text("وحدة الوزن", fontWeight = FontWeight.Bold); Text("اختر الوحدة المفضلة", style = MaterialTheme.typography.bodySmall) } }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { SegmentedButton(vm.unit == UnitMode.KG, { vm.updateUnit(UnitMode.KG) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("كيلو kg") }; SegmentedButton(vm.unit == UnitMode.LB, { vm.updateUnit(UnitMode.LB) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("باوند lb") } }
                }
            }
        }
        item { UpdateCard() }
        item { Text("البيانات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { SettingsCard(Icons.Default.Upload, "تصدير نسخة احتياطية", "ملف JSON قابل للحفظ والمشاركة") { TextButton(onClick = export) { Text("تصدير") } } }
        item { SettingsCard(Icons.Default.Download, "استيراد نسخة احتياطية", "استرجاع تمارينك من ملف JSON") { TextButton(onClick = restore) { Text("استيراد") } } }
        item { SettingsCard(Icons.Default.Info, "عن IronLog", "المطور، الإصدار، ورابط GitHub") { TextButton(onClick = about) { Text("فتح") } } }
    }
}

@Composable private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: @Composable () -> Unit) { Card(shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; action() } } }

@Composable private fun EmptyState() { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Text("لسه مفيش تسجيلات", fontWeight = FontWeight.Bold); Text("اضغط + وسجّل أول تمرين", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun AddDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Int) -> Unit) { var e by remember { mutableStateOf("") }; var d by remember { mutableStateOf(LiftViewModel.fmt.format(Date())) }; var w by remember { mutableStateOf("") }; var r by remember { mutableStateOf("8") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل تمرين", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(e, { e = it }, label = { Text("اسم التمرين") }, singleLine = true); OutlinedTextField(d, { d = it }, label = { Text("اليوم YYYY-MM-DD") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(w, { w = it }, Modifier.weight(1f), label = { Text("الوزن") }, singleLine = true); OutlinedTextField(r, { r = it }, Modifier.weight(1f), label = { Text("التكرارات") }, singleLine = true) } } }, confirmButton = { Button(enabled = e.isNotBlank() && w.toFloatOrNull() != null, onClick = { onSave(e, d, w.toFloat(), r.toIntOrNull() ?: 1) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }) }

@Composable private fun IronLogTheme(vm: LiftViewModel, content: @Composable () -> Unit) { val systemDark = isSystemInDarkTheme(); val dark = when (vm.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }; val palettes = listOf(Color(0xFF0B5D46), Color(0xFF6750A4), Color(0xFF9C4146), Color(0xFF006874)); val accent = palettes.getOrElse(vm.accent) { palettes.first() }; val light = lightColorScheme(primary = accent, onPrimary = Color.White, primaryContainer = accent.copy(alpha = .22f), secondaryContainer = Color(0xFFFFD9B8), tertiaryContainer = Color(0xFFD9E2FF), background = Color(0xFFF8FAF6)); val darkScheme = darkColorScheme(primary = accent.copy(alpha = .9f), onPrimary = Color.White, primaryContainer = accent.copy(alpha = .5f), secondaryContainer = Color(0xFF70451D)); val scheme = if (vm.dynamicColors && android.os.Build.VERSION.SDK_INT >= 31) { val ctx = LocalContext.current; if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) } else if (dark) darkScheme else light; val english = FontFamily(Font(com.ironlog.app.R.font.google_sans_flex)); val arabic = FontFamily(Font(com.ironlog.app.R.font.noto_sans_arabic)); MaterialTheme(colorScheme = scheme, typography = Typography().run { copy(bodyLarge = bodyLarge.copy(fontFamily = arabic), bodyMedium = bodyMedium.copy(fontFamily = arabic), titleLarge = titleLarge.copy(fontFamily = english), headlineSmall = headlineSmall.copy(fontFamily = english), headlineMedium = headlineMedium.copy(fontFamily = english)) }, content = content) }
