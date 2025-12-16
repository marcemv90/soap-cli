---
layout: default
title: Debug and dry-run
---

# Debug and dry-run

`SoapCli` provides two flags that help you understand exactly what is being sent and to troubleshoot problems:

- `--dry-run` – show what would be sent **without** performing the HTTP call.
- `--debug` – send the request **and** print extra diagnostics to stderr.

## Dry-run mode (`--dry-run`)

Use `--dry-run` when you want to verify the effective configuration and SOAP body but do not want to touch the remote service.

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --dry-run
```

Dry-run mode prints:

- Endpoint and proxy information.
- HTTP request headers, including any added via profiles or `--header` / auth options.
- The final SOAP XML body after:
  - Profiles have been applied.
  - XML placeholders and `--arg` overrides have been resolved.
  - Attachments (`--attachment`) and MTOM/XOP (`--mtom`) transformations have been applied.

Then the program exits **without sending** the HTTP request.

The XML body is pretty-printed and, if color output is enabled, also colorized using the current theme.

## Debug mode (`--debug`)

Use `--debug` for low-level troubleshooting while still sending the request.

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --debug
```

When `--debug` is enabled, `SoapCli`:

- Prints a full request preview similar to `--dry-run` (endpoint, proxy info, headers, final SOAP XML body).
- Prints a summary of the HTTP request (endpoint, content length, attachment metadata) while **omitting** raw binary/Base64 payloads.
- Still performs the HTTP call and prints the normal response body to stdout.

All debug information is written to **stderr**, so your normal stdout output (e.g. SOAP response XML) remains suitable for piping or capturing in scripts.

## Combining with other options

Both `--dry-run` and `--debug` take into account the same configuration sources as a normal run:

- Profiles (`--profile`, `--profile-file`).
- XML placeholders and `--arg` overrides.
- Attachments via `--attachment` and `--mtom`.
- Custom headers via `--header`.
- HTTP Basic auth via `--auth-basic` / `--auth-basic-env`.

This makes them ideal for verifying complex requests before running them in a sensitive environment.
