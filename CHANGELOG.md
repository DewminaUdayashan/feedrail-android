# Changelog

All notable changes to the FeedRail Android SDK are documented in this file. This
changelog follows [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and the
SDK adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0/).

Tags are bare SemVer (`0.1.1`) because JitPack builds the Maven coordinate from the tag
name. Each released version is tagged on its `release/vX.Y` branch, never on trunk.

> This file was started after `0.1.1` shipped. Entries for `0.1.0` and `0.1.1` were not
> recorded at the time and are intentionally left out rather than reconstructed.

## [Unreleased]

## [0.8.0] - 2026-11-10

### Added

- **Report and block, for App Store Guideline 1.2 and Google Play's UGC policy.**
  Other end users' posts and comments now carry **Report…** and **Block <name>**: from
  the ⋮ menu on a post's detail screen and on each comment, and by long-pressing a
  post in the feed. Reporting asks for a reason (spam, harassment, hate, sexual
  content, violence, other) and an optional note, then confirms in a snackbar;
  blocking asks for confirmation first. Neither is offered on team posts, team or AI
  comments, or the reader's own content.
- Reported content, and everything by a blocked author, disappears from the screen
  immediately, including the feed underneath the detail screen. Reporting a post or
  blocking its author from that post's detail screen returns to the feed. The API
  keeps it hidden on every later fetch.
- **Blocked users** on the reader's own profile, with **Unblock**. Other people's
  profiles can't be opened, so this is where a block is undone.
- A user the team has banned now sees "You can no longer post or comment here." when
  a post, comment or vote is refused, instead of a generic plan error or nothing at
  all. Banned users can still read, report and block. `FeedRailError.Banned` is the
  new error case.
- All new text is translated into the SDK's ten languages.
### Fixed

- **Submitting feedback failed with a 400.** The submit screen sent the board as
  `boardId`, but `POST /portal/feedback` names the board by `boardSlug` and rejects
  fields it doesn't know. So every new post was refused and the reader saw an error
  instead of their post. The SDK now sends the selected board's slug. As far as the
  history shows, the field has never matched the API: the API has required `boardSlug`
  since March 2026, and the SDK has sent `boardId` since its first version.

## [0.7.3] - 2026-08-10

### Fixed

- **Tapping a board filter took the filter bar off screen.** Selecting a board clears
  the list and reloads it, which counted as "still loading" — and the bar was hidden
  while loading, so the control the reader had just pressed vanished under their
  finger and came back a moment later. The bar now stays put through a reload, with
  the current board still selected, while only the posts below it change.

- **A submitted post never appeared.** The screen popped back to a feed that was
  never refreshed, so the post was simply not there — and a refresh would not have
  been enough either: the default order is Top Voted, so a new post with one vote
  sorts pages down, and a post awaiting review is not returned to anybody but its
  author. The new post now goes straight to the top of the list, tinted, with a
  banner saying it was received — and, when the project moderates submissions, that
  it is waiting for review and only visible to its author until approved. It stays at
  the top through a refresh or a sort change for as long as that banner is up.

### Fixed

- **A nameless user's profile showed a bare "Anonymous".** Everywhere else they appear as
  "Anonymous · 9f3a", with a short id suffix that keeps two of them apart — so a profile
  disagreed with the post the reader had just tapped through from. The suffix now comes
  from the model, matching the name the API computes for feedback and comments.

- **Rows in a profile's Requests tab had no byline at all, and its Comments tab said
  "Anonymous".** The activity endpoints return a user's own posts without repeating the
  author on each row, so there was nothing to draw. The API now sends the author, and both
  lists fall back to the profile's own user against older deployments — they wrote
  everything in those lists by definition.

## [0.7.1] - 2026-08-08

### Fixed

