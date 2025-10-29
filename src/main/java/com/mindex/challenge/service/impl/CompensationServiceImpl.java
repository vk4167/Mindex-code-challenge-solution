
package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.service.CompensationService;
import com.mindex.challenge.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompensationServiceImpl implements CompensationService {

    private static final Logger LOG = LoggerFactory.getLogger(CompensationServiceImpl.class);

    @Autowired
    private CompensationRepository compensationRepository;

    @Autowired
    private EmployeeService employeeService;

    @Override
    public Compensation create(Compensation compensation) {
        LOG.debug("Creating compensation [{}]", compensation);
        Employee emp = employeeService.read(compensation.getEmployee().getEmployeeId());
        compensation.setEmployee(emp);
        return compensationRepository.save(compensation);
    }

    @Override
    public Compensation read(String employeeId) {
        LOG.debug("Reading compensation for [{}]", employeeId);
        Compensation comp = compensationRepository.findByEmployee_EmployeeId(employeeId);
        if (comp == null) {
            throw new RuntimeException("No compensation found for employeeId: " + employeeId);
        }
        return comp;
    }

    @Override
    public Compensation update(Compensation compensation) {
        LOG.debug("Updating compensation [{}]", compensation);
        Employee emp = employeeService.read(compensation.getEmployee().getEmployeeId());
        compensation.setEmployee(emp);
        return compensationRepository.save(compensation);
    }
}
