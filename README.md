# FeedRail Android SDK

A native Android SDK for embedding the [FeedRail](https://feedrail.ai) feedback widget directly in your app. No web views — pure **Jetpack Compose** built on modern Kotlin, Retrofit, and Material 3.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| minSdk | 24 |
| Kotlin | 1.9+ |
| Core library desugaring | Enabled |

---

## Installation

The SDK is published as `com.feedrail:feedrail-sdk`. Until it ships to a public registry, publish it to your local Maven repo:

```bash
./gradlew :feedrail-sdk:publishToMavenLocal
```

Then add it to your app module (with `mavenLocal()` in your repositories):

```kotlin
dependencies {
    implementation("com.feedrail:feedrail-sdk:0.0.1")
}
```

---

## Quick Start

### 1. Configure the SDK (once, at app startup)

```kotlin
import com.feedrail.sdk.public.FeedRailSDK
import com.feedrail.sdk.public.FeedRailConfig
import com.feedrail.sdk.public.FeedRailEnvironment

FeedRailSDK.configure(
    config = FeedRailConfig(
        organizationSlug = "your-org-slug",
        projectSlug = "your-project-slug",
        // Production: use FeedRailEnvironment.Production() (https://api.feedrail.ai)
        environment = FeedRailEnvironment.Development(baseUrl = "http://10.0.2.2:3002"),
    ),
    context = applicationContext,
)
```

### 2. Present the feedback widget

**Compose:**

```kotlin
var showFeedback by remember { mutableStateOf(false) }

Button(onClick = { showFeedback = true }) { Text("Give Feedback") }

if (showFeedback) {
    FeedRailSDK.FeedbackSheet(onDismiss = { showFeedback = false })
}
```

**From a plain-View / non-Compose host:**

```kotlin
FeedRailSDK.present(context = this)   // pass an Activity so the sheet appears over it
```

---

## Identity & Anonymous Users

Every user is identified — no code required.

1. **Anonymous by default.** On first launch the SDK generates a stable, device-scoped anonymous ID (`anon_…`) and stores it in **EncryptedSharedPreferences**. The same profile — their posts and votes — persists across app restarts automatically. This works on **all plans** at no cost.
2. **Identify on sign-in.** When your app authenticates a user, call `identify(userId = …)`. The prior anonymous profile is automatically **merged** into the identified account (RevenueCat-style aliasing), so the user keeps their history. This requires `apiKey` in your `FeedRailConfig` (an SDK key from the FeedRail dashboard → **Settings → API Keys**, scoped to your project), which proves the identity server-side.
3. **Logout.** `logout()` clears the identified session and returns the SDK to a **fresh anonymous identity**, so the feedback UI stays usable immediately.
4. **Automatic metadata.** Device and platform details (device, model, OS, OS version, app version) plus the source (`Android SDK`) are attached to every request automatically and appear in your dashboard's **Audience** view and on each user's profile.

```kotlin
// 1. Anonymous — nothing to do. The SDK already has a persistent `anon_…` identity.

// 2. Identify once your user signs in (requires `apiKey` in configure):
FeedRailSDK.identify(
    userId = currentUser.id,          // required
    userName = currentUser.name,
    userEmail = currentUser.email,
    userAvatarUrl = currentUser.avatarUrl,
    isPaidCustomer = currentUser.isSubscribed,
    productId = currentUser.planId,
    userMetadata = mapOf("company" to currentUser.companyName),
)
// The anonymous profile is merged into this account automatically.

// 3. On sign-out — reverts to a fresh anonymous identity:
FeedRailSDK.logout()
```

> You can also identify a user up front by passing `apiKey` + `userId` (and friends) to `configure()` if they're already signed in at launch.

---

## Configuration Options

```kotlin
FeedRailConfig(
    // Required
    organizationSlug = "acme",
    projectSlug = "main",

    // Environment (optional — defaults to Production, https://api.feedrail.ai)
    environment = FeedRailEnvironment.Production(),

    // Authenticated user (optional — omit for anonymous mode)
    apiKey = "your-project-sdk-key",   // Dashboard → Settings → API Keys → New Key → "SDK Key"
    userId = currentUser.id,           // required when using apiKey
    userName = currentUser.name,
    userEmail = currentUser.email,
    userAvatarUrl = currentUser.avatarUrl,
    isPaidCustomer = currentUser.isSubscribed,
    productId = currentUser.planId,
    userMetadata = mapOf("role" to currentUser.role),

    // Locale override (defaults to device locale)
    preferredLocale = "ar",   // "en", "ar", "de", "es", "fr", "it", "ja", "ko", "zh-Hans", "zh-Hant"
)
```

---

## License

MIT — see [LICENSE](LICENSE)

---

## Support

- Issues: https://github.com/DewminaUdayashan/FeedRail-Android/issues
- Email: support@feedrail.ai
