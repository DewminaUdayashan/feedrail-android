# Changelog

All notable changes to the FeedRail Android SDK are documented in this file. This
changelog follows [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and the
SDK adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0/).

Tags are bare SemVer (`0.1.1`) because JitPack builds the Maven coordinate from the tag
name. Each released version is tagged on its `release/vX.Y` branch, never on trunk.

> This file was started after `0.1.1` shipped. Entries for `0.1.0` and `0.1.1` were not
> recorded at the time and are intentionally left out rather than reconstructed.

## [Unreleased]

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
