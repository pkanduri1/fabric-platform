/**
 * Performance Testing Script for Fabric Platform Frontend
 * Uses Lighthouse and Puppeteer for performance analysis
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */

const lighthouse = require('lighthouse');
const chromeLauncher = require('chrome-launcher');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Performance thresholds
const PERFORMANCE_THRESHOLDS = {
    performance: 85,
    accessibility: 90,
    bestPractices: 85,
    seo: 80,
    pwa: 60
};

// Test configuration
const TEST_CONFIG = {
    baseUrl: 'http://localhost:3000',
    pages: [
        '/',
        '/login',
        '/template-configuration',
        '/manual-job-configuration',
        '/monitoring-dashboard'
    ],
    outputDir: './test/reports/performance',
    chromePath: null // Will auto-detect Chrome
};

class PerformanceTestRunner {
    constructor(config = TEST_CONFIG) {
        this.config = config;
        this.results = [];
        this.browser = null;
        this.chrome = null;
        
        // Ensure output directory exists
        if (!fs.existsSync(this.config.outputDir)) {
            fs.mkdirSync(this.config.outputDir, { recursive: true });
        }
    }

    async initialize() {
        console.log('🚀 Initializing Performance Test Suite');
        
        // Launch Chrome for Lighthouse
        this.chrome = await chromeLauncher.launch({
            chromeFlags: ['--headless', '--no-sandbox', '--disable-gpu']
        });
        
        // Initialize Puppeteer for additional metrics
        this.browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        
        console.log('✅ Performance test environment initialized');
    }

    async cleanup() {
        console.log('🧹 Cleaning up performance test environment');
        
        if (this.browser) {
            await this.browser.close();
        }
        
        if (this.chrome) {
            await this.chrome.kill();
        }
        
        console.log('✅ Cleanup completed');
    }

    async runLighthouseAudit(url) {
        console.log(`🔍 Running Lighthouse audit for: ${url}`);
        
        const options = {
            logLevel: 'info',
            output: 'html',
            onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo', 'pwa'],
            port: this.chrome.port,
            disableDeviceEmulation: false,
            chromeFlags: ['--headless', '--no-sandbox']
        };

        try {
            const runnerResult = await lighthouse(url, options);
            
            // Extract scores
            const scores = {
                performance: runnerResult.lhr.categories.performance.score * 100,
                accessibility: runnerResult.lhr.categories.accessibility.score * 100,
                bestPractices: runnerResult.lhr.categories['best-practices'].score * 100,
                seo: runnerResult.lhr.categories.seo.score * 100,
                pwa: runnerResult.lhr.categories.pwa.score * 100
            };

            // Extract key metrics
            const metrics = this.extractKeyMetrics(runnerResult.lhr);
            
            // Save HTML report
            const reportName = `lighthouse_${this.sanitizeUrl(url)}_${Date.now()}.html`;
            const reportPath = path.join(this.config.outputDir, reportName);
            fs.writeFileSync(reportPath, runnerResult.report);
            
            return {
                url,
                scores,
                metrics,
                reportPath,
                timestamp: new Date().toISOString()
            };
            
        } catch (error) {
            console.error(`❌ Lighthouse audit failed for ${url}:`, error.message);
            return null;
        }
    }

    extractKeyMetrics(lhr) {
        const audits = lhr.audits;
        
        return {
            firstContentfulPaint: audits['first-contentful-paint']?.numericValue || 0,
            largestContentfulPaint: audits['largest-contentful-paint']?.numericValue || 0,
            firstMeaningfulPaint: audits['first-meaningful-paint']?.numericValue || 0,
            speedIndex: audits['speed-index']?.numericValue || 0,
            timeToInteractive: audits['interactive']?.numericValue || 0,
            totalBlockingTime: audits['total-blocking-time']?.numericValue || 0,
            cumulativeLayoutShift: audits['cumulative-layout-shift']?.numericValue || 0
        };
    }

