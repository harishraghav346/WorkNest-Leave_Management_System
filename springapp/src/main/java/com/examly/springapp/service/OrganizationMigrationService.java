package com.examly.springapp.service;

import com.examly.springapp.model.OrganizationMigrationReport;

public interface OrganizationMigrationService {
    OrganizationMigrationReport previewMigration();
    OrganizationMigrationReport executeMigration();
}
