package com.example.hello.service;

import com.example.hello.entity.Employee;
import com.example.hello.entity.EmployeeProject;
import com.example.hello.mapper.EmployeeMapper;
import com.example.hello.mapper.EmployeeProjectMapper;
import de.mkammerer.argon2.Argon2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    
    @Autowired
    private EmployeeProjectMapper employeeProjectMapper;
    
    @Autowired
    private Argon2 argon2;

    public Employee login(String nameOrPhone, String password) {
        // 先根据用户名查询用户
        Employee employee = employeeMapper.findByName(nameOrPhone);
        // 如果没找到，再根据手机号查询
        if (employee == null) {
            employee = employeeMapper.findByPhone(nameOrPhone);
        }
        if (employee == null || employee.getStatus() != 1) {
            return null;
        }
        
        // 使用 Argon2 验证密码
        if (!argon2.verify(employee.getPassword(), password.toCharArray())) {
            return null;
        }
        
        return employee;
    }

    public Employee findById(Long id) {
        return employeeMapper.findById(id);
    }

    public List<Employee> findAll() {
        List<Employee> employees = employeeMapper.findAll();
        // 为每个员工填充隶属的项目名称
        for (Employee emp : employees) {
            List<String> projectNames = employeeMapper.findProjectNamesByEmployeeId(emp.getId());
            if (projectNames != null && !projectNames.isEmpty()) {
                emp.setProjectNames(String.join(", ", projectNames));
            }
        }
        return employees;
    }
    
    public List<Employee> findByProjectId(Long projectId) {
        List<Employee> employees = employeeMapper.findByProjectId(projectId);
        // 为每个员工填充隶属的项目名称
        for (Employee emp : employees) {
            List<String> projectNames = employeeMapper.findProjectNamesByEmployeeId(emp.getId());
            if (projectNames != null && !projectNames.isEmpty()) {
                emp.setProjectNames(String.join(", ", projectNames));
            }
        }
        return employees;
    }

    public List<Employee> findByRoleCode(String roleCode) {
        return employeeMapper.findByRoleCode(roleCode);
    }

    public List<Employee> findFinanceList() {
        return employeeMapper.findFinanceList();
    }

    public void encodePassword(Employee employee) {
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            employee.setPassword(argon2.hash(2, 65536, 1, employee.getPassword().toCharArray()));
        }
    }

    public List<Long> findProjectIdsByEmployeeId(Long employeeId) {
        return employeeMapper.findProjectIdsByEmployeeId(employeeId);
    }

    public boolean save(Employee employee) {
        if (employee.getId() == null) {
            return employeeMapper.insert(employee) > 0;
        } else {
            return employeeMapper.update(employee) > 0;
        }
    }
    
    @Transactional
    public boolean save(Employee employee, List<Long> projectIds) {
        boolean success;
        if (employee.getId() == null) {
            success = employeeMapper.insert(employee) > 0;
        } else {
            success = employeeMapper.update(employee) > 0;
        }
        
        // 处理项目关联：先删除旧的，再插入新的
        if (success) {
            Long employeeId = employee.getId();
            // 删除该员工所有旧的项目关联
            employeeProjectMapper.deleteByEmployeeId(employeeId);
            // 插入新选择的项目关联
            if (projectIds != null && !projectIds.isEmpty()) {
                for (Long projectId : projectIds) {
                    EmployeeProject ep = new EmployeeProject();
                    ep.setEmployeeId(employeeId);
                    ep.setProjectId(projectId);
                    employeeProjectMapper.insert(ep);
                }
            }
        }
        
        return success;
    }

    public boolean deleteById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }
}
