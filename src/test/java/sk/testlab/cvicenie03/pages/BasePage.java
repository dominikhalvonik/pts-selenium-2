package sk.testlab.cvicenie03.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Spoločné UI operácie. V pollingu iba čítame, nevykonávame opakovane zápisové akcie. */
public abstract class BasePage {
    protected final WebDriver driver;
    protected final Duration timeout;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.timeout = timeout;
        wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(Duration.ofMillis(200));
        wait.ignoring(StaleElementReferenceException.class);
    }

    protected static By testId(String value) {
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Unsupported data-testid.");
        }
        return By.cssSelector("[data-testid='" + value + "']");
    }

    protected WebElement visible(By locator) {
        return wait.withMessage("Visible element: " + locator).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        WebElement element = wait.withMessage("Clickable element: " + locator).until(
            ExpectedConditions.elementToBeClickable(locator));
        element.click(); // Mimo polling callbacku: akciu naslepo neopakujeme.
    }

    protected void type(By locator, String value) {
        WebElement element = wait.withMessage("Editable element: " + locator).until(
            ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(value);
    }

    protected String text(By locator) {
        return wait.withMessage("Non-empty text: " + locator).until(d -> {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed()) return null;
            String value = element.getText().trim();
            return value.isEmpty() ? null : value;
        });
    }

    protected String attribute(By locator, String name) {
        return wait.withMessage("Attribute " + name + ": " + locator).until(d -> {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed()) return null;
            String value = element.getDomAttribute(name);
            return value == null || value.isBlank() ? null : value;
        });
    }

    protected String inputValue(By locator) {
        return wait.withMessage("Input value: " + locator).until(d -> {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed()) return null;
            String value = element.getDomProperty("value");
            return value == null || value.isBlank() ? null : value;
        });
    }
}
