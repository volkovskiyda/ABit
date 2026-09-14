# App-specific R8 rules for the Wear app.
#
# Deliberately near-empty: every library this app uses ships its own consumer rules, and a keep rule
# written here without evidence only makes the APK bigger. Add one when a release build actually
# misbehaves, with a comment saying which symptom it fixes.

# Keeps stack traces in a release crash report readable. Paired with the R8 mapping Crashlytics
# uploads, this is what turns an obfuscated frame back into a file and line.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
