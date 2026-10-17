import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  FaCalendarAlt,
  FaUsers,
  FaShieldAlt,
  FaChartLine,
  FaBuilding,
  FaArrowRight,
  FaCheckCircle,
  FaPlayCircle,
  FaLock,
  FaCheck
} from 'react-icons/fa';
import './PublicWebsite.css';

const PublicWebsite = () => {
  const [activeTab, setActiveTab] = useState('employee');

  return (
    <div className="public-page">
      {/* Top Header */}
      <header className="public-header">
        <div className="public-logo">
          <div className="logo-badge">W</div>
          <span className="logo-text">WorkNest</span>
          <span className="logo-tag">HRMS</span>
        </div>

        <nav className="public-nav-links">
          <a href="#features">Features</a>
          <a href="#walkthrough">Product Tour</a>
          <a href="#security">Enterprise Security</a>
          <Link to="/activate">Activate Invite</Link>
        </nav>

        <div className="public-header-actions">
          <Link to="/login" className="btn-public-login">
            Sign In
          </Link>
          <Link to="/onboard" className="btn-public-cta">
            Onboard Company
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-pill">
          <span className="hero-pill-badge">WorkNest v2.0</span>
          <span>Multi-Tenant Enterprise HR Leave Management</span>
        </div>

        <h1 className="hero-title">
          Smart Leave Management for <br />
          <span className="hero-gradient-text">Modern High-Velocity Teams</span>
        </h1>

        <p className="hero-subtitle">
          Eliminate spreadsheet errors, automate time-off approvals, and unify
          multi-department leave balances with strict cryptographic tenant isolation.
        </p>

        <div className="hero-cta-group">
          <Link to="/onboard" className="btn-hero-primary">
            <span>Onboard Your Organization</span>
            <FaArrowRight />
          </Link>
          <Link to="/login" className="btn-hero-secondary">
            <span>Employee Sign In</span>
          </Link>
          <Link to="/activate" className="btn-hero-secondary">
            <span>Activate With Token</span>
          </Link>
        </div>

        <div className="hero-stats-row">
          <div className="hero-stat-item">
            <span className="stat-value">100%</span>
            <span className="stat-label">Tenant Isolation</span>
          </div>
          <div className="hero-stat-item">
            <span className="stat-value">&lt; 10s</span>
            <span className="stat-label">Leave Approvals</span>
          </div>
          <div className="hero-stat-item">
            <span className="stat-value">Zero</span>
            <span className="stat-label">Spreadsheet Headaches</span>
          </div>
          <div className="hero-stat-item">
            <span className="stat-value">256-bit</span>
            <span className="stat-label">Cryptographic Tokens</span>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="features-section">
        <div className="section-header">
          <div className="section-tag">Platform Capabilities</div>
          <h2 className="section-title">Engineered for Scalable HR Operations</h2>
          <p className="section-desc">
            From single startups to multi-organization enterprises, WorkNest delivers
            bulletproof leave workflows and real-time absence tracking.
          </p>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#eff6ff', color: '#2563eb' }}>
              <FaCalendarAlt />
            </div>
            <h3 className="feature-title">Intuitive Leave Requests</h3>
            <p className="feature-desc">
              Employees submit single-day or date-range requests with instant entitlement
              balance deduction, reason tracking, and automated status notifications.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#f0fdf4', color: '#16a34a' }}>
              <FaUsers />
            </div>
            <h3 className="feature-title">Department Scoping & Hierarchy</h3>
            <p className="feature-desc">
              Department managers only see leaves within their purview. Employee transfers
              and reassignments immediately update approval routing without data leakage.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#fef2f2', color: '#dc2626' }}>
              <FaShieldAlt />
            </div>
            <h3 className="feature-title">Multi-Tenant Isolation</h3>
            <p className="feature-desc">
              Every request is server-side verified against active organization memberships.
              Cross-tenant leaks, ambiguous tenant access, and spoofed headers are strictly blocked.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#faf5ff', color: '#9333ea' }}>
              <FaChartLine />
            </div>
            <h3 className="feature-title">Tamper-Proof Audit Trail</h3>
            <p className="feature-desc">
              All leave creation, approvals, rejections, and administrative policy changes
              are cryptographically attributed and timestamped in an immutable audit ledger.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#fffbeb', color: '#d97706' }}>
              <FaBuilding />
            </div>
            <h3 className="feature-title">Self-Service Onboarding</h3>
            <p className="feature-desc">
              Companies can register and configure their tenant in seconds. Organization admins
              issue secure, one-time invitation links to onboard staff with zero friction.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper" style={{ background: '#ecfeff', color: '#0891b2' }}>
              <FaCheckCircle />
            </div>
            <h3 className="feature-title">Live Team Calendars</h3>
            <p className="feature-desc">
              High-visibility company calendars prevent department scheduling conflicts,
              allowing managers to plan coverage and staffing with complete confidence.
            </p>
          </div>
        </div>
      </section>

      {/* Product Walkthrough / Demo Section */}
      <section id="walkthrough" className="demo-section">
        <div className="section-header">
          <div className="section-tag">Interactive Tour</div>
          <h2 className="section-title">See WorkNest in Action</h2>
          <p className="section-desc">
            Explore the specialized interfaces tailored for each role in your organization.
          </p>
        </div>

        <div className="demo-container">
          <div className="demo-topbar">
            <div className="demo-dots">
              <span className="demo-dot dot-red"></span>
              <span className="demo-dot dot-yellow"></span>
              <span className="demo-dot dot-green"></span>
            </div>
            <div className="demo-tabs">
              <button
                className={`demo-tab-btn ${activeTab === 'employee' ? 'active' : ''}`}
                onClick={() => setActiveTab('employee')}
              >
                Employee Self-Service
              </button>
              <button
                className={`demo-tab-btn ${activeTab === 'manager' ? 'active' : ''}`}
                onClick={() => setActiveTab('manager')}
              >
                Manager Approvals
              </button>
              <button
                className={`demo-tab-btn ${activeTab === 'admin' ? 'active' : ''}`}
                onClick={() => setActiveTab('admin')}
              >
                Organization Settings
              </button>
            </div>
          </div>

          <div className="demo-content-area">
            {activeTab === 'employee' && (
              <div className="demo-placeholder-card">
                <div className="demo-notice-title">
                  <FaPlayCircle style={{ verticalAlign: 'middle', marginRight: '8px' }} />
                  Employee Experience Walkthrough
                </div>
                <p className="demo-notice-text">
                  Employees enjoy a frictionless dashboard showing live leave balances, quick
                  apply workflows, vacation planners, and request approval status histories.
                </p>
                <div className="demo-instructions">
                  {`// Media Asset Configuration Notes:
// To link a real recorded product video walkthrough, place 'walkthrough-employee.mp4'
// in the 'reactapp/public/assets/' directory and embed via HTML5 video tag.`}
                </div>
              </div>
            )}

            {activeTab === 'manager' && (
              <div className="demo-placeholder-card">
                <div className="demo-notice-title">
                  <FaPlayCircle style={{ verticalAlign: 'middle', marginRight: '8px' }} />
                  Manager & Department Approval Queue
                </div>
                <p className="demo-notice-text">
                  Department managers review pending leave requests with one-click approve/reject
                  actions, team coverage warnings, and automated employee notifications.
                </p>
                <div className="demo-instructions">
                  {`// Media Asset Configuration Notes:
// To link a manager demo, place 'walkthrough-manager.mp4' in 'reactapp/public/assets/'.`}
                </div>
              </div>
            )}

            {activeTab === 'admin' && (
              <div className="demo-placeholder-card">
                <div className="demo-notice-title">
                  <FaPlayCircle style={{ verticalAlign: 'middle', marginRight: '8px' }} />
                  Organization Admin Command Center
                </div>
                <p className="demo-notice-text">
                  Admins manage company departments, invite employees with secure single-use tokens,
                  tune annual leave allowances, and inspect immutable audit events.
                </p>
                <div className="demo-instructions">
                  {`// Media Asset Configuration Notes:
// To link an admin walkthrough, place 'walkthrough-admin.mp4' in 'reactapp/public/assets/'.`}
                </div>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Enterprise Security Banner */}
      <section id="security" className="security-banner">
        <div className="security-banner-content">
          <div className="security-badge">
            <FaLock style={{ verticalAlign: 'middle', marginRight: '6px' }} />
            Enterprise-Grade Isolation
          </div>
          <h2 className="security-title">Built with Security as a First Principle</h2>
          <p className="security-desc">
            WorkNest enforces multi-tenant boundary checks in memory and at the persistence layer.
            Client-supplied organization IDs are never trusted blindly, tokens are salted with HMAC-SHA256,
            and brute-force requests are throttled at the gateway.
          </p>
          <div className="security-pill-row">
            <div className="sec-pill"><FaCheck style={{ color: '#10b981', marginRight: '6px' }} /> Server-Side Tenant Scoping</div>
            <div className="sec-pill"><FaCheck style={{ color: '#10b981', marginRight: '6px' }} /> Bounded LRU Rate Limiting</div>
            <div className="sec-pill"><FaCheck style={{ color: '#10b981', marginRight: '6px' }} /> Cryptographic Invite Tokens</div>
            <div className="sec-pill"><FaCheck style={{ color: '#10b981', marginRight: '6px' }} /> BCrypt Password Hashing</div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="public-footer">
        <div className="footer-inner">
          <div className="footer-brand">
            <h4>WorkNest</h4>
            <p>
              The unified multi-tenant leave management platform designed for high-performance
              companies, modern HR teams, and global workforces.
            </p>
          </div>

          <div className="footer-col">
            <h5>Product</h5>
            <ul>
              <li><a href="#features">Features</a></li>
              <li><a href="#walkthrough">Product Tour</a></li>
              <li><Link to="/onboard">Organization Onboarding</Link></li>
              <li><Link to="/activate">Employee Activation</Link></li>
            </ul>
          </div>

          <div className="footer-col">
            <h5>Access</h5>
            <ul>
              <li><Link to="/login">Employee Sign In</Link></li>
              <li><Link to="/onboard">Create Company Account</Link></li>
              <li><a href="#security">Security Specs</a></li>
            </ul>
          </div>

          <div className="footer-col">
            <h5>Governance</h5>
            <ul>
              <li><span>SOC2 Compliant Architecture</span></li>
              <li><span>Multi-Tenant Boundary Standard</span></li>
              <li><span>Production Hardened</span></li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <span>&copy; {new Date().getFullYear()} WorkNest. All rights reserved.</span>
          <span>Version 2.0 (Phase D4 Multi-Tenant Release)</span>
        </div>
      </footer>
    </div>
  );
};

export default PublicWebsite;
