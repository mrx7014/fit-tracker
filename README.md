<div dir="rtl">

# Fit Tracker 🏋️

[![الإصدار](https://img.shields.io/github/v/release/mrx7014/fit-tracker?display_name=tag&style=flat-square&label=الإصدار)](https://github.com/mrx7014/fit-tracker/releases)
[![البناء](https://img.shields.io/github/actions/workflow/status/mrx7014/fit-tracker/android.yml?style=flat-square&label=البناء)](https://github.com/mrx7014/fit-tracker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)

**Fit Tracker** هو تطبيق أندرويد حديث لتسجيل التمارين ومتابعة التقدم وبناء عادة رياضية مستمرة. صُمم التطبيق باستخدام Kotlin وJetpack Compose وMaterial 3، مع دعم كامل للغتين العربية والإنجليزية وتجرب�� استخدام بسيطة وخصوصية تركز على تخزين البيانات محليًا.

[تحميل آخر إصدار](https://github.com/mrx7014/fit-tracker/releases/latest) · [سجل التغييرات](CHANGELOG.md) · [النسخة الإنجليزية](README.en.md)

</div>

## ✨ المميزات

- تسجيل التمرين باسم التمرين والتاريخ والوزن والعدّات.
- جدول تمارين أسبوعي مع إضافة وحذف تمارين لكل يوم.
- لوحة رئيسية تعرض أهم الأرقام وسجل التمارين وسلسلة التمارين.
- إحصائيات أسبوعية وشهرية للجلسات والعدّات والأيام النشطة ومتوسط الوزن.
- رسم بياني يوضح العلاقة بين الوزن والعدّات مع أسماء التمارين.
- إنجازات تحفّزك على الاستمرار وتحقيق أهداف جديدة.
- تذكير يومي بوقت قابل للتخصيص.
- الوضع الفاتح والداكن ووضع النظام والألوان الديناميكية.
- اختيار وحدات الوزن بين الكيلوجرام والرطل.
- دعم العربية والإنجليزية واتجاه RTL للغة العربية.
- دعم الأرقام العربية والهندية والفارسية في الإدخال.
- تصدير واستيراد نسخة احتياطية من بيانات التمارين بصيغة JSON.
- لا يحتاج إلى حساب أو خادم؛ بيانات التمارين محفوظة محليًا على الجهاز.

## 📱 شاشات التطبيق

يحتوي التطبيق على الشاشات التالية:

| الشاشة | الوظيفة |
| --- | --- |
| الرئيسية | ملخص الأداء وسجل التمارين وإضافة تمرين جديد |
| الجدول | تخطيط تمارين الأسبوع |
| الإحصائيات | تحليل أسبوعي وشهري ورسم الوزن مقابل العدّات |
| الإنجازات | متابعة الإنجازات وسلسلة التمارين |
| الإعدادات | الملف الشخصي والمظهر واللغة والتذكيرات والنسخ الاحتياطي |
| عن التطبيق | معلومات Fit Tracker والمطور والإصدارات |

> لا توجد لقطات شاشة منشورة حاليًا داخل المستودع. يمكن إضافة الصور لاحقًا داخل مجلد `docs/images/` وربطها بهذا القسم.

## 🧰 التقنيات المستخدمة

- **Kotlin**
- **Android Jetpack**
- **Jetpack Compose**
- **Material 3**
- **Compose Material Icons**
- **ViewModel**
- **SharedPreferences** للتخزين المحلي الحالي
- **Gradle Kotlin DSL**
- **GitHub Actions** للبناء ونشر الإصدارات

## 📋 متطلبات التشغيل والتطوير

- Android Studio Ladybug أو إصدار أحدث.
- JDK 21.
- Android SDK API 35.
- جهاز أو محاكي يعمل بنظام Android 8.0 (API 26) أو أحدث.
- اتصال بالإنترنت عند أول مزامنة لاعتمادات Gradle.

## 🚀 التشغيل محليًا

```bash
git clone https://github.com/mrx7014/fit-tracker.git
cd fit-tracker
```

افتح المشروع في Android Studio، ثم انتظر اكتمال مزامنة Gradle. لبناء نسخة Debug من الطرفية:

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
./gradlew.bat assembleDebug
```

سيتم إنشاء ملف APK هنا:

```text
app/build/outputs/apk/debug/app-debug.apk
```

يمكنك أيضًا تشغيل التطبيق مباشرة من Android Studio على محاكي أو جهاز أندرويد متصل.

## 📦 التحميل

يمكن تحميل أحدث APK من صفحة [GitHub Releases](https://github.com/mrx7014/fit-tracker/releases/latest).

الإصدار الحالي هو **3.0.0**، ومعرّف التطبيق هو:

```text
com.fittracker.app
```

## 🤖 GitHub Actions

يحتوي المشروع على سير عمل يدوي في `.github/workflows/android.yml`:

1. افتح تبويب **Actions** في GitHub.
2. اختر **Build Fit Tracker app**.
3. اضغط **Run workflow**.
4. اختر ما إذا كنت تريد نشر Release.
5. عند نشر إصدار، أدخل رقمًا بصيغة مثل `3.0.0` أو `3.0.0-beta1`.

يقوم سير العمل ببناء APK ورفعه كـ Artifact، ويمكنه نشره تلقائيًا ضمن GitHub Release عند تفعيل خيار النشر.

## 🔐 البيانات والخصوصية

يخزن التطبيق بيانات الملف الشخصي والتمارين والإعدادات محليًا على الجهاز، ولا يتطلب خادمًا أو حسابًا. يمكنك استخدام ميزة النسخ الاحتياطي لتصدير بيانات التمارين إلى ملف JSON واستعادتها لاحقًا.

> احفظ ملفات النسخ الاحتياطي في مكان آمن؛ حذف التطبيق أو مسح بياناته قد يؤدي إلى فقدان البيانات المحلية.

## 🗂️ هيكل المشروع

```text
fit-tracker/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fittracker/app/MainActivity.kt
│       └── res/
├── .github/workflows/android.yml
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── CHANGELOG.md
├── README.md
└── README.en.md
```

## 🤝 المساهمة

المساهمات مرحب بها:

1. اعمل Fork للمستودع.
2. أنشئ فرعًا جديدًا لميزتك أو إصلاحك.
3. نفّذ التغييرات وأضف Commit واضحًا.
4. افتح Pull Request مع شرح مختصر للتغييرات.

## 📄 الترخيص

لا يحتوي المشروع حاليًا على ملف ترخيص. تواصل مع مالك المستودع قبل استخدام الكود أو إعادة توزيعه في مشروع تجاري أو إنتاجي.

## 👤 المطور

- **MRX7014**
- [حساب GitHub](https://github.com/mrx7014)
- [مستودع Fit Tracker](https://github.com/mrx7014/fit-tracker)
- [الإصدارات](https://github.com/mrx7014/fit-tracker/releases)

## 📚 المزيد

للاطلاع على تفاصيل الإصدار الحالي والتغييرات السابقة، راجع [CHANGELOG.md](CHANGELOG.md).

---

<div dir="rtl">

صُنع بـ ❤️ لمساعدة الرياضيين على التسجيل والاستمرار والتطور.

</div>
