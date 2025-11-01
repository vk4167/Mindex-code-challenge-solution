# Coding Challenge
## What's Provided
A simple [Spring Boot](https://projects.spring.io/spring-boot/) web application has been created and bootstrapped with data. The application contains 
information about all employees at a company. On application start-up, an in-memory Mongo database is bootstrapped with 
a serialized snapshot of the database. While the application runs, the data may be accessed and mutated in the database 
without impacting the snapshot.

### How to Run
The application may be executed by running `gradlew bootRun`.

*Spring Boot 3 requires Java 17 or higher. This project targets Java 17. If you want to change the targeted Java 
version, you can modify the `sourceCompatibility` variable in the `build.gradle` file.*

### How to Use
The following endpoints are available to use:
```
* CREATE
    * HTTP Method: POST 
    * URL: localhost:8080/employee
    * PAYLOAD: Employee
    * RESPONSE: Employee
* READ
    * HTTP Method: GET 
    * URL: localhost:8080/employee/{id}
    * RESPONSE: Employee
* UPDATE
    * HTTP Method: PUT 
    * URL: localhost:8080/employee/{id}
    * PAYLOAD: Employee
    * RESPONSE: Employee
```

The Employee has a JSON schema of:
```json
{
  "title": "Employee",
  "type": "object",
  "properties": {
    "employeeId": {
      "type": "string"
    },
    "firstName": {
      "type": "string"
    },
    "lastName": {
      "type": "string"
    },
    "position": {
      "type": "string"
    },
    "department": {
      "type": "string"
    },
    "directReports": {
      "type": "array",
      "items": {
        "anyOf": [
          {
            "type": "string"
          },
          {
            "type": "object"
          }
        ]
      }
    }
  }
}
```
For all endpoints that require an `id` in the URL, this is the `employeeId` field.

## What to Implement
This coding challenge was designed to allow for flexibility in the approaches you take. While the requirements are 
minimal, we encourage you to explore various design and implementation strategies to create functional features. Keep in
mind that there are multiple valid ways to solve these tasks. What's important is your ability to justify and articulate
the reasoning behind your design choices. We value your thought process and decision-making skills. Also, If you 
identify any areas in the existing codebase that you believe can be enhanced, feel free to make those improvements.

### Task 1
Create a new type called `ReportingStructure` that has two fields: `employee` and `numberOfReports`.

The field `numberOfReports` should equal the total number of reports under a given employee. The number of reports is 
determined by the number of `directReports` for an employee, all of their distinct reports, and so on. For example,
given the following employee structure:
```
                   John Lennon
                 /             \
         Paul McCartney     Ringo Starr
                            /         \
                       Pete Best    George Harrison
```
The `numberOfReports` for employee John Lennon (`employeeId`: 16a596ae-edd3-4847-99fe-c4518e82c86f) would be equal to 4.

This new type should have a new REST endpoint created for it. This new endpoint should accept an `employeeId` and return
the fully filled out `ReportingStructure` for the specified `employeeId`. The values should be computed on the fly and 
will not be persisted.

### SOLUTION: HOW I SOLVED TASK 1 : Reporting Structure

To compute an employee’s total number of direct and indirect reports, I implemented a `recursive Depth-First Search (DFS)` solution optimized to minimize database calls and handle cyclic references safely.

`Recursive DFS Traversal`
I used a depth-first approach where each employee’s directReports are visited recursively. This naturally captures all nested reporting relationships in the hierarchy.

`Pre-loaded Employee Map`
Instead of querying MongoDB on each recursive step, I loaded all employees once into a `HashMap<employeeId, Employee>` for constant-time lookups. This avoids repeated I/O and ensures overall linear performance.

`Cycle Detection and Safety`
A HashSet of visited employee IDs prevents infinite recursion in case of malformed or circular references within the organization tree.

`DTO Conversion for Clean API Output`
To avoid returning incomplete or null directReports, I introduced an EmployeeDTO object that represents a fully materialized yet minimal hierarchy for the API response.

`Complexity and Performance`

Time Complexity: O(N) — each employee is processed once.

Space Complexity: O(N) — from recursion stack, hash map, and visited set.

For deeper hierarchies, this design can be easily refactored into an iterative BFS traversal to remove recursion stack overhead while maintaining O(N) time.

`Result`
The `/reportingStructure/{employeeId}` endpoint returns the target employee along with the computed numberOfReports, including both direct and indirect subordinates.
For example, `John Lennon correctly yields 4 reports in total`.

### Task 2
Create a new type called `Compensation` to represent an employee's compensation details. A `Compensation` should have at 
minimum these two fields: `salary` and `effectiveDate`. Each `Compensation` should be associated with a specific 
`Employee`. How that association is implemented is up to you.

Create two new REST endpoints to create and read `Compensation` information from the database. These endpoints should 
persist and fetch `Compensation` data for a specific `Employee` using the persistence layer.

### SOLUTION: HOW I SOLVED TASK 2 — Compensation Service

To fulfill the requirement of persisting and retrieving compensation details for each employee, I implemented a clean, service-oriented solution following Spring Boot and REST best practices.

`Data Model Design`
I created a new domain class Compensation containing three fields:

`employee` – reference to an existing Employee object,

`salary` – the employee’s current compensation value, and

`effectiveDate` – the date the salary became effective.
This structure ensures each compensation record is directly tied to a specific employee.

`Persistence Layer`
I defined a CompensationRepository interface extending MongoRepository, which provides CRUD operations for Compensation objects and a custom method findByEmployee_EmployeeId(String employeeId) for quick lookups.

`Service Layer Implementation`
The CompensationServiceImpl handles the business logic:

`Create:` Validates the employee exists by calling employeeService.read() before saving the compensation.

`Read:` Retrieves the compensation entry by employee ID and throws a meaningful exception if none is found.

`Update:` Allows modifications to salary or effective date while re-validating employee linkage.
This design centralizes validation, preventing stale or orphaned compensation data.

`REST Controller Layer`
The CompensationController exposes three endpoints:

`POST /compensation` – to create new records,

`GET /compensation/{id}` – to fetch details by employee ID, and

`PUT /compensation` – to update existing entries.
Each endpoint logs activity using SLF4J for observability and debugging.

`Complexity and Performance`

Time Complexity: O(1) for both read and write operations because each employee’s compensation is indexed by ID.

Space Complexity: O(N) relative to the number of compensation records stored in MongoDB.
Since compensation data changes infrequently, this structure is both performant and cost-efficient for persistence.

`Testing and Validation`
I verified the implementation using Invoke-RestMethod (PowerShell) and curl calls to confirm that both endpoints correctly persisted and retrieved records, including salary and effective date, for employeeId = 16a596ae-edd3-4847-99fe-c4518e82c86f.

`Design Rationale`
By decoupling compensation logic into its own service, the architecture remains modular and easy to extend—for example, to support historical salary tracking, currency conversions, or effective-date versioning.
The design maintains clear separation of concerns between data, service, and presentation layers, adhering to standard enterprise Spring Boot architecture.

### Testing & Validation

I verified all endpoints through both unit tests (JUnit + Spring Boot Test) and manual integration testing using curl and PowerShell’s Invoke-RestMethod.

`Task 1 — Reporting Structure`

Added multiple automated test cases in EmployeeServiceImplTest.java covering:

Employee with multiple indirect reports (John Lennon → 4 total reports).

Employee with no reports (Pete Best → 0 reports).

Invalid employee IDs to confirm proper exception handling.

Verified that the /reportingStructure/{id} endpoint returns a fully materialized employee structure with accurate numberOfReports and no null fields.

Confirmed performance remains linear (O(N)) with constant-time lookups via in-memory HashMap.

`Task 2 — Compensation`

Verified creation and retrieval of compensation details using both automated and manual tests:

`POST /compensation` to create a record tied to an existing employee.

`GET /compensation/{id}` to fetch the persisted data by employee ID.

Confirmed that compensation records persist correctly in the MongoDB in-memory instance.

Manually tested with:

Invoke-RestMethod -Uri "http://localhost:8080/compensation" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{ "employee": { "employeeId": "16a596ae-edd3-4847-99fe-c4518e82c86f" }, "salary": 150000, "effectiveDate": "2025-10-29" }'


and validated the response structure and persisted state.
## Changes/Updates Made

### Bug Fixes & Improvements

#### 1. **Fixed Cycle Detection in Reporting Structure**
- **Issue:** The visited set wasn't being shared between counting and DTO conversion phases, which could cause infinite loops with circular references
- **Fix:** Now the visited set is cleared and reused for both phases, ensuring consistent cycle detection throughout
- **Impact:** Prevents potential infinite recursion and stack overflow errors

#### 2. **Prevented Duplicate Compensation Records**
- **Issue:** Multiple compensation records could be created for the same employee, causing data inconsistency
- **Fix:** Added dual protection:
  - Application-level check in `CompensationServiceImpl.create()` 
  - Database-level unique compound index on `employee.employeeId`
- **Impact:** Ensures data integrity and prevents conflicting salary information

#### 3. **Added MongoDB Document ID to Compensation**
- **Issue:** Compensation entity lacked proper MongoDB `@Id` field for document tracking
- **Fix:** Added `@Id private String id;` field and `@Document` annotation
- **Impact:** Enables proper CRUD operations and document identification in MongoDB

#### 4. **Fixed Controller Domain Object Mutation**
- **Issue:** The Employee read endpoint was mutating domain objects by setting `directReports = null`
- **Fix:** Changed endpoint to return `EmployeeDTO` instead, with proper field mapping
- **Impact:** Prevents cache corruption and follows proper DTO pattern for API responses

#### 5. **Added Self-Reference Protection**
- **Issue:** Employees could potentially reference themselves in directReports
- **Fix:** Added explicit self-reference checks in both `countReports()` and `convertToDTO()` methods
- **Impact:** Handles edge case gracefully without counting self-references

#### 6. **Enhanced Test Coverage**
- **Added:** Test for duplicate compensation creation
- **Coverage:** Now tests both happy paths and error scenarios
- **Impact:** Better regression protection and validation of business rules

### Code Quality Improvements

- **Cleaner separation of concerns** with DTO pattern in controller layer
- **Improved logging** with descriptive messages for debugging
- **Better error handling** with validation before database operations
- **Consistent cycle detection** across all recursive operations
- **Database constraints** to enforce business rules at multiple layers

### Performance Optimizations

- **Pre-loading employee map** to avoid N+1 query problem (O(1) lookups)
- **Efficient cycle detection** with HashSet for O(1) membership checks
- **Linear time complexity** O(N) for reporting structure computation

## Future Enhancements
- Add caching for precomputed reporting structures to reduce repeated traversal time to O(1).
- Maintain historical compensation records with effective-date versioning.
- Implement authentication and role-based access for employee data APIs.
- Add OpenAPI (Swagger) documentation for endpoint discovery.
- Add input validation annotations (@NotNull, @Positive) on entity fields.
- Implement custom exception classes for better error handling.
- Add proper HTTP status codes (201 for creation, 404 for not found).

## Delivery
Please upload your results to a publicly accessible Git repo. Free ones are provided by GitHub and Bitbucket.

