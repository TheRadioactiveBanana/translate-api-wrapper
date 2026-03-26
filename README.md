# Translate API Wrapper

A lightweight HTTP wrapper for translation.
Built specifically for `>|||> Fish Mindustry Servers`, but usable otherwise.

## Current support

- Backends: Google Translate, DeepL, LibreTranslate, and an LLM-compatible HTTP backend
- Language auto-detection (`from: auto`)
- Translation cache (`memory` or MongoDB-backed)
- Request rate limiting

## Configuration

Root [`config.yml`](config.yml) is an example config.

Cache-related keys:
- `cache.type: memory|db` (optional, defaults to `memory`)
- `cache.max-entries: <int>` (optional, defaults to `1000`)
- `cache.expire-minutes: <int>` (optional, defaults to `10`)
- `db.uri`, `db.database` (required only when `cache.type: db`)

Backend-related keys:
- `google: true|false`
- `deepl: true|false`
- `deepl-auth-key: "<your deepl api key>"` (required when `deepl: true`)
- `libre: true|false`
- `libre-url: "<libretranslate /translate endpoint>"` (optional)
- `llm: true|false`
- `llm-url: "http://127.0.0.1:8081/v1/chat/completions"` (required when `llm: true`)
- `llm-max-tokens: <int>` (optional, defaults to `256`)

Proxy-related keys:
- `proxy.enabled: true|false` (optional, defaults to `false`)
  When enabled, client IPs are resolved from `Forwarded`, then `X-Forwarded-For`, then `X-Real-IP`.

## Endpoints

### `POST /api/translate`

Headers:
- `token: <token>`
- `to: <lang>`
- `from: <lang>|auto` (optional, defaults to `auto`)
- `backend: google|deepl|libre|llm|null` (optional, defaults to first configured backend)

Body:
- Plain text to translate

Response:
- Header `backend: <backend>|cache`
- Header `time: <ms>`
- Body: translated text

### `GET /api/languages`

Returns a JSON array of supported language objects:

```json
[
  {
    "code": "en",
    "name": "English"
  }
]
```
