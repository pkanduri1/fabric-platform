/**
 * Accessibility Testing Script for Fabric Platform Frontend
 * Uses axe-core for comprehensive accessibility validation
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

const { AxePuppeteer } = require('@axe-core/puppeteer');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Accessibility configuration
const ACCESSIBILITY_CONFIG = {
    baseUrl: 'http://localhost:3000',
    pages: [
        { path: '/', name: 'Home' },
        { path: '/login', name: 'Login' },
        { path: '/template-configuration', name: 'Template Configuration' },
        { path: '/manual-job-configuration', name: 'Manual Job Configuration' },
        { path: '/monitoring-dashboard', name: 'Monitoring Dashboard' }
    ],
    outputDir: './test/reports/accessibility',
    // WCAG compliance levels: 'wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'
    tags: ['wcag2aa', 'wcag21aa', 'section508', 'best-practice'],
    // Violation impact levels to include
    impactLevels: ['critical', 'serious', 'moderate', 'minor'],
    // Elements to exclude from testing
    exclude: [
        '.loading-spinner', // Dynamic loading elements
        '.toast-notification', // Temporary notifications
        '[data-testid="captcha"]' // Third-party captcha
    ]
};

class AccessibilityTestRunner {
    constructor(config = ACCESSIBILITY_CONFIG) {
        this.config = config;
        this.results = [];
        this.browser = null;
        
        // Ensure output directory exists
        if (!fs.existsSync(this.config.outputDir)) {
            fs.mkdirSync(this.config.outputDir, { recursive: true });
        }
    }

    async initialize() {
        console.log('♿ Initializing Accessibility Test Suite');
        
        this.browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-web-security']
        });
        
        console.log('✅ Accessibility test environment initialized');
    }

    async cleanup() {
        console.log('🧹 Cleaning up accessibility test environment');
        
        if (this.browser) {
            await this.browser.close();
        }
        
        console.log('✅ Cleanup completed');
    }

    async runAccessibilityAudit(pageInfo) {
        console.log(`🔍 Running accessibility audit for: ${pageInfo.name}`);
        
        const page = await this.browser.newPage();
        
        try {
            // Set viewport for consistent testing
            await page.setViewport({ width: 1200, height: 800 });
            
            // Navigate to page
            const fullUrl = `${this.config.baseUrl}${pageInfo.path}`;
            await page.goto(fullUrl, { waitUntil: 'networkidle2', timeout: 30000 });
            
            // Wait for any dynamic content to load
            await page.waitForTimeout(2000);
            
            // Initialize axe-puppeteer
            const axe = new AxePuppeteer(page);
            
            // Configure axe with our settings
            axe.withTags(this.config.tags);
            
            if (this.config.exclude.length > 0) {
                axe.exclude(this.config.exclude);
            }
            
            // Run accessibility analysis
            const results = await axe.analyze();
            
            // Process results
            const processedResults = this.processAxeResults(results, pageInfo);
            
            return processedResults;
            
        } catch (error) {
            console.error(`❌ Accessibility audit failed for ${pageInfo.name}:`, error.message);
            return {
                pageName: pageInfo.name,
                url: `${this.config.baseUrl}${pageInfo.path}`,
                error: error.message,
                violations: [],
                passes: [],
                incomplete: [],
                timestamp: new Date().toISOString()
            };
        } finally {
            await page.close();
        }
    }

    processAxeResults(results, pageInfo) {
        const processed = {
            pageName: pageInfo.name,
            url: results.url,
            violations: this.processViolations(results.violations),
            passes: results.passes.length,
            incomplete: results.incomplete.length,
            timestamp: new Date().toISOString(),
            summary: {
                critical: 0,
                serious: 0,
                moderate: 0,
                minor: 0,
                total: results.violations.length
            }
        };
        
        // Count violations by impact
        results.violations.forEach(violation => {
            if (violation.impact) {
                processed.summary[violation.impact]++;
            }
        });
        
        return processed;
    }

    processViolations(violations) {
        return violations.map(violation => ({
            id: violation.id,
            impact: violation.impact,
            tags: violation.tags,
            description: violation.description,
            help: violation.help,
            helpUrl: violation.helpUrl,
            nodes: violation.nodes.map(node => ({
                html: node.html,
                target: node.target,
                failureSummary: node.failureSummary,
                impact: node.impact
            }))
        }));
    }

    async testKeyboardNavigation(pageInfo) {
        console.log(`⌨️ Testing keyboard navigation for: ${pageInfo.name}`);
        
        const page = await this.browser.newPage();
        
        try {
            const fullUrl = `${this.config.baseUrl}${pageInfo.path}`;
            await page.goto(fullUrl, { waitUntil: 'networkidle2' });
            
            // Test tab navigation
            const focusableElements = await this.findFocusableElements(page);
            const tabNavigationResults = await this.testTabNavigation(page, focusableElements);
            
            // Test skip links
            const skipLinkResults = await this.testSkipLinks(page);
            
            return {
                pageName: pageInfo.name,
                url: fullUrl,
                focusableElementCount: focusableElements.length,
                tabNavigation: tabNavigationResults,
                skipLinks: skipLinkResults,
                timestamp: new Date().toISOString()
            };
            
        } catch (error) {
            console.error(`❌ Keyboard navigation test failed for ${pageInfo.name}:`, error.message);
            return null;
        } finally {
            await page.close();
        }
    }

    async findFocusableElements(page) {
        return await page.evaluate(() => {
            const focusableSelectors = [
                'a[href]',
                'button:not([disabled])',
                'input:not([disabled])',
                'select:not([disabled])',
                'textarea:not([disabled])',
                '[tabindex]:not([tabindex="-1"])'
            ];
            
            const elements = Array.from(document.querySelectorAll(focusableSelectors.join(', ')));
            return elements.map(el => ({
                tagName: el.tagName,
                id: el.id,
                className: el.className,
                ariaLabel: el.getAttribute('aria-label'),
                tabIndex: el.tabIndex
            }));
        });
    }

    async testTabNavigation(page, focusableElements) {
        const navigationResults = {
            totalElements: focusableElements.length,
            successfulNavigations: 0,
            failedNavigations: 0,
            tabTrapIssues: [],
            focusVisibilityIssues: []
        };
        
        try {
            // Start from beginning of page
            await page.keyboard.press('Tab');
            
            for (let i = 0; i < Math.min(focusableElements.length, 20); i++) {
                // Check if focus is visible
                const focusVisible = await page.evaluate(() => {
                    const activeElement = document.activeElement;
                    if (!activeElement) return false;
                    
                    const styles = window.getComputedStyle(activeElement);
                    return styles.outline !== 'none' || 
                           styles.outlineWidth !== '0px' ||
                           activeElement.getAttribute('data-focus-visible') !== null;
                });
                
                if (!focusVisible) {
                    navigationResults.focusVisibilityIssues.push(i);
                }
                
                navigationResults.successfulNavigations++;
                await page.keyboard.press('Tab');
                await page.waitForTimeout(100); // Small delay for focus changes
            }
            
        } catch (error) {
            navigationResults.failedNavigations++;
            console.warn('Tab navigation test encountered error:', error.message);
        }
        
        return navigationResults;
    }

    async testSkipLinks(page) {
        const skipLinkResults = {
            found: false,
            functional: false,
            accessible: false
        };
        
        try {
            // Look for skip links
            const skipLinks = await page.evaluate(() => {
                const links = Array.from(document.querySelectorAll('a[href^="#"], [data-skip-link]'));
                return links.map(link => ({
                    href: link.href,
                    text: link.textContent.trim(),
                    visible: link.offsetParent !== null
                }));
            });
            
            if (skipLinks.length > 0) {
                skipLinkResults.found = true;
                
                // Test functionality of first skip link
                await page.keyboard.press('Tab');
                const firstSkipLink = skipLinks[0];
                
                if (firstSkipLink.href) {
                    await page.keyboard.press('Enter');
                    await page.waitForTimeout(500);
                    
                    // Check if focus moved to target
                    const targetReached = await page.evaluate((href) => {
                        const targetId = href.split('#')[1];
                        const targetElement = document.getElementById(targetId);
                        return targetElement && document.activeElement === targetElement;
                    }, firstSkipLink.href);
                    
                    skipLinkResults.functional = targetReached;
                }
            }
            
        } catch (error) {
            console.warn('Skip link test encountered error:', error.message);
        }
        
        return skipLinkResults;
    }

    async testColorContrast(pageInfo) {
        console.log(`🎨 Testing color contrast for: ${pageInfo.name}`);
        
        const page = await this.browser.newPage();
        
        try {
            const fullUrl = `${this.config.baseUrl}${pageInfo.path}`;
            await page.goto(fullUrl, { waitUntil: 'networkidle2' });
            
            // Inject color contrast analysis script
            const contrastResults = await page.evaluate(() => {
                const elements = Array.from(document.querySelectorAll('*'));
                const contrastIssues = [];
                
                elements.forEach(element => {
                    const styles = window.getComputedStyle(element);
                    const color = styles.color;
                    const backgroundColor = styles.backgroundColor;
                    
                    // Simple contrast check (would use proper contrast library in production)
                    if (color && backgroundColor && 
                        color !== 'rgba(0, 0, 0, 0)' && 
                        backgroundColor !== 'rgba(0, 0, 0, 0)') {
                        
                        contrastIssues.push({
                            element: element.tagName,
                            className: element.className,
                            color: color,
                            backgroundColor: backgroundColor
                        });
                    }
                });
                
                return {
                    totalElements: elements.length,
                    elementsWithColor: contrastIssues.length,
                    potentialIssues: contrastIssues.slice(0, 10) // Limit for report
                };
            });
            
            return {
                pageName: pageInfo.name,
                url: fullUrl,
                ...contrastResults,
                timestamp: new Date().toISOString()
            };
            
        } catch (error) {
            console.error(`❌ Color contrast test failed for ${pageInfo.name}:`, error.message);
            return null;
        } finally {
            await page.close();
        }
    }

    async runAllAccessibilityTests() {
        console.log('🚀 Starting comprehensive accessibility test suite');
        
        for (const pageInfo of this.config.pages) {
            console.log(`\n🔍 Testing page: ${pageInfo.name}`);
            
            // Run axe accessibility audit
            const axeResults = await this.runAccessibilityAudit(pageInfo);
            
            // Test keyboard navigation
            const keyboardResults = await this.testKeyboardNavigation(pageInfo);
            
            // Test color contrast
            const contrastResults = await this.testColorContrast(pageInfo);
            
            // Combine results
            this.results.push({
                ...axeResults,
                keyboardNavigation: keyboardResults,
                colorContrast: contrastResults
            });
        }
        
        console.log('✅ All accessibility tests completed');
    }

    validateAccessibilityStandards() {
        console.log('\n📋 Validating accessibility standards');
        
        let totalViolations = 0;
        let criticalViolations = 0;
        let seriousViolations = 0;
        
        this.results.forEach(result => {
            console.log(`\n🔍 ${result.pageName}:`);
            console.log(`  Total Violations: ${result.summary.total}`);
            console.log(`  Critical: ${result.summary.critical}`);
            console.log(`  Serious: ${result.summary.serious}`);
            console.log(`  Moderate: ${result.summary.moderate}`);
            console.log(`  Minor: ${result.summary.minor}`);
            
            totalViolations += result.summary.total;
            criticalViolations += result.summary.critical;
            seriousViolations += result.summary.serious;
        });
        
        console.log(`\n📊 Overall Summary:`);
        console.log(`  Total Violations: ${totalViolations}`);
        console.log(`  Critical Violations: ${criticalViolations}`);
        console.log(`  Serious Violations: ${seriousViolations}`);
        
        return {
            totalViolations,
            criticalViolations,
            seriousViolations,
            passThreshold: criticalViolations === 0 && seriousViolations <= 5
        };
    }

    generateAccessibilityReport() {
        console.log('\n📝 Generating accessibility report');
        
        const reportData = {
            testRun: {
                timestamp: new Date().toISOString(),
                totalPages: this.results.length,
                configuration: this.config
            },
            results: this.results,
            summary: this.generateOverallSummary()
        };
        
        // Save JSON report
        const jsonReportPath = path.join(this.config.outputDir, `accessibility-report-${Date.now()}.json`);
        fs.writeFileSync(jsonReportPath, JSON.stringify(reportData, null, 2));
        
        // Generate HTML report
        const htmlReportPath = this.generateHtmlReport(reportData);
        
        console.log(`📄 JSON Report: ${jsonReportPath}`);
        console.log(`🌐 HTML Report: ${htmlReportPath}`);
        
        return { jsonReportPath, htmlReportPath };
    }

    generateOverallSummary() {
        const summary = {
            totalPages: this.results.length,
            totalViolations: 0,
            violationsByImpact: { critical: 0, serious: 0, moderate: 0, minor: 0 },
            keyboardNavigationIssues: 0,
            colorContrastIssues: 0
        };
        
        this.results.forEach(result => {
            summary.totalViolations += result.summary.total;
            Object.keys(summary.violationsByImpact).forEach(impact => {
                summary.violationsByImpact[impact] += result.summary[impact];
            });
            
            if (result.keyboardNavigation) {
                summary.keyboardNavigationIssues += result.keyboardNavigation.failedNavigations;
            }
        });
        
        return summary;
    }

    generateHtmlReport(reportData) {
        const htmlContent = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fabric Platform Accessibility Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
        .header { background: #f0f8ff; padding: 20px; border-radius: 5px; border-left: 5px solid #007acc; }
        .summary { margin: 20px 0; }
        .violation { margin: 15px 0; padding: 15px; border-radius: 5px; }
        .critical { background: #ffebee; border-left: 4px solid #f44336; }
        .serious { background: #fff3e0; border-left: 4px solid #ff9800; }
        .moderate { background: #f3e5f5; border-left: 4px solid #9c27b0; }
        .minor { background: #e8f5e8; border-left: 4px solid #4caf50; }
        .page-result { margin: 30px 0; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
        .impact-badge { padding: 2px 8px; border-radius: 3px; color: white; font-size: 0.8em; }
        .impact-critical { background: #f44336; }
        .impact-serious { background: #ff9800; }
        .impact-moderate { background: #9c27b0; }
        .impact-minor { background: #4caf50; }
        .node { margin: 10px 0; padding: 10px; background: #f5f5f5; border-radius: 3px; }
        pre { background: #f8f8f8; padding: 10px; border-radius: 3px; overflow-x: auto; }
        .stats { display: flex; gap: 20px; margin: 20px 0; }
        .stat-card { padding: 15px; border-radius: 5px; text-align: center; min-width: 120px; }
        .stat-critical { background: #ffebee; }
        .stat-serious { background: #fff3e0; }
        .stat-moderate { background: #f3e5f5; }
        .stat-minor { background: #e8f5e8; }
    </style>
</head>
<body>
    <div class="header">
        <h1>♿ Fabric Platform Accessibility Report</h1>
        <p><strong>Generated:</strong> ${reportData.testRun.timestamp}</p>
        <p><strong>Pages Tested:</strong> ${reportData.testRun.totalPages}</p>
        <p><strong>Standards:</strong> ${reportData.testRun.configuration.tags.join(', ')}</p>
    </div>
    
    <div class="summary">
        <h2>📊 Overall Summary</h2>
        <div class="stats">
            <div class="stat-card stat-critical">
                <h3>${reportData.summary.violationsByImpact.critical}</h3>
                <p>Critical</p>
            </div>
            <div class="stat-card stat-serious">
                <h3>${reportData.summary.violationsByImpact.serious}</h3>
                <p>Serious</p>
            </div>
            <div class="stat-card stat-moderate">
                <h3>${reportData.summary.violationsByImpact.moderate}</h3>
                <p>Moderate</p>
            </div>
            <div class="stat-card stat-minor">
                <h3>${reportData.summary.violationsByImpact.minor}</h3>
                <p>Minor</p>
            </div>
        </div>
    </div>
    
    <div class="results">
        <h2>📋 Detailed Results</h2>
        ${reportData.results.map(result => `
            <div class="page-result">
                <h3>🔗 ${result.pageName}</h3>
                <p><strong>URL:</strong> ${result.url}</p>
                <p><strong>Total Violations:</strong> ${result.summary.total}</p>
                <p><strong>Tests Passed:</strong> ${result.passes}</p>
                
                ${result.violations.map(violation => `
                    <div class="violation ${violation.impact}">
                        <h4>
                            ${violation.help}
                            <span class="impact-badge impact-${violation.impact}">${violation.impact}</span>
                        </h4>
                        <p><strong>Rule:</strong> ${violation.id}</p>
                        <p><strong>Description:</strong> ${violation.description}</p>
                        <p><strong>Help:</strong> <a href="${violation.helpUrl}" target="_blank">Learn more</a></p>
                        
                        <h5>Affected Elements:</h5>
                        ${violation.nodes.map(node => `
                            <div class="node">
                                <p><strong>Target:</strong> ${node.target.join(', ')}</p>
                                <p><strong>HTML:</strong></p>
                                <pre>${this.escapeHtml(node.html)}</pre>
                                ${node.failureSummary ? `<p><strong>Issue:</strong> ${node.failureSummary}</p>` : ''}
                            </div>
                        `).join('')}
                    </div>
                `).join('')}
                
                ${result.keyboardNavigation ? `
                    <h4>⌨️ Keyboard Navigation</h4>
                    <p>Focusable Elements: ${result.keyboardNavigation.totalElements}</p>
                    <p>Successful Navigations: ${result.keyboardNavigation.successfulNavigations}</p>
                    <p>Failed Navigations: ${result.keyboardNavigation.failedNavigations}</p>
                ` : ''}
            </div>
        `).join('')}
    </div>
</body>
</html>`;

        const htmlReportPath = path.join(this.config.outputDir, `accessibility-report-${Date.now()}.html`);
        fs.writeFileSync(htmlReportPath, htmlContent);
        
        return htmlReportPath;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Main execution
async function runAccessibilityTests() {
    const testRunner = new AccessibilityTestRunner();
    
    try {
        await testRunner.initialize();
        await testRunner.runAllAccessibilityTests();
        
        const validation = testRunner.validateAccessibilityStandards();
        const reports = testRunner.generateAccessibilityReport();
        
        // Exit with error code if critical accessibility issues found
        if (!validation.passThreshold) {
            console.log(`\n❌ Accessibility standards not met:`);
            console.log(`  Critical violations: ${validation.criticalViolations}`);
            console.log(`  Serious violations: ${validation.seriousViolations}`);
            process.exit(1);
        } else {
            console.log('\n✅ Accessibility standards met!');
            process.exit(0);
        }
        
    } catch (error) {
        console.error('❌ Accessibility test execution failed:', error.message);
        process.exit(1);
    } finally {
        await testRunner.cleanup();
    }
}

// Run if called directly
if (require.main === module) {
    runAccessibilityTests();
}

module.exports = AccessibilityTestRunner;