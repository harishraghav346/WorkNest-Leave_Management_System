import React from "react";
import { Link } from "react-router-dom";
import { 
  FaPlusCircle, 
  FaRobot, 
  FaShieldAlt, 
  FaUserCheck, 
  FaUmbrellaBeach, 
  FaCogs, 
  FaThLarge, 
  FaTasks, 
  FaLock
} from 'react-icons/fa';
import { useAuth } from './AuthContext';
import "./Home.css";

const Home = () => {
  const { user, isAdmin } = useAuth();

  return (
    <div className="home-container">
      {/* Hero Header */}
      <header className="home-hero">
        <div className="hero-content">
          <span className="hero-badge">
            <FaRobot style={{ marginRight: '6px' }} /> Enterprise AI Workforce Platform
          </span>
          <h1 className="hero-title">Flexible Leave Management for Modern Teams</h1>
          <p className="hero-subtitle">
            Welcome back{user?.name ? `, ${user.name}` : ''}! Experience instant approvals, location-based policies, automated out-of-office generation, and unified workforce intelligence.
          </p>
          <div className="hero-cta-group">
            <Link to="/self-service" className="hero-primary-btn">
              <FaUserCheck style={{ marginRight: '8px' }} /> Employee Self-Service
            </Link>
            <Link to="/leave-entitlements" className="hero-secondary-btn">
              <FaUmbrellaBeach style={{ marginRight: '8px' }} /> Leave Balances & Quotas
            </Link>
            <Link to="/add-leave" className="hero-secondary-btn">
              <FaPlusCircle style={{ marginRight: '8px' }} /> Apply Leave
            </Link>
          </div>
        </div>

        {/* Live Preview Card */}
        <div className="hero-illustration">
          <div className="hero-card-preview">
            <div className="preview-header">
              <span className="pulse-dot"></span>
              <h4>Workforce Intelligence Live</h4>
            </div>
            <div className="preview-stat-row">
              <div className="preview-metric">
                <span className="metric-val">20 Days</span>
                <span className="metric-lbl">Annual Quota</span>
              </div>
              <div className="preview-metric">
                <span className="metric-val" style={{ color: '#10b981' }}>Instant</span>
                <span className="metric-lbl">AI Approvals</span>
              </div>
            </div>
            <div className="preview-ai-pill">
              <FaRobot style={{ marginRight: '6px' }} /> 24/7 AI Assistant Active (Floating)
            </div>
          </div>
        </div>
      </header>

      {/* Main Structural Modules Grid */}
      <main className="home-main">
        <div className="features-grid">
          
          {/* 1. Employee Self-Service Portal (Reference Image 5) */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#fff7ed', color: '#ea580c' }}>
              <FaUserCheck />
            </div>
            <h2 className="feature-title">Employee Self-Service</h2>
            <p className="feature-description">
              Manage personal profiles, download encrypted payslips, track daily attendance timelines, and submit leave requests.
            </p>
            <Link to="/self-service" className="primary-cta" style={{ background: '#ea580c', color: '#fff' }}>
              Open ESS Portal &rarr;
            </Link>
          </div>

          {/* 2. Flexible Leave Management (Reference Image 4) */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#e0f2fe', color: '#0284c7' }}>
              <FaUmbrellaBeach />
            </div>
            <h2 className="feature-title">Flexible Leave Balances</h2>
            <p className="feature-description">
              Year-round entitlements across Casual, Sick, Annual, and Comp Off with location-based rules and holiday calendars.
            </p>
            <Link to="/leave-entitlements" className="primary-cta">
              View Entitlements &rarr;
            </Link>
          </div>

          {/* 3. Customized Leave Policies (Reference Images 1 & 3) */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#f3e8ff', color: '#9333ea' }}>
              <FaCogs />
            </div>
            <h2 className="feature-title">Customized Leave Policies</h2>
            <p className="feature-description">
              Step-by-step wizard to configure entitlements, credit frequencies, resets, encashments, and geographic rules.
            </p>
            <Link to="/leave-policy-wizard" className="primary-cta">
              Policy Wizard &rarr;
            </Link>
          </div>

          {/* 4. Unified Workforce Management (Reference Image 2) */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#f0fdf4', color: '#16a34a' }}>
              <FaThLarge />
            </div>
            <h2 className="feature-title">Unified Workforce Hub</h2>
            <p className="feature-description">
              Connected environment for employee records, timesheets, shift scheduling, performance, and case management.
            </p>
            <Link to="/workforce-hub" className="primary-cta">
              Workforce Hub &rarr;
            </Link>
          </div>

          {/* 5. Streamlined Office Operations */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#ecfeff', color: '#0891b2' }}>
              <FaTasks />
            </div>
            <h2 className="feature-title">Office Operations & Rules</h2>
            <p className="feature-description">
              Automates repetitive routines, sends handover alerts, and broadcasts workplace communications smoothly.
            </p>
            <Link to="/office-operations" className="primary-cta">
              Operations Hub &rarr;
            </Link>
          </div>

          {/* 6. Advanced Security & Privacy */}
          <div className="feature-card">
            <div className="feature-icon" style={{ background: '#fef2f2', color: '#dc2626' }}>
              <FaLock />
            </div>
            <h2 className="feature-title">Security & Privacy</h2>
            <p className="feature-description">
              Multi-factor authentication (MFA), hardware-encrypted payslip vault, and granular role-based permissions.
            </p>
            <Link to="/security-privacy" className="primary-cta">
              Security Center &rarr;
            </Link>
          </div>

          {/* Manager / Admin Command Center */}
          {isAdmin() && (
            <div className="feature-card admin-highlight" style={{ gridColumn: 'span 1' }}>
              <div className="feature-icon" style={{ background: '#ede9fe', color: '#6d28d9' }}>
                <FaShieldAlt />
              </div>
              <h2 className="feature-title">Manager Command Center</h2>
              <p className="feature-description">
                Evaluate AI workforce impact scores, detect concurrent team overlaps, and approve or deny requests.
              </p>
              <Link to="/admin/dashboard" className="primary-cta admin-cta">
                Admin Console &rarr;
              </Link>
            </div>
          )}

        </div>
      </main>
    </div>
  );
};

export default Home;