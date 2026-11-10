# Applied to every app that depends on the FeedRail SDK and enables R8.
#
# The SDK talks to its API through Retrofit and parses JSON with Moshi. Most models
# have generated adapters, but generic wrappers (and anything without @JsonClass) go
# through KotlinJsonAdapterFactory, which reads Kotlin metadata and constructor
# parameters by reflection. Without these rules R8 strips that information and the
# feed fails to load in release builds only, with a ClassCastException.

-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# Kotlin metadata, read by Moshi's reflective adapter.
-keep class kotlin.Metadata { *; }

# Request and response models: field names are the JSON keys.
-keep class com.feedrail.sdk.models.** { *; }

# Retrofit builds the API at runtime from this interface's annotations and generic
# return types.
-keep interface com.feedrail.sdk.api.FeedRailApiService { *; }

# Moshi codegen adapters are looked up by name next to each model.
-keep class com.feedrail.sdk.**JsonAdapter { *; }
