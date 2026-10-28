# SHIPAA unified Android WebView + offline media (preview)

## Product boundary
The live site at https://shipaa.ir remains the only source of UI, course navigation and login.
No WordPress modifications, API activations or alterations to existing dashboard plugins are part of this branch.

## Components
- `SiteShellActivity` displays the real WordPress site with the existing HTML/CSS navigation. It preserves session cookies.
- The lightweight native library entry is visible even when web pages cannot load offline.
- `WebViewCompat.addWebMessageListener` accepts messages exclusively from https://shipaa.ir on its main frame.
- Video controls are appended to existing HTML5 <video> lesson players. Unsupported iframes/HLS are not modified.
- `NativeDownloadStore` saves MP4 into app-scoped external files through Android DownloadManager, with status and deletion.
- `OfflineDownloadsActivity` is independent of the network and plays completed downloads via existing Media3 `PlayerActivity`.

## Known limitations / deployment gate
1. **Not production-certified:** A valid logged-in session and lesson entitlement should be verified by a server API for each issued download token before a public rollout. Existing publicly accessible MP4 URLs on dl.shipaa.ir do not provide cryptographic access control.
2. Downloaded files are private to the app namespace but **not encrypted DRM content**. This is not an anti-piracy mechanism.
3. Downloads based on the HTML player's MP4 URL only; HLS, third-party iframes, attachment-only audio and DRM have no native integration.
4. Persistent watch position is local. Cross-device and WordPress progress sync are not implemented.
5. Logout and account switching handling still require server-backed identity-aware download ownership before production access. Do not use on shared devices with multiple accounts until addressed.
6. Native playback after device reboot, large-file download retries, available storage and portrait/landscape behavior require testing on real devices.
7. Auto-injected button may be absent if a lesson is rendered by an iframe or nonstandard lazy-loaded player.
8. The global offline button is deliberately small; validate against the site's current mobile header and navigation to avoid occluding them.

## Acceptance steps
1. Build using Android GitHub workflow on isolated branch; confirm APK is a nontrivial size.
2. Install APK on a dedicated test Android phone. Verify home and existing bottom navigation visually against live website.
3. Sign in with authorized test account.
4. Open a lesson with a permitted direct MP4; tap 'ذخیره آفلاین در اپ'. Grant download.
5. Wait until download manager reports complete; play from native library.
6. Close app and disable all networks. Reopen via launcher and tap native library icon. Verify seek, playback to end.
7. Reboot device and repeat. Test interruption and resume, low disk and cancellation.
8. Confirm no live WordPress component or production student data was modified.

## Release requirements
Server-side entitlement/temporary media access API; login/logout binding; security review; complete loading states; no overlapping navigation or redundant screens; full tests; signed release keystore; staged rollout. These are mandatory to claim general release readiness.
