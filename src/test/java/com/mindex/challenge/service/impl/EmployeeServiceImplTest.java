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

import static org.junit.Assert.*;

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

    //ReportingStructure Tests (Task 1)
    @Test
    public void testReportingStructureForJohnLennon() {
        String johnId = "16a596ae-edd3-4847-99fe-c4518e82c86f";    // John Lennon has 4 total reports (Paul, Ringo, Pete, George)
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
        String peteId = "62c1084e-6e34-4630-93fd-9153afb65309"; // Pete Best
        ReportingStructure structure =
                restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, peteId).getBody();

        assertNotNull(structure);
        assertEquals("Pete", structure.getEmployee().getFirstName());
        assertEquals(0, structure.getNumberOfReports());
    }

    @Test(expected = RuntimeException.class)
    public void testReportingStructureWithInvalidEmployeeId() {
        // Invalid employee ID should trigger RuntimeException
        restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, "invalid-1234");
    }

    //Compensation API Tests (Task 2)
    @Test
    public void testCreateAndReadCompensation() {
        // 1️.Create payload
        Compensation testComp = new Compensation();
        Employee emp = new Employee();
        emp.setEmployeeId("16a596ae-edd3-4847-99fe-c4518e82c86f"); // John Lennon
        testComp.setEmployee(emp);
        testComp.setSalary(150000);
        testComp.setEffectiveDate(java.time.LocalDate.of(2025, 10, 29));

        // 2.POST - create
        Compensation created = restTemplate.postForEntity(compensationUrl, testComp, Compensation.class).getBody();
        assertNotNull(created);
        assertEquals(150000, created.getSalary(), 0.01);

        // 3️.GET - read
        Compensation fetched = restTemplate.getForEntity(compensationIdUrl, Compensation.class, emp.getEmployeeId()).getBody();
        assertNotNull(fetched);
        assertEquals(created.getSalary(), fetched.getSalary(), 0.01);
        assertEquals("John", fetched.getEmployee().getFirstName());
        assertEquals("Development Manager", fetched.getEmployee().getPosition());
    }

    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }
}
