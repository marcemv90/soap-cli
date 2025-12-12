import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class SoapCLI {

    private static class Config {
        String endpoint;
        String requestFile;
        String keystorePath;
        String keystoreType = "JKS";
        String keystoreAlias;
        char[] keystorePassword;
        char[] keystoreAliasPkPass;
        String proxyType; // "http" or "socks"
        String proxyHost;
        Integer proxyPort;
        String proxyUser;
        char[] proxyPassword;
        boolean colorOutput;
        ColorTheme theme = ColorTheme.DARCULA;
        List<Attachment> attachments = new ArrayList<>();
        boolean debug;
        boolean mtom;
        boolean dryRun;
        String authBasic;
        List<Header> headers = new ArrayList<>();
        Map<String, String> placeholders = new HashMap<>();
    }

    private static class Attachment {
        final String cid;
        final File file;

        Attachment(String cid, File file) {
            this.cid = cid;
            this.file = file;
        }
    }

    private static void listProfiles(File profilesFile) {
        if (!profilesFile.exists()) {
            throw new IllegalArgumentException("Profiles file '" + profilesFile.getPath() + "' not found");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(profilesFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read profiles file: " + e.getMessage(), e);
        }

        JsonNode profilesNode = root.get("profiles");
        if (profilesNode == null || !profilesNode.isObject()) {
            throw new IllegalArgumentException("Invalid profiles file: missing 'profiles' object");
        }

        java.util.Iterator<String> it = profilesNode.fieldNames();
        if (!it.hasNext()) {
            System.out.println("No profiles defined in profiles file '" + profilesFile.getPath() + "'.");
            return;
        }

        System.out.println("Available profiles (from '" + profilesFile.getPath() + "'):");
        while (it.hasNext()) {
            String name = it.next();
            System.out.println("- " + name);
        }
    }

    private static void listProfilesDetailed(File profilesFile) {
        if (!profilesFile.exists()) {
            throw new IllegalArgumentException("Profiles file '" + profilesFile.getPath() + "' not found");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(profilesFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read profiles file: " + e.getMessage(), e);
        }

        JsonNode profilesNode = root.get("profiles");
        if (profilesNode == null || !profilesNode.isObject()) {
            throw new IllegalArgumentException("Invalid profiles file: missing 'profiles' object");
        }

        java.util.Iterator<String> it = profilesNode.fieldNames();
        if (!it.hasNext()) {
            System.out.println("No profiles defined in profiles file '" + profilesFile.getPath() + "'.");
            return;
        }

        System.out.println("Profiles details (from '" + profilesFile.getPath() + "'):");
        while (it.hasNext()) {
            String name = it.next();
            JsonNode profile = profilesNode.get(name);
            System.out.println("- " + name + ":");

            JsonNode endpointNode = profile.get("endpoint");
            if (endpointNode != null && !endpointNode.isNull()) {
                System.out.println("    endpoint: " + endpointNode.asText());
            }

            JsonNode requestFileNode = profile.get("requestFile");
            if (requestFileNode != null && !requestFileNode.isNull()) {
                System.out.println("    requestFile: " + requestFileNode.asText());
            }

            JsonNode themeNode = profile.get("theme");
            if (themeNode != null && !themeNode.isNull()) {
                System.out.println("    theme: " + themeNode.asText());
            }

            JsonNode keystoreNode = profile.get("keystore");
            if (keystoreNode != null && keystoreNode.isObject()) {
                System.out.println("    keystore:");
                JsonNode pathNode = keystoreNode.get("path");
                if (pathNode != null && !pathNode.isNull()) {
                    System.out.println("        path: " + pathNode.asText());
                }
                JsonNode typeNode = keystoreNode.get("type");
                if (typeNode != null && !typeNode.isNull()) {
                    System.out.println("        type: " + typeNode.asText());
                }
                JsonNode aliasNode = keystoreNode.get("alias");
                if (aliasNode != null && !aliasNode.isNull()) {
                    System.out.println("        alias: " + aliasNode.asText());
                }
                JsonNode passwordNode = keystoreNode.get("password");
                if (passwordNode != null && !passwordNode.isNull()) {
                    System.out.println("        password: **** (set)");
                }
                JsonNode aliasPkPassNode = keystoreNode.get("aliasPkPass");
                if (aliasPkPassNode != null && !aliasPkPassNode.isNull()) {
                    System.out.println("        aliasPkPass: **** (set)");
                }
            }

            JsonNode proxyNode = profile.get("proxy");
            if (proxyNode != null && proxyNode.isObject()) {
                System.out.println("    proxy:");
                JsonNode typeNode = proxyNode.get("type");
                if (typeNode != null && !typeNode.isNull()) {
                    System.out.println("        type: " + typeNode.asText());
                }
                JsonNode hostNode = proxyNode.get("host");
                if (hostNode != null && !hostNode.isNull()) {
                    System.out.println("        host: " + hostNode.asText());
                }
                JsonNode portNode = proxyNode.get("port");
                if (portNode != null && portNode.canConvertToInt()) {
                    System.out.println("        port: " + portNode.intValue());
                }
                JsonNode userNode = proxyNode.get("user");
                if (userNode != null && !userNode.isNull()) {
                    System.out.println("        user: " + userNode.asText());
                }
                JsonNode passwordNode = proxyNode.get("password");
                if (passwordNode != null && !passwordNode.isNull()) {
                    System.out.println("        password: **** (set)");
                }
            }
        }
    }

    private static class Header {
        final String name;
        final String value;

        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private enum ColorTheme {
        MONOKAI(
            "\u001B[38;5;148m", 
            "\u001B[38;5;81m", 
            "\u001B[38;5;208m"
        ),
        SOLARIZED_DARK(
            "\u001B[38;5;109m", 
            "\u001B[38;5;136m", 
            "\u001B[38;5;166m"
        ),
        SOLARIZED_LIGHT(
            "\u001B[38;5;24m", 
            "\u001B[38;5;60m", 
            "\u001B[38;5;166m"
        ),
        DARCULA(
            "\u001B[38;5;81m", 
            "\u001B[38;5;113m", 
            "\u001B[38;5;179m"
        ),
        GRUVBOX_DARK(
            "\u001B[38;5;142m", 
            "\u001B[38;5;109m", 
            "\u001B[38;5;208m"
        ),
        GRUVBOX_LIGHT(
            "\u001B[38;5;24m", 
            "\u001B[38;5;137m", 
            "\u001B[38;5;166m"
        ),
        DRACULA(
            "\u001B[38;5;170m", 
            "\u001B[38;5;117m", 
            "\u001B[38;5;84m"
        ),
        NORD(
            "\u001B[38;5;110m", 
            "\u001B[38;5;109m", 
            "\u001B[38;5;145m"
        );

        static final String RESET = "\u001B[0m";

        final String TAG;
        final String ATTR_NAME;
        final String ATTR_VALUE;

        ColorTheme(String tag, String attrName, String attrValue) {
            this.TAG = tag;
            this.ATTR_NAME = attrName;
            this.ATTR_VALUE = attrValue;
        }
    }

    private static ColorTheme parseColorTheme(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        switch (v) {
            case "monokai":
                return ColorTheme.MONOKAI;
            case "solarized-dark":
            case "solarized_dark":
            case "solarizeddark":
                return ColorTheme.SOLARIZED_DARK;
            case "solarized-light":
            case "solarized_light":
            case "solarizedlight":
                return ColorTheme.SOLARIZED_LIGHT;
            case "darcula":
                return ColorTheme.DARCULA;
            case "gruvbox-dark":
            case "gruvbox_dark":
            case "gruvboxdark":
                return ColorTheme.GRUVBOX_DARK;
            case "gruvbox-light":
            case "gruvbox_light":
            case "gruvboxlight":
                return ColorTheme.GRUVBOX_LIGHT;
            case "dracula":
                return ColorTheme.DRACULA;
            case "nord":
                return ColorTheme.NORD;
            default:
                throw new IllegalArgumentException(
                        "Unknown color theme: '" + value + "'. Supported themes: " +
                                "monokai, solarized-dark, solarized-light, darcula, " +
                                "gruvbox-dark, gruvbox-light, dracula, nord");
        }
    }

    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Throwable t) {
            printError("Unexpected error: " + t.getMessage(), t);
            System.exit(1);
        }
    }

    private static void normalizeConfig(Config config) {
        if (config == null) {
            return;
        }

        config.endpoint = normalizeString(config.endpoint);
        config.requestFile = normalizeString(config.requestFile);
        config.keystorePath = normalizeString(config.keystorePath);
        config.keystoreType = normalizeString(config.keystoreType);
        config.keystoreAlias = normalizeString(config.keystoreAlias);
        config.proxyType = normalizeString(config.proxyType);
        config.proxyHost = normalizeString(config.proxyHost);
        config.proxyUser = normalizeString(config.proxyUser);
        config.authBasic = normalizeString(config.authBasic);

        if (config.keystorePassword != null && config.keystorePassword.length == 0) {
            config.keystorePassword = null;
        }
        if (config.keystoreAliasPkPass != null && config.keystoreAliasPkPass.length == 0) {
            config.keystoreAliasPkPass = null;
        }
        if (config.proxyPassword != null && config.proxyPassword.length == 0) {
            config.proxyPassword = null;
        }
    }

    private static String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String prettyPrintXml(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            return xml;
        }
    }

    private static String sanitizeXmlText(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }

        StringBuilder sb = new StringBuilder(xml.length());
        for (int i = 0; i < xml.length(); i++) {
            char ch = xml.charAt(i);

            // Strip common invisible characters that tend to break external XML tools
            if (ch == '\u200B' || ch == '\u200C' || ch == '\u200D' || ch == '\uFEFF') {
                continue;
            }

            // XML 1.0 allows TAB, LF, CR and characters >= 0x20, all others are forbidden control chars
            if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') {
                continue;
            }

            // Also drop DEL
            if (ch == 0x7F) {
                continue;
            }

            sb.append(ch);
        }
        return sb.toString();
    }

    private static String colorizeXml(String xml, ColorTheme theme) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }

        if (theme == null) {
            theme = ColorTheme.DARCULA;
        }
        final String RESET = ColorTheme.RESET;
        final String TAG = theme.TAG;
        final String ATTR_NAME = theme.ATTR_NAME;
        final String ATTR_VALUE = theme.ATTR_VALUE;

        StringBuilder result = new StringBuilder();
        Pattern tagPattern = Pattern.compile("<[^>]+>");
        Matcher matcher = tagPattern.matcher(xml);
        int lastEnd = 0;
        while (matcher.find()) {
            // text between tags (leave uncolored)
            result.append(xml, lastEnd, matcher.start());

            String tag = matcher.group();
            StringBuilder coloredTag = new StringBuilder();
            coloredTag.append(TAG).append("<");

            String inner = tag.substring(1, tag.length() - 1);

            // handle closing tags like </foo>
            if (inner.startsWith("/")) {
                coloredTag.append("/");
                inner = inner.substring(1).trim();
                coloredTag.append(inner).append(">").append(RESET);
                result.append(coloredTag);
                lastEnd = matcher.end();
                continue;
            }

            // self-closing detection
            boolean selfClosing = inner.endsWith("/");
            if (selfClosing) {
                inner = inner.substring(0, inner.length() - 1).trim();
            }

            // Split into name and attributes
            String[] parts = inner.split("\\s+", 2);
            String name = parts[0];
            String attrs = parts.length > 1 ? parts[1] : null;

            coloredTag.append(name);

            if (attrs != null && !attrs.isEmpty()) {
                // Attribute names may contain characters such as ':' (e.g. xmlns:xmime),
                // so match up to but not including the '=' rather than just \w.
                Pattern attrPattern = Pattern.compile("([^\\s=]+)(\\s*=\\s*)(\"[^\"]*\"|'[^']*')");
                Matcher am = attrPattern.matcher(attrs);
                int attrLastEnd = 0;

                // The original XML had at least one space between the tag name and
                // the first attribute. Because we split on whitespace above, that
                // space is no longer present in 'attrs', so we reintroduce a single
                // space here to keep the output well-formed.
                coloredTag.append(' ');

                while (am.find()) {
                    // whitespace or other chars before the attribute
                    coloredTag.append(attrs, attrLastEnd, am.start());
                    coloredTag.append(ATTR_NAME).append(am.group(1)).append(RESET);
                    coloredTag.append(am.group(2));
                    coloredTag.append(ATTR_VALUE).append(am.group(3)).append(RESET);
                    attrLastEnd = am.end();
                }
                coloredTag.append(attrs.substring(attrLastEnd));
            }

            if (selfClosing) {
                coloredTag.append("/");
            }
            coloredTag.append(">").append(RESET);

            result.append(coloredTag);
            lastEnd = matcher.end();
        }
        result.append(xml.substring(lastEnd));
        return result.toString();
    }

    private static int run(String[] args) {
        if (Arrays.asList(args).contains("--list-profiles-detailed")) {
            String profileFilePath = null;
            for (int i = 0; i < args.length; i++) {
                if ("--profile-file".equals(args[i]) && i + 1 < args.length) {
                    profileFilePath = args[i + 1];
                    break;
                }
            }

            File profilesFile = resolveProfilesFile(profileFilePath);
            try {
                listProfilesDetailed(profilesFile);
                return 0;
            } catch (IllegalArgumentException e) {
                printError("Profiles error: " + e.getMessage(), null);
                return 1;
            }
        }

        if (Arrays.asList(args).contains("--list-profiles")) {
            String profileFilePath = null;
            for (int i = 0; i < args.length; i++) {
                if ("--profile-file".equals(args[i]) && i + 1 < args.length) {
                    profileFilePath = args[i + 1];
                    break;
                }
            }

            File profilesFile = resolveProfilesFile(profileFilePath);
            try {
                listProfiles(profilesFile);
                return 0;
            } catch (IllegalArgumentException e) {
                printError("Profiles error: " + e.getMessage(), null);
                return 1;
            }
        }

        if (args.length == 0) {
            printUsage();
            return 1;
        }

        if (Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
            printUsage();
            return 0;
        }

        Config config;
        try {
            config = parseArgs(args);
        } catch (IllegalArgumentException e) {
            printError("Argument error: " + e.getMessage(), null);
            printUsage();
            return 1;
        }

        String requestBody;
        try {
            requestBody = loadRequestBody(config.requestFile);
        } catch (IOException e) {
            printError("Failed to read request XML: " + e.getMessage(), e);
            return 1;
        }

        String effectiveRequestBody = applyPlaceholders(requestBody, config.placeholders);
        if (!config.mtom) {
            try {
                effectiveRequestBody = applyInlineAttachments(effectiveRequestBody, config.attachments, config.debug);
            } catch (IOException e) {
                printError("Failed to process attachments: " + e.getMessage(), e);
                return 1;
            }

            // After inlining, attachments are no longer needed for HTTP sending (no multipart).
            // However, in dry-run or debug mode we keep them so they can be shown in the summary/preview.
            if (!config.dryRun && !config.debug) {
                config.attachments.clear();
            }
        }

        if (config.dryRun) {
            try {
                performDryRun(config, effectiveRequestBody);
                return 0;
            } catch (Exception e) {
                printError("Dry-run failed: " + e.getMessage(), e);
                return 1;
            }
        }

        if (config.debug) {
            try {
                printRequestPreview(config, effectiveRequestBody, System.err,
                        "==== SOAP-CLI DEBUG: REQUEST PREVIEW ====",
                        "==== END SOAP-CLI DEBUG REQUEST PREVIEW ====");
            } catch (Exception e) {
                printError("Debug preview failed: " + e.getMessage(), e);
            }
        }

        try {
            SoapResponse soapResponse = sendSoapRequest(config, effectiveRequestBody);
            String cleanedBody = sanitizeXmlText(soapResponse.body);
            String output = prettyPrintXml(cleanedBody);

            // Normalize line endings and strip whitespace-only lines so responses
            // don't show empty lines between each visible XML line on some terminals.
            if (output != null) {
                // Normalize Windows CRLF to LF
                output = output.replace("\r\n", "\n");

                // Remove lines that contain only whitespace characters
                String[] lines = output.split("\n", -1);
                StringBuilder normalized = new StringBuilder(output.length());
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].trim().isEmpty()) {
                        continue;
                    }
                    if (normalized.length() > 0) {
                        normalized.append('\n');
                    }
                    normalized.append(lines[i]);
                }
                output = normalized.toString();
            }

            if (config.colorOutput) {
                output = colorizeXml(output, config.theme);
            }

            if (config.debug) {
                System.err.println("==== SOAP-CLI DEBUG: RESPONSE BODY ====");
            }

            System.out.print(output);
            int status = soapResponse.statusCode;
            if (status >= 200 && status < 400) {
                return 0;
            }
            return status > 0 ? status : 1;
        } catch (Exception e) {
            printError("SOAP request failed: " + e.getMessage(), e);
            return 1;
        }
    }

    private static class SoapResponse {
        final int statusCode;
        final String body;

        SoapResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();
        String profileName = null;
        String profileFilePath = null;

        // default: color enabled; can be disabled explicitly with --no-color
        config.colorOutput = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--profile".equals(arg)) {
                profileName = requireNext(args, ++i, "--profile");
            } else if ("--profile-file".equals(arg)) {
                profileFilePath = requireNext(args, ++i, "--profile-file");
            }
        }

        if (profileName != null) {
            File profilesFile = resolveProfilesFile(profileFilePath);
            applyProfile(profileName, profilesFile, config);
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--endpoint":
                    config.endpoint = requireNext(args, ++i, "--endpoint");
                    break;
                case "--request-file":
                    config.requestFile = requireNext(args, ++i, "--request-file");
                    break;
                case "--keystore":
                    config.keystorePath = requireNext(args, ++i, "--keystore");
                    break;
                case "--keystore-type":
                    config.keystoreType = requireNext(args, ++i, "--keystore-type");
                    break;
                case "--keystore-alias":
                    config.keystoreAlias = requireNext(args, ++i, "--keystore-alias");
                    break;
                case "--keystore-password":
                    String ksPass = requireNext(args, ++i, "--keystore-password");
                    config.keystorePassword = ksPass.toCharArray();
                    break;
                case "--keystore-alias-pkpass":
                    String pkPass = requireNext(args, ++i, "--keystore-alias-pkpass");
                    config.keystoreAliasPkPass = pkPass.toCharArray();
                    break;
                case "--proxy-type":
                    config.proxyType = requireNext(args, ++i, "--proxy-type");
                    break;
                case "--proxy-host":
                    config.proxyHost = requireNext(args, ++i, "--proxy-host");
                    break;
                case "--proxy-port":
                    String proxyPortValue = requireNext(args, ++i, "--proxy-port");
                    try {
                        config.proxyPort = Integer.parseInt(proxyPortValue);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid --proxy-port, must be an integer: " + proxyPortValue, e);
                    }
                    break;
                case "--proxy-user":
                    config.proxyUser = requireNext(args, ++i, "--proxy-user");
                    break;
                case "--proxy-password":
                    String proxyPass = requireNext(args, ++i, "--proxy-password");
                    config.proxyPassword = proxyPass.toCharArray();
                    break;
                case "--header":
                    String headerSpec = requireNext(args, ++i, "--header");
                    int colonIdx = headerSpec.indexOf(':');
                    if (colonIdx <= 0 || colonIdx == headerSpec.length() - 1) {
                        throw new IllegalArgumentException("Invalid --header value '" + headerSpec + "'. Expected format 'Name: value'");
                    }
                    String headerName = headerSpec.substring(0, colonIdx).trim();
                    String headerValue = headerSpec.substring(colonIdx + 1).trim();
                    if (headerName.isEmpty()) {
                        throw new IllegalArgumentException("Header name must not be empty in --header '" + headerSpec + "'");
                    }
                    config.headers.add(new Header(headerName, headerValue));
                    break;
                case "--auth-basic-env":
                    String envVar = requireNext(args, ++i, "--auth-basic-env");
                    String envValue = System.getenv(envVar);
                    if (envValue == null) {
                        throw new IllegalArgumentException("Environment variable '" + envVar + "' is not set for --auth-basic-env");
                    }
                    if (config.authBasic == null || config.authBasic.isEmpty()) {
                        config.authBasic = envValue;
                    }
                    break;
                case "--auth-basic":
                    String authBasic = requireNext(args, ++i, "--auth-basic");
                    config.authBasic = authBasic;
                    break;
                case "--theme":
                    String themeValue = requireNext(args, ++i, "--theme");
                    ColorTheme cliTheme = parseColorTheme(themeValue);
                    if (cliTheme != null) {
                        config.theme = cliTheme;
                    }
                    break;
                case "--color":
                    config.colorOutput = true;
                    break;
                case "--no-color":
                    config.colorOutput = false;
                    break;
                case "--mtom":
                    config.mtom = true;
                    break;
                case "--debug":
                    config.debug = true;
                    break;
                case "--dry-run":
                    config.dryRun = true;
                    break;
                case "--profile-file":
                    // Already processed in first pass
                    requireNext(args, ++i, "--profile-file");
                    break;
                case "--profile":
                    // Already processed in first pass
                    requireNext(args, ++i, "--profile");
                    break;
                case "--attachment":
                    String attachmentSpec = requireNext(args, ++i, "--attachment");
                    int eqIdx = attachmentSpec.indexOf('=');
                    if (eqIdx <= 0 || eqIdx == attachmentSpec.length() - 1) {
                        throw new IllegalArgumentException("Invalid --attachment value '" + attachmentSpec + "'. Expected format CID=path");
                    }
                    String cid = attachmentSpec.substring(0, eqIdx).trim();
                    String path = attachmentSpec.substring(eqIdx + 1).trim();
                    if (cid.isEmpty()) {
                        throw new IllegalArgumentException("Attachment CID must not be empty in --attachment " + attachmentSpec);
                    }
                    if (path.isEmpty()) {
                        throw new IllegalArgumentException("Attachment file path must not be empty in --attachment " + attachmentSpec);
                    }
                    File attachmentFile = new File(path);
                    if (!attachmentFile.exists() || !attachmentFile.isFile() || !attachmentFile.canRead()) {
                        throw new IllegalArgumentException("Attachment file '" + path + "' does not exist or is not readable");
                    }
                    config.attachments.add(new Attachment(cid, attachmentFile));
                    break;
                case "--arg":
                case "--placeholder": // backwards-compatible alias
                    String placeholderSpec = requireNext(args, ++i, arg);
                    int placeholderEqIdx = placeholderSpec.indexOf('=');
                    if (placeholderEqIdx <= 0 || placeholderEqIdx == placeholderSpec.length() - 1) {
                        throw new IllegalArgumentException("Invalid " + arg + " value '" + placeholderSpec + "'. Expected format NAME=VALUE");
                    }
                    String placeholderName = placeholderSpec.substring(0, placeholderEqIdx).trim();
                    String placeholderValue = placeholderSpec.substring(placeholderEqIdx + 1).trim();
                    if (placeholderName.isEmpty()) {
                        throw new IllegalArgumentException("Placeholder name must not be empty in " + arg + " '" + placeholderSpec + "'");
                    }
                    config.placeholders.put(placeholderName, placeholderValue);
                    break;
                case "--help":
                case "-h":
                    // handled before
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (config.endpoint == null || config.endpoint.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter --endpoint <URL> or profile endpoint");
        }

        if (config.keystorePath != null) {
            if (config.keystoreType == null || config.keystoreType.isEmpty()) {
                throw new IllegalArgumentException("When using a keystore, keystore type must be provided (via profile or --keystore-type)");
            }
            if (config.keystoreAlias == null || config.keystoreAlias.isEmpty()) {
                throw new IllegalArgumentException("When using a keystore, keystore alias must be provided (via profile or --keystore-alias)");
            }
            if (config.keystorePassword == null || config.keystorePassword.length == 0) {
                throw new IllegalArgumentException("When using a keystore, keystore password must be provided (via profile or --keystore-password)");
            }
        }

        if (config.proxyHost != null || config.proxyPort != null || config.proxyType != null
                || config.proxyUser != null || config.proxyPassword != null) {
            if (config.proxyHost == null || config.proxyHost.isEmpty()) {
                throw new IllegalArgumentException("When using a proxy, proxy host must be provided (via profile or --proxy-host)");
            }
            if (config.proxyPort == null || config.proxyPort <= 0) {
                throw new IllegalArgumentException("When using a proxy, proxy port must be provided and > 0 (via profile or --proxy-port)");
            }
            if (config.proxyType == null || config.proxyType.isEmpty()) {
                throw new IllegalArgumentException("When using a proxy, proxy type must be provided (via profile or --proxy-type). Supported: http, socks");
            }
        }

        normalizeConfig(config);

        return config;
    }

    private static File resolveProfilesFile(String explicitPath) {
        if (explicitPath != null && !explicitPath.isEmpty()) {
            return new File(explicitPath);
        }

        String home = System.getProperty("user.home");
        if (home != null && !home.isEmpty()) {
            File configDir = new File(home, ".config");
            return new File(configDir, "soap-cli-profiles.json");
        }

        return new File("soap-cli-profiles.json");
    }

    private static void applyProfile(String profileName, File profilesFile, Config config) {
        if (!profilesFile.exists()) {
            throw new IllegalArgumentException("Profiles file '" + profilesFile.getPath() + "' not found, but --profile was specified");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(profilesFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read profiles file: " + e.getMessage(), e);
        }

        JsonNode profilesNode = root.get("profiles");
        if (profilesNode == null || !profilesNode.isObject()) {
            throw new IllegalArgumentException("Invalid profiles file: missing 'profiles' object");
        }

        JsonNode profileNode = profilesNode.get(profileName);
        if (profileNode == null || profileNode.isNull()) {
            throw new IllegalArgumentException("Profile '" + profileName + "' not found in profiles file");
        }

        JsonNode endpointNode = profileNode.get("endpoint");
        if (endpointNode != null && !endpointNode.isNull()) {
            config.endpoint = endpointNode.asText();
        }

        JsonNode requestFileNode = profileNode.get("requestFile");
        if (requestFileNode != null && !requestFileNode.isNull()) {
            config.requestFile = requestFileNode.asText();
        }

        JsonNode themeNode = profileNode.get("theme");
        if (themeNode != null && !themeNode.isNull()) {
            ColorTheme profileTheme = parseColorTheme(themeNode.asText());
            if (profileTheme != null) {
                config.theme = profileTheme;
            }
        }

        JsonNode keystoreNode = profileNode.get("keystore");
        if (keystoreNode != null && keystoreNode.isObject()) {
            JsonNode pathNode = keystoreNode.get("path");
            if (pathNode != null && !pathNode.isNull()) {
                config.keystorePath = pathNode.asText();
            }

            JsonNode typeNode = keystoreNode.get("type");
            if (typeNode != null && !typeNode.isNull()) {
                config.keystoreType = typeNode.asText();
            }

            JsonNode aliasNode = keystoreNode.get("alias");
            if (aliasNode != null && !aliasNode.isNull()) {
                config.keystoreAlias = aliasNode.asText();
            }

            JsonNode passwordNode = keystoreNode.get("password");
            if (passwordNode != null && !passwordNode.isNull()) {
                String value = passwordNode.asText();
                config.keystorePassword = value != null ? value.toCharArray() : null;
            }

            JsonNode aliasPkPassNode = keystoreNode.get("aliasPkPass");
            if (aliasPkPassNode != null && !aliasPkPassNode.isNull()) {
                String value = aliasPkPassNode.asText();
                config.keystoreAliasPkPass = value != null ? value.toCharArray() : null;
            }
        }

        JsonNode proxyNode = profileNode.get("proxy");
        if (proxyNode != null && proxyNode.isObject()) {
            JsonNode typeNode = proxyNode.get("type");
            if (typeNode != null && !typeNode.isNull()) {
                config.proxyType = typeNode.asText();
            }

            JsonNode hostNode = proxyNode.get("host");
            if (hostNode != null && !hostNode.isNull()) {
                config.proxyHost = hostNode.asText();
            }

            JsonNode portNode = proxyNode.get("port");
            if (portNode != null && portNode.canConvertToInt()) {
                config.proxyPort = portNode.intValue();
            }

            JsonNode userNode = proxyNode.get("user");
            if (userNode != null && !userNode.isNull()) {
                config.proxyUser = userNode.asText();
            }

            JsonNode passwordNode = proxyNode.get("password");
            if (passwordNode != null && !passwordNode.isNull()) {
                String value = passwordNode.asText();
                config.proxyPassword = value != null ? value.toCharArray() : null;
            }
        }
    }

    private static String requireNext(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static String loadRequestBody(String requestFile) throws IOException {
        if (requestFile != null) {
            try (InputStream in = new FileInputStream(requestFile)) {
                return readAll(in, StandardCharsets.UTF_8);
            }
        }
        // If no request file is provided, read from stdin
        if (System.in.available() == 0) {
            throw new IOException("No --request-file provided and stdin is empty. Provide a file or pipe XML via stdin.");
        }
        return readAll(System.in, StandardCharsets.UTF_8);
    }

    private static String applyPlaceholders(String body, Map<String, String> placeholders) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        Pattern placeholderPattern = Pattern.compile("___([^:]+?):(.*?)___", Pattern.DOTALL);
        Matcher matcher = placeholderPattern.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String defaultValue = matcher.group(2);
            String value = (placeholders != null && placeholders.containsKey(name))
                    ? placeholders.get(name)
                    : defaultValue;
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void printRequestPreview(Config config, String body, PrintStream out, String title, String endTitle) throws Exception {
        out.println(title);
        out.println("Endpoint: " + config.endpoint);

        if (config.proxyHost != null || config.proxyPort != null || config.proxyType != null || config.proxyUser != null) {
            out.println();
            out.println("Proxy:");
            if (config.proxyType != null) {
                out.println("  type: " + config.proxyType);
            }
            if (config.proxyHost != null) {
                out.println("  host: " + config.proxyHost);
            }
            if (config.proxyPort != null) {
                out.println("  port: " + config.proxyPort);
            }
            if (config.proxyUser != null) {
                out.println("  user: " + config.proxyUser);
            }
        }

        boolean hasAttachments = config.attachments != null && !config.attachments.isEmpty();

        Map<String, String> headers = new HashMap<>();
        String contentType;
        String accept = "text/xml, application/soap+xml, */*;q=0.9";
        long contentLength;

        if (config.mtom && hasAttachments) {
            String boundary = "----=_soapcli_mtom_" + System.currentTimeMillis();
            String rootContentId = "<rootpart@soapui.org>";
            contentType = "multipart/related; type=\"application/xop+xml\"; start=\"" + rootContentId +
                    "\"; start-info=\"application/soap+xml\"; action=\"\"; boundary=\"" + boundary + "\"";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeMtomMultipart(baos, body, config.attachments, boundary, rootContentId);
            byte[] bytes = baos.toByteArray();
            contentLength = bytes.length;
        } else {
            contentType = "application/soap+xml; charset=utf-8";
            byte[] bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
            contentLength = bytes.length;
        }

        headers.put("Content-Type", contentType);
        headers.put("Accept", accept);
        headers.put("Content-Length", Long.toString(contentLength));

        if (config.authBasic != null && !config.authBasic.isEmpty()) {
            String encoded = Base64.getEncoder().encodeToString(config.authBasic.getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + encoded);
        }

        if (config.headers != null && !config.headers.isEmpty()) {
            for (Header header : config.headers) {
                if (header != null && header.name != null && !header.name.isEmpty()) {
                    headers.put(header.name, header.value != null ? header.value : "");
                }
            }
        }

        if (hasAttachments) {
            out.println();
            out.println("Attachments:");
            for (Attachment attachment : config.attachments) {
                if (attachment != null && attachment.file != null) {
                    long size = attachment.file.isFile() ? attachment.file.length() : -1L;
                    String sizeInfo = size >= 0 ? size + " bytes" : "unknown size";
                    out.println("  cid=" + attachment.cid + ", file=" + attachment.file.getPath() + ", size=" + sizeInfo);
                } else if (attachment != null) {
                    out.println("  cid=" + attachment.cid + ", file=<null>");
                }
            }
        }

        out.println();
        out.println("HTTP headers:");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            out.println(entry.getKey() + ": " + entry.getValue());
        }

        String xmlToPrint = body != null ? prettyPrintXml(body) : "";

        if (config.mtom && hasAttachments) {
            String mtomSoapBody = body != null ? body : "";
            if (config.attachments != null) {
                String attrPlaceholder = "__soapcli_cid__";
                mtomSoapBody = mtomSoapBody.replace("href=\"cid:", "href=\"" + attrPlaceholder);
                mtomSoapBody = mtomSoapBody.replace("payloadId=\"cid:", "payloadId=\"" + attrPlaceholder);

                for (Attachment attachment : config.attachments) {
                    String token = "cid:" + attachment.cid;
                    String include = "<inc:Include href=\"" + token + "\" xmlns:inc=\"http://www.w3.org/2004/08/xop/include\"/>";
                    mtomSoapBody = mtomSoapBody.replace(token, include);
                }

                mtomSoapBody = mtomSoapBody.replace("href=\"" + attrPlaceholder, "href=\"cid:");
                mtomSoapBody = mtomSoapBody.replace("payloadId=\"" + attrPlaceholder, "payloadId=\"cid:");
            }
            xmlToPrint = prettyPrintXml(mtomSoapBody);
        }

        // Normalize line endings to avoid extra blank lines on some terminals
        if (xmlToPrint != null) {
            xmlToPrint = xmlToPrint.replace("\r\n", "\n");
            // Remove lines that contain only whitespace to avoid visual blank lines
            xmlToPrint = xmlToPrint.replaceAll("(?m)^[ \t]+\n", "");
        }

        if (config.colorOutput) {
            xmlToPrint = colorizeXml(xmlToPrint, config.theme);
        }

        out.println();
        out.println("Request body:");
        out.print(xmlToPrint);
        out.println(endTitle);
    }

    private static void performDryRun(Config config, String body) throws Exception {
        printRequestPreview(config, body, System.out,
                "==== SOAP-CLI DRY-RUN ====",
                "==== END SOAP-CLI DRY-RUN ====");
    }

    private static SoapResponse sendSoapRequest(Config config, String body) throws Exception {
        URI uri = URI.create(config.endpoint);
        URL url = uri.toURL();
        Proxy proxy = null;
        if (config.proxyType != null && config.proxyHost != null && config.proxyPort != null && config.proxyPort > 0) {
            String type = config.proxyType.toLowerCase();
            Proxy.Type proxyType;
            if ("socks".equals(type)) {
                proxyType = Proxy.Type.SOCKS;
            } else if ("http".equals(type)) {
                proxyType = Proxy.Type.HTTP;
            } else {
                throw new IllegalArgumentException("Unsupported proxy type: " + config.proxyType + ". Supported: http, socks");
            }
            proxy = new Proxy(proxyType, new InetSocketAddress(config.proxyHost, config.proxyPort));
        }

        if (config.proxyUser != null && !config.proxyUser.isEmpty() && config.proxyPassword != null && config.proxyPassword.length > 0) {
            final String proxyUser = config.proxyUser;
            final char[] proxyPassword = config.proxyPassword;
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(proxyUser, proxyPassword);
                    }
                    return null;
                }
            });
        }

        HttpURLConnection conn = proxy != null
                ? (HttpURLConnection) url.openConnection(proxy)
                : (HttpURLConnection) url.openConnection();

        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            if (config.keystorePath != null) {
                SSLSocketFactory sslSocketFactory = createSslSocketFactory(config);
                httpsConn.setSSLSocketFactory(sslSocketFactory);
            }
        }

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        if (config.authBasic != null && !config.authBasic.isEmpty()) {
            String encoded = Base64.getEncoder().encodeToString(config.authBasic.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }

        boolean hasAttachments = config.attachments != null && !config.attachments.isEmpty();

        if (config.mtom && hasAttachments) {
            String boundary = "----=_soapcli_mtom_" + System.currentTimeMillis();
            String rootContentId = "<rootpart@soapui.org>";
            conn.setRequestProperty("Content-Type",
                    "multipart/related; type=\"application/xop+xml\"; start=\"" + rootContentId +
                            "\"; start-info=\"application/soap+xml\"; action=\"\"; boundary=\"" + boundary + "\""
            );
            conn.setRequestProperty("Accept", "text/xml, application/soap+xml, */*;q=0.9");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeMtomMultipart(baos, body, config.attachments, boundary, rootContentId);
            byte[] bytes = baos.toByteArray();
            conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));

            if (config.debug) {
                PrintStream err = System.err;
                err.println("==== SOAP-CLI DEBUG: MTOM multipart HTTP request body (binary content omitted) ====");
                err.println("Endpoint: " + config.endpoint);
                err.println("Content-Length: " + bytes.length + " bytes");
                if (config.attachments != null && !config.attachments.isEmpty()) {
                    err.println("Attachments:");
                    for (Attachment attachment : config.attachments) {
                        long size = attachment.file != null && attachment.file.isFile() ? attachment.file.length() : -1L;
                        String sizeInfo = size >= 0 ? size + " bytes" : "unknown size";
                        err.println("  cid=" + attachment.cid + ", file=" + attachment.file.getPath() + ", size=" + sizeInfo);
                    }
                } else {
                    err.println("Attachments: none");
                }
                err.println("==== END SOAP-CLI DEBUG MTOM BODY ====");
            }

            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
        } else {
            conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            conn.setRequestProperty("Accept", "text/xml, application/soap+xml, */*;q=0.9");

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));

            if (config.debug) {
                PrintStream err = System.err;
                err.println("==== SOAP-CLI DEBUG: SOAP request body after inlining attachments (body omitted) ====");
                err.println("Endpoint: " + config.endpoint);
                err.println("Content-Length: " + bytes.length + " bytes");
                err.println("==== END SOAP-CLI DEBUG SOAP BODY ====");
            }

            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
        }

        if (config.headers != null && !config.headers.isEmpty()) {
            for (Header header : config.headers) {
                if (header != null && header.name != null && !header.name.isEmpty()) {
                    conn.setRequestProperty(header.name, header.value != null ? header.value : "");
                }
            }
        }

        int status = conn.getResponseCode();

        InputStream responseStream;
        boolean isError = status < 200 || status >= 300;
        if (!isError) {
            responseStream = conn.getInputStream();
        } else {
            responseStream = conn.getErrorStream();
            if (responseStream == null) {
                throw new IOException("HTTP " + status + " " + conn.getResponseMessage());
            }
        }

        String response = readAll(responseStream, StandardCharsets.UTF_8);

        return new SoapResponse(status, response);
    }

    private static SSLSocketFactory createSslSocketFactory(Config config) throws Exception {
        KeyStore ks = KeyStore.getInstance(config.keystoreType);
        try (InputStream ksIn = new FileInputStream(config.keystorePath)) {
            ks.load(ksIn, config.keystorePassword);
        } catch (IOException | CertificateException e) {
            throw new IOException("Failed to load keystore: " + e.getMessage(), e);
        }

        char[] keyPass = config.keystoreAliasPkPass != null && config.keystoreAliasPkPass.length > 0
                ? config.keystoreAliasPkPass
                : config.keystorePassword;

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyPass);
        KeyManager[] keyManagers = kmf.getKeyManagers();
        // Wrap key manager to force the specified alias
        for (int i = 0; i < keyManagers.length; i++) {
            if (keyManagers[i] instanceof X509KeyManager) {
                keyManagers[i] = new FixedAliasKeyManager((X509KeyManager) keyManagers[i], config.keystoreAlias);
            }
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        TrustManager[] trustManagers = tmf.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private static String applyInlineAttachments(String body, List<Attachment> attachments, boolean debug) throws IOException {
        if (body == null || body.isEmpty()) {
            return body;
        }
        if (attachments == null || attachments.isEmpty()) {
            return body;
        }

        String result = body;
        for (Attachment attachment : attachments) {
            byte[] fileBytes = Files.readAllBytes(attachment.file.toPath());
            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            String token = "cid:" + attachment.cid;
            result = replaceCidInTextNodes(result, token, base64);
        }

        if (debug) {
            PrintStream err = System.err;
            err.println("==== SOAP-CLI DEBUG: SOAP body before inlining attachments (no binary) ====");
            err.println(body);
            err.println("Original body length: " + body.length() + " characters");
            err.println("Result body length (after inlining, not shown): " + result.length() + " characters");
            err.println("Inline attachments: " + attachments.size());
            for (Attachment attachment : attachments) {
                long size = attachment.file != null && attachment.file.isFile() ? attachment.file.length() : -1L;
                String sizeInfo = size >= 0 ? size + " bytes" : "unknown size";
                err.println("  cid=" + attachment.cid + ", file=" + attachment.file.getPath() + ", size=" + sizeInfo);
            }
            err.println("==== END SOAP-CLI DEBUG INLINE BODY (RESULT OMITTED) ====");
        }

        return result;
    }

    private static void writeMtomMultipart(OutputStream out, String soapBody, List<Attachment> attachments,
                                           String boundary, String rootContentId) throws IOException {
        String lineBreak = "\r\n";

        // Transform cid:CID tokens in the SOAP body into xop:Include references.
        // For MTOM we must generate real <inc:Include> elements in element content, while
        // keeping attribute values like href="cid:..." and payloadId="cid:..." unchanged.
        String mtomSoapBody = soapBody;
        if (attachments != null) {
            // Temporarily protect attribute occurrences so our replacement only affects text content
            final String ATTR_PLACEHOLDER = "__soapcli_cid__";
            mtomSoapBody = mtomSoapBody.replace("href=\"cid:", "href=\"" + ATTR_PLACEHOLDER);
            mtomSoapBody = mtomSoapBody.replace("payloadId=\"cid:", "payloadId=\"" + ATTR_PLACEHOLDER);

            for (Attachment attachment : attachments) {
                String token = "cid:" + attachment.cid;
                String include = "<inc:Include href=\"" + token + "\" xmlns:inc=\"http://www.w3.org/2004/08/xop/include\"/>";
                mtomSoapBody = mtomSoapBody.replace(token, include);
            }

            // Restore attribute prefixes
            mtomSoapBody = mtomSoapBody.replace("href=\"" + ATTR_PLACEHOLDER, "href=\"cid:");
            mtomSoapBody = mtomSoapBody.replace("payloadId=\"" + ATTR_PLACEHOLDER, "payloadId=\"cid:");
        }

        // Root SOAP envelope part
        writeString(out, "--" + boundary + lineBreak);
        writeString(out,
                "Content-Type: application/xop+xml; charset=UTF-8; type=\"application/soap+xml\"; action=\"submitMessage\"" + lineBreak);
        writeString(out, "Content-Transfer-Encoding: 8bit" + lineBreak);
        writeString(out, "Content-ID: " + rootContentId + lineBreak);
        writeString(out, lineBreak);
        writeString(out, mtomSoapBody);
        writeString(out, lineBreak);

        // Attachments
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                writeString(out, "--" + boundary + lineBreak);
                writeString(out, "Content-Type: application/octet-stream" + lineBreak);
                writeString(out, "Content-ID: <" + attachment.cid + ">" + lineBreak);
                writeString(out, "Content-Transfer-Encoding: binary" + lineBreak);
                writeString(out, lineBreak);

                try (InputStream in = new FileInputStream(attachment.file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                writeString(out, lineBreak);
            }
        }

        // Closing boundary
        writeString(out, "--" + boundary + "--" + lineBreak);
    }

    private static String replaceCidInTextNodes(String xml, String token, String replacement) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));

            String pattern = java.util.regex.Pattern.quote(token) + "(?=[^0-9A-Za-z_]|$)";
            replaceCidInNode(doc, pattern, replacement);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new IOException("Failed to process attachments in XML: " + e.getMessage(), e);
        }
    }

    private static void replaceCidInNode(Node node, String pattern, String replacement) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getNodeValue();
            if (text != null && !text.isEmpty()) {
                text = text.replaceAll(pattern, java.util.regex.Matcher.quoteReplacement(replacement));
                node.setNodeValue(text);
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            replaceCidInNode(child, pattern, replacement);
            child = next;
        }
    }

    private static void writeString(OutputStream out, String value) throws IOException {
        if (value != null) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeMultipartRelated(OutputStream out, String soapBody, List<Attachment> attachments, String boundary) throws IOException {
        String lineBreak = "\r\n";
        String startCid = "<rootpart>";

        // Root SOAP envelope part
        writeString(out, "--" + boundary + lineBreak);
        // Use application/soap+xml for better interoperability with SwA-aware stacks (e.g. CXF)
        writeString(out, "Content-Type: application/soap+xml; charset=UTF-8" + lineBreak);
        writeString(out, "Content-Transfer-Encoding: binary" + lineBreak);
        writeString(out, "Content-ID: " + startCid + lineBreak);
        writeString(out, lineBreak);
        writeString(out, soapBody);
        writeString(out, lineBreak);

        // Attachments
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                writeString(out, "--" + boundary + lineBreak);
                writeString(out, "Content-Type: application/octet-stream" + lineBreak);
                writeString(out, "Content-ID: <" + attachment.cid + ">" + lineBreak);
                writeString(out, "Content-Transfer-Encoding: binary" + lineBreak);
                writeString(out, lineBreak);

                try (InputStream in = new FileInputStream(attachment.file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                writeString(out, lineBreak);
            }
        }

        // Closing boundary
        writeString(out, "--" + boundary + "--" + lineBreak);
    }

    private static String readAll(InputStream in, java.nio.charset.Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toString(charset.name());
    }

    private static void printUsage() {
        PrintStream err = System.err;
        err.println("Usage: java -jar SoapCLI.jar --endpoint <URL> [options]\n");
        err.println("Required:");
        err.println("  --endpoint <URL>                 SOAP endpoint URL");
        err.println();
        err.println("Request body:");
        err.println("  --request-file <file.xml>        SOAP request XML file");
        err.println("                                   If omitted, request XML is read from stdin.");
        err.println();
        err.println("  --attachment <CID>=<path>        Inline attachment: replaces cid:CID with base64(file)");
        err.println("                                   Can be repeated for multiple attachments.");
        err.println();
        err.println("  --mtom                           Enable MTOM/XOP: send multipart/related with xop:Include for attachments");
        err.println();
        err.println("  --arg NAME=VALUE                 Override placeholder ___<NAME>:<defaultValue>___ in the request XML");
        err.println("      --placeholder NAME=VALUE     Backwards-compatible alias for --arg");
        err.println("                                   Can be repeated for multiple arguments.");
        err.println();
        err.println("Keystore (optional, for HTTPS client auth / custom trust):");
        err.println("  --keystore <file.jks>            JKS keystore path");
        err.println("  --keystore-type <type>           Keystore type (default: JKS)");
        err.println("  --keystore-alias <alias>         Certificate alias to use for client auth");
        err.println("  --keystore-password <password>   Keystore password (also used as key password by default)");
        err.println("  --keystore-alias-pkpass <pass>   Private key password if different from keystore password");
        err.println();
        err.println("Proxy (optional, supports HTTP/HTTPS and SOCKS):");
        err.println("  --proxy-type <type>              Proxy type: http or socks");
        err.println("  --proxy-host <host>              Proxy host name or IP");
        err.println("  --proxy-port <port>              Proxy port");
        err.println("  --proxy-user <user>              Proxy username (optional)");
        err.println("  --proxy-password <password>      Proxy password (optional)");
        err.println();
        err.println("Other:");
        err.println("  --header 'Name: value'           Add custom HTTP header (can be repeated)");
        err.println("  --profile <name>                 Use configuration profile");
        err.println("  --profile-file <path>            Profiles JSON file (default: ~/.config/soap-cli-profiles.json)");
        err.println("  --list-profiles                  List available profiles from the profiles file");
        err.println("  --list-profiles-detailed         List profiles with detailed configuration information");
        err.println("  --auth-basic <user:pass>         HTTP Basic auth credentials (user:password)");
        err.println("  --auth-basic-env <ENV_VAR>       Read HTTP Basic auth credentials from environment variable");
        err.println("  --theme <name>                   Color theme for XML output (monokai, solarized-dark, etc.)");
        err.println("  --color                          Enable colored XML output (default)");
        err.println("  --no-color                       Disable colored XML output");
        err.println("  --dry-run                        Print request details (endpoint, headers, final XML) without sending");
        err.println("  --debug                          Enable verbose debug output (including raw HTTP body with attachments)");
        err.println("  --help, -h                       Show this help message");
    }

    private static void printError(String message, Throwable t) {
        PrintStream err = System.err;
        err.println(message);
        if (t != null) {
            t.printStackTrace(err);
        }
    }

    private static class FixedAliasKeyManager extends X509ExtendedKeyManager {
        private final X509KeyManager delegate;
        private final String alias;

        FixedAliasKeyManager(X509KeyManager delegate, String alias) {
            this.delegate = delegate;
            this.alias = alias;
        }

        @Override
        public String[] getClientAliases(String keyType, java.security.Principal[] issuers) {
            return new String[]{alias};
        }

        @Override
        public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return alias;
        }

        @Override
        public String[] getServerAliases(String keyType, java.security.Principal[] issuers) {
            return delegate.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(String keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return delegate.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public java.security.cert.X509Certificate[] getCertificateChain(String alias) {
            return delegate.getCertificateChain(alias);
        }

        @Override
        public java.security.PrivateKey getPrivateKey(String alias) {
            return delegate.getPrivateKey(alias);
        }
    }
}
