package com.mindex.challenge.service.impl;

import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.data.Compensation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.Assert.*;

import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeServiceImplTest {

    private String employeeUrl;
    private String employeeIdUrl;
    private String reportingStructureUrl;
    private String compensationUrl;
    private String compensationIdUrl;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setup() {
        employeeUrl = "http://localhost:" + port + "/employee";
        employeeIdUrl = "http://localhost:" + port + "/employee/{id}";
        reportingStructureUrl = "http://localhost:" + port + "/reportingStructure/{id}";
        compensationUrl = "http://localhost:" + port + "/compensation";
        compensationIdUrl = "http://localhost:" + port + "/compensation/{id}";
    }

    // EMPLOYEE CRUD TESTS
    
    @Test
    public void testCreateReadUpdate() {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create
        Employee createdEmployee =
                restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);

        // Read
        Employee readEmployee =
                restTemplate.getForEntity(employeeIdUrl, Employee.class, createdEmployee.getEmployeeId()).getBody();

        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);

        // Update
        readEmployee.setPosition("Development Manager");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee = restTemplate.exchange(
                employeeIdUrl,
                HttpMethod.PUT,
                new HttpEntity<>(readEmployee, headers),
                Employee.class,
                readEmployee.getEmployeeId()).getBody();

        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }

    @Test
    public void testReadInvalidEmployeeId() {
        try {
            restTemplate.getForEntity(employeeIdUrl, Employee.class, "invalid-employee-id-12345");
            fail("Expected exception for invalid employee ID");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Expected - should return 4xx or 5xx error
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
        }
    }

    // REPORTING STRUCTURE TESTS (TASK 1)
    @Test
    public void testReportingStructureForJohnLennon() {
        // John Lennon has 4 total reports (Paul, Ringo, Pete, George)
        String johnId = "16a596ae-edd3-4847-99fe-c4518e82c86f";
        ReportingStructure structure =
                restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, johnId).getBody();

        assertNotNull(structure);
        assertNotNull(structure.getEmployee());
        assertEquals(4, structure.getNumberOfReports());
        assertEquals("John", structure.getEmployee().getFirstName());
        assertEquals("Lennon", structure.getEmployee().getLastName());
    }

    @Test
    public void testReportingStructureForEmployeeWithNoReports() {
        // Pete Best has no direct or indirect reports
        String peteId = "62c1084e-6e34-4630-93fd-9153afb65309";
        ReportingStructure structure =
                restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, peteId).getBody();

        assertNotNull(structure);
        assertNotNull(structure.getEmployee());
        assertEquals("Pete", structure.getEmployee().getFirstName());
        assertEquals(0, structure.getNumberOfReports());
    }

    @Test
    public void testReportingStructureForMidLevelEmployee() {
        // Ringo Starr has 2 direct reports (Pete Best and George Harrison)
        String ringoId = "03aa1462-ffa9-4978-901b-7c001562cf6f";
        ReportingStructure structure =
                restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, ringoId).getBody();

        assertNotNull(structure);
        assertNotNull(structure.getEmployee());
        assertEquals("Ringo", structure.getEmployee().getFirstName());
        assertEquals(2, structure.getNumberOfReports());
    }

    @Test
    public void testReportingStructureWithInvalidEmployeeId() {
        try {
            restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, "invalid-1234");
            fail("Expected exception for invalid employee ID");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Expected - should return error
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
        }
    }

    @Test
    public void testReportingStructureWithNullEmployeeId() {
        try {
            restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, (Object) null);
            fail("Expected exception for null employee ID");
        } catch (Exception e) {
            // Expected - null ID should cause error
            assertTrue(true);
        }
    }

    @Test
    public void testReportingStructureWithEmptyEmployeeId() {
        try {
            restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, "");
            fail("Expected exception for empty employee ID");
        } catch (HttpClientErrorException e) {
            // Expected - empty ID should return 404 or similar
            assertTrue(e.getStatusCode().is4xxClientError());
        }
    }

    // COMPENSATION TESTS (TASK 2)
    
    @Test
    public void testCreateAndReadCompensation() {
        // Create a unique employee for this test to avoid conflicts
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("User");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();
        String empId = createdEmployee.getEmployeeId();

        // Create compensation
        Compensation testComp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(empId);
        testComp.setEmployee(emp);
        testComp.setSalary(150000);
        testComp.setEffectiveDate(LocalDate.of(2025, 10, 29));

        // POST - create
        Compensation created = restTemplate.postForEntity(compensationUrl, testComp, Compensation.class).getBody();
        assertNotNull(created);
        assertEquals(150000, created.getSalary(), 0.01);
        assertNotNull(created.getEmployee());

        // GET - read
        Compensation fetched = restTemplate.getForEntity(compensationIdUrl, Compensation.class, empId).getBody();
        assertNotNull(fetched);
        assertEquals(created.getSalary(), fetched.getSalary(), 0.01);
        assertEquals("Test", fetched.getEmployee().getFirstName());
    }

    @Test
    public void testDuplicateCompensationCreation() {
        // Create a unique employee for this test
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Duplicate");
        testEmployee.setLastName("Test");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();
        String empId = createdEmployee.getEmployeeId();

        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(empId);
        comp.setEmployee(emp);
        comp.setSalary(100000);
        comp.setEffectiveDate(LocalDate.now());

        // First insert - should succeed
        ResponseEntity<Compensation> first = restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
        assertEquals(HttpStatus.OK, first.getStatusCode());

        // Second insert - should fail
        try {
            restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
            fail("Expected exception for duplicate compensation");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Expected - duplicate should be rejected
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
        }
    }

    @Test
    public void testCreateCompensationWithInvalidEmployeeId() {
        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId("non-existent-employee-id-12345");
        comp.setEmployee(emp);
        comp.setSalary(100000);
        comp.setEffectiveDate(LocalDate.now());

        try {
            restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
            fail("Expected exception for invalid employee ID");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Expected - invalid employee should be rejected
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
        }
    }

    @Test
    public void testCreateCompensationWithNullEmployee() {
        Compensation comp = new Compensation();
        comp.setEmployee(null); // Null employee
        comp.setSalary(100000);
        comp.setEffectiveDate(LocalDate.now());

        try {
            restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
            fail("Expected exception for null employee");
        } catch (Exception e) {
            // Expected - null employee should cause error
            assertTrue(true);
        }
    }

    @Test
    public void testCreateCompensationWithNegativeSalary() {
        // Create a unique employee
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Negative");
        testEmployee.setLastName("Salary");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(createdEmployee.getEmployeeId());
        comp.setEmployee(emp);
        comp.setSalary(-50000); // Negative salary
        comp.setEffectiveDate(LocalDate.now());

        // Note: Currently no validation exists for negative salary
        // This test documents current behavior - should add validation in future
        ResponseEntity<Compensation> response = restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
        
        // We Should add validation to reject negative salaries...Just in case writing this as TODO
        // For now, just verify it doesn't crash
        assertNotNull(response);
    }

    @Test
    public void testCreateCompensationWithZeroSalary() {
        // Create a unique employee
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Zero");
        testEmployee.setLastName("Salary");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Intern");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(createdEmployee.getEmployeeId());
        comp.setEmployee(emp);
        comp.setSalary(0); // Zero salary
        comp.setEffectiveDate(LocalDate.now());

        // Zero salary might be valid for interns/volunteers
        ResponseEntity<Compensation> response = restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getSalary(), 0.01);
    }

    @Test
    public void testReadCompensationForNonExistentEmployee() {
        try {
            restTemplate.getForEntity(compensationIdUrl, Compensation.class, "non-existent-employee-id");
            fail("Expected exception for non-existent compensation");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Expected - should return error for missing compensation
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
        }
    }

    @Test
    public void testUpdateCompensation() {
        // Create a unique employee
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Update");
        testEmployee.setLastName("Test");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();
        String empId = createdEmployee.getEmployeeId();

        // Create initial compensation
        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(empId);
        comp.setEmployee(emp);
        comp.setSalary(100000);
        comp.setEffectiveDate(LocalDate.now());

        restTemplate.postForEntity(compensationUrl, comp, Compensation.class);

        // Update salary
        comp.setSalary(120000);
        comp.setEffectiveDate(LocalDate.now().plusMonths(6));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Compensation> updated = restTemplate.exchange(
            compensationUrl,
            HttpMethod.PUT,
            new HttpEntity<>(comp, headers),
            Compensation.class
        );

        assertNotNull(updated.getBody());
        assertEquals(120000, updated.getBody().getSalary(), 0.01);
    }

    @Test
    public void testCreateCompensationWithFutureEffectiveDate() {
        // Create a unique employee
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("Future");
        testEmployee.setLastName("Date");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Senior Developer");
        
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        Compensation comp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId(createdEmployee.getEmployeeId());
        comp.setEmployee(emp);
        comp.setSalary(150000);
        comp.setEffectiveDate(LocalDate.now().plusMonths(3)); // Future date

        // Future dates might be valid for scheduled raises
        ResponseEntity<Compensation> response = restTemplate.postForEntity(compensationUrl, comp, Compensation.class);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getEffectiveDate().isAfter(LocalDate.now()));
    }

    //HELPER METHODS
    
    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }
}
