# soap-cli – Improvement Ideas Checklist

## 1. Request / Transport Features

- [x] **NOT_TESTED**Custom HTTP headers (e.g. `--header "Name: value"`, repeatable)
- [ ] Dedicated `--soap-action` option (sets `SOAPAction` header)
- [x] HTTP Basic auth (e.g. `--auth-basic user:pass`)
- [x] HTTP Basic auth (e.g. `--auth-basic-env VAR`)
- [ ] Bearer token auth (e.g. `--auth-bearer <token>`)
- [ ] Configurable connect timeout (e.g. `--connect-timeout-ms`)
- [ ] Configurable read timeout (e.g. `--read-timeout-ms`)
- [ ] Option to follow redirects (e.g. `--follow-redirects`)
- [ ] Transparent gzip/deflate support (send `Accept-Encoding` and auto-decompress)
- [ ] Option to disable compression handling (e.g. `--no-compression`)

## 2. Profiles & Configuration

- [ ] Profile inheritance (e.g. `"extends": "base-profile"`)
- [ ] Environment variable interpolation in profiles (e.g. `"password": "${SOAPCLI_PASS}"`)
- [x] CLI to list profiles (e.g. `--list-profiles`)
- [x] CLI to list profiles (e.g. `--list-profiles-detailed`)
- [ ] CLI to show a profile (e.g. `--show-profile <name>`)

## 3. Output, UX & Scripting

- [ ] Option to write response body to file (e.g. `--output-file response.xml`)
- [ ] Machine-readable logging mode (e.g. `--log-json` to stderr)
- [ ] `--version` flag printing tool version and exiting

## 4. Attachments / MTOM Enhancements

- [ ] Directory-based attachments (e.g. `--attachment-dir <dir>` with a filename→CID convention)
- [ ] Per-attachment Content-Type override (e.g. `--attachment cid=/tmp/file.bin:text/plain`)
- [ ] Strict attachment validation mode (e.g. `--validate-cids` to ensure matching CIDs)

## 5. Security & Keystore Handling

- [ ] Keystore password from environment variable (e.g. `--keystore-password-env VAR`)
- [ ] Separate truststore configuration (`--truststore`, `--truststore-type`, `--truststore-password`)
- [ ] TLS protocol configuration (e.g. `--tls-protocols TLSv1.2,TLSv1.3`)

## 6. Developer Experience & Robustness

- [ ] Add unit tests for XML pretty-printing
- [ ] Add unit tests for XML colorization
- [ ] Add unit tests for profile loading and precedence
- [ ] Add unit tests for inline attachment handling
- [ ] Add unit tests for MTOM/XOP multipart generation
- [ ] Refactor `SoapCLI` into smaller classes (config parsing, profiles, HTTP client, XML utilities)
- [ ] Improve and standardize error messages (profiles, proxy, keystore, attachments)

## 7. Stretch Features

- [ ] WSDL-aware mode (read WSDL, pick operation, generate skeleton request)
- [ ] Snapshot/fixture mode (e.g. `--snapshot-dir snapshots/foo` to store request+response pairs)
