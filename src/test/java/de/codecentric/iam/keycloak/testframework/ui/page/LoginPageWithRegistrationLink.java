package de.codecentric.iam.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URI;
import java.net.URL;

/**
 * Version of {@link LoginPage} that is capable of retrieving the session-specific link to the Keycloak registration
 * page.
 */
public class LoginPageWithRegistrationLink extends LoginPage {
    @FindBy(xpath = "//div[@id='kc-registration']/span/a")
    private WebElement registrationLink;

    public LoginPageWithRegistrationLink(WebDriver driver) {
        super(driver);
    }

    public URL buildRegistrationUrl(URL baseUrl) {
        try {
            return new URI(baseUrl.toString() + registrationLink.getDomAttribute("href")).toURL();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
