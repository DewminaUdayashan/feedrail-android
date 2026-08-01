# Changelog

All notable changes to the FeedRail Android SDK are documented in this file. This
changelog follows [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and the
SDK adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0/).

Tags are bare SemVer (`0.1.1`) because JitPack builds the Maven coordinate from the tag
name. Each released version is tagged on its `release/vX.Y` branch, never on trunk.

> This file was started after `0.1.1` shipped. Entries for `0.1.0` and `0.1.1` were not
> recorded at the time and are intentionally left out rather than reconstructed.

## [Unreleased]

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

### Known gaps

- Comments cannot carry attachments on Android yet. `FeedbackDetailViewModel.submitComment`
  accepts an `attachments` argument, but the comment composer never populates it — the iOS
  SDK does support this.
