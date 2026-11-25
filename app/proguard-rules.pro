# Pertahankan model Gson & DataStore agar serialisasi/deserialisasi tidak rusak oleh R8
-keep class com.phpnuxbill.mobile.core.model.** { *; }
-keep class com.phpnuxbill.mobile.feature.**.model.** { *; }
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# Wajib untuk TypeToken (DataStoreSessionStorage.parseServers) di R8 Full Mode
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }

# Pertahankan JavascriptInterface pada CoordinatePickerDialog (WebView Leaflet)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp, Okio & Annotation warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
