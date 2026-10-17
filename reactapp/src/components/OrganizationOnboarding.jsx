import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import apiClient from '../services/apiClient';
import { FaBuilding, FaUserTie, FaExclamationTriangle, FaArrowRight } from 'react-icons/fa';
import './OrganizationOnboarding.css';

const OrganizationOnboarding = () => {
  const [formData, setFormData] = useState({
    organizationName: '',
    organizationSlug: '',
    adminFullName: '',
    adminEmail: '',
    adminUsername: '',
    adminPassword: '',
    confirmPassword: '',
  });

  const [slugManuallyEdited, setSlugManuallyEdited] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { login, selectTenant } = useAuth();
  const navigate = useNavigate();

  const handleNameChange = (e) => {
    const name = e.target.value;
    const updates = { organizationName: name };
    if (!slugManuallyEdited) {
      updates.organizationSlug = name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
    }
    setFormData((prev) => ({ ...prev, ...updates }));
  };

  const handleSlugChange = (e) => {
    setSlugManuallyEdited(true);
    setFormData((prev) => ({
      ...prev,
      organizationSlug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''),
    }));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    // Client-side validations
    if (!formData.organizationName.trim()) {
      return setError('Organization name is required.');
    }
    if (!formData.organizationSlug.trim() || formData.organizationSlug.length < 3) {
      return setError('Organization slug must be at least 3 characters.');
    }
    if (!formData.adminFullName.trim()) {
      return setError('Administrator full name is required.');
    }
    if (!formData.adminEmail.trim() || !formData.adminEmail.includes('@')) {
      return setError('A valid administrator work email is required.');
    }
    if (!formData.adminUsername.trim() || formData.adminUsername.length < 3) {
      return setError('Administrator username must be at least 3 characters.');
    }
    if (!formData.adminPassword || formData.adminPassword.length < 8) {
      return setError('Password must be at least 8 characters long.');
    }
    if (formData.adminPassword !== formData.confirmPassword) {
      return setError('Passwords do not match.');
    }

    try {
      setLoading(true);
      const payload = {
        organizationName: formData.organizationName.trim(),
        organizationSlug: formData.organizationSlug.trim(),
        adminFullName: formData.adminFullName.trim(),
        adminEmail: formData.adminEmail.trim(),
        adminUsername: formData.adminUsername.trim(),
        adminPassword: formData.adminPassword,
      };

      const response = await apiClient.post('/auth/onboard-organization', payload);
      if (response.data?.requiresEmailVerification) {
        navigate(`/verification-pending?email=${encodeURIComponent(formData.adminEmail.trim())}&org=${encodeURIComponent(formData.organizationName.trim())}`);
      } else if (response.data && response.data.token) {
        // Direct active path (when verification is disabled)
        const userObj = {
          id: response.data.id,
          username: response.data.username,
          role: response.data.role,
        };
        login(userObj, response.data.token);
        if (response.data.organization) {
          selectTenant(response.data.organization, response.data.role, response.data.token);
        }
        navigate('/dashboard');
      } else {
        navigate(`/verification-pending?email=${encodeURIComponent(formData.adminEmail.trim())}`);
      }
    } catch (err) {
      console.error('Onboarding failed', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to complete organization onboarding.';
      setError(typeof msg === 'string' ? msg : 'Failed to complete organization onboarding.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-page">
      <div className="onboarding-card">
        <div className="onboarding-header">
          <div className="onboarding-badge">W</div>
          <h2 className="onboarding-title">Onboard Your Organization</h2>
          <p className="onboarding-subtitle">
            Set up your dedicated WorkNest company workspace in under two minutes.
          </p>
        </div>

        {error && (
          <div className="onboard-alert-error">
            <FaExclamationTriangle />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Organization Details */}
          <div className="onboarding-section-title">
            <FaBuilding style={{ color: '#2563eb' }} />
            <span>Company Information</span>
          </div>

          <div className="form-group">
            <label className="form-label">Organization Name *</label>
            <input
              type="text"
              name="organizationName"
              className="form-input"
              placeholder="e.g. Acme Technologies Inc."
              value={formData.organizationName}
              onChange={handleNameChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Workspace URL Slug *</label>
            <div className="slug-preview">
              <span className="slug-prefix">worknest.app/</span>
              <input
                type="text"
                name="organizationSlug"
                className="slug-input"
                placeholder="acme-technologies"
                value={formData.organizationSlug}
                onChange={handleSlugChange}
                required
              />
            </div>
            <span className="form-hint">Used for tenant isolation and team recognition.</span>
          </div>

          {/* Initial Administrator */}
          <div className="onboarding-section-title">
            <FaUserTie style={{ color: '#2563eb' }} />
            <span>Initial Organization Administrator</span>
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label className="form-label">Admin Full Name *</label>
              <input
                type="text"
                name="adminFullName"
                className="form-input"
                placeholder="Jane Doe"
                value={formData.adminFullName}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Work Email Address *</label>
              <input
                type="email"
                name="adminEmail"
                className="form-input"
                placeholder="jane.doe@company.com"
                value={formData.adminEmail}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Admin Username *</label>
            <input
              type="text"
              name="adminUsername"
              className="form-input"
              placeholder="jane_admin"
              value={formData.adminUsername}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label className="form-label">Password (min 8 chars) *</label>
              <input
                type="password"
                name="adminPassword"
                className="form-input"
                placeholder="••••••••"
                value={formData.adminPassword}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Confirm Password *</label>
              <input
                type="password"
                name="confirmPassword"
                className="form-input"
                placeholder="••••••••"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <button type="submit" className="btn-onboard-submit" disabled={loading}>
            {loading ? (
              'Creating Organization Workspace...'
            ) : (
              <>
                <span>Launch Company Workspace</span>
                <FaArrowRight />
              </>
            )}
          </button>
        </form>

        <div className="onboarding-footer">
          Already part of an organization? <Link to="/login">Sign In</Link> • Have an invite token? <Link to="/activate">Activate Invite</Link>
        </div>
      </div>
    </div>
  );
};

export default OrganizationOnboarding;
