#!/usr/bin/env bash
# Cuts a release: asks for the version name, tags main as v<name>, pushes the tag. The Release
# workflow does everything else — builds, signs, and publishes the APKs, the DMG and the R8 mappings.
#
# There is nothing to edit in any build file: versionCode is the commit count and versionName is the
# tag plus that count, so v1.3 at commit 348 reports 1.3.348. Single-branch by design.
set -euo pipefail

cd "$(dirname "$0")/.."

# --tags as well: every main push leaves a build-<versionCode> release tag behind, and the
# duplicate check below has to see them.
git fetch --tags origin main

if [[ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]]; then
  echo "HEAD is not origin/main. Push your work (and let CI go green) first." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is dirty — commit or stash before tagging." >&2
  exit 1
fi

# --match 'v*', or this reports the build-<versionCode> tag of the last main push instead.
echo "Last release:  $(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || echo '<none>')"
echo "versionCode:   $(git rev-list --count HEAD)  (computed, not entered)"
read -rp "New version name (without the leading v): " version

# MAJOR.MINOR with a non-zero major, because jpackage rejects anything else for a DMG and the
# desktop build derives its package version from this tag.
if [[ ! "$version" =~ ^[1-9][0-9]*\.[0-9]+$ ]]; then
  echo "Version must be MAJOR.MINOR with a non-zero major, e.g. 1.0 — the DMG requires it." >&2
  exit 1
fi

if git rev-parse "v$version" >/dev/null 2>&1; then
  echo "Tag v$version already exists." >&2
  exit 1
fi

git tag "v$version"
git push origin "v$version"

echo "Pushed v$version — the Release workflow takes it from here:"
echo "  https://github.com/volkovskiyda/ABit/actions/workflows/release.yml"