    async measureLoadTimes(url) {
        console.log(`⏱️ Measuring load times for: ${url}`);
        
        const page = await this.browser.newPage();
        
        try {
            // Enable performance monitoring
            await page.evaluateOnNewDocument(() => {
                window.performanceMetrics = [];
            });

            // Navigate and measure
            const startTime = Date.now();
            
            await page.goto(url, { waitUntil: 'networkidle2', timeout: 30000 });
            
            const endTime = Date.now();
            const totalLoadTime = endTime - startTime;

            // Get browser performance metrics
            const performanceMetrics = await page.evaluate(() => {
                const perf = performance.getEntriesByType('navigation')[0];
                return {
                    domContentLoaded: perf.domContentLoadedEventEnd - perf.navigationStart,
                    domComplete: perf.domComplete - perf.navigationStart,
                    loadComplete: perf.loadEventEnd - perf.navigationStart,
                    firstPaint: performance.getEntriesByType('paint').find(entry => entry.name === 'first-paint')?.startTime || 0,
                    firstContentfulPaint: performance.getEntriesByType('paint').find(entry => entry.name === 'first-contentful-paint')?.startTime || 0
                };
            });

            // Measure bundle sizes
            const bundleInfo = await this.measureBundleSizes(page);

            return {
                url,
                totalLoadTime,
                ...performanceMetrics,
                ...bundleInfo,
                timestamp: new Date().toISOString()
            };

        } catch (error) {
            console.error(`❌ Load time measurement failed for ${url}:`, error.message);
            return null;
        } finally {
            await page.close();
        }
    }

    async measureBundleSizes(page) {
        try {
            const resourceSizes = await page.evaluate(() => {
                const resources = performance.getEntriesByType('resource');
                const bundleInfo = {
                    totalSize: 0,
                    jsSize: 0,
                    cssSize: 0,
                    imageSize: 0,
                    fontSize: 0
                };

                resources.forEach(resource => {
                    const size = resource.transferSize || 0;
                    bundleInfo.totalSize += size;

                    if (resource.name.includes('.js')) {
                        bundleInfo.jsSize += size;
                    } else if (resource.name.includes('.css')) {
                        bundleInfo.cssSize += size;
                    } else if (resource.name.match(/\.(png|jpg|jpeg|gif|svg|webp)$/i)) {
                        bundleInfo.imageSize += size;
                    } else if (resource.name.match(/\.(woff|woff2|ttf|eot)$/i)) {
                        bundleInfo.fontSize += size;
                    }
                });

                return bundleInfo;
            });

            return resourceSizes;
        } catch (error) {
            console.error('Failed to measure bundle sizes:', error.message);
            return {
                totalSize: 0,
                jsSize: 0,
                cssSize: 0,
                imageSize: 0,
                fontSize: 0
            };
        }
    }

    async runPerformanceTests() {
        console.log('🏃 Starting performance test suite');
        
        for (const pagePath of this.config.pages) {
            const fullUrl = `${this.config.baseUrl}${pagePath}`;
            
            console.log(`\n📊 Testing page: ${fullUrl}`);
            
            // Run Lighthouse audit
            const lighthouseResult = await this.runLighthouseAudit(fullUrl);
            
            // Measure load times
            const loadTimeResult = await this.measureLoadTimes(fullUrl);
            
            // Combine results
            if (lighthouseResult && loadTimeResult) {
                this.results.push({
                    ...lighthouseResult,
                    loadTimes: loadTimeResult
                });
            }
        }
        
        console.log('✅ Performance tests completed');
    }

    validatePerformanceThresholds() {
        console.log('\n📋 Validating performance thresholds');
        
        const failures = [];
        
        this.results.forEach(result => {
            console.log(`\n🔍 Checking ${result.url}:`);
            
            Object.entries(PERFORMANCE_THRESHOLDS).forEach(([category, threshold]) => {
                const score = result.scores[category];
                const passed = score >= threshold;
                
                console.log(`  ${category}: ${score.toFixed(1)}/100 ${passed ? '✅' : '❌'}`);
                
                if (!passed) {
                    failures.push({
                        url: result.url,
                        category,
                        score,
                        threshold,
                        difference: threshold - score
                    });
                }
            });
        });
        
        return failures;
    }

    generatePerformanceReport() {
        console.log('\n📝 Generating performance report');
        
        const reportData = {
            testRun: {
                timestamp: new Date().toISOString(),
                totalPages: this.results.length,
                thresholds: PERFORMANCE_THRESHOLDS
            },
            results: this.results,
            summary: this.generateSummary()
        };
        
        // Save JSON report
        const jsonReportPath = path.join(this.config.outputDir, `performance-report-${Date.now()}.json`);
        fs.writeFileSync(jsonReportPath, JSON.stringify(reportData, null, 2));
        
        // Generate HTML report
        const htmlReportPath = this.generateHtmlReport(reportData);
        
        console.log(`📄 JSON Report: ${jsonReportPath}`);
        console.log(`🌐 HTML Report: ${htmlReportPath}`);
        
        return { jsonReportPath, htmlReportPath };
    }

