# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ========================
# Room Database (These rules are important)
# ========================
-keepclassmembers class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
# The rule is dontwarn androidx.room.paging.** usually not required with new versions of R8

# ========================
# Kotlin Coroutines (These rules are important)
# ========================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========================
# Gson (These rules are important for preserving the models used in reflection.)
# ========================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Важно! Сохраняем классы моделей данных (data classes), которые Gson будет (де)сериализовать.
# Убедитесь, что путь 'com.markdownbinder.models.**' правильный.
-keep class com.markdownbinder.models.** { *; }

# ========================
# ViewBinding (R8 обычно справляется сам, но это правило не повредит)
# ========================
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static * bind(android.view.View);
}

# ========================
# Accessibility Service (it is important to save if used)
# ========================
-keep class com.markdownbinder.services.MarkDownAccessibilityService { *; }
-keep class com.markdownbinder.services.OverlayService { *; }

# ========================
# Enum, Parcelable, Serializable (useful general rules)
# ========================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}