import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { FaBuilding, FaCheckCircle, FaExclamationTriangle, FaEnvelope, FaPaperPlane, FaSignInAlt } from 'react-icons/fa';
import './OrganizationVerification.css';

const OrganizationVerification = ({ isPendingScreen = false }) => {
  const [searchParams] = useSearchParams();

  // Extract token from URL fragment (#token=...), fallback to query parameter (?token=...)
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
  const [token, setToken] = useState(initialToken);
  const [email] = useState(searchParams.get('email') || '');
  const [status, setStatus] = useState(isPendingScreen ? 'pending_notice' : (initialToken ? 'verifying' : 'idle'));
  const [errorMessage, setErrorMessage] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Resend state
  const [resendEmail, setResendEmail] = useState(searchParams.get('email') || '');
  const [resending, setResending] = useState(false);
  const [resendFeedback, setResendFeedback] = useState(null);

  useEffect(() => {
    const captured = extractToken();
    if (captured) {
      setToken(captured);
      // Clean visible browser URL and history immediately
      try {
        if (window.history && window.history.replaceState) {
          window.history.replaceState(null, document.title, window.location.pathname);
        }
      } catch (e) {
        // Fallback
      }
      performVerification(captured);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const performVerification = async (tokenToVerify) => {
    setStatus('verifying');
    setErrorMessage(null);

    try {
      const payload = {
        token: tokenToVerify.trim(),
        email: email ? email.trim() : undefined,
      };

      const response = await apiClient.post('/auth/verify-organization', payload);
      setStatus('success');
      setSuccessMessage(response.data?.message || 'Organization workspace verified and activated successfully!');
    } catch (err) {
      console.error('Verification failed', err);
      setStatus('error');
      const msg = err.response?.data?.message || err.response?.data || 'Verification failed. The token may be expired or already redeemed.';
      setErrorMessage(typeof msg === 'string' ? msg : 'Verification token is invalid or expired.');
    }
  };

  const handleManualVerify = (e) => {
    e.preventDefault();
    if (!token.trim()) {
      setErrorMessage('Verification token is required.');
      return;
    }
    performVerification(token);
  };

  const handleResend = async (e) => {
    e.preventDefault();
    if (!resendEmail.trim() || !resendEmail.includes('@')) {
      setResendFeedback({ type: 'error', text: 'Please enter a valid administrator work email.' });
      return;
    }

    try {
      setResending(true);
      setResendFeedback(null);
      await apiClient.post('/auth/resend-verification', { email: resendEmail.trim() });
      setResendFeedback({
        type: 'success',
        text: 'If an unverified organization exists for this email, a new verification link has been sent.',
      });
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Too many requests. Please wait before trying again.';
      setResendFeedback({ type: 'error', text: typeof msg === 'string' ? msg : 'Failed to resend verification.' });
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="verification-page">
      <div className="verification-card">
        {status === 'verifying' && (
          <div>
            <div className="verification-badge pending">
              <FaBuilding />
            </div>
            <h2 className="verification-title">Verifying Workspace</h2>
            <p className="verification-subtitle">
              Validating your organization activation credentials...
            </p>
          </div>
        )}

        {status === 'success' && (
          <div>
            <div className="verification-badge success">
              <FaCheckCircle />
            </div>
            <h2 className="verification-title">Workspace Activated!</h2>
            <p className="verification-subtitle">
              {successMessage || 'Your company workspace is now active and ready for your team.'}
            </p>
            <Link to="/login" className="btn-primary-action">
              <FaSignInAlt />
              <span>Sign In to WorkNest</span>
            </Link>
          </div>
        )}

        {status === 'pending_notice' && (
          <div>
            <div className="verification-badge pending">
              <FaEnvelope />
            </div>
            <h2 className="verification-title">Check Your Email</h2>
            <p className="verification-subtitle">
              We have dispatched a verification link to your work email. Click the link in the email to activate your WorkNest workspace.
            </p>
            <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
              The verification link is valid for 24 hours.
            </p>
          </div>
        )}

        {(status === 'error' || status === 'idle') && (
          <div>
            <div className={`verification-badge ${status === 'error' ? 'error' : 'pending'}`}>
              {status === 'error' ? <FaExclamationTriangle /> : <FaBuilding />}
            </div>
            <h2 className="verification-title">
              {status === 'error' ? 'Verification Failed' : 'Verify Organization'}
            </h2>
            <p className="verification-subtitle">
              {status === 'error'
                ? errorMessage
                : 'Enter your verification token to activate your organization workspace.'}
            </p>

            {status === 'idle' && (
              <form onSubmit={handleManualVerify} style={{ marginBottom: '1.5rem', textAlign: 'left' }}>
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                    Verification Token *
                  </label>
                  <input
                    type="text"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    placeholder="Paste 64-character token"
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1.5px solid #cbd5e1',
                      borderRadius: '8px',
                      fontSize: '0.95rem',
                      fontFamily: 'monospace',
                    }}
                    required
                  />
                </div>
                <button type="submit" className="btn-primary-action">
                  Verify Workspace
                </button>
              </form>
            )}
          </div>
        )}

        {/* Resend Verification Form */}
        {(status === 'error' || status === 'pending_notice' || status === 'idle') && (
          <div className="resend-box">
            <h4 style={{ margin: '0 0 0.5rem 0', color: '#1e293b', fontSize: '0.95rem' }}>
              Need a new verification link?
            </h4>
            <p style={{ margin: '0 0 1rem 0', color: '#64748b', fontSize: '0.85rem' }}>
              Enter your administrator work email below to receive a new link.
            </p>

            {resendFeedback && (
              <div className={resendFeedback.type === 'error' ? 'verification-alert-error' : 'verification-alert-success'}>
                {resendFeedback.type === 'error' ? <FaExclamationTriangle /> : <FaCheckCircle />}
                <span>{resendFeedback.text}</span>
              </div>
            )}

            <form onSubmit={handleResend} style={{ display: 'flex', gap: '8px' }}>
              <input
                type="email"
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                placeholder="admin@yourcompany.com"
                style={{
                  flex: 1,
                  padding: '0.65rem 0.85rem',
                  border: '1.5px solid #cbd5e1',
                  borderRadius: '6px',
                  fontSize: '0.9rem',
                }}
                required
              />
              <button
                type="submit"
                disabled={resending}
                style={{
                  background: '#0284c7',
                  color: '#ffffff',
                  border: 'none',
                  padding: '0.65rem 1.1rem',
                  borderRadius: '6px',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  cursor: resending ? 'not-allowed' : 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <FaPaperPlane />
                <span>{resending ? 'Sending...' : 'Resend'}</span>
              </button>
            </form>
          </div>
        )}

        <div style={{ marginTop: '1.5rem' }}>
          <Link to="/login" style={{ fontSize: '0.9rem', color: '#2563eb', textDecoration: 'none', fontWeight: 600 }}>
            Back to Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};

export default OrganizationVerification;
