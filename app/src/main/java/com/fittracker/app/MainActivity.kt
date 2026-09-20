package com.fittracker.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private data class Lift(val id: Long, val exercise: String, val date: String, val weight: Float, val reps: Int)
private data class Achievement(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val unlocked: Boolean)
private enum class UnitMode { KG, LB }
private enum class ThemeMode { SYSTEM, LIGHT, DARK }
private enum class AppLanguage { SYSTEM, ENGLISH, ARABIC }
private const val PREFS = "fittracker"
private const val LIFTS = "lifts"
private const val ONBOARDING = "onboarding_done"
private const val USER_NAME = "user_name"
private const val DARK = "dark_mode"
private const val UNIT = "unit"
private const val REMINDER = "reminder"
private const val REMINDER_CHANNEL = "fittracker_reminders"
private const val THEME = "theme_mode"
private const val DYNAMIC = "dynamic_color"
private const val ACCENT = "accent_color"
private const val LANGUAGE = "language"
private const val APK_URL = "https://github.com/mrx7014/fit-tracker/releases/latest/download/app-debug.apk"
private const val APK_FILE = "fittracker-update.apk"

private object JsonBackup {
    fun encode(lifts: List<Lift>): String = "{\"version\":1,\"app\":\"FitTracker\",\"lifts\":[" + lifts.joinToString(",") { "{\"id\":${it.id},\"exercise\":\"${it.exercise.replace("\\", "\\\\").replace("\"", "\\\"")}\",\"date\":\"${it.date}\",\"weight\":${it.weight},\"reps\":${it.reps}}" } + "]}"
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
    var language by mutableStateOf(AppLanguage.valueOf(prefs.getString(LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name)); private set
    var userName by mutableStateOf(prefs.getString(USER_NAME, "") ?: ""); private set
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
    fun updateLanguage(value: AppLanguage) { language = value; prefs.edit().putString(LANGUAGE, value.name).apply() }
    fun updateUserName(value: String) { val cleaned = value.trim(); if (cleaned.isNotEmpty()) { userName = cleaned; prefs.edit().putString(USER_NAME, cleaned).apply() } }
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
    fun schedule(context: Context) { val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "FitTracker reminders", NotificationManager.IMPORTANCE_DEFAULT)); val intent = PendingIntent.getBroadcast(context, 77, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val first = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis; alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, intent) }
    fun cancel(context: Context) { val intent = PendingIntent.getBroadcast(context, 77, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(intent) }
}

class ReminderReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent?) { val notification = android.app.Notification.Builder(context, REMINDER_CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("Fit Tracker").setContentText("حافظ على الاستريك وسجّل تمرينك اليوم").setAutoCancel(true).build(); (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(77, notification) } }

private object UpdateManager { fun download(context: Context) { val request = DownloadManager.Request(Uri.parse(APK_URL)).setTitle("Fit Tracker 1.0.0").setDescription("جاري تنزيل تحديث Fit Tracker").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE).setMimeType("application/vnd.android.package-archive"); (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) }; fun install(context: Context) { val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE); if (!file.exists()) return; val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION); runCatching { context.startActivity(intent) }.onFailure { if (Build.VERSION.SDK_INT >= 26) context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }

class UpdateReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent?) { if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) UpdateManager.install(context) } }

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { val vm: LiftViewModel = viewModel(factory = object : ViewModelProvider.Factory { override fun <T : ViewModel> create(c: Class<T>) = LiftViewModel(applicationContext) as T }); FitTrackerTheme(vm) { FitTrackerApp(vm) } } }
}

private val LocalAppLanguage = compositionLocalOf { "en" }
@Composable private fun L(ar: String, en: String): String = if (LocalAppLanguage.current == "ar") ar else en
private fun String.normalizeDigits(): String = map { char -> when (char) { in '\u0660'..'\u0669' -> ('0'.code + (char.code - '\u0660'.code)).toChar(); in '\u06F0'..'\u06F9' -> ('0'.code + (char.code - '\u06F0'.code)).toChar(); else -> char } }.joinToString("")

