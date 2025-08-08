// src/data/mockData.ts
import { SourceSystem, SourceField, Configuration } from '../types/configuration';

export const mockSourceSystems: SourceSystem[] = [
  {
    id: 'hr',
    name: 'HR System',
    description: 'Human Resources management system for employee data processing',
    systemType: 'SAP',
    jobs: [
      {
        name: 'p327',
        sourceSystem: 'hr',
        jobName: 'p327',
        description: 'Employee payroll processing',
        files: [],
        multiTxn: true,
        supportedTransactionTypes: ['200', '900', 'default'],
        defaultFileType: 'payroll'
      },
      {
        name: 'atoctran',
        sourceSystem: 'hr',
        jobName: 'atoctran',
        description: 'Automated timesheet transactions',
        files: [],
        multiTxn: false,
        supportedTransactionTypes: ['default'],
        defaultFileType: 'timesheet'
      }
    ],
    inputBasePath: '/data/hr/input',
    outputBasePath: '/data/hr/output',
    supportedFileTypes: ['payroll', 'timesheet', 'benefits'],
    supportedTransactionTypes: ['200', '900', 'default']
  },
  {
    id: 'finance',
    name: 'Finance System',
    description: 'Financial data processing and accounting system',
    systemType: 'Oracle',
    jobs: [
      {
        name: 'gl_extract',
        sourceSystem: 'finance',
        jobName: 'gl_extract',
        description: 'General ledger data extraction',
        files: [],
        multiTxn: true,
        supportedTransactionTypes: ['100', '200', 'default'],
        defaultFileType: 'ledger'
      },
      {
        name: 'ap_batch',
        sourceSystem: 'finance',
        jobName: 'ap_batch',
        description: 'Accounts payable batch processing',
        files: [],
        multiTxn: false,
        supportedTransactionTypes: ['default'],
        defaultFileType: 'invoice'
      },
      {
        name: 'budget_sync',
        sourceSystem: 'finance',
        jobName: 'budget_sync',
        description: 'Budget synchronization process',
        files: [],
        multiTxn: false,
        supportedTransactionTypes: ['default'],
        defaultFileType: 'budget'
      }
    ],
    inputBasePath: '/data/finance/input',
    outputBasePath: '/data/finance/output',
    supportedFileTypes: ['ledger', 'invoice', 'budget'],
    supportedTransactionTypes: ['100', '200', 'default']
  },
  {
    id: 'inventory',
    name: 'Inventory Management',
    description: 'Warehouse and inventory tracking system',
    systemType: 'Custom',
    jobs: [
      {
        name: 'stock_update',
        sourceSystem: 'inventory',
        jobName: 'stock_update',
        description: 'Real-time stock level updates',
        files: [],
        multiTxn: true,
        supportedTransactionTypes: ['stock_in', 'stock_out', 'adjustment'],
        defaultFileType: 'inventory'
      }
    ],
    inputBasePath: '/data/inventory/input',
    outputBasePath: '/data/inventory/output',
    supportedFileTypes: ['inventory', 'warehouse'],
    supportedTransactionTypes: ['stock_in', 'stock_out', 'adjustment']
  }
];

export const mockSourceFields: SourceField[] = [
  { name: 'employee_id', dataType: 'string', maxLength: 10, nullable: false, description: 'Unique employee identifier' },
  { name: 'first_name', dataType: 'string', maxLength: 50, nullable: false, description: 'Employee first name' },
  { name: 'last_name', dataType: 'string', maxLength: 50, nullable: false, description: 'Employee last name' },
  { name: 'department', dataType: 'string', maxLength: 30, nullable: true, description: 'Department code' },
  { name: 'salary', dataType: 'numeric', maxLength: 12, nullable: false, description: 'Annual salary amount' },
  { name: 'hire_date', dataType: 'date', nullable: false, description: 'Date of hire' },
  { name: 'status', dataType: 'string', maxLength: 10, nullable: false, description: 'Employment status' },
  { name: 'manager_id', dataType: 'string', maxLength: 10, nullable: true, description: 'Manager employee ID' }
];

export const mockConfiguration: Configuration = {
  fileType: 'payroll',
  transactionType: '200',
  sourceSystem: 'hr',
  jobName: 'p327',
  fieldMappings: [
    {
        id: 'field_1',
        fieldName: 'emp_id',
        sourceField: 'employee_id',
        targetField: 'EMPLOYEE_ID',
        targetPosition: 1,
        length: 10,
        dataType: 'string',
        transformationType: 'source',
        transactionType: '200',
        required: false,
        expression: undefined
    },
    {
        id: 'field_2',
        fieldName: 'full_name',
        targetField: 'FULL_NAME',
        targetPosition: 2,
        length: 50,
        dataType: 'string',
        transformationType: 'composite',
        sources: [
            { field: 'first_name' },
            { field: 'last_name' }
        ],
        transform: 'concat',
        delimiter: ' ',
        transactionType: '200',
        required: false,
        expression: undefined
    },
    {
        id: 'field_3',
        fieldName: 'record_type',
        targetField: 'RECORD_TYPE',
        targetPosition: 3,
        length: 3,
        dataType: 'string',
        transformationType: 'constant',
        value: '200',
        transactionType: '200',
        required: false,
        expression: undefined
    }
  ],
  availableTransactionTypes: ['200', '900', 'default']
};

// Mock API responses
export const mockApiResponses = {
  getSourceSystems: () => Promise.resolve(mockSourceSystems),
  getSourceFields: (systemId: string) => Promise.resolve(mockSourceFields),
  getFieldMappings: (system: string, job: string) => Promise.resolve([mockConfiguration]),
  validateMapping: () => Promise.resolve({
    isValid: true,
    warnings: [],
    errors: [],
    summary: {
      totalFields: 3,
      recordLength: 63,
      sourceFieldsUsed: 2,
      constantFields: 1,
      transactionTypes: 1
    }
  })
};