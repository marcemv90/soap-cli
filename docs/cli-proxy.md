---
layout: default
title: Proxy support
---

# Proxy support

`SoapCli` supports both HTTP/HTTPS and SOCKS proxies.

## Proxy via CLI flags

- `--proxy-type <type>` – `http` or `socks`.
- `--proxy-host <host>` – proxy host name or IP.
- `--proxy-port <port>` – proxy port.
- `--proxy-user <user>` – proxy username (optional).
- `--proxy-password <password>` – proxy password (optional).

Example: HTTP proxy

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --proxy-type http \
  --proxy-host proxy.example.com \
  --proxy-port 3128
```

Example: SOCKS5 proxy (DNS resolved by the proxy)

```bash
java -jar SoapCLI.jar \
  --endpoint https://internal.service/soap \
  --request-file request.xml \
  --proxy-type socks \
  --proxy-host 127.0.0.1 \
  --proxy-port 1080
```

## Proxy in profiles

Proxy settings can also be defined in profiles under a `proxy` object:

```json
{
  "profiles": {
    "myProfile": {
      "endpoint": "https://example.com/soap",
      "requestFile": "requests/example.xml",
      "proxy": {
        "type": "socks",
        "host": "127.0.0.1",
        "port": 1080,
        "user": "user",
        "password": "pass"
      }
    }
  }
}
```

CLI flags still override profile values.
