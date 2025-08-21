/**
 * WebdriverIO Configuration for Fabric Platform E2E Testing
 * Supports Chrome, Firefox, and Edge browsers with comprehensive reporting
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

const { join } = require('path');

exports.config = {
    // Test runner configuration
    runner: 'local',
    
    // Test specification patterns
    specs: [
        './test/e2e/specs/**/*.spec.js'
    ],
    
    // Exclude specific test patterns
    exclude: [
        './test/e2e/specs/**/*.skip.js'
    ],
    
    // Browser capabilities
    capabilities: [{
        // Chrome configuration
        maxInstances: 5,
        browserName: 'chrome',
        acceptInsecureCerts: true,
        'goog:chromeOptions': {
            args: [
                '--no-sandbox',
                '--disable-infobars',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--window-size=1920,1080',
                // Add headless mode for CI/CD
                ...(process.env.CI ? ['--headless'] : [])
            ]
        }
    }],
    
    // Test execution configuration
    logLevel: 'info',
    bail: 0,
    baseUrl: 'http://localhost:3000',
    waitforTimeout: 10000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    
    // Services configuration
    services: [
        ['chromedriver', {
            logFileName: 'wdio-chromedriver.log',
            outputDir: './test/reports/driver-logs',
            args: ['--silent']
        }]
    ],
    
    // Framework configuration
    framework: 'mocha',
    mochaOpts: {
        ui: 'bdd',
        timeout: 60000
    },
    
    // Reporting configuration
    reporters: [
        'spec',
        ['allure', {
            outputDir: './test/reports/allure-results',
            disableWebdriverStepsReporting: false,
            disableWebdriverScreenshotsReporting: false,
            useCucumberStepReporter: false
        }],
        ['junit', {
            outputDir: './test/reports/junit',
            outputFileFormat: function(options) {
                return `junit-results-${new Date().getTime()}.xml`;
            }
        }]
    ],
    
    // Hook functions
    onPrepare: function (config, capabilities) {
        console.log('🚀 Starting Fabric Platform E2E Test Suite');
        console.log(`📊 Base URL: ${config.baseUrl}`);
        console.log(`🌐 Browser: ${capabilities[0].browserName}`);
        console.log(`⚡ Max Instances: ${capabilities[0].maxInstances}`);
    },
    
    onComplete: function(exitCode, config, capabilities, results) {
        console.log('\n📋 E2E Test Suite Complete');
        console.log(`✅ Passed: ${results.passed}`);
        console.log(`❌ Failed: ${results.failed}`);
        console.log(`⏭️  Skipped: ${results.skipped}`);
        
        const successRate = (results.passed / (results.passed + results.failed)) * 100;
        console.log(`📊 Success Rate: ${successRate.toFixed(1)}%`);
        
        if (results.failed > 0) {
            console.log('\n📄 Check reports in: ./test/reports/');
            console.log('🔍 Allure Report: npm run test:report');
        }
    },
    
    beforeSession: function (config, capabilities, specs) {
        // Setup before each session
        console.log(`🎯 Starting session for: ${specs.join(', ')}`);
    },
    
    before: function (capabilities, specs) {
        // Global setup before all tests
        global.expect = require('chai').expect;
        
        // Set global timeouts
        browser.setTimeout({
            'implicit': 5000,
            'pageLoad': 10000,
            'script': 60000
        });
        
        // Maximize browser window
        browser.maximizeWindow();
    },
    
    beforeTest: function (test, context) {
        console.log(`🧪 Running: ${test.title}`);
    },
    
    afterTest: function(test, context, { error, result, duration, passed, retries }) {
        // Take screenshot on failure
        if (!passed) {
            const timestamp = new Date().toISOString().replace(/[:]/g, '-');
            const filename = `${test.title.replace(/\s/g, '_')}_${timestamp}.png`;
            const screenshotPath = join('./test/reports/screenshots', filename);
            
            try {
                browser.saveScreenshot(screenshotPath);
                console.log(`📸 Screenshot saved: ${screenshotPath}`);
            } catch (e) {
                console.error('❌ Failed to capture screenshot:', e.message);
            }
        }
        
        console.log(`${passed ? '✅' : '❌'} ${test.title} (${duration}ms)`);
    },
    
    after: function (result, capabilities, specs) {
        // Cleanup after all tests
    },
    
    afterSession: function (config, capabilities, specs) {
        console.log(`🏁 Session complete for: ${specs.join(', ')}`);
    }
};

// Environment-specific configurations
if (process.env.BROWSER === 'firefox') {
    exports.config.capabilities = [{
        maxInstances: 3,
        browserName: 'firefox',
        acceptInsecureCerts: true,
        'moz:firefoxOptions': {
            args: process.env.CI ? ['-headless'] : []
        }
    }];
    
    exports.config.services = [
        ['geckodriver', {
            logFileName: 'wdio-geckodriver.log',
            outputDir: './test/reports/driver-logs'
        }]
    ];
}

// CI/CD specific configurations
if (process.env.CI) {
    exports.config.maxInstances = 2;
    exports.config.connectionRetryCount = 5;
    exports.config.waitforTimeout = 15000;
}