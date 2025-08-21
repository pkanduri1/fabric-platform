/**
 * Base Page Object for Fabric Platform E2E Testing
 * Provides common functionality and utilities for all page objects
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

class BasePage {
    constructor() {
        this.baseUrl = 'http://localhost:3000';
        this.timeout = 10000;
    }

    /**
     * Navigate to a specific URL
     * @param {string} path - Path to navigate to
     */
    open(path = '') {
        const url = path.startsWith('http') ? path : `${this.baseUrl}${path}`;
        browser.url(url);
        this.waitForPageLoad();
    }

    /**
     * Wait for page to load completely
     */
    waitForPageLoad() {
        browser.waitUntil(
            () => browser.execute(() => document.readyState === 'complete'),
            {
                timeout: this.timeout,
                timeoutMsg: 'Page did not load within timeout'
            }
        );
    }

    /**
     * Wait for element to be present
     * @param {WebdriverIO.Element} element - Element to wait for
     * @param {number} timeout - Timeout in milliseconds
     */
    waitForElement(element, timeout = this.timeout) {
        element.waitForExist({ timeout });
    }

    /**
     * Wait for element to be clickable
     * @param {WebdriverIO.Element} element - Element to wait for
     * @param {number} timeout - Timeout in milliseconds
     */
    waitForClickable(element, timeout = this.timeout) {
        element.waitForClickable({ timeout });
    }

    /**
     * Safe click that waits for element to be clickable
     * @param {WebdriverIO.Element} element - Element to click
     */
    safeClick(element) {
        this.waitForClickable(element);
        element.click();
    }

    /**
     * Safe input that clears field before entering text
     * @param {WebdriverIO.Element} element - Input element
     * @param {string} text - Text to enter
     */
    safeInput(element, text) {
        this.waitForClickable(element);
        element.clearValue();
        element.setValue(text);
    }

    /**
     * Get element text with wait
     * @param {WebdriverIO.Element} element - Element to get text from
     * @returns {string} Element text
     */
    getElementText(element) {
        this.waitForElement(element);
        return element.getText();
    }

    /**
     * Check if element exists without throwing error
     * @param {string} selector - CSS selector
     * @returns {boolean} True if element exists
     */
    elementExists(selector) {
        try {
            const element = $(selector);
            return element.isExisting();
        } catch (error) {
            return false;
        }
    }

    /**
     * Wait for loading to complete
     * @param {number} timeout - Timeout in milliseconds
     */
    waitForLoadingComplete(timeout = this.timeout) {
        // Wait for common loading indicators to disappear
        const loadingSelectors = [
            '[data-testid="loading-spinner"]',
            '.loading-spinner',
            '[aria-label="Loading"]',
            '.MuiCircularProgress-root'
        ];

        loadingSelectors.forEach(selector => {
            try {
                const element = $(selector);
                if (element.isExisting()) {
                    element.waitForExist({ timeout, reverse: true });
                }
            } catch (error) {
                // Ignore if element doesn't exist
            }
        });
    }

    /**
     * Scroll element into view
     * @param {WebdriverIO.Element} element - Element to scroll to
     */
    scrollIntoView(element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        browser.pause(500); // Brief pause for scroll animation
    }

    /**
     * Take screenshot with timestamp
     * @param {string} name - Screenshot name
     */
    takeScreenshot(name) {
        const timestamp = new Date().toISOString().replace(/[:]/g, '-');
        const filename = `${name}_${timestamp}.png`;
        browser.saveScreenshot(`./test/reports/screenshots/${filename}`);
        return filename;
    }

    /**
     * Handle alert/confirmation dialogs
     * @param {boolean} accept - Whether to accept or dismiss
     */
    handleAlert(accept = true) {
        try {
            if (accept) {
                browser.acceptAlert();
            } else {
                browser.dismissAlert();
            }
        } catch (error) {
            // No alert present
        }
    }

    /**
     * Retry action with exponential backoff
     * @param {Function} action - Action to retry
     * @param {number} maxRetries - Maximum retry attempts
     * @param {number} delay - Initial delay in milliseconds
     */
    retry(action, maxRetries = 3, delay = 1000) {
        let attempts = 0;
        
        const attempt = () => {
            try {
                return action();
            } catch (error) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw error;
                }
                
                browser.pause(delay * Math.pow(2, attempts - 1));
                return attempt();
            }
        };
        
        return attempt();
    }

    /**
     * Verify page title contains expected text
     * @param {string} expectedTitle - Expected title text
     */
    verifyPageTitle(expectedTitle) {
        const actualTitle = browser.getTitle();
        expect(actualTitle).to.include(expectedTitle);
    }

    /**
     * Verify current URL contains expected path
     * @param {string} expectedPath - Expected path
     */
    verifyCurrentUrl(expectedPath) {
        const currentUrl = browser.getUrl();
        expect(currentUrl).to.include(expectedPath);
    }

    /**
     * Get current timestamp for test data
     * @returns {string} Timestamp string
     */
    getTimestamp() {
        return new Date().getTime().toString();
    }

    /**
     * Generate unique test data
     * @param {string} prefix - Prefix for test data
     * @returns {string} Unique test string
     */
    generateTestData(prefix = 'test') {
        return `${prefix}_${this.getTimestamp()}_${Math.random().toString(36).substr(2, 5)}`;
    }

    /**
     * Wait for AJAX requests to complete
     * @param {number} timeout - Timeout in milliseconds
     */
    waitForAjaxComplete(timeout = this.timeout) {
        browser.waitUntil(
            () => {
                return browser.execute(() => {
                    if (typeof jQuery !== 'undefined') {
                        return jQuery.active === 0;
                    }
                    
                    // For applications without jQuery, check for common loading states
                    const loadingElements = document.querySelectorAll('[data-loading="true"], .loading');
                    return loadingElements.length === 0;
                });
            },
            {
                timeout,
                timeoutMsg: 'AJAX requests did not complete within timeout'
            }
        );
    }
}