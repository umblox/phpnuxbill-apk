# Pertahankan model Gson & DataStore agar serialisasi/deserialisasi tidak rusak oleh R8
-keep class com.phpnuxbill.mobile.core.model.** { *; }
-keep class com.phpnuxbill.mobile.feature.**.model.** { *; }
-keepattributes Signature, *Annotation*

# Pertahankan JavascriptInterface pada CoordinatePickerDialog (WebView Leaflet)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp & Gson standard rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.google.gson.** { *; }
