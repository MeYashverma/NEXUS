<!-- Pull request checklist — small PRs land faster. -->

## What

<!-- One or two sentences: what does this PR do? -->

## Why

<!-- Link the issue, or explain the motivation. -->

## Product rules check

- [ ] Core features still need **nothing installed on the computer**
- [ ] No new permissions (or justification written in this PR + docs updated)
- [ ] No fake features / overclaimed compatibility; honesty chips updated if needed
- [ ] No telemetry or network access added

## Quality

- [ ] `./gradlew testDebugUnitTest` passes (extended if HID/crypto/models changed)
- [ ] `./gradlew assembleDebug` passes
- [ ] Docs / website updated if behaviour or claims changed
      (edit `tools/pages_source.py`, run `python3 tools/build_site.py`)
- [ ] Screenshots included for user-visible changes

## Testing on real devices

<!-- What phone(s) and computer(s) did you verify on? -->
