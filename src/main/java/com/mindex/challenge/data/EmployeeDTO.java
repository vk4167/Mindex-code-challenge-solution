package com.mindex.challenge.data;
/*
  Data Transfer Object (DTO) representing an Employee and their direct reports.
  This class is designed to be used in API responses to avoid circular references
  and recursive loading issues that can occur when serializing complex
  hierarchical structures (like the ReportingStructure).
 
  Each EmployeeDTO instance may contain a list of nested EmployeeDTOs
  representing their direct reports, forming a lightweight tree structure.
 
  Example structure:
      John Lennon
        --Paul McCartney
        --Ringo Starr
              --Pete Best
              -- George Harrison
 */
import java.util.List;

public class EmployeeDTO {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String position;
    private String department;
    private List<EmployeeDTO> directReports;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<EmployeeDTO> getDirectReports() {
        return directReports;
    }

    public void setDirectReports(List<EmployeeDTO> directReports) {
        this.directReports = directReports;
    }
}