@Composable private fun FitTrackerApp(vm: LiftViewModel) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var intro by remember { mutableStateOf(!context.getSharedPreferences(PREFS, 0).getBoolean(ONBOARDING, false)) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(JsonBackup.encode(vm.lifts).toByteArray()) } } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> vm.restore(JsonBackup.decode(reader.readText())) } } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val langCode = when (vm.language) { AppLanguage.ARABIC -> "ar"; AppLanguage.ENGLISH -> "en"; AppLanguage.SYSTEM -> if (Locale.getDefault().language == "ar") "ar" else "en" }
    CompositionLocalProvider(LocalAppLanguage provides langCode, LocalLayoutDirection provides if (langCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (intro) {
            Onboarding { context.getSharedPreferences(PREFS, 0).edit().putBoolean(ONBOARDING, true).apply(); intro = false }
        } else if (vm.userName.isBlank()) {
            NameSetup { vm.updateUserName(it) }
        } else {
            BackHandler(enabled = page != 0) { page = if (page == 4) 3 else 0 }
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { AppNavigation(page) { page = it } },
                floatingActionButton = { if (page == 0) FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp), containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "إضافة") } },
                floatingActionButtonPosition = FabPosition.End
            ) { pad ->
                Column(Modifier.padding(pad).fillMaxSize()) {
                    Header()
                    AnimatedContent(page, label = "navigation") { current ->
                        when (current) {
                            0 -> Summary(vm)
                            1 -> History(vm)
                            2 -> StatsScreen(vm)
                            3 -> Settings(vm, { export.launch("fittracker-backup.json") }, { restore.launch(arrayOf("application/json")) }, { page = 4 }, { enabled -> if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch("android.permission.POST_NOTIFICATIONS"); vm.updateReminders(enabled) })
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
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        NavigationBarItem(page == 0, { onPage(0) }, icon = { Icon(Icons.Default.Insights, null) }, label = { Text(L("ملخص", "Summary")) })
        NavigationBarItem(page == 1, { onPage(1) }, icon = { Icon(Icons.Default.History, null) }, label = { Text(L("السجل", "History")) })
        NavigationBarItem(page == 2, { onPage(2) }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text(L("إحصائيات", "Stats")) })
        NavigationBarItem(page == 3, { onPage(3) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(L("الإعدادات", "Settings")) })
    }
}

@Composable private fun Header() { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(27.dp)) } }; Spacer(Modifier.width(12.dp)); Column { Text("FIT TRACKER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("TRAIN SMARTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun NameSetup(onSave: (String) -> Unit) { var name by remember { mutableStateOf("") }; Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Surface(Modifier.size(76.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Person, null, Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(24.dp)); Text(L("أهلاً بيك في Fit Tracker", "Welcome to Fit Tracker"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp)); Text(L("اكتب اسمك عشان نستخدمه في تحيتك", "Enter your name so we can personalize your greeting"), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(24.dp)); OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(L("اسم المستخدم", "Your name")) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(16.dp)); Spacer(Modifier.height(18.dp)); Button(onClick = { onSave(name.trim()) }, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text(L("متابعة", "Continue"), fontWeight = FontWeight.Bold) } } } }
@Composable private fun Onboarding(done: () -> Unit) { var step by remember { mutableIntStateOf(0) }; val titles = listOf("ابنِ قوتك بذكاء", "سجّل في ثواني", "شاهد التطور") ; val bodies = listOf("كل تمرين، كل تكرار، وكل رقم له مكان في رحلتك.", "واجهة سريعة مصممة عشان تسجل وتكمل تمرينك بدون تشتيت.", "streaks، badges، ورسوم بيانية تخليك شايف تقدمك بوضوح."); val icons = listOf(Icons.Default.FitnessCenter, Icons.Default.Bolt, Icons.Default.TrendingUp); Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(58.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(52.dp), shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.FitnessCenter, null, Modifier.padding(13.dp), tint = MaterialTheme.colorScheme.onPrimary) }; Spacer(Modifier.width(12.dp)); Text("FIT TRACKER", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }; Spacer(Modifier.weight(1f)); AnimatedContent(step, label = "onboarding") { current -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Surface(Modifier.size(124.dp), shape = RoundedCornerShape(38.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icons[current], null, Modifier.padding(32.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(30.dp)); Text(titles[current], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); Text(bodies[current], style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.weight(1f)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Box(Modifier.size(if (it == step) 28.dp else 8.dp, 8.dp).clip(RoundedCornerShape(8.dp)).background(if (it == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) } }; Spacer(Modifier.height(24.dp)); Button(onClick = { if (step == 2) done() else step++ }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text(if (step == 2) "ابدأ رحلتك" else "التالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(18.dp)) } } }

@Composable private fun Summary(vm: LiftViewModel) { var range by remember { mutableIntStateOf(7) }; val scoped = vm.lifts.filter { it.date >= LiftViewModel.fmt.format(Date(System.currentTimeMillis() - (range - 1) * 86400000L)) }; val unitLabel = if (vm.unit == UnitMode.KG) "kg" else "lb"; LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) { Column(Modifier.padding(22.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${vm.userName}،", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge); Text("يلا بينا يا بطل", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }; Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .16f)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.onPrimary); Text("${vm.streak}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("STREAK", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall) } } }; Spacer(Modifier.height(20.dp)); Text("استمرارية صغيرة كل يوم = فرق كبير.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium) } } }; item { AdaptiveMetricLayout(first = { MetricTile("${scoped.size}", L("جلسات الفترة", "Period sessions"), Icons.Default.Bolt, MaterialTheme.colorScheme.secondaryContainer) }, second = { MetricTile("${"%.1f".format(vm.displayWeight(vm.bestWeight))}", L("أفضل وزن $unitLabel", "Best weight $unitLabel"), Icons.Default.TrendingUp, MaterialTheme.colorScheme.tertiaryContainer) }) }; item { AdaptiveMetricLayout(first = { MetricTile("${vm.lifts.size}", L("إجمالي الأوزان", "Total weights"), Icons.Default.FitnessCenter, MaterialTheme.colorScheme.secondaryContainer) }, second = { MetricTile("${vm.lifts.sumOf { it.reps }}", L("إجمالي العدّات", "Total reps"), Icons.Default.Repeat, MaterialTheme.colorScheme.tertiaryContainer) }, third = { MetricTile("${vm.lifts.map { it.exercise.trim().lowercase() }.distinct().size}", L("إجمالي التمرينات", "Total exercises"), Icons.Default.List, MaterialTheme.colorScheme.primaryContainer) }) }; item { Row(verticalAlignment = Alignment.CenterVertically) { Text(L("آخر التمرينات", "Recent workouts"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Spacer(Modifier.weight(1f)); Text("${vm.lifts.size} تسجيل", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts.take(4), key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun MetricTile(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color)) { Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(10.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun AdaptiveMetricLayout(first: @Composable () -> Unit, second: @Composable () -> Unit, third: (@Composable () -> Unit)? = null) { BoxWithConstraints(Modifier.fillMaxWidth()) { if (third == null) { if (maxWidth < 420.dp) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { first(); second() } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.weight(1f)) { first() }; Box(Modifier.weight(1f)) { second() } } } else if (maxWidth < 420.dp) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { first(); second(); third() } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.weight(1f)) { first() }; Box(Modifier.weight(1f)) { second() }; Box(Modifier.weight(1f)) { third() } } } }

@Composable private fun StatCard(modifier: Modifier = Modifier, v: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(modifier, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp)) { Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)) { Icon(icon, null, Modifier.padding(8.dp)) }; Spacer(Modifier.height(10.dp)); Text(v, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) } } }

@Composable private fun History(vm: LiftViewModel) { LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text(L("سجل التقدم", "Progress history"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(L("كل تسجيل يقربك من نسختك الأقوى", "Every log moves you closer to your strongest self"), color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)) }; if (vm.lifts.isEmpty()) item { EmptyState() } else items(vm.lifts, key = { it.id }) { LiftRow(it, vm) } } }

@Composable private fun StatsScreen(vm: LiftViewModel) { val points = vm.lifts.take(20); val maxWeight = (points.maxOfOrNull { vm.displayWeight(it.weight) } ?: 1f).coerceAtLeast(1f); val maxReps = (points.maxOfOrNull { it.reps } ?: 1).coerceAtLeast(1); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text("إحصائيات متقدمة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("الوزن أفقي والعدّات رأسي — كل نقطة تمرين مسجل", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(18.dp)) { Text("الوزن مقابل العدّات — آخر ${points.size} تسجيل", fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); WeightRepsChart(points, maxWeight, maxReps, vm) } } }; item { AdaptiveMetricLayout(first = { StatCard(Modifier.fillMaxWidth(), "${vm.lifts.distinctBy { it.exercise }.size}", L("تمارين مختلفة", "Exercises"), Icons.Default.FitnessCenter, MaterialTheme.colorScheme.primaryContainer) }, second = { StatCard(Modifier.fillMaxWidth(), "${"%.1f".format(vm.displayWeight(vm.bestWeight))}", L("أعلى وزن", "Best weight"), Icons.Default.TrendingUp, MaterialTheme.colorScheme.tertiaryContainer) }) }; item { Text("الإنجازات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(vm.achievements) { (title, description, icon, unlocked) -> AchievementCard(title, description, icon, unlocked) }; item { Text("أفضل أوزان حسب التمرين", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(vm.lifts.groupBy { it.exercise }.entries.sortedByDescending { it.value.maxOf { lift -> lift.weight } }.take(8)) { (exercise, lifts) -> ListItem(headlineContent = { Text(exercise, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${lifts.size} تسجيلات") }, trailingContent = { Text("${"%.1f".format(vm.displayWeight(lifts.maxOf { it.weight }))} ${if (vm.unit == UnitMode.KG) "kg" else "lb"}", fontWeight = FontWeight.Bold) }) } } }

@Composable private fun AchievementCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unlocked: Boolean) { Card(colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(32.dp), tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (unlocked) "مفتوح" else "مقفل", color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold) } } }

@Composable private fun WeightRepsChart(lifts: List<Lift>, maxWeight: Float, maxReps: Int, vm: LiftViewModel) { val axisColor = MaterialTheme.colorScheme.onSurfaceVariant; val gridColor = MaterialTheme.colorScheme.outlineVariant; val pointColor = MaterialTheme.colorScheme.primary; val unitLabel = if (vm.unit == UnitMode.KG) "kg" else "lb"; Canvas(Modifier.fillMaxWidth().height(280.dp)) { val left = 48.dp.toPx(); val right = 12.dp.toPx(); val top = 16.dp.toPx(); val bottom = 42.dp.toPx(); val plotWidth = (size.width - left - right).coerceAtLeast(1f); val plotHeight = (size.height - top - bottom).coerceAtLeast(1f); val labelPaint = android.graphics.Paint().apply { isAntiAlias = true; color = axisColor.toArgb(); textSize = 11.dp.toPx(); typeface = android.graphics.Typeface.DEFAULT }; val namePaint = android.graphics.Paint().apply { isAntiAlias = true; color = axisColor.toArgb(); textSize = 10.dp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD }; fun x(weight: Float) = left + (weight / maxWeight.coerceAtLeast(1f)) * plotWidth; fun y(reps: Int) = top + plotHeight - (reps.toFloat() / maxReps.coerceAtLeast(1)) * plotHeight; for (i in 0..4) { val fraction = i / 4f; val yValue = maxReps * fraction; val yPosition = top + plotHeight - fraction * plotHeight; drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, yPosition), androidx.compose.ui.geometry.Offset(size.width - right, yPosition), strokeWidth = 1f); drawContext.canvas.nativeCanvas.drawText(yValue.roundToInt().toString(), 4.dp.toPx(), yPosition + 4.dp.toPx(), labelPaint) }; for (i in 0..4) { val fraction = i / 4f; val weightValue = maxWeight * fraction; val xPosition = left + fraction * plotWidth; drawLine(gridColor, androidx.compose.ui.geometry.Offset(xPosition, top), androidx.compose.ui.geometry.Offset(xPosition, top + plotHeight), strokeWidth = 1f); drawContext.canvas.nativeCanvas.drawText("${"%.1f".format(weightValue)}", xPosition - 10.dp.toPx(), size.height - 14.dp.toPx(), labelPaint) }; drawLine(axisColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + plotHeight), strokeWidth = 2f); drawLine(axisColor, androidx.compose.ui.geometry.Offset(left, top + plotHeight), androidx.compose.ui.geometry.Offset(size.width - right, top + plotHeight), strokeWidth = 2f); lifts.forEach { lift -> val point = androidx.compose.ui.geometry.Offset(x(vm.displayWeight(lift.weight)), y(lift.reps)); drawCircle(pointColor, 7.dp.toPx(), point); drawCircle(MaterialTheme.colorScheme.surface, 3.dp.toPx(), point); drawContext.canvas.nativeCanvas.drawText(lift.exercise.take(16), point.x + 8.dp.toPx(), point.y - 8.dp.toPx(), namePaint) }; drawContext.canvas.nativeCanvas.drawText("الوزن ($unitLabel)", left + plotWidth / 2f - 32.dp.toPx(), size.height - 1.dp.toPx(), labelPaint); drawContext.canvas.nativeCanvas.drawText("العدّات", 4.dp.toPx(), top - 4.dp.toPx(), labelPaint) } }
@Composable private fun AboutScreen() { val arabic = Locale.getDefault().language == "ar"; val uri = "https://github.com/mrx7014/fit-tracker"; val uriHandler = LocalUriHandler.current; LazyColumn(contentPadding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Icon(Icons.Default.FitnessCenter, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary); Text(if (arabic) "عن Fit Tracker" else "About Fit Tracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(if (arabic) "Fit Tracker هو تطبيق بسيط وخصوصي لتسجيل أوزان تمارين المقاومة ومتابعة التطور." else "Fit Tracker is a simple, private strength-training log built to help you track progress.", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }; item { Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text(if (arabic) "المطور" else "Developer", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(if (arabic) "تم تطوير هذا التطبيق بواسطة صاحب المشروع لمساعدة الرياضيين على الاستمرارية وبناء عادات أفضل." else "Built by the project owner to help athletes stay consistent and build better habits."); Spacer(Modifier.height(10.dp)); Text(if (arabic) "الإصدار 1.0.0 • Material 3 Expressive" else "Version 1.0.0 • Material 3 Expressive", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; item { OutlinedButton(onClick = { uriHandler.openUri(uri) }) { Icon(Icons.Default.Code, null); Spacer(Modifier.width(8.dp)); Text(if (arabic) "مستودع GitHub" else "GitHub repository") } } } }

@Composable private fun LiftRow(lift: Lift, vm: LiftViewModel) { var visible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { visible = true }; AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(46.dp), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.FitnessCenter, null, Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(lift.exercise, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text("${lift.date} • ${lift.reps} تكرار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Column(horizontalAlignment = Alignment.End) { Text("${"%.1f".format(vm.displayWeight(lift.weight))}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text(if (vm.unit == UnitMode.KG) "kg" else "lb", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { vm.remove(lift.id) }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error) } } } } }

@Composable private fun UpdateCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(L("التحديثات", "Updates"), fontWeight = FontWeight.Bold); Text("الإصدار الحالي 2.1.0", style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { UpdateManager.download(context) }) { Text("تنزيل وتثبيت") }
            }
            Spacer(Modifier.height(8.dp))
            Text("آخر changelog: Dashboard جديد، بطاقات عصرية، وملاحة محسنة.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { uriHandler.openUri("https://github.com/mrx7014/fit-tracker/releases") }) { Text(L("فتح صفحة الإصدارات", "Open releases")) }
        }
    }
}

@Composable private fun Settings(vm: LiftViewModel, export: () -> Unit, restore: () -> Unit, about: () -> Unit, reminder: (Boolean) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("تحكم في تجربتك وبياناتك", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { NameSettings(vm) }
        item { Text(L("المظهر", "Appearance"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Language, null); Spacer(Modifier.width(12.dp)); Column { Text(L("اللغة", "Language"), fontWeight = FontWeight.Bold); Text(L("اختر لغة التطبيق", "Choose your app language"), style = MaterialTheme.typography.bodySmall) } }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { listOf(AppLanguage.SYSTEM to L("تلقائي", "System"), AppLanguage.ENGLISH to "English", AppLanguage.ARABIC to "العربية").forEachIndexed { i, pair -> SegmentedButton(vm.language == pair.first, { vm.updateLanguage(pair.first) }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(pair.second) } } }
                }
            }
        }
        item { SettingsCard(Icons.Default.Notifications, L("تذكير يومي", "Daily reminder"), L("تنبيه الساعة 8 مساءً للحفاظ على الاستريك", "8 PM reminder to keep your streak alive")) { Switch(vm.reminders, reminder) } }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Straighten, null); Spacer(Modifier.width(12.dp)); Column { Text("وحدة الوزن", fontWeight = FontWeight.Bold); Text("اختر الوحدة المفضلة", style = MaterialTheme.typography.bodySmall) } }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { SegmentedButton(vm.unit == UnitMode.KG, { vm.updateUnit(UnitMode.KG) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("كيلو kg") }; SegmentedButton(vm.unit == UnitMode.LB, { vm.updateUnit(UnitMode.LB) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("باوند lb") } }
                }
            }
        }
        item { UpdateCard() }
        item { Text(L("البيانات", "Data"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { SettingsCard(Icons.Default.Upload, L("تصدير نسخة احتياطية", "Export backup"), L("ملف JSON قابل للحفظ والمشاركة", "A JSON file you can save and share")) { TextButton(onClick = export) { Text(L("تصدير", "Export")) } } }
        item { SettingsCard(Icons.Default.Download, L("استيراد نسخة احتياطية", "Import backup"), L("استرجاع تمارينك من ملف JSON", "Restore workouts from a JSON file")) { TextButton(onClick = restore) { Text(L("استيراد", "Import")) } } }
        item { SettingsCard(Icons.Default.Info, L("عن Fit Tracker", "About Fit Tracker"), L("المطور، الإصدار، ورابط GitHub", "Developer, version, and GitHub link")) { TextButton(onClick = about) { Text(L("فتح", "Open")) } } }
    }
}

@Composable private fun NameSettings(vm: LiftViewModel) { var name by remember(vm.userName) { mutableStateOf(vm.userName) }; Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column { Text(L("اسم المستخدم", "Username"), fontWeight = FontWeight.Bold); Text(L("غيّر الاسم الظاهر في التحية", "Change the name shown in your greeting"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(L("الاسم", "Name")) }, shape = RoundedCornerShape(16.dp)); Button(onClick = { vm.updateUserName(name) }, enabled = name.trim().isNotEmpty() && name.trim() != vm.userName, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(L("حفظ الاسم", "Save name")) } } } }
@Composable private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: @Composable () -> Unit) { Card(shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; action() } } }

