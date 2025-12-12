---
layout: default
---

# TO-DO List

!!! info "Disclaimer"
    This list is not exhaustive and is subject to change. I'm developing this tool in my free time. I'm not a professional developer, so please don't expect miracles. I'm open to contributions, but I can't promise anything.

    This project is PR welcome!

## 1. Request / Transport Features

- [x] Custom HTTP headers (e.g. `--header "Name: value"`, repeatable)
- [x] HTTP Basic auth (e.g. `--auth-basic user:pass`)
- [x] HTTP Basic auth (e.g. `--auth-basic-env VAR`)
- [ ] Bearer token auth (e.g. `--auth-bearer <token>`)
- [ ] Option to follow redirects (e.g. `--follow-redirects`)

## 2. Profiles & Configuration

- [ ] Profile inheritance (e.g. `"extends": "base-profile"`)
- [ ] Environment variable interpolation in profiles (e.g. `"password": "${SOAPCLI_PASS}"`)
- [x] CLI to list profiles (e.g. `--list-profiles`)
- [x] CLI to list profiles (e.g. `--list-profiles-detailed`)
- [ ] CLI to show a profile (e.g. `--show-profile <name>`)

## 3. Output, UX & Scripting

- [ ] Machine-readable logging mode (e.g. `--log-json` to stderr)
- [ ] `--version` flag printing tool version and exiting

## 4. Attachments / MTOM Enhancements

- [ ] Directory-based attachments (e.g. `--attachment-dir <dir>` with a filename→CID convention)
- [ ] Per-attachment Content-Type override (e.g. `--attachment cid=/tmp/file.bin:text/plain`)

## 5. Security & Keystore Handling

- [ ] Keystore password from environment variable (e.g. `--keystore-password-env VAR`)
- [ ] Separate truststore configuration (`--truststore`, `--truststore-type`, `--truststore-password`)
- [ ] TLS protocol configuration (e.g. `--tls-protocols TLSv1.2,TLSv1.3`)

## 6. Developer Experience & Robustness

- [ ] Refactor `SoapCLI` into smaller classes (config parsing, profiles, HTTP client, XML utilities)
- [ ] Improve and standardize error messages (profiles, proxy, keystore, attachments)

## 7. Other

- [ ] WSDL-aware mode (read WSDL, pick operation, generate skeleton request)

