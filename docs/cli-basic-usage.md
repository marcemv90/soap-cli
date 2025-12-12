---
layout: default
title: Basic CLI usage
---

# Basic usage

## Required arguments

At minimum, you must specify an endpoint and a request body:

```bash
java -jar SoapCLI.jar --endpoint https://example.com/soap
```

The request body can be provided either from a file or from stdin.

**From a file**

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml
```

**From stdin**

```bash
# Read the output from a script
./my-script.sh | java -jar SoapCLI.jar --endpoint https://example.com/soap

# Or from a command
sed 's/old/new/g' request.xml | java -jar SoapCLI.jar --endpoint https://example.com/soap
```

!!! tip
    Piping from stdin is especially useful when you want to generate the request body dynamically, for example from the output of a previous command or script.

If neither `--request-file` is provided nor data is available on stdin, the tool will fail with an error.

## Custom HTTP headers (SOAPAction, correlation IDs, etc.)

Some SOAP endpoints require additional HTTP headers (for example `SOAPAction`, correlation IDs, or custom authentication schemes). You can add arbitrary headers using the repeatable `--header` option:

- `--header "Name: value"` – adds an HTTP request header with the given name and value.

You can specify `--header` multiple times; headers are applied after the defaults so they can override them when using the same header name.

Example: set a `SOAPAction` header:

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --header "SOAPAction: \"urn:myAction\""
```

Example: multiple custom headers:

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --header "X-Correlation-ID: 12345" \
  --header "X-Env: acceptance"
```

## Getting help

To see all available options:

```bash
java -jar SoapCLI.jar --help
```
