import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import apiClient from '../services/apiClient';
import { FaBuilding, FaArrowRight, FaExclamationTriangle, FaSignOutAlt } from 'react-icons/fa';
import './TenantSelection.css';

const TenantSelection = () => {
  const { user, memberships, selectTenant, logout } = useAuth();
  const [tenantList, setTenantList] = useState(memberships || []);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchMemberships = async () => {
      if (tenantList.length === 0) {
        try {
          setLoading(true);
          const response = await apiClient.get('/auth/memberships');
          if (response.data && Array.isArray(response.data)) {
            setTenantList(response.data);
          }
        } catch (err) {
          console.error('Failed to load user memberships', err);
          setError('Unable to load organization memberships. Please log in again.');
        } finally {
          setLoading(false);
        }
      }
    };
    fetchMemberships();
  }, [tenantList.length]);

  const handleSelectTenant = async (mem) => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.post('/auth/select-tenant', {
        organizationId: mem.organizationId,
        organizationSlug: mem.organizationSlug,
      });

      if (response.data && response.data.token) {
        selectTenant(response.data.organization, response.data.role, response.data.token);
        navigate('/dashboard');
      } else {
        setError('Tenant selection failed. Invalid response from server.');
      }
    } catch (err) {
      console.error('Tenant selection error', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to select organization. Access denied or organization suspended.';
      setError(typeof msg === 'string' ? msg : 'Failed to select organization.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="tenant-selection-container">
      <div className="tenant-selection-card">
        <div className="tenant-header">
          <div className="tenant-badge">W</div>
          <h2 className="tenant-title">Select Organization</h2>
          <p className="tenant-subtitle">
            Welcome back, <strong>{user?.username}</strong>. Choose an organization workspace to enter:
          </p>
        </div>

        {error && (
          <div className="tenant-alert-error">
            <FaExclamationTriangle />
            <span>{error}</span>
          </div>
        )}

        {loading && <div style={{ textAlign: 'center', padding: '1rem', color: '#64748b' }}>Loading organizations...</div>}

        <div className="tenant-list">
          {tenantList.map((mem) => (
            <div
              key={mem.id || mem.organizationId}
              className="tenant-item"
              onClick={() => handleSelectTenant(mem)}
            >
              <div className="tenant-item-info">
                <div className="tenant-item-name">
                  <FaBuilding style={{ marginRight: '8px', color: '#2563eb' }} />
                  {mem.organizationName || mem.organizationSlug}
                </div>
                <div className="tenant-item-meta">
                  <span>Slug: <code>{mem.organizationSlug}</code></span>
                  <span>•</span>
                  <span className="tenant-role-badge">{mem.role || 'EMPLOYEE'}</span>
                </div>
              </div>
              <div className="tenant-item-action">
                <span>Enter</span>
                <FaArrowRight />
              </div>
            </div>
          ))}

          {tenantList.length === 0 && !loading && (
            <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
              No active organization memberships found for this account.
            </div>
          )}
        </div>

        <div className="tenant-footer-actions">
          <button className="btn-tenant-logout" onClick={handleLogout}>
            <FaSignOutAlt style={{ marginRight: '6px' }} />
            Sign Out / Switch Account
          </button>
        </div>
      </div>
    </div>
  );
};

export default TenantSelection;
