# sqlite-wasm

The official SQLite WASM build, vendored rather than pulled from npm so the web app has no
JavaScript dependency graph of its own and the exact bytes that ship are the ones in review.

Copied from [`@sqlite.org/sqlite-wasm`](https://www.npmjs.com/package/@sqlite.org/sqlite-wasm)
3.53.0-build1, via [nowinandroid-kmp](https://github.com/skydoves/nowinandroid-kmp). Only the three
files the worker actually loads are kept.

`../sqlite-worker.js` is the other half of the protocol `androidx.sqlite:sqlite-web` speaks; the
driver that starts it is `core/database/src/wasmJsMain/.../di/PlatformDatabaseModule.wasmJs.kt`.
