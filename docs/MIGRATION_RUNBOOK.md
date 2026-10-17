# WorkNest Migration, Backup, and Disaster Recovery Runbook

**Version:** 1.0 (Phase D4.2 Remediation)  
**Applies To:** WorkNest Backend (`springapp`), H2 Database (`leavetracker.mv.db`), Multi-Tenant Architecture  
**Target Audience:** Site Reliability Engineers (SRE), Database Administrators (DBA), DevOps Engineers

---

## ⚠️ MANDATORY SAFETY DIRECTIVES

> [!CAUTION]
> **CRITICAL PRODUCTION SAFETY RULE:**
> 1. **NEVER** run `/admin/migration/execute` directly against the live production database without completing a 100% successful staging rehearsal on a restored backup copy.
> 2. **NEVER** copy or snapshot a live, running H2 MVStore file (`leavetracker.mv.db`) without either shutting down the application or issuing a consistent H2 SQL `BACKUP TO` command. Raw OS-level file copies of an open database can result in corrupted page maps, torn writes, or inconsistent transaction states.
> 3. Verify all file checksums (SHA-256) before and after copying.
> 4. Automated regression tests must NEVER connect to `leavetracker.mv.db`; tests must run strictly against in-memory databases (`jdbc:h2:mem:...`).

---

## 1. Maintenance Window & Application Shutdown

### 1.1 Scheduling & Pre-Flight Communication
- **Maintenance Window:** Recommended during low-traffic hours (e.g., Sunday 02:00 – 04:00 UTC).
- **Duration:** Allocate a 2-hour window (30 min backup & staging rehearsal, 30 min migration execution, 30 min verification, 30 min rollback buffer).
- **Communication:** Post maintenance banners on the public portal and mobile interfaces 24 hours prior.

### 1.2 Graceful Application Drain & Shutdown
Before backing up the file database, all active connections and write operations must be ceased.

1. **Enable Maintenance Mode / Drain Traffic:**
   Route incoming traffic to a static maintenance page at the reverse proxy (Nginx / Cloudflare):
   ```nginx
   # Return HTTP 503 Service Unavailable with Retry-After header
   return 503;
   ```

2. **Drain In-Flight Requests:**
   Allow current HTTP requests to complete (wait 30–60 seconds).

3. **Stop Application Service Gracefully:**
   ```bash
   # Systemd (Linux)
   sudo systemctl stop worknest-backend.service

   # Or Windows Service / PowerShell process
   Stop-Process -Name "java" -Force -PassThru
   ```

4. **Verify Process Termination & Lock Release:**
   Ensure no lingering Java process holds an open file descriptor on the database file:
   ```powershell
   # Windows PowerShell
   Get-Process -Name java -ErrorAction SilentlyContinue
   ```
   ```bash
   # Linux
   lsof /path/to/springapp/data/leavetracker.mv.db
   ```
   *The command should return empty, confirming all OS file locks are released.*

---

## 2. Taking a Consistent H2 Backup

### 2.1 Why Raw Copies of an Active H2 File are Unsafe
H2 version 2.x uses the **MVStore** (Multi-Version Store) storage engine:
- In-flight writes are buffered in memory before being flushed in chunks.
- Background compaction and commit threads continuously alter pointers.
- An OS-level file copy (`cp`, `Copy-Item`) performed while H2 is open captures a "torn" file state where the catalog, chunk metadata, and data pages are out of sync.
- Opening such a damaged copy often triggers `DbException: File corrupted while reading record` or silent data loss.

### 2.2 Backup Method A: Cold File Copy (Recommended Post-Shutdown)
Once the application is completely stopped:

1. **Record the SHA-256 Checksum and Metadata:**
   ```powershell
   # Windows PowerShell
   Get-FileHash -Path ".\springapp\data\leavetracker.mv.db" -Algorithm SHA256 | Format-List
   Get-Item ".\springapp\data\leavetracker.mv.db" | Select-Object Name, Length, LastWriteTime
   ```

