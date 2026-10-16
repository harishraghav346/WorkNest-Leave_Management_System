package com.examly.springapp.controller;

import com.examly.springapp.model.OrganizationMigrationReport;
import com.examly.springapp.service.OrganizationMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/migration")
public class OrganizationMigrationController {

    @Autowired
    private OrganizationMigrationService migrationService;

    @Autowired
    private com.examly.springapp.service.TenantContextService tenantContextService;

    @GetMapping("/preview")
    public ResponseEntity<OrganizationMigrationReport> previewMigration(Authentication authentication) {
        tenantContextService.validatePlatformAdmin(authentication);
        return ResponseEntity.ok(migrationService.previewMigration());
    }

    @PostMapping("/execute")
    public ResponseEntity<OrganizationMigrationReport> executeMigration(Authentication authentication) {
        tenantContextService.validatePlatformAdmin(authentication);
        return ResponseEntity.ok(migrationService.executeMigration());
    }
}
