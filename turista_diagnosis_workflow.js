export const meta = {
    name: 'turista-endpoint-diagnosis',
    description: 'Diagnose why the /api/turistas endpoint returns empty array',
    phases: [
      { title: 'Database Connection Check', detail: 'Verify Oracle database connectivity' },
      { title: 'Table Existence Check', detail: 'Verify TURISTA table exists and structure' },
      { title: 'Data Existence Check', detail: 'Check if TURISTA table contains data' },
      { title: 'DAO Verification', detail: 'Test TuristaDAO directly' },
      { title: 'Endpoint Verification', detail: 'Verify TuristaHandler integration' }
    ],
  }

// Phase 1: Database Connection Check
phase('Database Connection Check')
const dbConnectionResult = await agent('Test database connection to Oracle 10g XE at 192.168.64.4:1521:xe with username yosef/yosef123', {label: 'db-connection-check'})

// Phase 2: Table Existence Check
phase('Table Existence Check')
const tableCheckResult = await agent('Verify TURISTA table exists in Oracle database and check its structure (columns: ID_TURISTA, NOME_TURISTA, NACIONALIDADE)', {label: 'table-existence-check'})

// Phase 3: Data Existence Check
phase('Data Existence Check')
const dataCheckResult = await agent('Query TURISTA table to count records and show sample data if any exist', {label: 'data-existence-check'})

// Phase 4: DAO Verification
phase('DAO Verification')
const daoTestResult = await agent('Test TuristaDAO.getAllTuristas() method directly to see what it returns', {label: 'dao-verification'})

// Phase 5: Endpoint Verification
phase('Endpoint Verification')
const endpointTestResult = await agent('Verify TuristaHandler correctly calls TuristaDAO and serializes response to JSON', {label: 'endpoint-verification'})

// Collect results
const results = {
  dbConnection: dbConnectionResult,
  tableCheck: tableCheckResult,
  dataCheck: dataCheckResult,
  daoTest: daoTestResult,
  endpointTest: endpointTestResult
}

return { results }