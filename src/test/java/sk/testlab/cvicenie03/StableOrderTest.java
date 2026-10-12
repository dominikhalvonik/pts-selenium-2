package sk.testlab.cvicenie03;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import sk.testlab.cvicenie03.pages.*;
import sk.testlab.cvicenie03.support.*;
import static org.junit.jupiter.api.Assertions.*;

/** Scenár a assertions patria sem. Lokátory a UI operácie patria do pages/. */
@Tag("gui")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DisplayName("Cvičenie 03 – stabilita GUI a Page Object Model")
public class StableOrderTest {
    private WebDriver driver;
    private TestConfig config;
    private Artifacts artifacts;
    private String ownOrderId = "";
    private boolean submitAttempted;
    private Throwable primaryFailure;

    @BeforeEach
    void prepare(RepetitionInfo repetition) throws Exception {
        try {
            config = TestConfig.load(repetition.getCurrentRepetition());
            artifacts = new Artifacts(repetition.getCurrentRepetition(), config.password());
            // Deklarácia pri príprave fixtures; priestor neoverujeme priamym API volaním.
            artifacts.put("workspace.declared", config.workspace());
            artifacts.put("expected.total.cents", 8563);
            artifacts.put("expected.total.text", "85,63 €");
            artifacts.put("expected.status", "created");
            step("Vytvorenie nového prehliadača");
            driver = BrowserFactory.create(config);
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.manage().timeouts().pageLoadTimeout(config.timeout().multipliedBy(2));
            driver.manage().window().setSize(new Dimension(1440, 1000));
            if (driver instanceof RemoteWebDriver remote) {
                artifacts.put("browser.name", remote.getCapabilities().getBrowserName());
                artifacts.put("browser.version", remote.getCapabilities().getBrowserVersion());
            }
        } catch (Exception | AssertionError error) {
            primaryFailure = error;
            if (artifacts != null) artifacts.capture(driver, "setup-failure", error);
            throw error;
        }
    }

    @RepeatedTest(value = 3, name = "Nezávislé opakovanie {currentRepetition}/{totalRepetitions}")
    void referenceOrderRemainsStable() throws Exception {
        try {
            // ARRANGE
            step("Prihlásenie do prideleného pracovného priestoru");
            CatalogPage catalog = LoginPage.open(driver, config.timeout(), config.baseUrl())
                .loginAs(config.email(), config.password());
            int initialKeyboardStock = catalog.stock("KB-001");
            int initialMouseStock = catalog.stock("MS-001");
            artifacts.put("initial.stock.KB-001", initialKeyboardStock);
            artifacts.put("initial.stock.MS-001", initialMouseStock);
            assertAll("Predpoklady testu",
                () -> assertEquals(0, catalog.cartCount(), "Košík musí byť prázdny."),
                () -> assertTrue(initialKeyboardStock >= 2, "Potrebujeme aspoň 2 klávesnice."),
                () -> assertTrue(initialMouseStock >= 1, "Potrebujeme aspoň 1 myš.")
            );

            // ACT
            step("Výber produktov");
            CartPage cart = catalog.addToCart("KB-001", 2).addToCart("MS-001", 1).openCart();
            assertAll("Vybrané množstvá",
                () -> assertEquals(2, cart.quantity("KB-001")),
                () -> assertEquals(1, cart.quantity("MS-001"))
            );
            step("Uplatnenie kupónu a čakanie na nový cenový súhrn");
            CheckoutPage checkout = cart.useCourier().applyCoupon("STUDENT10").checkout();
            step("Vyplnenie simulovaných kontaktných údajov");
            checkout.fillCustomer("Študent Testovací", "objednavka@example.test",
                "Testovacia 12", "Žilina", "010 01", "Cvičenie 03; beh " + artifacts.runId)
                .acceptTerms();
            step("Jednorazové odoslanie objednávky");
            submitAttempted = true;
            OrderPage order = checkout.submit();

            // UUID uložíme pred assertions ceny, aby sme poznali vlastnú objednávku.
            ownOrderId = order.id();
            artifacts.write("order-id.txt", ownOrderId + System.lineSeparator());
            artifacts.put("order.id", ownOrderId);
            int actualCents = order.totalCents();
            String actualText = order.visibleTotal();
            String actualStatus = order.status();
            String addressId = order.idInAddress();
            int keyboards = order.quantity("KB-001");
            int mice = order.quantity("MS-001");
            artifacts.put("observed.total.cents", actualCents);
            artifacts.put("observed.total.text", actualText);
            artifacts.put("observed.status", actualStatus);
            artifacts.put("observed.quantity.KB-001", keyboards);
            artifacts.put("observed.quantity.MS-001", mice);

            // ASSERT: očakávania pochádzajú z ACCEPTANCE.md, nie z výpočtu aplikácie.
            step("Porovnanie výsledku so špecifikáciou");
            assertAll("Referenčná objednávka",
                () -> assertTrue(OrderPage.isUuid(ownOrderId), "Neplatné UUID objednávky."),
                () -> assertEquals(8563, actualCents, "Celková suma v centoch."),
                () -> assertEquals("85,63 €", actualText, "Viditeľná suma a mena."),
                () -> assertEquals("created", actualStatus, "Počiatočný stav objednávky."),
                () -> assertEquals(ownOrderId, addressId, "UUID detailu a adresy sa musia zhodovať."),
                () -> assertEquals(2, keyboards, "Počet klávesníc."),
                () -> assertEquals(1, mice, "Počet myší.")
            );
            artifacts.put("assertions", "PASSED");
            artifacts.capture(driver, "passed-before-cleanup", null);
            System.out.println("PASS: " + ownOrderId + " | 8563 centov | created");
        } catch (Exception | AssertionError error) {
            primaryFailure = error;
            artifacts.capture(driver, "failure", error);
            throw error;
        }
    }

