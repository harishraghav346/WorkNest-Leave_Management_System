import React, { useState } from "react";
import { FaLock, FaKey, FaUserShield, FaCheckCircle } from "react-icons/fa";

const SecurityPrivacy = () => {
  const [mfaEnabled, setMfaEnabled] = useState(true);
  const encryptionStandard = "AES-256";

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '2.5rem 2rem 5rem' }}>
      <div style={{ marginBottom: '2.5rem' }}>
        <h1 style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--slate-900)', margin: 0 }}>
          Advanced Security & Privacy Governance
        </h1>
        <p style={{ color: 'var(--slate-500)', fontSize: '0.95rem', marginTop: '4px' }}>
          Protects workforce data with multi-factor authentication, encrypted document storage, and granular role-based permissions.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '24px' }}>
        
        {/* MFA Card */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '20px', padding: '2rem', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: '#f0fdf4', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem', marginBottom: '1.25rem' }}>
            <FaKey />
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
            Multi-Factor Authentication (MFA)
          </h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.5rem' }}>
            Requires secondary authenticator app verification (TOTP / Google Authenticator) for manager actions and sensitive employee records.
          </p>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
            <div>
              <span style={{ fontWeight: 700, color: '#1e293b', fontSize: '0.92rem' }}>Enforce MFA</span>
              <p style={{ margin: 0, fontSize: '0.78rem', color: '#94a3b8' }}>Status: {mfaEnabled ? 'Protected' : 'Disabled'}</p>
            </div>
            <button 
              onClick={() => setMfaEnabled(!mfaEnabled)}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: mfaEnabled ? '#10b981' : '#cbd5e1',
                color: '#fff',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              {mfaEnabled ? 'Enabled ✓' : 'Disabled'}
            </button>
          </div>
        </div>

        {/* Encrypted Document Storage */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '20px', padding: '2rem', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: '#e0f2fe', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem', marginBottom: '1.25rem' }}>
            <FaLock />
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
            Encrypted Document Storage
          </h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.5rem' }}>
            All employee medical leave attachments, salary slips, and contracts are protected at rest with hardware-backed encryption keys.
          </p>

          <div style={{ padding: '14px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FaCheckCircle style={{ color: '#16a34a' }} />
            <span style={{ fontSize: '0.88rem', color: '#334155', fontWeight: 600 }}>
              Protocol: <strong>{encryptionStandard} GCM</strong> with SHA-256 HMAC integrity
            </span>
          </div>
        </div>

        {/* Role-Based Permissions Matrix */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '20px', padding: '2rem', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: '#f3e8ff', color: '#9333ea', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem', marginBottom: '1.25rem' }}>
            <FaUserShield />
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', marginBottom: '8px' }}>
            Role-Based Access Control (RBAC)
          </h3>
          <p style={{ color: '#64748b', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.5rem' }}>
            Strict role isolation guarantees employees only see their own records, preventing unauthorized profile modifications or self-approvals.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '0.85rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 10px', background: '#f8fafc', borderRadius: '6px' }}>
              <span>Employee:</span>
              <strong style={{ color: '#0284c7' }}>View Self, Apply, Generate OOO</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 10px', background: '#f8fafc', borderRadius: '6px' }}>
              <span>Manager:</span>
              <strong style={{ color: '#d97706' }}>Team Overlap Analytics, Approve/Reject</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 10px', background: '#f8fafc', borderRadius: '6px' }}>
              <span>Admin / HR:</span>
              <strong style={{ color: '#9333ea' }}>Custom Policy Wizard, Full Governance</strong>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default SecurityPrivacy;
