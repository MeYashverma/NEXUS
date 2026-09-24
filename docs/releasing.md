# Release process

Versioning: semver (`MAJOR.MINOR.PATCH`). Version lives in `app/app/build.gradle.kts`
(`versionCode`, `versionName`) and in `tools/build_site.py` (`VERSION`) — update both.

## Checklist

1. **Freeze & test**
   - [ ] `./gradlew testDebugUnitTest assembleDebug assembleRelease` green locally and in CI
   - [ ] Manual smoke on a real phone + real computer: pair, connect, every mode, disconnect,
         reconnect, Relay start/stop
2. **Docs**
   - [ ] `docs/changelog.md` — new section (Added/Changed/Fixed/Known issues/Upgrade notes)
   - [ ] `tools/pages_source.py` changelog page updated → `python3 tools/build_site.py`
   - [ ] Compatibility tables still true (spot-check against device reports)
3. **Version bump**
   - [ ] `app/app/build.gradle.kts` versionCode+1, versionName
   - [ ] `tools/build_site.py` VERSION
   - [ ] Commit: `release: v0.1.0`
4. **Tag & release**
   - [ ] `git tag vX.Y.Z && git push origin vX.Y.Z`
   - [ ] GitHub Release from the tag: title `Elevon vX.Y.Z`, body = changelog section
   - [ ] Attach APKs from the CI artifacts (`elevon-vX.Y.Z-debug.apk`,
         `elevon-vX.Y.Z-release.apk`) + SHA-256SUMS file
   - [ ] Publish. The website links Releases dynamically; nothing else to update.

## Signing reality (read this)

CI signs release APKs with the **debug key** so anyone can clone, build and sideload. That is
fine for an open-source alpha and stated openly in the app build file. For a Play release or
a "production" sideload channel:

1. Generate a real keystore, store it **outside** the repo.
2. Replace the `signingConfig` in `app/app/build.gradle.kts` with env-var-driven config.
3. Publish the release certificate's SHA-256 in the release notes so people can verify
   upgrades (signature continuity matters for sideloaded updates).
4. Re-read [SECURITY.md](../SECURITY.md) and keep the keystore out of CI secrets history.

## Post-release

- [ ] Flip the roadmap "In progress" items
- [ ] Close the release tracking issue
- [ ] Announce with the social card (`branding/social/github-social.png`)