@Composable private fun EmptyState() { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Text("لسه مفيش تسجيلات", fontWeight = FontWeight.Bold); Text("اضغط + وسجّل أول تمرين", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun AddDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Int) -> Unit) { var e by remember { mutableStateOf("") }; var d by remember { mutableStateOf(LiftViewModel.fmt.format(Date())) }; var w by remember { mutableStateOf("") }; var r by remember { mutableStateOf("8") }; val normalizedWeight = w.normalizeDigits().toFloatOrNull(); val normalizedReps = r.normalizeDigits().toIntOrNull(); Dialog(onDismissRequest = onDismiss) { Card(Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Add, null, Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(12.dp)); Column { Text(L("تسجيل جديد", "New workout"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(L("سجّل أوزانك", "Log your weights"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } ; OutlinedTextField(e, { e = it }, Modifier.fillMaxWidth(), label = { Text(L("اسم التمرين", "Exercise name")) }, leadingIcon = { Icon(Icons.Default.FitnessCenter, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)); OutlinedTextField(d, { d = it }, Modifier.fillMaxWidth(), label = { Text(L("التاريخ", "Date")) }, leadingIcon = { Icon(Icons.Default.CalendarToday, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(w, { w = it }, Modifier.weight(1f), label = { Text(L("الوزن", "Weight")) }, suffix = { Text("kg") }, singleLine = true, shape = RoundedCornerShape(16.dp)); OutlinedTextField(r, { r = it }, Modifier.weight(1f), label = { Text(L("التكرارات", "Reps")) }, singleLine = true, shape = RoundedCornerShape(16.dp)) }; Spacer(Modifier.height(4.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp)) { Text(L("إلغاء", "Cancel")) }; Button(enabled = e.isNotBlank() && normalizedWeight != null, onClick = { onSave(e, d.normalizeDigits(), normalizedWeight ?: 0f, normalizedReps ?: 1) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(L("حفظ التمرين", "Save workout"), fontWeight = FontWeight.Bold) } } } } } }

@Composable private fun FitTrackerTheme(vm: LiftViewModel, content: @Composable () -> Unit) { val systemDark = isSystemInDarkTheme(); val dark = when (vm.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }; val palettes = listOf(Color(0xFF0B5D46), Color(0xFF6750A4), Color(0xFF9C4146), Color(0xFF006874)); val accent = palettes.getOrElse(vm.accent) { palettes.first() }; val light = lightColorScheme(primary = accent, onPrimary = Color.White, primaryContainer = accent.copy(alpha = .22f), secondaryContainer = Color(0xFFFFD9B8), tertiaryContainer = Color(0xFFD9E2FF), background = Color(0xFFF8FAF6)); val darkScheme = darkColorScheme(primary = accent.copy(alpha = .9f), onPrimary = Color.White, primaryContainer = accent.copy(alpha = .5f), secondaryContainer = Color(0xFF70451D)); val scheme = if (vm.dynamicColors && android.os.Build.VERSION.SDK_INT >= 31) { val ctx = LocalContext.current; if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) } else if (dark) darkScheme else light; val english = FontFamily(Font(com.fittracker.app.R.font.google_sans_flex)); val arabic = FontFamily(Font(com.fittracker.app.R.font.noto_sans_arabic)); MaterialTheme(colorScheme = scheme, typography = Typography().run { copy(bodyLarge = bodyLarge.copy(fontFamily = arabic), bodyMedium = bodyMedium.copy(fontFamily = arabic), titleLarge = titleLarge.copy(fontFamily = english), headlineSmall = headlineSmall.copy(fontFamily = english), headlineMedium = headlineMedium.copy(fontFamily = english)) }, content = content) }
