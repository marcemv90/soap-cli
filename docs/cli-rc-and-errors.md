---
layout: default
title: Return codes and errors
---

# Return codes and errors

`SoapCli` is designed to be script/CI friendly. The exit code reflects the HTTP status code or
the presence of non-HTTP errors so you can use it in shell scripts or pipelines.

## Exit codes

- HTTP **2xx** or **3xx** → exit code **0**.
- HTTP **4xx** or **5xx** → exit code is the HTTP status (e.g. 404, 500).
- Non-HTTP errors (I/O failures, malformed config, etc.) → exit code **1**.

In all cases where an HTTP response is available, the tool prints **only the response body**
(e.g. SOAP XML), without the Java stack trace.

## Using exit codes from scripts

### Bash example

```bash
#!/usr/bin/env bash

set -euo pipefail

java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml

status=$?

if [ "$status" -eq 0 ]; then
  echo "Request succeeded"
elif [ "$status" -ge 400 ] && [ "$status" -lt 600 ]; then
  echo "HTTP error from server: $status" >&2
  exit "$status"
else
  echo "Non-HTTP error while calling SoapCli (exit code $status)" >&2
  exit 1
fi
```

This pattern lets you:

- Treat 2xx/3xx as success.
- Surface specific HTTP error codes (4xx/5xx) in your CI logs.
- Differentiate HTTP failures from local problems (network issues, bad config, etc.).

### Using `set -e` carefully

If you use `set -e` in scripts, remember that non-zero exit codes will stop the script immediately.
You may want to capture and inspect the `SoapCli` exit code explicitly (as in the example above)
so you can decide how to handle different failure cases.
