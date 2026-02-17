---
description: Create a new GitHub release with the APK attached
---

# Release Workflow

// turbo-all

Only run this workflow when the user **explicitly** asks to create a release.

## Steps

1. Make sure all changes are committed and pushed first.

2. Run the release script:
```bash
cd "/home/zivpeltz/Projects/Israel Polymarket" && bash scripts/release.sh
```

The script will:
- Read the version from `app/build.gradle`
- Build the debug APK
- Create a GitHub release tagged `v{version}` with the APK attached
- Auto-generate release notes from commits since the last tag

## Notes

- If the release tag already exists, the script will error — bump the version in `app/build.gradle` first.
- The APK is named `neviim-v{version}.apk` and attached to the release.
