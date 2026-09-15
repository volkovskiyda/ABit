/*
 * Designed and developed by 2026 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Adapted from nowinandroid-kmp (https://github.com/skydoves/nowinandroid-kmp), under the Apache
// 2.0 licence reproduced above. Changes: the FTS4-to-FTS5 rewrite is gone, because ABit has no
// full-text tables, and the comments name this project's own files.
//
// The SQLite worker `androidx.sqlite:sqlite-web` talks to. That library ships only the Kotlin half
// of the protocol, so this is the other half.
//
// Envelope: every message in either direction is `{ id, data, error }`. `data` carries the request
// (`{ cmd, ... }`) or its result, and a non-null `error` makes the Kotlin side throw.
//
// This is an ES module worker, because the official sqlite-wasm build is ESM only and cannot be
// pulled in with `importScripts`. `PlatformDatabaseModule.wasmJs.kt` constructs it with `type: "module"`.

import sqlite3InitModule from "./sqlite/sqlite3.mjs";

// SQLite's own type codes, matching androidx.sqlite's SQLITE_DATA_* constants.
const INTEGER = 1;
const FLOAT = 2;
const TEXT = 3;
const BLOB = 4;
const NULL = 5;

// Matches ABIT_DATABASE_NAME in core:database. The pool uses it as the pool-internal filename.
const DEFAULT_DATABASE_NAME = "abit.db";

let sqlite3 = null;
// The OPFS sync-access-handle pool, or null when this browser has no OPFS to give us.
let sahPool = null;
const databases = new Map();
const statements = new Map();
let nextDatabaseId = 1;
let nextStatementId = 1;

const ready = sqlite3InitModule().then(async (module) => {
    sqlite3 = module;
    // opfs-sahpool, not OpfsDb: the sync-access-handle pool needs no SharedArrayBuffer, so the page
    // does not have to be cross-origin isolated. COOP/COEP would break the Firebase auth popup and
    // every cross-origin fetch, which is why the first cut of this file stayed in memory.
    try {
        sahPool = await sqlite3.installOpfsSAHPoolVfs({ name: "abit", initialCapacity: 6 });
    } catch (failure) {
        // Private windows, third-party-storage blocking and some embedded webviews have no OPFS. The
        // app still runs; it simply forgets on reload, which is what it did before this change.
        console.warn("[ABit] OPFS unavailable, falling back to an in-memory database:", failure);
        sahPool = null;
    }
});

self.onmessage = async (event) => {
    const { id, data } = event.data;
    try {
        await ready;
        const result = handle(data);
        // `close` is sent fire and forget: the Kotlin side registers no pending message for it, and
        // answering one it is not waiting on is reported as an unexpected result.
        if (data.cmd !== "close") {
            self.postMessage({ id, data: result, error: null });
        }
    } catch (failure) {
        // The Kotlin side rethrows this text, so send something a stack trace can be read from.
        self.postMessage({ id, data: null, error: describe(failure) });
    }
};

function describe(failure) {
    if (failure && failure.message) return failure.message;
    return String(failure);
}

function handle(request) {
    switch (request.cmd) {
        case "open":
            return open(request);
        case "prepare":
            return prepare(request);
        case "step":
            return step(request);
        case "close":
            return close(request);
        default:
            throw new Error(`Unknown command: ${request.cmd}`);
    }
}

function open(request) {
    const name = request.name ?? DEFAULT_DATABASE_NAME;
    const db = sahPool ? new sahPool.OpfsSAHPoolDb(name) : new sqlite3.oo1.DB(":memory:", "c");
    const databaseId = nextDatabaseId++;
    databases.set(databaseId, db);
    // `persistent` is informational: the driver reads `databaseId` and ignores the rest. It is here
    // so a console inspection of the envelope answers "did OPFS work" without reading this file.
    return { databaseId, persistent: sahPool !== null };
}

function prepare(request) {
    const db = databases.get(request.databaseId);
    if (!db) throw new Error(`No database with id ${request.databaseId}`);
    const statement = db.prepare(request.sql);
    const statementId = nextStatementId++;
    statements.set(statementId, statement);
    return {
        statementId,
        // Both are properties on the oo1 API, not getters.
        parameterCount: statement.parameterCount,
        // `getColumnNames` asserts that column 0 exists, so a statement that returns no columns at
        // all — an INSERT, a CREATE — has to skip it rather than throw.
        columnNames: statement.columnCount > 0 ? statement.getColumnNames([]) : [],
    };
}


function step(request) {
    const statement = statements.get(request.statementId);
    if (!statement) throw new Error(`No statement with id ${request.statementId}`);

    // One `step` request runs the statement to completion, so each one starts from a clean slate.
    statement.reset();
    // `clearBindings` is only meaningful, and only safe, when there is something bound.
    if (statement.parameterCount > 0) {
        statement.clearBindings();
    }

    // The array is 0-based while SQLite's bind indexes start at 1, so slot i is parameter i + 1.
    const bindings = request.bindings || [];
    for (let index = 0; index < bindings.length; index++) {
        const value = bindings[index];
        if (value !== undefined) {
            statement.bind(index + 1, value === null ? null : value);
        }
    }

    const rows = [];
    while (statement.step()) {
        rows.push(statement.get([]));
    }
    return { rows, columnTypes: columnTypesOf(statement, rows) };
}

/**
 * A column's type is only knowable once there is a row to look at, so this reads the first one and
 * falls back to NULL for an empty result — which is what the Kotlin side does per cell anyway.
 */
function columnTypesOf(statement, rows) {
    const columnCount = statement.columnCount;
    const first = rows.length > 0 ? rows[0] : null;
    const types = new Array(columnCount);
    for (let column = 0; column < columnCount; column++) {
        types[column] = first === null ? NULL : typeOf(first[column]);
    }
    return types;
}

function typeOf(value) {
    if (value === null || value === undefined) return NULL;
    if (typeof value === "string") return TEXT;
    if (typeof value === "bigint") return INTEGER;
    if (typeof value === "number") return Number.isInteger(value) ? INTEGER : FLOAT;
    if (value instanceof Uint8Array) return BLOB;
    return NULL;
}

function close(request) {
    if (request.statementId !== null && request.statementId !== undefined) {
        statements.get(request.statementId)?.finalize();
        statements.delete(request.statementId);
    }
    if (request.databaseId !== null && request.databaseId !== undefined) {
        databases.get(request.databaseId)?.close();
        databases.delete(request.databaseId);
    }
    return null;
}
