## What this changes

<!-- One or two sentences. The commit messages carry the detail. -->

## Checks

- [ ] `scripts/run-tests.sh` passes locally (static analysis, unit, desktop UI, screenshot goldens)
- [ ] No secret is committed — `keystore.properties`, `*.jks`, `google-services.json` and
      `kotzilla.json` are git-ignored, and only their `.example.*` twins belong in a diff
- [ ] No analysis baseline was added. A detekt or lint finding is fixed, not recorded
- [ ] Screenshot goldens, if they changed, were re-baked deliberately and the diff was looked at
