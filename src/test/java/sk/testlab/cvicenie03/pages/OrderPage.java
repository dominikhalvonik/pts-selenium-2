package sk.testlab.cvicenie03.pages;

import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class OrderPage extends BasePage {
    private static final By ID = testId("order-id");
    private static final By TOTAL = testId("order-total");
    private static final By STATUS = testId("order-status");
    private static final By CANCEL = testId("cancel-order");
    private static final By CONFIRM_CANCEL = testId("confirm-cancel");
    private final String orderId;

    public OrderPage(WebDriver driver, Duration timeout, String previousId) {
        super(driver, timeout);
        orderId = wait.withMessage("New visible order detail with non-empty ID").until(d -> {
            WebElement element = d.findElement(ID);
            if (!element.isDisplayed()) return null;
            String value = element.getText().trim();
            return !value.isEmpty() && !value.equals(previousId) ? value : null;
        });
    }

    public String id() { return orderId; }

    public int totalCents() {
        return Integer.parseInt(attribute(TOTAL, "data-cents"));
    }

    public String visibleTotal() {
        return text(TOTAL).replace('\u00a0', ' ').replace('\u202f', ' ')
            .replaceAll("\\s+", " ").trim();
    }

    public String status() {
        return attribute(STATUS, "data-status");
    }

    public int quantity(String sku) {
        return Integer.parseInt(text(testId("order-quantity-" + sku)));
    }

    public String idInAddress() { return idFromUrl(driver.getCurrentUrl()); }

    public static boolean isUuid(String text) {
        return text != null && text.matches(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    public static String idFromUrl(String url) {
        try {
            String fragment = URI.create(url).getFragment();
            return fragment != null && fragment.startsWith("/orders/")
                ? fragment.substring("/orders/".length()) : "";
        } catch (IllegalArgumentException error) {
            return "";
        }
    }

    public static OrderPage openOwn(WebDriver driver, Duration timeout,
            String baseUrl, String knownId) {
        if (!isUuid(knownId)) {
            throw new IllegalArgumentException("Cannot open an invalid order UUID.");
        }
        driver.get(baseUrl + "#/orders/" + knownId);
        return new OrderPage(driver, timeout, "");
    }

    public void cancelOwnOrder(String knownId) {
        // Bezpečnostná poistka pred zmenou dát, nie overenie obchodnej požiadavky.
        if (!isUuid(knownId) || !knownId.equals(id()) || !knownId.equals(idInAddress())) {
            throw new IllegalStateException("Refusing to cancel an unrelated order.");
        }
        String current = status();
        if ("cancelled".equals(current)) return;
        if (!"created".equals(current)) {
            throw new IllegalStateException("Cannot cancel state: " + current);
        }
        click(CANCEL);
        click(CONFIRM_CANCEL);
        wait.withMessage("Own order was not cancelled").until(
            ExpectedConditions.attributeToBe(STATUS, "data-status", "cancelled"));
    }
}