- **A project's logo never appeared in the feedback header — and took the project name
  down with it.** The API returns `logoUrl` as a path relative to the API host
  (`/api/v1/uploads/logos/…`), which the image loader was handed unchanged and could not
  fetch. Worse, the presence of a `logoUrl` alone committed the header to the image, so
  the failure left the header empty rather than falling back to the project name — there
  was no route back to the text once the image branch was taken. The path is now resolved
  against the configured base URL, and any logo that still fails to load (404, offline)
  falls back to the name. Projects with no logo were unaffected.

### Changed

- **Board filter chips show their board's colour**, as a dot before the label — the cue
  iOS has always had, and the one thing tying a chip to the coloured board label on the
  cards below it.
- **The sort control is an icon alone.** The order it is currently set to is named, and
  checked, in the sheet it opens, and carried for screen readers by its content
  description.

## [0.7.0] - 2026-08-08

### Changed

- **The feedback UI is a full screen instead of a bottom sheet.** The whole experience —
  list, detail, submit form, profile, and its bottom navigation — lived inside a single
  `ModalBottomSheet`, which is a container built for one short, dismissable choice. Being
  a multi-screen flow inside one, it inherited every sheet behaviour whether it wanted it
  or not, and end users hit all of them:

  - **A downward drag anywhere dismissed everything.** The sheet owned vertical gestures
    for its entire subtree, so a stray swipe on the profile, a feedback detail or a
    half-filled submit form closed the experience and destroyed the back stack with it.
    There is no sheet now, so there is nothing to drag away.
  - **The back button quit instead of going back.** The sheet consumed the back press
    before navigation ever saw it. Back now does what it does everywhere else in Android:
    on a detail, form or profile screen it returns to the list; on a secondary tab it
    returns to the feedback tab; only on the feedback list itself does it leave. The
    predictive-back gesture previews your app underneath while it does so.
  - **There was no visible way out.** The list screen offered only the invisible (and, on
    a scrollable screen, unreliable) downward drag. Every screen now has a close or back
    button in its app bar — including the "failed to load" state, which previously hid the
    chrome entirely and left the user with no exit at all.

  Bottom sheets are still used inside the flow for what they are actually good at: the
  sort picker is now one, replacing a dropdown menu.

- **`FeedRailSDK.FeedbackSheet()` is deprecated in favour of `FeedbackScreen()`.** It
  still works and now presents the full-screen UI, so no integration breaks; the new name
  describes what it does. `FeedbackContent()` is also available for hosts that want to
  embed the experience as a destination in their own navigation graph rather than overlay
  it. `present(context)` is unchanged.

- **The feed's filter controls fit on one row.** Board chips and the sort control took a
  row each, and the sort row held nothing but a single pill against the far edge — a band
  of empty space above every feed. The boards now scroll horizontally in the space they
  need, fading out under a sort control pinned beside them, and a single-board project
  drops the chips rather than showing a filter with one option.

- **Feedback cards are tonal rather than elevated.** Each row carried its own drop shadow,
  so a screenful stacked a dozen of them into visual noise. Cards now sit one tonal step
  off the background, which separates them without the weight. Row spacing is also a
  consistent 12dp — the card's own padding was stacking with the list's and making the
  real gap 20dp.

- **The feedback list no longer shows "Powered by FeedRail" twice** when scrolled to the
  end. One was pinned above the toolbar and another sat at the bottom of the list.

- **The feedback UI no longer opens in FeedRail's brand colour before switching to
  yours.** Branding was fetched only after the auth handshake finished, so the sheet spent
  the whole of it painted in the fallback green and then snapped to the project's real
  colour. Four things now prevent that:

  - **Branding no longer waits for authentication.** `/portal/boards` needs no token, but
    the load was held behind the auth state, so the UI could not even ask what colour it
    should be until sign-in completed. The feedback *list* still waits, because it carries
    per-user state such as `hasVoted`.
  - `configure()` warms `/portal/boards` at launch, so presenting the sheet usually has
    branding already in hand instead of starting a round-trip the user watches. It cannot
    slow or break startup, and it deliberately does **not** authenticate, so no anonymous
    end user is registered for launches that never open feedback.
  - The last-known branding is cached on device and applied before the first frame, so
    every launch after the first is correct immediately. A refresh always follows.
  - If you pass `themeColor`, it is now used *while* branding is still loading rather than
    only after the server confirms your plan allows it. Showing FeedRail green inside your
    app looked broken; showing your own colour looks right even if the server later
    overrides it.