2. **Create Timestamped Backup Directory:**
   ```powershell
   $TIMESTAMP = (Get-Date).ToString("yyyyMMdd_HHmmss")
   $BACKUP_DIR = ".\backups\$TIMESTAMP"
   New-Item -ItemType Directory -Force -Path $BACKUP_DIR
   ```

3. **Perform Binary Copy:**
   ```powershell
   Copy-Item ".\springapp\data\leavetracker.mv.db" -Destination "$BACKUP_DIR\leavetracker.mv.db"
   ```

4. **Verify Integrity of the Backup Copy:**
   ```powershell
   $ORIG_HASH = (Get-FileHash -Path ".\springapp\data\leavetracker.mv.db" -Algorithm SHA256).Hash
   $BACK_HASH = (Get-FileHash -Path "$BACKUP_DIR\leavetracker.mv.db" -Algorithm SHA256).Hash

   if ($ORIG_HASH -eq $BACK_HASH) {
       Write-Host "✅ Checksum verified: $BACK_HASH" -ForegroundColor Green
   } else {
       Write-Error "❌ CHECKSUM MISMATCH! Backup is corrupted."
   }
   ```

### 2.3 Backup Method B: Online H2 Hot Backup (Alternative)
If an online backup is required without stopping the application, run an administrative query via the H2 Console or a secured management connection:
```sql
BACKUP TO 'backups/backup_online_20261017.zip';
```
This executes an atomic, read-locked snapshot within the H2 engine, guaranteeing transaction consistency without requiring an OS restart.

---

## 3. Restoring Backup to Staging Environment

To guarantee that a future migration will succeed without unexpected schema violations or foreign key failures, the backup MUST be restored and rehearsed on an isolated staging instance.

### 3.1 Setup Staging Sandbox
Create an isolated staging workspace completely separate from production:
```powershell
$STAGING_DIR = ".\staging_env"
New-Item -ItemType Directory -Force -Path "$STAGING_DIR\data"

# Copy verified backup into staging
Copy-Item "$BACKUP_DIR\leavetracker.mv.db" -Destination "$STAGING_DIR\data\leavetracker.mv.db"
```

### 3.2 Configure Isolated Staging Application
Create an isolated configuration file `application-staging.properties` or set environment variables:
```properties
# Staging Datasource pointing to restored copy
spring.datasource.url=jdbc:h2:file:./staging_env/data/leavetracker;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Isolated Port to prevent collision
server.port=8089

# Safe Staging Settings (Email delivery disabled / mock)
spring.mail.host=
app.security.dev-token-delivery=false
app.rate-limiting.trusted-proxies=127.0.0.1
```

### 3.3 Boot Staging Instance
```bash
java -jar -Dspring.profiles.active=staging springapp-0.0.1-SNAPSHOT.jar
```

---

## 4. Staging Data Validation Protocol

Before attempting any migration actions on staging, perform exhaustive verification of the restored data:

| Entity | Validation Checks | Verification Method |
| :--- | :--- | :--- |
| **Organizations** | Count, status (`ACTIVE`, `PENDING_VERIFICATION`), slug uniqueness | SQL / Admin API |
| **Users** | User count, password hash integrity, role assignments (`ROLE_ADMIN`, `ROLE_EMPLOYEE`) | `/auth/login` test |
| **Memberships** | Tenant mapping, default tenant indicators, status | Cross-tenant query |
| **Departments** | Department names, manager assignments, tenant isolation | `GET /departments` |
| **Leave Records** | Total count, pending/approved/rejected statuses, employee associations | `GET /leaves` |
| **Audit Events** | Historical audit trail, event types, timestamps | Audit log query |
| **Notifications**| Unread counts, recipient mappings | Notification repository query |

---

## 5. Running Migration Preview & Reconciling Unresolved Records

### 5.1 Execute Migration Preview (Read-Only)
On the **staging instance**, invoke the migration preview endpoint:
```http
GET /admin/migration/preview
Authorization: Bearer <STAGING_ADMIN_JWT_TOKEN>
```

