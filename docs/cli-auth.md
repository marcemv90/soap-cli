---
layout: default
title: Authentication
---

# Authentication

This page covers:

- Using client certificates and custom trust via keystores.
- HTTP Basic authentication options.

## HTTPS and keystore options

For HTTPS endpoints that require a client certificate (mutual TLS) or a custom trust configuration,
you can specify a keystore:

- `--keystore <file>` – keystore path (e.g. JKS, PKCS12).
- `--keystore-type <type>` – keystore type, defaults to `JKS` if not overridden.
- `--keystore-alias <alias>` – alias of the certificate/key to use.
- `--keystore-password <password>` – keystore password (also used as key password by default).
- `--keystore-alias-pkpass <password>` – private key password if different from the keystore password.

Example:

```bash
java -jar SoapCLI.jar \
  --endpoint https://secure.example.com/soap \
  --request-file request.xml \
  --keystore /home/user/certs/keystore.jks \
  --keystore-type JKS \
  --keystore-alias client-cert \
  --keystore-password changeit
```

When a keystore is configured, all three of `keystoreType`, `keystoreAlias`, and `keystorePassword`
must be provided (either directly via CLI flags or via a profile).

## HTTP Basic authentication

Some SOAP endpoints require HTTP Basic authentication at the transport level. You can send a
`Basic` `Authorization` header using either a direct value or an environment variable:

- `--auth-basic user:password` – sets `Authorization: Basic ...` using the given `user:password`.
- `--auth-basic-env VAR` – reads `user:password` from the environment variable `VAR` and uses it
  as the Basic credential.

If both options are used, the explicit `--auth-basic` value takes precedence over `--auth-basic-env`.

Example (direct value):

```bash
java -jar SoapCLI.jar \
  --endpoint https://secure.example.com/soap \
  --request-file request.xml \
  --auth-basic user:password
```

Example (environment variable):

```bash
export SOAPCLI_BASIC_AUTH='user:password'

java -jar SoapCLI.jar \
  --endpoint https://secure.example.com/soap \
  --request-file request.xml \
  --auth-basic-env SOAPCLI_BASIC_AUTH
```

**Security note**: Putting credentials directly on the command line means they may appear in your
shell history or process listings. Environment variables can reduce this exposure, but you should
still treat them as sensitive. Consider using dedicated test accounts and avoid reusing production
passwords.
