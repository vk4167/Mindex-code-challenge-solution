package com.mindex.challenge.data;
/*ReportingStructure represents a hierarchical view of an employee along with the total number of direct and indirect reports under them. This class is used as the response model for the
 /reportingStructure/{employeeId} REST endpoint.*/
public class ReportingStructure {
    private EmployeeDTO employee;    // Fully expanded employee object (using DTO to avoid recursion depth)
    private int numberOfReports;

    public ReportingStructure() {
    }

    public ReportingStructure(EmployeeDTO employee, int numberOfReports) {
        this.employee = employee;
        this.numberOfReports = numberOfReports;
    }

    public EmployeeDTO getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeDTO employee) {
        this.employee = employee;
    }

    public int getNumberOfReports() {
        return numberOfReports;
    }

    public void setNumberOfReports(int numberOfReports) {
        this.numberOfReports = numberOfReports;
    }
}


