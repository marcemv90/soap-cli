---
layout: default
title: Placeholders
---

# Placeholders

`SoapCli` lets you parameterize your SOAP XML using **placeholders with built-in defaults**.
This is useful when you want to:

- Reuse the same XML template across environments.
- Inject values (IDs, tags, headers, etc.) from the command line.
- Keep sensible defaults in the XML while still allowing overrides.

!!! note
    Of course this is just very basic parameterization. For more advanced use cases, you will need to use a proper scripting and pipe the resulting XML from stdin.

## Placeholder syntax

A placeholder has the following syntax and can appear **anywhere** in the XML text:

```console
___NAME:defaultValue___
```

Where:

- `NAME` is the placeholder key.
- `defaultValue` is used when you do **not** provide an override on the CLI.

## Overriding placeholders with `--arg`

At runtime, you override placeholders using the repeatable `--arg` option:

```console
--arg NAME=VALUE
```

Because replacement happens on the raw XML text **before** any attachment or MTOM processing,
a placeholder can be used almost anywhere in the XML:

- Element names.
- Attribute names and values.
- Text nodes.
- Comments.
- CDATA sections.
- Namespace prefixes and URIs.

Example XML snippet:

```xml
<___TAG:order___ id="___CID:defaultCid___">
  <!-- Request for ___TAG:order___ with CID ___CID:defaultCid___ -->
  <![CDATA[customerId=___CID:defaultCid___]]>
</___TAG:order___>
```

Run with overrides:

```bash
java -jar SoapCLI.jar \
  --endpoint https://example.com/soap \
  --request-file request.xml \
  --arg TAG=invoice \
  --arg CID=abc123
```

If you omit `--arg TAG=...` or `--arg CID=...`, the defaults from the XML (`order` and
`defaultCid` in the example above) are used automatically.
