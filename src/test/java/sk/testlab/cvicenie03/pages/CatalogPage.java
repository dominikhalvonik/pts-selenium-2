package sk.testlab.cvicenie03.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class CatalogPage extends BasePage {
    private static final By READY = testId("catalog-ready");
    private static final By USER = testId("current-user");
    private static final By CART_COUNT = testId("cart-count");
    private static final By NAV_CART = testId("nav-cart");

    public CatalogPage(WebDriver driver, Duration timeout) {
        super(driver, timeout);
        visible(USER);
        visible(READY);
    }

    public int cartCount() {
        return Integer.parseInt(text(CART_COUNT));
    }

    public int stock(String sku) {
        return Integer.parseInt(attribute(testId("stock-" + sku), "data-stock"));
    }

    public CatalogPage addToCart(String sku, int quantity) {
        if (quantity < 1 || quantity > 20) {
            throw new IllegalArgumentException("Quantity must be 1..20.");
        }
        for (int i = 0; i < quantity; i++) {
            int before = cartCount();
            click(testId("add-to-cart-" + sku));
            wait.withMessage("Cart counter did not update after adding " + sku)
                .until(ExpectedConditions.textToBe(CART_COUNT, String.valueOf(before + 1)));
        }
        return this;
    }

    public CartPage openCart() {
        click(NAV_CART);
        return new CartPage(driver, timeout);
    }
}
