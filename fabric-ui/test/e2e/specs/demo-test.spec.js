/**
 * Demo Selenium Test to Validate Testing Framework
 * Simple test to verify WebdriverIO and Selenium integration is working
 */

const { expect } = require('chai');

describe('Fabric Platform Demo Test', () => {
    
    before(() => {
        console.log('🚀 Starting Fabric Platform Demo Test');
    });
    
    after(() => {
        console.log('✅ Fabric Platform Demo Test Complete');
    });

    it('should load the React application homepage', () => {
        console.log('🧪 Testing React application homepage load');
        
        // Navigate to the application
        browser.url('/');
        
        // Wait for the page to load
        browser.waitUntil(() => {
            return browser.getTitle().length > 0;
        }, { timeout: 10000, timeoutMsg: 'Page title not loaded' });
        
        // Verify the page loaded
        const title = browser.getTitle();
        console.log(`📄 Page title: ${title}`);
        
        // Check that we have a React app (basic validation)
        const body = $('body');
        expect(body.isDisplayed()).to.be.true;
        
        console.log('✅ React application loaded successfully');
    });
    
    it('should have basic navigation elements', () => {
        console.log('🧪 Testing basic navigation elements');
        
        // Navigate to homepage
        browser.url('/');
        
        // Wait for React to render
        browser.pause(2000);
        
        // Check for any navigation elements (this is basic validation)
        const bodyText = $('body').getText();
        console.log(`📝 Page contains content: ${bodyText.length > 0 ? 'Yes' : 'No'}`);
        
        // Basic verification that the page has content
        expect(bodyText.length).to.be.greaterThan(0);
        
        console.log('✅ Navigation elements validation complete');
    });
    
    it('should be responsive to browser window changes', () => {
        console.log('🧪 Testing responsive design');
        
        // Test different viewport sizes
        const viewports = [
            { width: 1920, height: 1080, name: 'Desktop' },
            { width: 768, height: 1024, name: 'Tablet' },
            { width: 375, height: 667, name: 'Mobile' }
        ];
        
        browser.url('/');
        
        viewports.forEach(viewport => {
            console.log(`📱 Testing ${viewport.name} viewport (${viewport.width}x${viewport.height})`);
            
            // Set viewport size
            browser.setWindowSize(viewport.width, viewport.height);
            
            // Wait for resize
            browser.pause(1000);
            
            // Verify page is still functional
            const body = $('body');
            expect(body.isDisplayed()).to.be.true;
            
            console.log(`✅ ${viewport.name} viewport test passed`);
        });
        
        // Reset to standard size
        browser.maximizeWindow();
    });
    
    it('should handle browser navigation', () => {
        console.log('🧪 Testing browser navigation functionality');
        
        // Navigate to homepage
        browser.url('/');
        
        // Test browser back/forward functionality
        const originalUrl = browser.getUrl();
        console.log(`📍 Original URL: ${originalUrl}`);
        
        // Navigate to a different path (if exists)
        browser.url('/configuration');
        
        // Go back
        browser.back();
        
        // Verify we're back to original location
        browser.waitUntil(() => {
            return browser.getUrl() === originalUrl;
        }, { timeout: 5000, timeoutMsg: 'Browser navigation failed' });
        
        console.log('✅ Browser navigation test passed');
    });
});