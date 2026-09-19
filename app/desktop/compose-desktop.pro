# ProGuard keep rules for the packaged macOS app (`packageReleaseDmg`).
#
# Compose Desktop runs ProGuard over the whole runtime classpath before jlink bundles a JRE, which
# is what takes the distributable from ~230 MB to something a tester will download. Every rule below
# exists because something in this app is reached by name rather than by a call site, and ProGuard's
# static analysis cannot see it.
#
# Obfuscation is off (see build.gradle.kts), so these rules protect classes from *removal*, not from
# renaming. Add a rule only with the stack trace that demanded it, and say which one in a comment.

# --- Entry point -------------------------------------------------------------------------------
-keep class com.gmail.volkovskiyda.abit.MainKt { *; }

# --- Kotlin ------------------------------------------------------------------------------------
# Coroutines' debug agent and its service loaders. The standard rules from the library's own
# consumer file, which is not applied here because this is not an Android build.
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

# kotlinx.serialization generates a `$$serializer` object per @Serializable class and reaches it
# through `Companion.serializer()`. ProGuard sees neither, so both go. The wire formats in
# core:sync and the preferences in core:datastore are what break — silently, as an empty sync.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    public static ** Companion;
    public static ** serializer(...);
}
-keep,includedescriptorclasses class com.gmail.volkovskiyda.abit.**$$serializer { *; }
-keepclassmembers class com.gmail.volkovskiyda.abit.** {
    *** Companion;
    *** serializer(...);
}

# --- Room --------------------------------------------------------------------------------------
# Room instantiates the generated implementation by name (`AbitDatabase_Impl`), and its bundled
# SQLite loads a JNI library through a resource path.
-keep class androidx.room.** { *; }
-keep class androidx.room3.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class com.gmail.volkovskiyda.abit.**_Impl { *; }
-dontwarn androidx.room.**
-dontwarn androidx.room3.**

# --- Firebase (the JVM port of the Android SDK) ------------------------------------------------
# firebase-java-sdk discovers its components through `ComponentRegistrar` classes named in resource
# files and instantiated with `Class.forName`, so nothing here has a call site to trace. Firestore
# then reaches the network through gRPC, whose transport is chosen the same way.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class dev.gitlive.firebase.** { *; }
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
# The generated Firestore messages are not in com.google.protobuf, and protobuf-lite reaches their
# fields by *name*: `MessageSchema` unpacks a `RawMessageInfo` string and calls `reflectField` for
# each one, so a private field with no call site — every optional field this app never sets — is
# shrunk away and the schema fails to build:
#   RuntimeException: Field select_ for com.google.firestore.v1.StructuredQuery not found.
#     at com.google.protobuf.MessageSchema.reflectField(MessageSchema.java:621)
#     at com.google.firebase.firestore.local.SQLiteTargetCache.saveTargetData
# Keeping the fields of every GeneratedMessageLite covers the generated code wherever it lives,
# which is four package trees, rather than naming them.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
-keep class android.** { *; }
# Firestore's local persistence is SQLite, and on this platform that is firebase-java-sdk's
# `android.database.sqlite` shim over org.xerial:sqlite-jdbc. Its native library resolves the Java
# callback classes by JNI name from inside `System.load`, so ProGuard sees no call site for any of
# them and shrinks them away. The first one the loader asks for takes the whole app's sync down:
#   java.lang.NoClassDefFoundError: org/sqlite/Function$Aggregate
#     at org.sqlite.SQLiteJDBCLoader.loadNativeLibrary(SQLiteJDBCLoader.java:266)
#     at com.google.firebase.firestore.local.SQLitePersistence.start(SQLitePersistence.java:138)
#     -> RuntimeException: Internal error in Cloud Firestore (24.10.0)
# Packaged builds only, and only once the app touches Firestore — the tray icon still appears, so
# this reads as "sync quietly does nothing" rather than as a crash. `./gradlew :app:desktop:run`
# never shows it: nothing shrinks the classpath there.
-keep class org.sqlite.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-dontwarn android.**
-dontwarn javax.naming.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn org.slf4j.**

# --- Koin --------------------------------------------------------------------------------------
# Definitions are keyed by KClass rather than by name, so Koin itself survives shrinking; the
# instances it builds do not, because the only reference to them is the lambda inside a module.
-keep class org.koin.** { *; }
-keep class io.kotzilla.** { *; }
-dontwarn org.koin.**
-dontwarn io.kotzilla.**

# --- Compose and Skiko -------------------------------------------------------------------------
# Skiko loads its native library by name out of the jar, and AWT reaches the tray through JNI.
-keep class org.jetbrains.skiko.** { *; }
-keep class org.jetbrains.skia.** { *; }
-dontwarn org.jetbrains.skiko.**
-dontwarn org.jetbrains.skia.**

# --- The Android shims that firebase-java-sdk drags along ---------------------------------------
# The JVM Firebase SDK is a port of the *Android* one, so its jars carry androidx classes —
# NotificationCompat, Fragment, ViewPager — whose own dependencies (aapt's generated `R`, the XML
# pull parser, LiveData) are not on a desktop classpath. ProGuard resolves what it can see and stops
# on 363 references it cannot; none of it is reachable from `main`, and none of it survives the
# shrink. Silencing them by package rather than globally, so a *real* missing class still fails.
-dontwarn androidx.core.**
-dontwarn androidx.fragment.**
-dontwarn androidx.viewpager.**
-dontwarn androidx.coordinatorlayout.**
-dontwarn androidx.loader.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.customview.**
-dontwarn androidx.drawerlayout.**
-dontwarn androidx.swiperefreshlayout.**
-dontwarn androidx.slidingpanelayout.**
-dontwarn androidx.recyclerview.**
-dontwarn androidx.transition.**
-dontwarn androidx.print.**
-dontwarn androidx.legacy.**
-dontwarn androidx.versionedparcelable.**
-dontwarn org.xmlpull.v1.**
# Guava ships annotations meant for its J2ObjC build; nothing reads them off a JVM classpath.
-dontwarn com.google.j2objc.**
-dontwarn com.google.common.**
-dontwarn androidx.asynclayoutinflater.**
-dontwarn androidx.cursoradapter.**
