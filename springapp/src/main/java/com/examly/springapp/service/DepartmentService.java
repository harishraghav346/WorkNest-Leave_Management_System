package com.examly.springapp.service;

import com.examly.springapp.model.Department;
import com.examly.springapp.model.DepartmentRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface DepartmentService {
    Department createDepartment(DepartmentRequest request, Authentication authentication);
    List<Department> getDepartments(Authentication authentication);
    Department getDepartmentById(Long id, Authentication authentication);
    Department updateDepartment(Long id, DepartmentRequest request, Authentication authentication);
    void assignEmployeeToDepartment(Long departmentId, String employeeId, Authentication authentication);
}