### 5.2 Review Migration Analysis Output
The preview response returns:
```json
{
  "totalLegacyUsers": 12,
  "alreadyMigratedUsers": 10,
  "unassignedUsers": 2,
  "unresolvedRecords": [
    {
      "entity": "Employee",
      "id": 104,
      "email": "orphan@unknown.org",
      "issue": "NO_MATCHING_ORGANIZATION_SLUG"
    }
  ],
  "canExecute": false
}
```

### 5.3 Reconciliation Playbook
If `unresolvedRecords` is not empty:
1. **Unassigned Users / Orphan Records:**
   - Map orphan users to the default platform organization (`legacy-org`) or create the designated destination organization.
2. **Duplicate Email / Username Collisions:**
   - Audit colliding identities. Standardize on the canonical company email address.
3. **Missing Department Links:**
   - Assign unlinked employees to a fallback "General" or "Unassigned" department within their organization.
4. **Re-run Preview:**
   - Continue resolving items until `canExecute` returns `true` with 0 unresolved errors.

---

## 6. Staging Rehearsal Execution

Once preview shows zero unresolved records:
1. Execute `/admin/migration/execute` on the **staging instance only**.
2. Verify:
   - All legacy records have corresponding `OrganizationMembership` and `EmployeeProfile` records.
   - Tenant isolation is strictly enforced.
   - Existing user credentials still work.
   - Historical leave records show correct status and requester links.
3. Record duration and exact SQL execution log.

---

## 7. Pre-Production Migration Signoff Checklist

Before running any migration on the production server, all criteria below must be checked:

- [ ] Maintenance window active, all external ingress blocked (HTTP 503).
- [ ] Production application stopped cleanly; no open file handles on `leavetracker.mv.db`.
- [ ] Cold copy backup taken and SHA-256 checksum recorded.
- [ ] Backup successfully restored to staging environment.
- [ ] Staging verification protocol executed with 100% data fidelity.
- [ ] `/admin/migration/preview` run on staging with 0 unresolved errors.
- [ ] `/admin/migration/execute` rehearsed on staging with verified success.
- [ ] Lead Architect and Security Reviewer signoff obtained.

---

## 8. Rollback and Disaster Recovery Procedure

If any anomaly, database lockup, unhandled exception, or data inconsistency occurs during a production migration attempt:

### 8.1 Immediate Abort Directives
1. **Halt Execution Immediately:** Stop the application service.
2. **Do Not Attempt Partial Fixes in Place:** Do not execute ad-hoc SQL updates on a half-migrated database.

### 8.2 Recovery Execution Steps
1. **Terminate All Processes:**
   ```powershell
   Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
   ```

2. **Quarantine Failed Database:**
   ```powershell
   $FAIL_DIR = ".\failed_run_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
   New-Item -ItemType Directory -Path $FAIL_DIR
   Move-Item ".\springapp\data\leavetracker.mv.db" -Destination "$FAIL_DIR\leavetracker_failed.mv.db"
   ```

3. **Restore Pre-Maintenance Cold Backup:**
   ```powershell
   Copy-Item "$BACKUP_DIR\leavetracker.mv.db" -Destination ".\springapp\data\leavetracker.mv.db"
   ```

4. **Verify Restored File Checksum:**
   ```powershell
   $REST_HASH = (Get-FileHash -Path ".\springapp\data\leavetracker.mv.db" -Algorithm SHA256).Hash
   if ($REST_HASH -ne $ORIG_HASH) {
       throw "CRITICAL: Restored file hash does not match pre-maintenance original!"
   }
   Write-Host "✅ Restored file hash matches original cold copy." -ForegroundColor Green
   ```

5. **Restart Production Application:**
   ```powershell
   Start-Process -FilePath "java" -ArgumentList "-jar", "springapp-0.0.1-SNAPSHOT.jar"
   ```

6. **Execute Smoke Tests:**
   - Verify health endpoint: `GET /actuator/health` or `GET /api/public/ping`.
   - Verify user login and leave records.

7. **Re-open Ingress:**
   - Remove maintenance banner and re-enable traffic at the reverse proxy.
   - Post-incident postmortem within 24 hours.
