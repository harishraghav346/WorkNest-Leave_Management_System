import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { 
  FaBuilding, 
  FaUser, 
  FaLock, 
  FaUserTie, 
  FaCheckCircle, 
  FaExclamationCircle, 
  FaArrowRight, 
  FaShieldAlt 
} from 'react-icons/fa';
import apiClient from "../services/apiClient";
import "./Auth.css";

const Signup = () => {
  const [form, setForm] = useState({ 
    username: "", 
    password: "", 
    confirmPassword: "", 
    role: "employee" 
  });
  const [loading, setLoading] = useState(false);
  const [signupSuccess, setSignupSuccess] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleRoleSelect = (roleName) => {
    setForm({ ...form, role: roleName });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match!");
      return;
    }

    if (form.password.length < 4) {
      setError("Password must be at least 4 characters long.");
      return;
    }

    setLoading(true);

    try {
      const response = await apiClient.post("/auth/register", {
        username: form.username,
        password: form.password,
        role: form.role
      });

      if (response.data) {
        setSignupSuccess(true);
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data || "Signup failed. Please try again.";
      setError(typeof errorMsg === 'string' ? errorMsg : "Signup failed. Please check inputs.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page-wrapper">
      <div className="auth-layout-grid">
        
        {/* Left Column: Brand & Role Guidance */}
        <div className="auth-showcase-panel">
          <div>
            <div className="showcase-brand">
              <div className="brand-icon-box">
                <FaBuilding />
              </div>
              <div className="brand-text-group">
                <h1>LeaveTracker</h1>
                <span className="brand-tagline">AI-Powered Workforce Platform</span>
              </div>
            </div>

            <h2 className="showcase-headline">
              Join Your Team's <span className="gradient-text">Smart Workspace.</span>
            </h2>
            <p className="showcase-description">
              Set up your profile to manage time-off, monitor allowances, and experience effortless workflow automation.
            </p>

            <div className="showcase-features-list">
              <div className="showcase-feature-item">
                <div className="feature-icon-circle">
                  <FaUser />
                </div>
                <div className="feature-text-content">
                  <h4>Employee Experience</h4>
                  <p>Apply for leaves, track approval stages, and draft out-of-office messages.</p>
                </div>
              </div>

              <div className="showcase-feature-item">
                <div className="feature-icon-circle">
                  <FaShieldAlt />
                </div>
                <div className="feature-text-content">
                  <h4>Manager Oversight</h4>
                  <p>Get AI impact scores and team scheduling conflict warnings before approving.</p>
                </div>
              </div>
            </div>
          </div>

          <div className="demo-accounts-card" style={{ background: 'rgba(255, 255, 255, 0.03)' }}>
            <p style={{ color: '#94a3b8', fontSize: '0.82rem', margin: 0 }}>
              💡 <strong>Tip:</strong> Roles can be chosen during registration. Select <em>Admin</em> to test approval workflows or <em>Employee</em> to test leave applications.
            </p>
          </div>
        </div>

        {/* Right Column: Registration Form */}
        <div className="auth-form-panel">
          <div className="form-header-area">
            <div className="form-header-badge">
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#10b981' }}></span>
              Fast Onboarding
            </div>
            <h2>Create Your Account</h2>
            <p>Fill in the details below to get started</p>
          </div>

          {signupSuccess ? (
            <div style={{ textAlign: 'center', padding: '1rem 0' }}>
              <div style={{ 
                width: 56, 
                height: 56, 
                borderRadius: '50%', 
                background: 'rgba(16, 185, 129, 0.15)', 
                color: '#10b981', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                fontSize: '1.75rem', 
                margin: '0 auto 1.25rem' 
              }}>
                <FaCheckCircle />
              </div>
              <h3 style={{ color: '#ffffff', fontSize: '1.35rem', marginBottom: '8px' }}>
                Account Created Successfully!
              </h3>
              <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
                Your account is ready. Log in to access your leave management workspace.
              </p>
              <button 
                type="button" 
                onClick={() => navigate("/login")} 
                className="auth-primary-submit-btn"
              >
                <span>Proceed to Sign In</span>
                <FaArrowRight style={{ fontSize: '0.85rem' }} />
              </button>
            </div>
          ) : (
            <>
              {error && (
                <div className="auth-alert-box error" style={{ marginBottom: '1.25rem' }}>
                  <FaExclamationCircle style={{ flexShrink: 0, fontSize: '1rem' }} />
                  <span>{error}</span>
                </div>
              )}

              <form className="modern-form" onSubmit={handleSubmit}>
                <div className="input-field-block">
                  <label htmlFor="username">Choose a Username</label>
                  <div className="input-wrapper-inner">
                    <FaUser className="input-left-icon" />
                    <input
                      type="text"
                      id="username"
                      name="username"
                      className="modern-text-input"
                      placeholder="e.g. sarah_connor"
                      value={form.username}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="input-field-block">
                  <label htmlFor="password">Password</label>
                  <div className="input-wrapper-inner">
                    <FaLock className="input-left-icon" />
                    <input
                      type="password"
                      id="password"
                      name="password"
                      className="modern-text-input"
                      placeholder="At least 4 characters"
                      value={form.password}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="input-field-block">
                  <label htmlFor="confirmPassword">Confirm Password</label>
                  <div className="input-wrapper-inner">
                    <FaLock className="input-left-icon" />
                    <input
                      type="password"
                      id="confirmPassword"
                      name="confirmPassword"
                      className="modern-text-input"
                      placeholder="Re-enter password"
                      value={form.confirmPassword}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="input-field-block">
                  <label htmlFor="role">Select Role</label>
                  <div className="role-pills-selector">
                    <button
                      type="button"
                      className={`role-pill-option ${form.role === 'employee' ? 'active' : ''}`}
                      onClick={() => handleRoleSelect('employee')}
                    >
                      <FaUser />
                      <span>Employee</span>
                    </button>
                    <button
                      type="button"
                      className={`role-pill-option ${form.role === 'admin' ? 'active' : ''}`}
                      onClick={() => handleRoleSelect('admin')}
                    >
                      <FaUserTie />
                      <span>Manager / Admin</span>
                    </button>
                  </div>

                  {/* Hidden select for strict HTML input / testing compatibility */}
                  <select
                    id="role"
                    name="role"
                    value={form.role}
                    onChange={handleChange}
                    style={{ display: 'none' }}
                  >
                    <option value="employee">Employee</option>
                    <option value="admin">Admin / Manager</option>
                  </select>
                </div>

                <button 
                  type="submit" 
                  className="auth-primary-submit-btn" 
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <div className="btn-spinner"></div>
                      <span>Creating Account...</span>
                    </>
                  ) : (
                    <>
                      <span>Create Account</span>
                      <FaArrowRight style={{ fontSize: '0.85rem' }} />
                    </>
                  )}
                </button>
              </form>

              <div className="auth-navigation-footer">
                Already have an account? 
                <Link to="/login">Sign in</Link>
              </div>
            </>
          )}
        </div>

      </div>
    </div>
  );
};

export default Signup;