    generateSummary() {
        if (this.results.length === 0) {
            return {};
        }

        const summary = {
            averageScores: {},
            averageLoadTimes: {}
        };

        // Calculate average scores
        Object.keys(PERFORMANCE_THRESHOLDS).forEach(category => {
            const scores = this.results.map(r => r.scores[category]);
            summary.averageScores[category] = scores.reduce((a, b) => a + b, 0) / scores.length;
        });

        // Calculate average load times
        const loadTimeMetrics = ['totalLoadTime', 'domContentLoaded', 'loadComplete', 'firstContentfulPaint'];
        loadTimeMetrics.forEach(metric => {
            const times = this.results
                .map(r => r.loadTimes[metric])
                .filter(t => t > 0);
            
            if (times.length > 0) {
                summary.averageLoadTimes[metric] = times.reduce((a, b) => a + b, 0) / times.length;
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
    <title>Fabric Platform Performance Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
        .summary { margin: 20px 0; }
        .results { margin-top: 30px; }
        .page-result { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .score { display: inline-block; margin: 5px 10px; padding: 5px 10px; border-radius: 3px; }
        .score.good { background: #d4edda; color: #155724; }
        .score.average { background: #fff3cd; color: #856404; }
        .score.poor { background: #f8d7da; color: #721c24; }
        .metrics { margin-top: 15px; }
        .metric { margin: 5px 0; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🚀 Fabric Platform Performance Report</h1>
        <p>Generated: ${reportData.testRun.timestamp}</p>
        <p>Pages Tested: ${reportData.testRun.totalPages}</p>
    </div>
    
    <div class="summary">
        <h2>📊 Summary</h2>
        <h3>Average Scores:</h3>
        ${Object.entries(reportData.summary.averageScores || {}).map(([category, score]) => `
            <span class="score ${this.getScoreClass(score, PERFORMANCE_THRESHOLDS[category])}">
                ${category}: ${score.toFixed(1)}
            </span>
        `).join('')}
        
        <h3>Average Load Times:</h3>
        <table>
            <tr><th>Metric</th><th>Time (ms)</th></tr>
            ${Object.entries(reportData.summary.averageLoadTimes || {}).map(([metric, time]) => `
                <tr><td>${metric}</td><td>${time.toFixed(0)}</td></tr>
            `).join('')}
        </table>
    </div>
    
    <div class="results">
        <h2>📋 Detailed Results</h2>
        ${reportData.results.map(result => `
            <div class="page-result">
                <h3>🔗 ${result.url}</h3>
                <div class="scores">
                    ${Object.entries(result.scores).map(([category, score]) => `
                        <span class="score ${this.getScoreClass(score, PERFORMANCE_THRESHOLDS[category])}">
                            ${category}: ${score.toFixed(1)}
                        </span>
                    `).join('')}
                </div>
                <div class="metrics">
                    <h4>Key Metrics:</h4>
                    <div class="metric">First Contentful Paint: ${result.metrics.firstContentfulPaint}ms</div>
                    <div class="metric">Largest Contentful Paint: ${result.metrics.largestContentfulPaint}ms</div>
                    <div class="metric">Time to Interactive: ${result.metrics.timeToInteractive}ms</div>
                    <div class="metric">Total Load Time: ${result.loadTimes.totalLoadTime}ms</div>
                </div>
            </div>
        `).join('')}
    </div>
</body>
</html>`;

        const htmlReportPath = path.join(this.config.outputDir, `performance-report-${Date.now()}.html`);
        fs.writeFileSync(htmlReportPath, htmlContent);
        
        return htmlReportPath;
    }

    getScoreClass(score, threshold) {
        if (score >= threshold) return 'good';
        if (score >= threshold - 15) return 'average';
        return 'poor';
    }

    sanitizeUrl(url) {
        return url.replace(/[^a-zA-Z0-9]/g, '_');
    }
}

// Main execution
async function runPerformanceTests() {
    const testRunner = new PerformanceTestRunner();
    
    try {
        await testRunner.initialize();
        await testRunner.runPerformanceTests();
        
        const failures = testRunner.validatePerformanceThresholds();
        const reports = testRunner.generatePerformanceReport();
        
        // Exit with error code if thresholds not met
        if (failures.length > 0) {
            console.log(`\n❌ ${failures.length} performance threshold failures detected:`);
            failures.forEach(failure => {
                console.log(`  ${failure.url} - ${failure.category}: ${failure.score} (need ${failure.threshold})`);
            });
            process.exit(1);
        } else {
            console.log('\n✅ All performance thresholds met!');
            process.exit(0);
        }
        
    } catch (error) {
        console.error('❌ Performance test execution failed:', error.message);
        process.exit(1);
    } finally {
        await testRunner.cleanup();
    }
}

// Run if called directly
if (require.main === module) {
    runPerformanceTests();
}

module.exports = PerformanceTestRunner;