# PocketWidget R8 rules.
#
# Room and Compose ship consumer rules; the data model is kept explicitly because
# Room's generated code reflects over fields and the widget layer reads the same models.
-keep class com.pocketwidget.data.** { *; }

# Keep enum names used by settings persistence.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