    @AfterEach
    void cleanUpAndClose() throws Exception {
        Throwable cleanupFailure = null;
        try {
            if (driver != null && submitAttempted) {
                cleanUpOwnOrder();
            } else if (artifacts != null) {
                artifacts.put("cleanup", "NO_ORDER_SUBMITTED");
            }
        } catch (Exception | AssertionError error) {
            cleanupFailure = error;
            if (artifacts != null) artifacts.capture(driver, "cleanup-failure", error);
        } finally {
            if (driver != null) {
                try { driver.quit(); }
                catch (RuntimeException error) {
                    if (cleanupFailure == null) cleanupFailure = error;
                    else cleanupFailure.addSuppressed(error);
                }
            }
        }
        if (artifacts != null) {
            try {
                artifacts.put("finished.utc", Instant.now().toString());
                artifacts.put("result", primaryFailure != null ? "FAILED"
                    : cleanupFailure != null ? "CLEANUP_FAILED" : "PASSED");
            } catch (Exception error) {
                if (cleanupFailure == null) cleanupFailure = error;
                else cleanupFailure.addSuppressed(error);
            }
        }
        if (cleanupFailure != null) {
            if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
            else throw new AssertionError("Upratanie po GUI teste zlyhalo.", cleanupFailure);
        }
    }

    private void cleanUpOwnOrder() throws Exception {
        // Obnova iba z URL po vlastnom odoslaní. Nikdy nevyberáme „poslednú“ objednávku.
        if (!OrderPage.isUuid(ownOrderId)) {
            String candidate = OrderPage.idFromUrl(driver.getCurrentUrl());
            if (OrderPage.isUuid(candidate)) {
                ownOrderId = candidate;
                artifacts.write("order-id.txt", ownOrderId + System.lineSeparator());
            }
        }
        if (!config.cleanupOrder()) {
            artifacts.put("cleanup", "DISABLED_MANUAL_REVIEW_IF_NEEDED");
            return;
        }
        if (!OrderPage.isUuid(ownOrderId)) {
            throw new IllegalStateException("Výsledok odoslania nie je známy. Skontrolujte vlastný "
                + "pracovný priestor; test nebude rušiť neidentifikovanú objednávku.");
        }
        OrderPage.openOwn(driver, config.timeout(), config.baseUrl(), ownOrderId)
            .cancelOwnOrder(ownOrderId);
        artifacts.put("cleanup", "CANCELLED_OWN_ORDER");
        artifacts.put("cleanup.order.id", ownOrderId);
    }

    private void step(String message) throws Exception {
        artifacts.put("step", message);
        System.out.println("[KROK] " + message);
    }
}
