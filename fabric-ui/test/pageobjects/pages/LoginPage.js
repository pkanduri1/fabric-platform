/**
 * Login Page Object for Fabric Platform E2E Testing
 * Handles user authentication and login workflow
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

const BasePage = require('../BasePage');

class LoginPage extends BasePage {
    constructor() {
        super();
        this.url = '/login';
    }

    // Page Elements - Selectors
    get pageTitle() { return $('h1, [data-testid="login-title"]'); }
    get loginForm() { return $('[data-testid="login-form"]'); }
    
    // Input Fields
    get usernameInput() { return $('[data-testid="username-input"]'); }
    get passwordInput() { return $('[data-testid="password-input"]'); }
    get rememberMeCheckbox() { return $('[data-testid="remember-me"]'); }
    
    // Buttons
    get loginButton() { return $('[data-testid="login-button"]'); }
    get forgotPasswordLink() { return $('[data-testid="forgot-password"]'); }
    get signupLink() { return $('[data-testid="signup-link"]'); }
    
    // Error and Status Messages
    get errorMessage() { return $('[data-testid="login-error"]'); }
    get successMessage() { return $('[data-testid="login-success"]'); }
    get loadingSpinner() { return $('[data-testid="loading-spinner"]'); }
    
    // Security Elements
    get captchaContainer() { return $('[data-testid="captcha-container"]'); }
    get twoFactorInput() { return $('[data-testid="2fa-input"]'); }

    // Page Navigation and Setup
    navigateToPage() {
        this.open(this.url);
        this.waitForPageLoad();
    }

    waitForPageReady() {
        this.waitForElement(this.pageTitle);
        this.waitForElement(this.loginForm);
        this.waitForLoadingComplete();
    }

    // Authentication Methods
    loginWithCredentials(username, password, rememberMe = false) {
        console.log(`🔐 Logging in with username: ${username}`);
        
        this.waitForPageReady();
        
        // Clear any existing values and enter credentials
        this.safeInput(this.usernameInput, username);
        this.safeInput(this.passwordInput, password);
        
        // Handle remember me option
        if (rememberMe) {
            const isChecked = this.rememberMeCheckbox.isSelected();
            if (!isChecked) {
                this.safeClick(this.rememberMeCheckbox);
            }
        }
        
        // Submit login form
        this.safeClick(this.loginButton);
        
        // Wait for login process to complete
        this.waitForLoginComplete();
        
        return this.isLoginSuccessful();
    }

    loginAsTestUser() {
        return this.loginWithCredentials('test-user', 'test-password');
    }

    loginAsAdmin() {
        return this.loginWithCredentials('admin-user', 'admin-password');
    }

    loginWithInvalidCredentials(username = 'invalid', password = 'invalid') {
        console.log(`🚫 Attempting login with invalid credentials`);
        
        this.waitForPageReady();
        this.safeInput(this.usernameInput, username);
        this.safeInput(this.passwordInput, password);
        this.safeClick(this.loginButton);
        
        // Wait for error message to appear
        this.waitForElement(this.errorMessage, 5000);
        
        return this.getErrorMessage();
    }

    // Two-Factor Authentication
    enterTwoFactorCode(code) {
        console.log(`🔐 Entering 2FA code`);
        
        this.waitForElement(this.twoFactorInput);
        this.safeInput(this.twoFactorInput, code);
        
        // Assuming there's a verify button or automatic submission
        const verifyButton = $('[data-testid="2fa-verify-button"]');
        if (verifyButton.isExisting()) {
            this.safeClick(verifyButton);
        }
        
        this.waitForLoginComplete();
    }

    // Validation and Status Methods
    waitForLoginComplete(timeout = 10000) {
        // Wait for loading spinner to disappear
        if (this.elementExists('[data-testid="loading-spinner"]')) {
            this.loadingSpinner.waitForExist({ timeout, reverse: true });
        }
        
        // Wait for either success (redirect) or error message
        browser.waitUntil(() => {
            return !browser.getUrl().includes('/login') ||
                   this.elementExists('[data-testid="login-error"]') ||
                   this.elementExists('[data-testid="login-success"]');
        }, { timeout });
    }

    isLoginSuccessful() {
        // Check if redirected away from login page
        const currentUrl = browser.getUrl();
        const isRedirected = !currentUrl.includes('/login');
        
        // Or check for success message
        const hasSuccessMessage = this.elementExists('[data-testid="login-success"]');
        
        return isRedirected || hasSuccessMessage;
    }

    hasLoginError() {
        return this.elementExists('[data-testid="login-error"]');
    }

    getErrorMessage() {
        if (this.hasLoginError()) {
            return this.getElementText(this.errorMessage);
        }
        return null;
    }

    getSuccessMessage() {
        if (this.elementExists('[data-testid="login-success"]')) {
            return this.getElementText(this.successMessage);
        }
        return null;
    }

    // Form Validation
    validateFormFields() {
        const validationErrors = [];
        
        // Check username field
        const usernameValue = this.usernameInput.getValue();
        if (!usernameValue || usernameValue.trim().length === 0) {
            validationErrors.push('Username is required');
        }
        
        // Check password field
        const passwordValue = this.passwordInput.getValue();
        if (!passwordValue || passwordValue.trim().length === 0) {
            validationErrors.push('Password is required');
        }
        
        return validationErrors;
    }

    isLoginButtonEnabled() {
        return this.loginButton.isEnabled();
    }

    // Security Features
    hasCaptcha() {
        return this.elementExists('[data-testid="captcha-container"]');
    }

    solveCaptcha() {
        // This would integrate with captcha solving service in real tests
        // For demo purposes, we'll simulate captcha interaction
        if (this.hasCaptcha()) {
            console.log('🤖 Captcha detected - simulating solution');
            
            // Click on captcha area
            this.safeClick(this.captchaContainer);
            
            // Wait for captcha verification
            browser.pause(2000);
        }
    }

    // Password Recovery
    initiatePasswordReset(email) {
        console.log(`🔄 Initiating password reset for: ${email}`);
        
        this.safeClick(this.forgotPasswordLink);
        
        // Wait for password reset dialog/page
        const emailInput = $('[data-testid="reset-email-input"]');
        const resetButton = $('[data-testid="reset-password-button"]');
        
        this.waitForElement(emailInput);
        this.safeInput(emailInput, email);
        this.safeClick(resetButton);
        
        // Wait for confirmation
        const confirmationMessage = $('[data-testid="reset-confirmation"]');
        this.waitForElement(confirmationMessage);
        
        return this.getElementText(confirmationMessage);
    }

    // Session Management
    logout() {
        console.log('🚪 Logging out user');
        
        // Look for logout button (might be in header/nav)
        const logoutButton = $('[data-testid="logout-button"]');
        if (logoutButton.isExisting()) {
            this.safeClick(logoutButton);
        } else {
            // Alternative: clear session by deleting cookies
            browser.deleteCookies();
            browser.refresh();
        }
        
        // Wait to return to login page
        this.waitForPageReady();
    }

    // Accessibility Helpers
    loginUsingKeyboard(username, password) {
        console.log('⌨️ Performing keyboard-only login');
        
        this.waitForPageReady();
        
        // Tab to username field and enter
        browser.keys(['Tab']);
        browser.keys(username);
        
        // Tab to password field and enter
        browser.keys(['Tab']);
        browser.keys(password);
        
        // Tab to login button and activate
        browser.keys(['Tab']);
        browser.keys(['Enter']);
        
        this.waitForLoginComplete();
        
        return this.isLoginSuccessful();
    }

    // Test Data Helpers
    generateTestCredentials() {
        const timestamp = this.getTimestamp();
        return {
            username: `test_user_${timestamp}`,
            password: `test_password_${timestamp}`,
            email: `test_${timestamp}@example.com`
        };
    }

    // Network Simulation
    simulateSlowNetwork() {
        // This would integrate with WebDriver network throttling
        console.log('🐌 Simulating slow network conditions');
        
        this.loginWithCredentials('test-user', 'test-password');
        
        // Verify login still works under slow conditions
        this.waitForLoginComplete(30000); // Extended timeout
        
        return this.isLoginSuccessful();
    }

    // Error Recovery
    retryFailedLogin(username, password, maxRetries = 3) {
        let attempts = 0;
        
        while (attempts < maxRetries) {
            attempts++;
            console.log(`🔄 Login attempt ${attempts}/${maxRetries}`);
            
            try {
                const success = this.loginWithCredentials(username, password);
                if (success) {
                    console.log('✅ Login successful');
                    return true;
                }
            } catch (error) {
                console.log(`❌ Login attempt ${attempts} failed:`, error.message);
            }
            
            // Wait before retry
            if (attempts < maxRetries) {
                browser.pause(1000 * attempts); // Exponential backoff
                browser.refresh();
                this.waitForPageReady();
            }
        }
        
        console.log('❌ All login attempts failed');
        return false;
    }

    // Browser Compatibility
    testBrowserSpecificFeatures() {
        const browserName = browser.capabilities.browserName.toLowerCase();
        
        console.log(`🌐 Testing browser-specific features for: ${browserName}`);
        
        // Test autofill detection
        if (browserName === 'chrome') {
            // Check Chrome autofill behavior
            this.usernameInput.click();
            browser.keys(['Control', 'a']); // Select all
            
            const hasAutofillSuggestions = this.elementExists(':-webkit-autofill');
            console.log(`Chrome autofill detected: ${hasAutofillSuggestions}`);
        }
        
        // Test Firefox specific features
        if (browserName === 'firefox') {
            // Check Firefox password manager integration
            console.log('Testing Firefox password manager integration');
        }
        
        return browserName;
    }
}

module.exports = LoginPage;