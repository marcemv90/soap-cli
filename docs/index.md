---
layout: default
title: Home
---

# SoapCli

`SoapCli` is a small, dependency-light command line tool for sending SOAP requests over HTTP/HTTPS.

It is designed to be:

- **Simple** – single JAR, straightforward options.
- **Script-friendly** – works well in shell scripts and CI pipelines.
- **Flexible** – supports HTTPS with client certificates and configurable profiles.

---

!!! info "Disclaimer"
    This project is being developed by a monkey with a shotgun (me) with the help of an AI agent. It is your responsibility to test the code and ensure it works as expected in your environment before using it in production or critical systems.

---

## Requirements

- Java **11** or later to run the JAR.
- Maven or Docker (optional) if you want to build the JAR from source.

---

## Main Features

- Send SOAP requests to a configurable HTTP/HTTPS endpoint with or without attachments.
- Request body from file or piped from stdin
- HTTPS with client certificates / custom trust using a keystore
- Pretty-printed and optionally colored XML output for human-friendly inspection.
- HTTP/HTTPS and SOCKS proxy support (including SOCKS DNS resolution) via CLI flags or profiles.
- Exit codes mirror HTTP status so scripts can act on success/failure.
- Named configuration profiles so you can define presets for different environments (e.g. `acceptance`, `production`).
