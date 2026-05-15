# شحن الفكة - Android App

تطبيق أندرويد لشحن كروت الفكة (قديم + جديد + كارت 26 ج + باقات مارد) باستخدام Vodafone Cash API مع تسجيل دخول تلقائي (seamless) عبر شبكة فودافون.

## المميزات
- ✅ تسجيل دخول تلقائي seamless لما تكون على داتا فودافون
- ✅ كل كروت الفكة بالفئتين (قديم/جديد) + كارت 26 جنيه الجديد
- ✅ باقات مارد (دقايق / فليكس / سوشيال)
- ✅ واجهة عربية احترافية بألوان فودافون (Material 3 + Jetpack Compose)
- ✅ سجل العمليات داخل التطبيق

## كيفية البناء (Build APK)

### المتطلبات
1. **Android Studio** (آخر إصدار) - https://developer.android.com/studio
2. **JDK 17** (متضمن مع Android Studio)

### الخطوات
1. افتح Android Studio
2. اختر **Open** → اختر مجلد `FakkaCashApp`
3. استنى لحد ما Gradle ينزل التبعيات (هياخد 2-5 دقايق أول مرة)
4. من القائمة العلوية: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. هتلاقي ملف الـ APK في:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### التوزيع
- انقل ملف `app-debug.apk` لأي موبايل أندرويد
- فعّل **"Install from Unknown Sources"** في إعدادات الأمان
- ثبّت التطبيق
- **تأكد إنك متصل بداتا فودافون (مش Wi-Fi)** عشان الـ seamless login يشتغل

## ملاحظات تقنية
- الـ minSdk = 24 (Android 7.0+)
- الـ targetSdk = 34 (Android 14)
- التطبيق يحتاج إنترنت + شبكة فودافون لتسجيل الدخول

## بناء نسخة Release موقّعة
```bash
cd FakkaCashApp
./gradlew assembleRelease
```
محتاج keystore - شوف توثيق Android: https://developer.android.com/studio/publish/app-signing

## تحذير قانوني
الكود ده بيستخدم APIs داخلية لتطبيق Vodafone Cash. الاستخدام على مسؤوليتك الشخصية - ممكن يخالف شروط استخدام فودافون.
