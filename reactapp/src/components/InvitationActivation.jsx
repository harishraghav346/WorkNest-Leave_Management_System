import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from './AuthContext';
import apiClient from '../services/apiClient';
import { FaUserCheck, FaExclamationTriangle, FaCheckCircle, FaArrowRight } from 'react-icons/fa';
import './InvitationActivation.css';

const InvitationActivation = () => {
  const [searchParams] = useSearchParams();

  // D4.2.3: Prefer URL fragment (#token=...), fall back to query parameter (?token=...)
  const extractToken = () => {
    try {
      const hash = window.location.hash;
      if (hash && hash.includes('token=')) {
        const hashParams = new URLSearchParams(hash.startsWith('#') ? hash.substring(1) : hash);
        const tok = hashParams.get('token');
        if (tok) return tok;
      }
    } catch (e) {
      // Fallback
    }
    return searchParams.get('token') || '';
  };

  const initialToken = extractToken();

  const [formData, setFormData] = useState({
    token: initialToken,
    email: searchParams.get('email') || '',
    username: '',
    password: '',
    confirmPassword: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { login, selectTenant } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const capturedToken = extractToken();
    if (capturedToken) {
      setFormData((prev) => ({ ...prev, token: capturedToken }));
      // D4.2.3: Remove token from visible browser URL and history immediately
      try {
        if (window.history && window.history.replaceState) {
          window.history.replaceState(null, document.title, window.location.pathname);
        }
      } catch (e) {
        // Safe fallback
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!formData.token.trim()) {
      return setError('Invitation token is required.');
    }
    if (!formData.email.trim() || !formData.email.includes('@')) {
      return setError('A valid work email is required to verify this invitation.');
    }
    if (!formData.username.trim() || formData.username.length < 3) {
      return setError('Username must be at least 3 characters.');
    }
    if (!formData.password || formData.password.length < 8) {
      return setError('Password must be at least 8 characters long.');
    }
    if (formData.password !== formData.confirmPassword) {
      return setError('Passwords do not match.');
    }

    try {
      setLoading(true);
      const payload = {
        token: formData.token.trim(),
        email: formData.email.trim(),
        username: formData.username.trim(),
        password: formData.password,
      };

      const response = await apiClient.post('/invitations/activate', payload);
      if (response.data && response.data.token) {
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
        setError('Activation completed but failed to receive session token.');
      }
    } catch (err) {
      console.error('Activation failed', err);
      const msg = err.response?.data?.message || err.response?.data || 'Failed to activate invitation. Token may be invalid or expired.';
      setError(typeof msg === 'string' ? msg : 'Activation failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="activation-page">
      <div className="activation-card">
        <div className="activation-header">
          <div className="activation-badge">
            <FaUserCheck />
          </div>
          <h2 className="activation-title">Activate Employee Account</h2>
          <p className="activation-subtitle">
            Enter your invitation details to join your company workspace on WorkNest.
          </p>
        </div>

        <div className="activation-info-note">
          <FaCheckCircle />
          <span>For security, your invitation email address must match the invitation record.</span>
        </div>

        {error && (
          <div className="activation-alert-error">
            <FaExclamationTriangle />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Invitation Token *</label>
            <input
              type="text"
              name="token"
              className="form-input"
              placeholder="e.g. 64-character token"
              value={formData.token}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Invited Work Email *</label>
            <input
              type="email"
              name="email"
              className="form-input"
              placeholder="employee@company.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Desired Username *</label>
            <input
              type="text"
              name="username"
              className="form-input"
              placeholder="choose_a_username"
              value={formData.username}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label className="form-label">New Password *</label>
              <input
                type="password"
                name="password"
                className="form-input"
                placeholder="••••••••"
                value={formData.password}
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

          <button type="submit" className="btn-activation-submit" disabled={loading}>
            {loading ? (
              'Activating Account...'
            ) : (
              <>
                <span>Activate & Join Workspace</span>
                <FaArrowRight />
              </>
            )}
          </button>
        </form>

        <div className="activation-footer">
          Already activated? <Link to="/login">Sign In</Link> • Need a new company account? <Link to="/onboard">Onboard Company</Link>
        </div>
      </div>
    </div>
  );
};

export default InvitationActivation;
