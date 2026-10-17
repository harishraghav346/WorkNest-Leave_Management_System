package com.examly.springapp.controller;

import com.examly.springapp.model.Department;
import com.examly.springapp.model.DepartmentRequest;
import com.examly.springapp.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody DepartmentRequest request, Authentication authentication) {
        Department created = departmentService.createDepartment(request, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Department>> getDepartments(Authentication authentication) {
        List<Department> departments = departmentService.getDepartments(authentication);
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id, Authentication authentication) {
        Department department = departmentService.getDepartmentById(id, authentication);
        return ResponseEntity.ok(department);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(
            @PathVariable Long id,
            @RequestBody DepartmentRequest request,
            Authentication authentication) {
        Department updated = departmentService.updateDepartment(id, request, authentication);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/assign-employee")
    public ResponseEntity<?> assignEmployee(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String employeeId = body != null ? body.get("employeeId") : null;
        if (employeeId == null || employeeId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "employeeId is required"));
        }
        departmentService.assignEmployeeToDepartment(id, employeeId, authentication);
        return ResponseEntity.ok(Map.of("message", "Employee '" + employeeId + "' assigned to department #" + id + " successfully"));
    }
}
