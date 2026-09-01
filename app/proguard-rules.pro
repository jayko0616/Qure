# ML Kit barcode scanning ships its model and native code; keep its entry points.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
