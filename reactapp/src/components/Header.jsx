import React, { useState } from 'react';
import { NavLink, useNavigate, Link } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { 
  FaDoorOpen, 
  FaUserCircle, 
  FaBell, 
  FaPlus, 
  FaQuestionCircle, 
  FaCalendarAlt,
  FaCogs,
  FaUserCheck
} from 'react-icons/fa';
import './Header.css';

const Header = () => {
  const { user, logout, isAdmin } = useAuth();
  const [showQuickMenu, setShowQuickMenu] = useState(false);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="app-header">
      {/* Brand Logo with WorkNest Badge */}
      <NavLink to="/" className="logo-container">
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, #0284c7 0%, #2563eb 100%)',
            color: '#ffffff',
            fontWeight: 900,
            fontSize: '1.1rem',
            padding: '4px 10px',
            borderRadius: '6px',
            letterSpacing: '0.05em'
          }}>
            W
          </div>
          <span style={{
            fontWeight: 800,
            fontSize: '1.35rem',
            color: '#0f172a',
            letterSpacing: '0.05em'
          }}>
            WorkNest
          </span>
          <span style={{
            fontSize: '0.72rem',
            background: 'rgba(2, 132, 199, 0.12)',
            color: '#0284c7',
            padding: '2px 8px',
            borderRadius: '9999px',
            fontWeight: 700
          }}>
            HRMS
          </span>
        </div>
      </NavLink>

      {/* Main Top Navigation */}
      <nav className="main-nav">
        <NavLink to="/" end className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          Home
        </NavLink>
        <NavLink to="/self-service" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          Self-Service
        </NavLink>
        <NavLink to="/leave-entitlements" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          Leave Balances
        </NavLink>
        <NavLink to="/workforce-hub" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          Workforce
        </NavLink>
        <NavLink to="/calendar" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
          Calendar
        </NavLink>
        {isAdmin() ? (
          <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
            Admin Console
          </NavLink>
        ) : (
          <NavLink to="/view-leaves" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
            My Requests
          </NavLink>
        )}
      </nav>

      {/* Right Tools Section (Matching Reference Images 1, 3, 4) */}
      <div className="auth-section">
        {user ? (
          <>
            {/* Quick Action (+) Button */}
            <div style={{ position: 'relative' }}>
              <button 
                type="button" 
                onClick={() => setShowQuickMenu(!showQuickMenu)}
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  border: '1px solid #cbd5e1',
                  background: '#ffffff',
                  color: '#475569',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                  fontSize: '0.95rem'
                }}
                title="Quick Add Action"
              >
                <FaPlus />
              </button>

              {showQuickMenu && (
                <div style={{
                  position: 'absolute',
                  top: '46px',
                  right: 0,
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: '12px',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
                  padding: '8px',
                  width: '200px',
                  zIndex: 200,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px'
                }}>
                  <Link 
                    to="/add-leave" 
                    onClick={() => setShowQuickMenu(false)}
                    style={{ padding: '8px 12px', borderRadius: '8px', textDecoration: 'none', color: '#1e293b', fontSize: '0.86rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                  >
                    <FaCalendarAlt style={{ color: '#0284c7' }} /> Apply for Leave
                  </Link>
                  <Link 
                    to="/leave-policy-wizard" 
                    onClick={() => setShowQuickMenu(false)}
                    style={{ padding: '8px 12px', borderRadius: '8px', textDecoration: 'none', color: '#1e293b', fontSize: '0.86rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                  >
                    <FaCogs style={{ color: '#9333ea' }} /> Configure Policy
                  </Link>
                  <Link 
                    to="/self-service" 
                    onClick={() => setShowQuickMenu(false)}
                    style={{ padding: '8px 12px', borderRadius: '8px', textDecoration: 'none', color: '#1e293b', fontSize: '0.86rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                  >
                    <FaUserCheck style={{ color: '#16a34a' }} /> Self-Service Portal
                  </Link>
                </div>
              )}
            </div>

            {/* Help / Guide */}
            <Link 
              to="/about"
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                color: '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                textDecoration: 'none',
                fontSize: '1rem'
              }}
              title="Help & Architecture"
            >
              <FaQuestionCircle />
            </Link>

            {/* Notification Bell with 99+ Badge */}
            <NavLink to="/notifications" className="notification-bell-btn" title="Workforce Notifications">
              <FaBell />
              <span style={{
                position: 'absolute',
                top: '-4px',
                right: '-4px',
                background: '#0284c7',
                color: '#ffffff',
                fontSize: '0.65rem',
                fontWeight: 800,
                padding: '1px 5px',
                borderRadius: '9999px',
                border: '2px solid #ffffff'
              }}>
                99+
              </span>
            </NavLink>

            {/* User Profile Avatar with Initials/Photo */}
            <div className="user-profile-badge">
              <div className="user-avatar-circle">
                {(user.name || user.username || "U").charAt(0).toUpperCase()}
              </div>
              <div className="user-info-text">
                <span className="user-name-label">{user.name || user.username}</span>
                <span className="user-role-pill">{user.role || "Employee"}</span>
              </div>
            </div>

            {/* Logout Button */}
            <button onClick={handleLogout} className="auth-button logout">
              <FaDoorOpen />
              <span>Logout</span>
            </button>
          </>
        ) : (
          <NavLink to="/login" className="auth-button login">
            <FaUserCircle />
            <span>Sign In</span>
          </NavLink>
        )}
      </div>
    </header>
  );
};

export default Header;