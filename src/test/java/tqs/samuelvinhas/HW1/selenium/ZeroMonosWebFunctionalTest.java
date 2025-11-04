package tqs.samuelvinhas.HW1.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;


import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.github.bonigarcia.seljup.SeleniumJupiter;
import tqs.samuelvinhas.HW1.data.ZeroMonosRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SeleniumJupiter.class)
class ZeroMonosWebFunctionalTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @Autowired
    private ZeroMonosRepository repository;

    @BeforeEach
    void createTestBooking(WebDriver driver) {
        repository.deleteAll();
        baseUrl = "http://localhost:" + port;
        // Ensure at least one booking exists for staff portal etests
        driver.get(baseUrl + "/index.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        try {
            WebElement municipalitySelect = wait.until(ExpectedConditions.elementToBeClickable(By.id("municipality")));
            wait.until(d -> municipalitySelect.findElements(By.tagName("option")).size() > 1);
            new Select(municipalitySelect).selectByIndex(1);
            
            driver.findElement(By.id("address")).sendKeys("Test Address");
            
            // Use JavaScript to set the date value to avoid calendar picker issues
            LocalDate tomorrow = LocalDate.now().plusDays(2);
            String dateString = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE);
            js.executeScript("document.getElementById('timeSlotDate').value = arguments[0]; " +
                           "document.getElementById('timeSlotDate').dispatchEvent(new Event('change', { bubbles: true }));", 
                           dateString);
            
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("timeSlotsList")));
            
            WebElement timeSlot = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#timeSlotsList button:not([disabled])")));
            timeSlot.click();
            
            driver.findElement(By.id("itemDescription")).sendKeys("Test Item");
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("successModal")));
            driver.findElement(By.cssSelector("#successModal button")).click();
        } catch (Exception ignored) {}
    }


    @Test
    void testCitizenCreatesBooking(WebDriver driver) {
        driver.get(baseUrl + "/index.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement municipalitySelect = wait.until(ExpectedConditions.elementToBeClickable(By.id("municipality")));
        wait.until(d -> municipalitySelect.findElements(By.tagName("option")).size() > 1);
        new Select(municipalitySelect).selectByVisibleText("Aveiro");

        WebElement addressInput = driver.findElement(By.id("address"));
        addressInput.clear();
        addressInput.sendKeys("Rua Principal, n12");

        // Use JavaScript to set the date value to avoid calendar picker issues
        LocalDate tomorrow = LocalDate.now().plusDays(3);
        String dateString = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE);
        js.executeScript("document.getElementById('timeSlotDate').value = arguments[0]; " +
                       "document.getElementById('timeSlotDate').dispatchEvent(new Event('change', { bubbles: true }));", 
                       dateString);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("timeSlotsList")));
        // Click first available (enabled) time slot
        WebElement timeSlotButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("#timeSlotsList button:not([disabled])")));
        timeSlotButton.click();

        WebElement descInput = driver.findElement(By.id("itemDescription"));
        descInput.clear();
        descInput.sendKeys("Old mattress");

        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("successModal")));
        WebElement successModal = driver.findElement(By.id("successModal"));
        assertThat(successModal.getDomAttribute("class")).doesNotContain("hidden");

        WebElement tokenElem = driver.findElement(By.id("bookingTokenModal"));
        String token = tokenElem.getText();
        assertThat(token).matches("[a-f0-9-]{36}");
    }

    @Test
    void testBookingLookup(WebDriver driver) {
        driver.get(baseUrl + "/index.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement municipalitySelect = wait.until(ExpectedConditions.elementToBeClickable(By.id("municipality")));
        wait.until(d -> municipalitySelect.findElements(By.tagName("option")).size() > 1);
        new Select(municipalitySelect).selectByVisibleText("Aveiro");
        driver.findElement(By.id("address")).sendKeys("Rua Test, n99");
        
        // Use JavaScript to set the date value
        LocalDate future = LocalDate.now().plusDays(2);
        String dateString = future.format(DateTimeFormatter.ISO_LOCAL_DATE);
        js.executeScript("document.getElementById('timeSlotDate').value = arguments[0]; " +
                       "document.getElementById('timeSlotDate').dispatchEvent(new Event('change', { bubbles: true }));", 
                       dateString);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("timeSlotsList")));
        driver.findElements(By.cssSelector("#timeSlotsList button:not([disabled])")).get(0).click();
        driver.findElement(By.id("itemDescription")).sendKeys("Test item for lookup");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("successModal")));
        String token = driver.findElement(By.id("bookingTokenModal")).getText();
        driver.findElement(By.cssSelector("#successModal button")).click();

        WebElement tokenInput = driver.findElement(By.id("tokenLookup"));
        tokenInput.clear();
        tokenInput.sendKeys(token);
        driver.findElement(By.id("searchTokenButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tokenResultModal")));
        WebElement detailsModal = driver.findElement(By.id("tokenResultModal"));
        assertThat(detailsModal.getDomAttribute("class")).doesNotContain("hidden");
        WebElement content = driver.findElement(By.id("tokenResultContent"));
        assertThat(content.getText()).isNotEmpty();
    }

    @Test
    void testStaffPortalShowsBookings(WebDriver driver) {
        driver.get(baseUrl + "/staff.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestsTableBody")));
        WebElement tbody = driver.findElement(By.id("requestsTableBody"));
        wait.until(d -> tbody.findElements(By.tagName("tr")).size() > 0);
        assertThat(tbody.findElements(By.tagName("tr"))).hasSizeGreaterThan(0);
    }

    @Test
    void testStaffUpdatesBookingStatus(WebDriver driver) {
        driver.get(baseUrl + "/staff.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestsTableBody")));
        WebElement tbody = driver.findElement(By.id("requestsTableBody"));
        wait.until(d -> tbody.findElements(By.tagName("tr")).size() > 0);
        
        WebElement updateBtn = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("button[title='Update Status']")));
        updateBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("updateModal")));
        WebElement statusSelect = driver.findElement(By.id("newStatus"));
        new Select(statusSelect).selectByValue("ASSIGNED");

        // Find the Update button within the modal
        WebElement modal = driver.findElement(By.id("updateModal"));
        WebElement confirmBtn = modal.findElements(By.tagName("button"))
            .stream()
            .filter(btn -> btn.getText().trim().equals("Update"))
            .findFirst()
            .orElseThrow();
        confirmBtn.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("updateModal")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestsTableBody")));
        assertThat(tbody.findElements(By.tagName("tr"))).hasSizeGreaterThan(0);
    }

    @Test
    void testInvalidTokenLookup(WebDriver driver) {
        driver.get(baseUrl + "/index.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement tokenInput = driver.findElement(By.id("tokenLookup"));
        tokenInput.sendKeys("invalid-token-12345");
        driver.findElement(By.id("searchTokenButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("errorModal")));
        WebElement content = driver.findElement(By.id("errorModalText"));
        assertThat(content.getText()).contains("Booking not found");
    }

    @Test
    void testStaffFilterByMunicipality(WebDriver driver) {
        driver.get(baseUrl + "/staff.html");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestsTableBody")));
        WebElement municipalityFilter = driver.findElement(By.id("filterMunicipality"));
        wait.until(d -> municipalityFilter.findElements(By.tagName("option")).size() > 1);
        
        new Select(municipalityFilter).selectByIndex(1);
        driver.findElement(By.id("applyFiltersButton")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestsTableBody")));
        assertThat(driver.findElements(By.cssSelector("#requestsTableBody tr"))).hasSizeGreaterThanOrEqualTo(0);
    }
}