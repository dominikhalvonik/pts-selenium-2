package sk.testlab.cvicenie03.support;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Konfigurácia jedného z troch nezávislých opakovaní. Heslá sa nelogujú. */
public record TestConfig(
        String baseUrl, String browser, boolean headless, Duration timeout,
        boolean cleanupOrder, String driverPath, String browserBinary,
        String workspace, String email, String password) {

    public static TestConfig load(int repetition) throws IOException {
        if (repetition < 1 || repetition > 3) {
            throw new IllegalArgumentException("Repetition must be 1, 2 or 3.");
        }
        Path file = Path.of(System.getProperty("test.config", "config/test.properties"));
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Missing " + file.toAbsolutePath()
                + ". Working directory must be the Maven project root.");
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        }

        Set<String> emails = new HashSet<>();
        Set<String> workspaces = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            String prefix = "run." + i + ".";
            String email = required(p, prefix + "email").trim();
            String workspace = required(p, prefix + "workspace").trim();
            required(p, prefix + "password");
            if (!workspace.matches("run-[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException(prefix + "workspace must be a real run-* workspace.");
            }
            if (!emails.add(email.toLowerCase(Locale.ROOT)) || !workspaces.add(workspace)) {
                throw new IllegalArgumentException(
                    "Each repetition must use a different account and workspace.");
            }
        }

        String base = value(p, "base.url", "http://127.0.0.1:8080/");
        URI uri = URI.create(base);
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                "base.url must be an HTTP(S) website URL without credentials, query or fragment.");
        }
        int seconds = Integer.parseInt(value(p, "timeout.seconds", "30"));
        if (seconds < 1 || seconds > 120) {
            throw new IllegalArgumentException("timeout.seconds must be 1..120.");
        }
        String prefix = "run." + repetition + ".";
        return new TestConfig(
            base.endsWith("/") ? base : base + "/",
            value(p, "browser", "chrome").toLowerCase(Locale.ROOT),
            bool(p, "headless", "false"), Duration.ofSeconds(seconds),
            bool(p, "cleanup.order", "true"), value(p, "driver.path", ""),
            value(p, "browser.binary", ""), required(p, prefix + "workspace").trim(),
            required(p, prefix + "email").trim(), required(p, prefix + "password")
        );
    }

    private static String value(Properties p, String key, String fallback) {
        return System.getProperty(key, p.getProperty(key, fallback)).trim();
    }

    private static boolean bool(Properties p, String key, String fallback) {
        String text = value(p, key, fallback);
        if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(key + " must be true or false.");
        }
        return Boolean.parseBoolean(text);
    }

    private static String required(Properties p, String key) {
        String text = p.getProperty(key);
        if (text == null || text.isBlank() || text.trim().startsWith("SEM_")) {
            throw new IllegalArgumentException("Fill in " + key + " in the config file.");
        }
        return text; // Heslá neorezávame.
    }

    @Override
    public String toString() {
        return "TestConfig[credentials=REDACTED]";
    }
}
