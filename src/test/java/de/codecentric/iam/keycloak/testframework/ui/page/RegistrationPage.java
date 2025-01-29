package de.codecentric.iam.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.page.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RegistrationPage extends AbstractPage {
    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(id = "firstName")
    private WebElement firstnameInput;

    @FindBy(id = "lastName")
    private WebElement lastnameInput;

    @FindBy(css = "[type=submit]")
    private WebElement submitButton;

    public RegistrationPage(WebDriver driver) {
        super(driver);
    }

    public void fillRegistration(String username, String password, String passwordConfirm, String firstname,
        String lastname) {
        emailInput.sendKeys(username);
        passwordInput.sendKeys(password);
        passwordConfirmInput.sendKeys(passwordConfirm);
        firstnameInput.sendKeys(firstname);
        lastnameInput.sendKeys(lastname);
    }

    public void submit() {
        submitButton.click();
    }
}
