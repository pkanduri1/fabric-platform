/**
 * Selenium WebDriver Demo Test
 * Demonstrates the Selenium testing framework functionality
 */

const { Builder, By, Key, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

async function runSeleniumDemo() {
    console.log('🚀 Starting Selenium WebDriver Demo Test');
    
    // Configure Chrome options
    const chromeOptions = new chrome.Options();
    chromeOptions.addArguments('--headless'); // Run in headless mode
    chromeOptions.addArguments('--no-sandbox');
    chromeOptions.addArguments('--disable-dev-shm-usage');
    chromeOptions.addArguments('--disable-gpu');
    chromeOptions.addArguments('--window-size=1920,1080');
    
    let driver;
    
    try {
        // Create WebDriver instance
        console.log('🌐 Initializing Chrome WebDriver...');
        driver = await new Builder()
            .forBrowser('chrome')
            .setChromeOptions(chromeOptions)
            .build();
        
        console.log('✅ Chrome WebDriver initialized successfully');
        
        // Test 1: Navigate to React application
        console.log('\n🧪 Test 1: Navigating to React application');
        await driver.get('http://localhost:3000');
        
        // Wait for page to load and get title
        await driver.wait(until.titleContains(''), 5000);
        const title = await driver.getTitle();
        console.log(`📄 Page title: "${title}"`);
        
        // Check if page loaded successfully
        const bodyElement = await driver.findElement(By.tagName('body'));
        const bodyText = await bodyElement.getText();
        console.log(`📝 Page content loaded: ${bodyText.length > 0 ? 'Yes' : 'No'} (${bodyText.length} characters)`);
        
        // Test 2: Check for React root element
        console.log('\n🧪 Test 2: Checking for React root element');
        try {
            await driver.wait(until.elementLocated(By.id('root')), 5000);
            const rootElement = await driver.findElement(By.id('root'));
            const rootContent = await rootElement.getAttribute('innerHTML');
            console.log(`⚛️  React root element found: ${rootContent.length > 0 ? 'Yes' : 'No'}`);
        } catch (error) {
            console.log(`⚛️  React root element: Not found (this is normal for some React apps)`);
        }
        
        // Test 3: Browser capabilities
        console.log('\n🧪 Test 3: Testing browser capabilities');
        const currentUrl = await driver.getCurrentUrl();
        console.log(`📍 Current URL: ${currentUrl}`);
        
        // Test viewport manipulation
        await driver.manage().window().setRect({ width: 1280, height: 720 });
        console.log('📱 Viewport resized to 1280x720');
        
        await driver.manage().window().setRect({ width: 1920, height: 1080 });
        console.log('📱 Viewport resized to 1920x1080');
        
        // Test 4: JavaScript execution
        console.log('\n🧪 Test 4: Testing JavaScript execution');
        const userAgent = await driver.executeScript('return navigator.userAgent;');
        console.log(`🖥️  User Agent: ${userAgent.substring(0, 50)}...`);
        
        const pageLoadState = await driver.executeScript('return document.readyState;');
        console.log(`📋 Page load state: ${pageLoadState}`);
        
        // Test 5: Element interaction simulation
        console.log('\n🧪 Test 5: Testing element interaction capabilities');
        try {
            // Try to find any clickable element
            const clickableElements = await driver.findElements(By.css('button, a, input, [role="button"]'));
            console.log(`🔘 Found ${clickableElements.length} potentially interactive elements`);
            
            // If we find interactive elements, we could test clicking them
            if (clickableElements.length > 0) {
                console.log('✅ Element interaction capability confirmed');
            }
        } catch (error) {
            console.log('ℹ️  No specific interactive elements found (normal for a basic page)');
        }
        
        // Test 6: Network and performance
        console.log('\n🧪 Test 6: Testing performance metrics');
        const loadTime = await driver.executeScript(`
            return performance.timing.loadEventEnd - performance.timing.navigationStart;
        `);
        console.log(`⚡ Page load time: ${loadTime}ms`);
        
        const domElements = await driver.executeScript('return document.querySelectorAll("*").length;');
        console.log(`🏗️  DOM elements count: ${domElements}`);
        
        console.log('\n✅ All Selenium WebDriver tests completed successfully!');
        
        return {
            success: true,
            tests: 6,
            pageTitle: title,
            currentUrl: currentUrl,
            loadTime: loadTime,
            domElements: domElements
        };
        
    } catch (error) {
        console.error('\n❌ Selenium test failed:', error.message);
        return {
            success: false,
            error: error.message
        };
    } finally {
        if (driver) {
            console.log('\n🔚 Closing WebDriver session...');
            await driver.quit();
            console.log('✅ WebDriver session closed');
        }
    }
}

// Run the demo
if (require.main === module) {
    runSeleniumDemo()
        .then(result => {
            console.log('\n🏁 Demo Results:', result);
            process.exit(result.success ? 0 : 1);
        })
        .catch(error => {
            console.error('\n💥 Demo failed:', error);
            process.exit(1);
        });
}

module.exports = { runSeleniumDemo };