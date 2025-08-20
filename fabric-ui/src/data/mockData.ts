// src/data/mockData.ts
import { SourceSystem, SourceField, Configuration } from '../types/configuration';

export const mockSourceSystems: SourceSystem[] = [
  {
    id: 'HR',
    name: 'HR',
    description: 'Human resources',
    systemType: 'Oracle',
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
    id: 'ENCORE',
    name: 'ENCORE',
    description: 'Lightstream loans',
    systemType: 'Oracle',
    jobs: [
      {
        name: 'atoctran',
        sourceSystem: 'ENCORE',
        jobName: 'atoctran',
        description: 'Transaction code 200 processing',
        files: [],
        multiTxn: false,
        supportedTransactionTypes: ['200'],
        defaultFileType: 'atoctran'
      }
    ],
    inputBasePath: '/data/encore/input',
    outputBasePath: '/data/encore/output',
    supportedFileTypes: ['atoctran'],
    supportedTransactionTypes: ['200']
  },
  {
    id: 'SHAW',
    name: 'SHAW',
    description: 'Loan Origination System',
    systemType: 'Oracle',
    jobs: [
      {
        name: 'shaw-p327',
        sourceSystem: 'SHAW',
        jobName: 'shaw-p327',
        description: 'Shaw P327 processing',
        files: [],
        multiTxn: true,
        supportedTransactionTypes: ['200', '300', 'default'],
        defaultFileType: 'shaw'
      }
    ],
    inputBasePath: '/data/shaw/input',
    outputBasePath: '/data/shaw/output',
    supportedFileTypes: ['shaw', 'loan'],
    supportedTransactionTypes: ['200', '300', 'default']
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