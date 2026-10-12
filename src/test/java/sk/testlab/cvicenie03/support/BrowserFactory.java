package sk.testlab.cvicenie03.support;

import java.io.File;
import java.nio.file.Path;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;

/** Každé opakovanie dostane novú reláciu prehliadača bez zdieľaného profilu. */
public final class BrowserFactory {
    private BrowserFactory() {}

    public static WebDriver create(TestConfig c) {
        File executable = c.driverPath().isBlank()
            ? null : Path.of(c.driverPath()).toFile();
        if (executable != null && !executable.isFile()) {
            throw new IllegalArgumentException("driver.path does not point to a file.");
        }
        return switch (c.browser()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                if (c.headless()) options.addArguments("--headless=new");
                if (!c.browserBinary().isBlank()) options.setBinary(c.browserBinary());
                yield executable == null ? new ChromeDriver(options)
                    : new ChromeDriver(new ChromeDriverService.Builder()
                        .usingDriverExecutable(executable).build(), options);
            }
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                if (c.headless()) options.addArguments("--headless=new");
                if (!c.browserBinary().isBlank()) options.setBinary(c.browserBinary());
                yield executable == null ? new EdgeDriver(options)
                    : new EdgeDriver(new EdgeDriverService.Builder()
                        .usingDriverExecutable(executable).build(), options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                if (c.headless()) options.addArguments("-headless");
                if (!c.browserBinary().isBlank()) options.setBinary(c.browserBinary());
                // Konkrétny Gecko builder, nie abstraktný FirefoxDriverService.Builder.
                yield executable == null ? new FirefoxDriver(options)
                    : new FirefoxDriver(new GeckoDriverService.Builder()
                        .usingDriverExecutable(executable).build(), options);
            }
            default -> throw new IllegalArgumentException("Supported browsers: chrome, edge, firefox.");
        };
    }
}