## [0.6.0] - 2026-08-02

### Fixed

- **"Top Voted" now actually sorts by votes.** The option sent `sort=top`, which the API
  does not recognise — it accepts `upvotes`, and falls back to newest-first for anything
  else. Picking "Top Voted" changed the pill label and left the list in its original
  order. It now sends `upvotes`, matching the iOS SDK and the web portal.

### Changed

- **The feedback list opens on "Top Voted" instead of "Newest".** The most-wanted posts
  are what a feedback board is for, and the web portal has always defaulted this way —
  the SDK was the odd one out. Newest is still one tap away in the sort menu.

## [0.5.0] - 2026-08-02

### Added

- **Comments now show their review state to their author.** A comment awaiting approval
  is marked "Pending review", and a rejected one shows the team's reason — matching what
  posts already did. The API has always returned `moderationStatus` and `rejectionReason`
  on comments, and only ever returns a non-approved comment to its own author, but the
  comment model didn't declare the fields so they were parsed away.


## [0.4.0] - 2026-08-02

### Added

- **Comments can carry image attachments**, closing the gap against the iOS SDK. The
  comment composer gets a photo picker, a strip of staged attachments with per-item
  upload progress and a remove button, and a five-attachment cap — matching the
  submit-feedback form. Images are compressed before upload (1600px long edge, JPEG
  q80) like everywhere else, and sending is blocked while an upload is still in flight,
  since an attachment only has a `storageKey` to send once it has finished uploading.

  The composer had an `onAttach` hook and `submitComment` already took an `attachments`
  argument, but nothing ever populated either.

## [0.3.0] - 2026-08-01

### Added

- **Authors can now see the review state of their own posts.** A post awaiting approval
  is marked "Pending review", and a rejected one is marked "Rejected" and shows the
  team's reason on the detail screen. The API has always returned `moderationStatus` and
  `rejectionReason` — and only ever returns a non-approved post to its own author — but
  the SDK's model didn't declare the fields, so they were parsed away and the author saw
  their post sitting in the list looking published with no hint that nobody else could
  see it. The raw value is kept as a `String` and mapped leniently, so a status added by
  a future API can't fail the whole response.

## [0.2.0] - 2026-08-01

### Added

- **Images open in a full-screen viewer inside the SDK** instead of launching a browser
  via `ACTION_VIEW`. Pinch to zoom, pan while zoomed, and swipe between all images
  attached to the same post. Being kicked out to a browser to look at a screenshot on
  your own feedback is jarring, and it also handed a short-lived signed attachment URL to
  the browser, where it lingers in history after it stops working. Non-image attachments
  (PDF, CSV, …) still open externally, which is the right place for them.

## [0.1.2] - 2026-08-01

### Fixed

- **Submitting feedback with an image attachment no longer fails.** The SDK sent the
  uploaded file's `url` where the API's `attachments[]` expects `storageKey`; because the
  API validates with `forbidNonWhitelisted`, every submission carrying an attachment was
  rejected with a 400 and the post never got created. `FeedRailUploadResponse` now parses
  the `storageKey` the upload endpoint already returns, and `AttachmentInput` sends it.

### Changed

- Images picked for a feedback post are downscaled to 1600px on the long edge and
  re-encoded as JPEG at quality 0.8 before upload — a typical camera photo goes from
  several MB to a few hundred KB, so uploads finish faster on cellular and take far less
  room on the server. EXIF orientation is baked into the pixels so portrait photos are not
  uploaded sideways, decoding happens off the main thread, and GIFs (which would lose their
  animation) plus anything that would grow when re-encoded are uploaded untouched.
