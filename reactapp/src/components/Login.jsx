import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { 
  FaBuilding, 
  FaUser, 
  FaLock, 
  FaRobot, 
  FaCalendarCheck, 
  FaArrowRight, 
  FaEye, 
  FaEyeSlash, 
  FaShieldAlt, 
  FaBolt, 
  FaExclamationCircle 
} from 'react-icons/fa';
import { useAuth } from "./AuthContext";
import apiClient from "../services/apiClient";
import "./Auth.css";

const Login = () => {
  const [form, setForm] = useState({ username: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  // Quick fill demo credentials for instantaneous access
  const handleQuickFill = (demoUsername, demoPassword) => {
    setForm({ username: demoUsername, password: demoPassword });
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await apiClient.post("/auth/login", {
        username: form.username,
        password: form.password
      });

      if (response.data) {
        const data = response.data;
        login({
          id: data.id,
          name: data.username,
          username: data.username,
          role: data.role
        }, data.token);

        // Immediate routing
        navigate(data.role === 'admin' ? '/admin/dashboard' : '/');
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data || "Invalid credentials. Please verify username and password.";
      setError(typeof errorMsg === 'string' ? errorMsg : "Invalid credentials. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page-wrapper">
      <div className="auth-layout-grid">
        
        {/* Left Column: Brand & Feature Showcase */}
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
              Intelligent Leave Management, <span className="gradient-text">Simplified.</span>
            </h2>
            <p className="showcase-description">
              Streamline time-off approvals, prevent team scheduling conflicts, and generate smart handoff communications with integrated AI assistants.
            </p>

            <div className="showcase-features-list">
              <div className="showcase-feature-item">
                <div className="feature-icon-circle">
                  <FaBolt />
                </div>
                <div className="feature-text-content">
                  <h4>AI Overlap & Impact Analysis</h4>
                  <p>Evaluates team capacity and detects concurrent leave risks instantly.</p>
                </div>
              </div>

              <div className="showcase-feature-item">
                <div className="feature-icon-circle">
                  <FaRobot />
                </div>
                <div className="feature-text-content">
                  <h4>24/7 AI Leave Assistant</h4>
                  <p>Get answers to company policies, quotas, and holiday schedules.</p>
                </div>
              </div>

              <div className="showcase-feature-item">
                <div className="feature-icon-circle">
                  <FaCalendarCheck />
                </div>
                <div className="feature-text-content">
                  <h4>Automated Out-of-Office Drafter</h4>
                  <p>Generate professional client coverage emails with a single click.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Demo Credentials Panel */}
          <div className="demo-accounts-card">
            <div className="demo-card-header">
              <span>Quick Demo Access</span>
              <span style={{ color: '#818cf8', fontSize: '0.75rem' }}>One-Click Fill</span>
            </div>
            <div className="demo-buttons-row">
              <button 
                type="button" 
                className="demo-fill-btn"
                onClick={() => handleQuickFill("admin", "password123")}
              >
                <FaShieldAlt style={{ color: '#a855f7' }} /> Fill Admin
              </button>
              <button 
                type="button" 
                className="demo-fill-btn"
                onClick={() => handleQuickFill("employee1", "password123")}
              >
                <FaUser style={{ color: '#38bdf8' }} /> Fill Employee
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Modern Login Form */}
        <div className="auth-form-panel">
          <div className="form-header-area">
            <div className="form-header-badge">
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#6366f1' }}></span>
              Secure Portal
            </div>
            <h2>Welcome Back!</h2>
            <p>Enter your credentials to access your dashboard</p>
          </div>

          {error && (
            <div className="auth-alert-box error" style={{ marginBottom: '1.25rem' }}>
              <FaExclamationCircle style={{ flexShrink: 0, fontSize: '1rem' }} />
              <span>{error}</span>
            </div>
          )}

          <form className="modern-form" onSubmit={handleSubmit}>
            <div className="input-field-block">
              <label htmlFor="username">Username</label>
              <div className="input-wrapper-inner">
                <FaUser className="input-left-icon" />
                <input
                  type="text"
                  id="username"
                  name="username"
                  className="modern-text-input"
                  placeholder="e.g. admin or employee1"
                  value={form.username}
                  onChange={handleChange}
                  autoComplete="username"
                  required
                />
              </div>
            </div>

            <div className="input-field-block">
              <label htmlFor="password">Password</label>
              <div className="input-wrapper-inner">
                <FaLock className="input-left-icon" />
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  name="password"
                  className="modern-text-input"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={handleChange}
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="input-eye-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
            </div>

            <button 
              type="submit" 
              className="auth-primary-submit-btn" 
              disabled={loading}
            >
              {loading ? (
                <>
                  <div className="btn-spinner"></div>
                  <span>Signing In...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <FaArrowRight style={{ fontSize: '0.85rem' }} />
                </>
              )}
            </button>
          </form>

          <div className="auth-navigation-footer">
            Don't have an account? 
            <Link to="/signup">Create account</Link>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Login;