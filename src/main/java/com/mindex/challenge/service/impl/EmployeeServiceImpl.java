package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.EmployeeDTO;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    // Reporting Structure
    @Override
    @Cacheable("reportingStructure")
    public ReportingStructure getReportingStructure(String employeeId) {
        log.info("Fetching ReportingStructure for employeeId: {}", employeeId);

        // Preload all employees at once to avoid multiple DB lookups
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));

        // Validate input employeeId
        Employee employee = employeeMap.get(employeeId);
        if (employee == null) {
            throw new RuntimeException("Invalid employeeId: " + employeeId);
        }

        //Initialize a visited set for cycle detection
        Set<String> visited = new HashSet<>();

        // Count all distinct reports recursively
        int numberOfReports = countReports(employee, employeeMap, visited);

        // Convert employee hierarchy to a DTO (fully expanded)
        visited.clear(); // reset visited to avoid skipping nodes in conversion
        EmployeeDTO dto = convertToDTO(employee, employeeMap, visited);

        log.info("ReportingStructure generated for {} with {} reports",
                employee.getFirstName(), numberOfReports);

        return new ReportingStructure(dto, numberOfReports);
    }

    // Recursively count all distinct reports under an employee
    private int countReports(Employee employee,
                             Map<String, Employee> employeeMap,
                             Set<String> visited) {

        if (employee == null || employee.getDirectReports() == null) {
            return 0;
        }

        int total = 0;

        for (Employee report : employee.getDirectReports()) {
            String id = report.getEmployeeId();
            if (id == null) continue;

            // Skip self-reference and prevent cycles
            if (employee.getEmployeeId().equals(id) || visited.contains(id)) {
                continue;
            }

            visited.add(id);
            Employee fullReport = employeeMap.get(id);
            if (fullReport != null) {
                total += 1 + countReports(fullReport, employeeMap, visited);
            }
        }

        return total;
    }

    // Recursively build full EmployeeDTO hierarchy with all report details
    private EmployeeDTO convertToDTO(Employee employee,
                                     Map<String, Employee> employeeMap,
                                     Set<String> visited) {

        if (employee == null) return null;
        if (visited.contains(employee.getEmployeeId())) {
            log.warn("Cycle detected during DTO conversion for employeeId: {}", employee.getEmployeeId());
            return null;
        }

        visited.add(employee.getEmployeeId());

        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setDepartment(employee.getDepartment());
        dto.setPosition(employee.getPosition());

        if (employee.getDirectReports() != null && !employee.getDirectReports().isEmpty()) {
            List<EmployeeDTO> reports = new ArrayList<>();
            for (Employee dr : employee.getDirectReports()) {
                String id = dr.getEmployeeId();
                if (id == null || id.equals(employee.getEmployeeId())) continue;

                Employee full = employeeMap.get(id);
                EmployeeDTO childDTO = convertToDTO(full, employeeMap, visited);
                if (childDTO != null) reports.add(childDTO);
            }
            dto.setDirectReports(reports);
        } else {
            dto.setDirectReports(Collections.emptyList());
        }

        return dto;
    }

    // CRUD Operations
    @Override
    public Employee create(Employee employee) {
        log.debug("Creating employee [{}]", employee);
        employee.setEmployeeId(UUID.randomUUID().toString());
        employeeRepository.insert(employee);
        return employee;
    }

    @Override
    public Employee read(String id) {
        log.debug("Reading employee with id [{}]", id);
        Employee employee = employeeRepository.findByEmployeeId(id);

        if (employee == null) {
            throw new RuntimeException("Invalid employeeId: " + id);
        }

        return employee;
    }

    @Override
    public Employee update(Employee employee) {
        log.debug("Updating employee [{}]", employee);
        Employee existingEmployee = employeeRepository.findByEmployeeId(employee.getEmployeeId());

        if (existingEmployee == null) {
            throw new RuntimeException("Invalid employeeId: " + employee.getEmployeeId());
        }

        employeeRepository.save(employee);
        return employee;
    }
}
