# Translate API Wrapper

A lightweight HTTP wrapper for translation.
Built specifically for `>|||> Fish Mindustry Servers`, but usable otherwise.

## Current support

- Backends: Google Translate, DeepL (Planned: LibreTranslate)
- Language auto-detection (`from: auto`)
- Persistent translation cache (MongoDB)
- Request rate limiting

## Configuration

Root [`config.yml`](config.yml) is an example config.

Backend-related keys:
- `google: true|false`
- `deepl: true|false`
- `deepl-auth-key: "<your deepl api key>"` (required when `deepl: true`)

## Endpoints

### `POST /api/translate`

Headers:
- `token: <token>`
- `to: <lang>`
- `from: <lang>|auto` (optional, defaults to `auto`)
- `backend: google|deepl|null` (optional, defaults to first configured backend)

Body:
- Plain text to translate

Response:
- Header `backend: <backend>|cache`
- Header `time: <ms>`
- Body translated text

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
