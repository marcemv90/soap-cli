---
layout: default
title: Profiles
---

# Profiles

Profiles let you define reusable configuration presets for different environments
(such as `acceptance`, `staging`, `production`). A profile can define:

- Endpoint URL.
- Default request file.
- Keystore configuration (path, type, alias, passwords).
- Proxy configuration.
- Default theme.

If a profile contains a key with an empty value, it will be ignored, and considered as if it was not present.

!!! note
    Any CLI flag that has a corresponding field in the profiles JSON can be configured per profile.
    CLI flags still override the profile values at runtime.

## Default profiles file location

By default, `SoapCli` looks for the profiles file at:

- `~/.config/soap-cli-profiles.json`

This is resolved using the current user’s home directory (`user.home`), so it works on:

- **Linux/macOS** – e.g. `/home/user/.config/soap-cli-profiles.json`.

You can override this location with the `--profile-file` argument.

## Profiles file structure

The profiles file is a JSON object with a top-level `profiles` object whose keys are
profile names:

```json
{
  "profiles": {
    "acceptance": {
      "endpoint": "https://acceptance.example.com/soap",
      "requestFile": "requests/acceptance-default.xml",
      "keystore": {
        "path": "/home/user/certs/acceptance.jks",
        "type": "JKS",
        "alias": "acceptance-client",
        "password": "changeit",
        "aliasPkPass": "changeit"
      },
      "proxy": {
        "type": "socks",
        "host": "127.0.0.1",
        "port": 1080,
        "user": "proxy-user",
        "password": "proxy-pass"
      },
      "theme": "nord"
    },
    "production": {
      "endpoint": "https://prod.example.com/soap",
      "keystore": {
        "path": "/home/user/certs/prod.p12",
        "type": "PKCS12",
        "alias": "prod-client",
        "password": "super-secret",
        "aliasPkPass": "super-secret"
      }
    }
  }
}
```

### Complete example with all fields

Below is a single profile that demonstrates **all** available fields:

```json
{
  "profiles": {
    "full-example": {
      "endpoint": "https://api.example.com/soap",
      "requestFile": "requests/example.xml",
      "theme": "nord",
      "keystore": {
        "path": "/home/user/certs/client-keystore.p12",
        "type": "PKCS12",
        "alias": "client-cert",
        "password": "changeit",
        "aliasPkPass": "changeit"
      },
      "proxy": {
        "type": "http",
        "host": "proxy.example.com",
        "port": 3128,
        "user": "proxy-user",
        "password": "proxy-pass"
      }
    }
  }
}
```

### Fields

- `profiles` – object mapping profile name → profile config.
    - `profiles.<name>.endpoint` – endpoint URL for this profile.
    - `profiles.<name>.requestFile` – default request file path (optional).
    - `profiles.<name>.theme` – default color theme for XML output (optional), one of the themes supported. See [Themes](cli-themes.md).
    - `profiles.<name>.keystore` – keystore configuration (optional):
        - `path` – keystore file path.
        - `type` – keystore type (e.g. `JKS`, `PKCS12`).
        - `alias` – certificate alias.
        - `password` – keystore password.
        - `aliasPkPass` – key password if different from `password`.
    - `profiles.<name>.proxy` – proxy configuration (optional):
        - `type` – `http` or `socks`.
        - `host` – proxy host.
        - `port` – proxy port.
        - `user` – proxy username (optional).
        - `password` – proxy password (optional).

## Using a profile

Select a profile with `--profile`:

```bash
java -jar SoapCLI.jar \
  --profile acceptance \
  --request-file my-request.xml
```

You can override values from the profile using normal flags. For example,
override the endpoint while keeping everything else from the profile:

```bash
java -jar SoapCLI.jar \
  --profile acceptance \
  --endpoint https://override.example.com/soap \
  --request-file override-request.xml
```

## Overriding the profiles file location

Use `--profile-file` to specify a different JSON file:

```bash
java -jar SoapCLI.jar \
  --profile production \
  --profile-file /opt/soap-cli/custom-profiles.json \
  --request-file request.xml
```

If `--profile` is specified but the profiles file is missing or malformed,
the tool will fail with a descriptive error.

## Listing available profiles

You can list the names of all profiles defined in a profiles file using `--list-profiles`.
Only the profile names are printed; passwords and other sensitive values are never shown.

List profiles from the default location:

```bash
java -jar SoapCLI.jar --list-profiles
```

List profiles from a specific profiles file:

```bash
java -jar SoapCLI.jar \
  --list-profiles \
  --profile-file /opt/soap-cli/custom-profiles.json
```

## Detailed profile view

For troubleshooting, you can print a more detailed view of the profiles configuration using
`--list-profiles-detailed`. This shows the main fields for each profile but masks password
values so they are never printed in clear text.

Show detailed profiles from the default location:

```bash
java -jar SoapCLI.jar --list-profiles-detailed
```

Show detailed profiles from a specific profiles file:

```bash
java -jar SoapCLI.jar \
  --list-profiles-detailed \
  --profile-file /opt/soap-cli/custom-profiles.json
```

## Precedence rules

When both profiles and CLI flags are used:

1. The selected profile (from the chosen profiles file) is loaded first.
2. CLI flags override the profile’s values.

You must end up with a valid configuration:

- `endpoint` must be set (from profile or `--endpoint`).
- If a keystore is configured (profile or CLI), the keystore type, alias, and password
  must all be provided (from either source).
