# Fit Tracker keeps Android components referenced from the manifest.
-keep public class com.fittracker.app.MainActivity { public <init>(); }
-keep public class com.fittracker.app.ReminderReceiver { public <init>(); }
-keep public class com.fittracker.app.UpdateReceiver { public <init>(); }

# Keep model members used by local JSON backup compatibility.
-keepclassmembers class com.fittracker.app.** { <fields>; }
