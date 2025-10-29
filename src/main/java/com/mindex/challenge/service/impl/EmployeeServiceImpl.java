
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

    // Advanced Implementation of getReportingStructure
    @Override
    @Cacheable("reportingStructure") // Optional: enables Spring caching
    public ReportingStructure getReportingStructure(String employeeId) {
        log.info("Fetching ReportingStructure for employeeId: {}", employeeId);

        // 1. Preload all employees at once to avoid N+1 DB queries
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));

        // 2. Validate ID
        Employee employee = employeeMap.get(employeeId);
        if (employee == null) {
            throw new RuntimeException("Invalid employeeId: " + employeeId);
        }

        // 3. Cycle protection
        Set<String> visited = new HashSet<>();

        // 4. Count reports recursively
        int numberOfReports = countReports(employee, employeeMap, visited);

        // 5. Convert to DTO for clean API output
        EmployeeDTO dto = convertToDTO(employee, employeeMap, new HashSet<>());

        log.info("ReportingStructure generated for {} with {} reports",
                employee.getFirstName(), numberOfReports);

        return new ReportingStructure(dto, numberOfReports);
    }

    // Recursive report counting
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

            // Prevent cycles
            if (visited.contains(id)) {
                log.warn("Cycle detected for employeeId: {}", id);
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

    // Recursive conversion to EmployeeDTO for clean API response
    private EmployeeDTO convertToDTO(Employee employee,
                                     Map<String, Employee> employeeMap,
                                     Set<String> visited) {

        if (employee == null) return null;

        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setDepartment(employee.getDepartment());
        dto.setPosition(employee.getPosition());

        if (employee.getDirectReports() != null) {
            List<EmployeeDTO> reports = employee.getDirectReports().stream()
                    .filter(r -> r.getEmployeeId() != null)
                    .map(r -> {
                        Employee full = employeeMap.get(r.getEmployeeId());
                        if (full != null && !visited.contains(full.getEmployeeId())) {
                            visited.add(full.getEmployeeId());
                            return convertToDTO(full, employeeMap, visited);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setDirectReports(reports);
        }

        return dto;
    }

    // (Other service methods like create/read/update stay unchanged)
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
