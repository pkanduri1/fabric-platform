const http = require('http');
const url = require('url');

const server = http.createServer((req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    // Handle OPTIONS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    const parsedUrl = url.parse(req.url, true);
    const path = parsedUrl.pathname;
    
    console.log(`\n🔍 [${new Date().toISOString()}] ${req.method} ${req.url}`);
    console.log('Headers:', req.headers);

    let body = '';
    req.on('data', chunk => {
        body += chunk.toString();
    });

    req.on('end', () => {
        if (body) {
            console.log('📦 Request Body:', body);
            
            try {
                const jsonData = JSON.parse(body);
                console.log('🔍 PARSED JSON REQUEST:', JSON.stringify(jsonData, null, 2));
                
                if (jsonData.fieldMappings) {
                    console.log('\n🎯 ANALYZING FIELD MAPPINGS:');
                    jsonData.fieldMappings.forEach((field, index) => {
                        console.log(`  Field ${index + 1}: ${field.fieldName}`);
                        console.log(`    transformationType: ${field.transformationType}`);
                        console.log(`    value: ${field.value}`);
                        console.log(`    sourceField: ${field.sourceField}`);
                        
                        if (field.transformationType === 'constant') {
                            if (!field.value || field.value === '') {
                                console.log(`    ❌ EMPTY CONSTANT VALUE DETECTED!`);
                            } else {
                                console.log(`    ✅ Constant value: "${field.value}"`);
                            }
                        }
                    });
                    
                    // Count transformation types
                    const transformationCounts = jsonData.fieldMappings.reduce((counts, field) => {
                        counts[field.transformationType || 'undefined'] = (counts[field.transformationType || 'undefined'] || 0) + 1;
                        return counts;
                    }, {});
                    console.log('\n📊 TRANSFORMATION TYPE SUMMARY:', transformationCounts);
                }
            } catch (e) {
                console.log('❌ Failed to parse JSON:', e.message);
            }
        }

        // Mock successful response
        if (path === '/ui/mappings/save') {
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            res.end('cfg_mock_' + Date.now());
        } else if (path === '/api/configuration/source-systems') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify([
                {
                    id: 'SHAW',
                    name: 'SHAW',
                    description: 'Shaw Source System',
                    systemType: 'Oracle',
                    inputBasePath: '/data/shaw/input',
                    outputBasePath: '/data/shaw/output'
                },
                {
                    id: 'ENCORE',
                    name: 'ENCORE', 
                    description: 'Encore Source System',
                    systemType: 'Oracle',
                    inputBasePath: '/data/encore/input',
                    outputBasePath: '/data/encore/output'
                }
            ]));
        } else if (path === '/template/file-types' || path === '/admin/templates/file-types') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify([
                {
                    fileType: 'atoctran',
                    description: 'Account Transaction File',
                    recordLength: 200
                },
                {
                    fileType: 'p327',
                    description: 'P327 File Format',
                    recordLength: 327
                }
            ]));
        } else if (path === '/template/transaction-types' || path.startsWith('/admin/templates/atoctran/transaction-types')) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(['200', '900']));
        } else if (path.startsWith('/template/fields/') || path.startsWith('/admin/templates/atoctran/900/fields')) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify([
                {
                    fieldName: 'location-code',
                    targetField: 'location-code',
                    targetPosition: 1,
                    length: 6,
                    dataType: 'CHAR',
                    transformationType: 'source',
                    required: 'Y'
                },
                {
                    fieldName: 'transaction-type',
                    targetField: 'transaction-type', 
                    targetPosition: 2,
                    length: 3,
                    dataType: 'CHAR',
                    transformationType: 'source',
                    required: 'Y'
                }
            ]));
        } else if (path.startsWith('/template/generate-from-template') || path.startsWith('/admin/templates/generate-from-template')) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                sourceSystem: 'SHAW',
                jobName: 'atoctran-900',
                transactionType: '900',
                fields: [
                    {
                        fieldName: 'location-code',
                        targetField: 'location-code',
                        targetPosition: 1,
                        length: 6,
                        dataType: 'CHAR',
                        transformationType: 'source',
                        sourceField: '',
                        value: null
                    },
                    {
                        fieldName: 'transaction-type',
                        targetField: 'transaction-type',
                        targetPosition: 2, 
                        length: 3,
                        dataType: 'CHAR',
                        transformationType: 'source',
                        sourceField: '',
                        value: null
                    }
                ],
                templateMetadata: {
                    fileType: 'atoctran',
                    transactionType: '900'
                }
            }));
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('Not Found');
        }
    });
});

const PORT = 8081;
server.listen(PORT, () => {
    console.log(`🚀 Mock Backend Server running on port ${PORT}`);
    console.log(`🔍 Will log all requests and analyze constant field values`);
});