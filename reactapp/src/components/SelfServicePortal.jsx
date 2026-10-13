import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { 
  FaUser, 
  FaTint, 
  FaBirthdayCake, 
  FaLaptopCode, 
  FaStar, 
  FaMapMarkerAlt, 
  FaCalendarCheck, 
  FaClock, 
  FaFileInvoiceDollar, 
  FaFileAlt
} from "react-icons/fa";
import "./SelfServicePortal.css";

const SelfServicePortal = () => {
  const { user } = useAuth();
  const [isCheckedIn, setIsCheckedIn] = useState(true);
  const [showPayslipModal, setShowPayslipModal] = useState(false);

  // Interactive Emoji Reactions (Image 5)
  const [reactions, setReactions] = useState([
    { emoji: "👍", count: 14, userReacted: false },
    { emoji: "👏", count: 9, userReacted: false },
    { emoji: "❤️", count: 22, userReacted: false },
    { emoji: "💡", count: 7, userReacted: false },
    { emoji: "🤔", count: 3, userReacted: false },
    { emoji: "😂", count: 18, userReacted: false },
    { emoji: "😢", count: 1, userReacted: false }
  ]);

  const handleToggleReaction = (index) => {
    setReactions(prev => prev.map((r, i) => {
      if (i === index) {
        return {
          ...r,
          count: r.userReacted ? r.count - 1 : r.count + 1,
          userReacted: !r.userReacted
        };
      }
      return r;
    }));
  };

  const employeeName = user?.name || user?.username || "Emily Dickinson";

  return (
    <div className="ess-portal-container">
      {/* Page Header */}
      <div className="ess-portal-header">
        <div>
          <h1 className="ess-portal-title">Employee Self-Service Portal</h1>
          <p className="ess-portal-subtitle">
            Manage everyday HR tasks, payslips, leave balances, attendance, and team communications.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button 
            type="button" 
            onClick={() => setShowPayslipModal(true)}
            style={{
              padding: '9px 18px',
              borderRadius: '10px',
              border: '1px solid #cbd5e1',
              background: '#ffffff',
              color: '#0f172a',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <FaFileInvoiceDollar style={{ color: '#0284c7' }} /> View Payslips
          </button>
          <Link to="/add-leave" className="btn-view-profile-orange" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '6px' }}>
            + Request Time Off
          </Link>
        </div>
      </div>

      {/* Top Layout Grid: Personal Information & Announcements */}
      <div className="ess-layout-grid">
        
        {/* Personal Information Card (Matching Reference Image 5) */}
        <div className="personal-info-card">
          <h3 className="personal-info-card-header">Personal Information</h3>
          <div className="personal-info-body">
            <div className="profile-avatar-container">
              <img 
                src="https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=300&q=80" 
                alt="Profile" 
                className="profile-avatar-img"
              />
              <button 
                type="button" 
                className="btn-view-profile-orange"
                onClick={() => alert(`Employee ID: EMP-2024-91\nName: ${employeeName}\nDepartment: Quality Assurance\nStatus: Active (Verified)`)}
              >
                View Profile
              </button>
            </div>

            <div className="personal-details-list">
              <div className="detail-line-item">
                <FaUser className="detail-icon" />
                <span>{employeeName}</span>
              </div>
              <div className="detail-line-item">
                <FaTint className="detail-icon" style={{ color: '#ef4444' }} />
                <span>Blood Group: <strong>B+</strong></span>
              </div>
              <div className="detail-line-item">
                <FaBirthdayCake className="detail-icon" style={{ color: '#f59e0b' }} />
                <span>DOB: <strong>4 September 1998</strong></span>
              </div>
              <div className="detail-line-item">
                <FaLaptopCode className="detail-icon" style={{ color: '#6366f1' }} />
                <span>Role: <strong>Quality Analyst</strong></span>
              </div>
              <div className="detail-line-item">
                <FaStar className="detail-icon" style={{ color: '#eab308' }} />
                <span>Experience: <strong>6 year(s) 9 month(s) 15 day(s)</strong></span>
              </div>
              <div className="detail-line-item">
                <FaMapMarkerAlt className="detail-icon" style={{ color: '#10b981' }} />
                <span>Location: <strong>Bangalore</strong></span>
              </div>
              <div className="detail-line-item">
                <FaCalendarCheck className="detail-icon" style={{ color: '#0284c7' }} />
                <span>Joining Date: <strong>30 October 2019</strong></span>
              </div>
            </div>
          </div>
        </div>

        {/* Announcements Card (Matching Reference Image 5) */}
        <div className="announcements-card">
          <h3 className="announcements-card-header">Announcements</h3>
          <div className="author-meta-row">
            <img 
              src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80" 
              alt="Author" 
              className="author-avatar-small"
            />
            <div className="author-info-text">
              <h4>Alen Alexander</h4>
              <span>Sunday, 24 January 2026</span>
            </div>
          </div>
          <p className="announcement-body-text">
            Looking for creative and fun ideas to celebrate fun Fridays to boost our employee happiness and engagement. Your great ideas are extremely welcome right now.
          </p>

          {/* Emoji Reactions Bar */}
          <div className="emoji-reactions-bar">
            {reactions.map((r, idx) => (
              <button 
                key={idx}
                type="button" 
                className="reaction-emoji-btn"
                onClick={() => handleToggleReaction(idx)}
                style={{ backgroundColor: r.userReacted ? 'rgba(99, 102, 241, 0.15)' : 'transparent' }}
                title={`Reaction ${r.emoji}`}
              >
                <span>{r.emoji}</span>
                <span className="reaction-counter">{r.count}</span>
              </button>
            ))}
          </div>
        </div>

      </div>

      {/* Secondary Grid: Leave Balance & My Attendance */}
      <div className="ess-secondary-grid">
        
        {/* Leave Balance Card (Matching Reference Image 5) */}
        <div className="leave-balance-card">
          <div className="balance-card-header">
            <h3>Leave Balance</h3>
            <Link to="/leave-entitlements" className="view-all-link">
              View all &rarr;
            </Link>
          </div>

          <div className="balance-boxes-row">
            <div className="balance-mini-box">
              <span className="balance-mini-box-label">Casual Leave</span>
              <span className="balance-mini-icon">🗓️</span>
              <span className="balance-mini-box-val">06</span>
              <span className="balance-mini-box-sub">Days</span>
            </div>
            <div className="balance-mini-box">
              <span className="balance-mini-box-label">Sick Leave</span>
              <span className="balance-mini-icon">📋</span>
              <span className="balance-mini-box-val">04</span>
              <span className="balance-mini-box-sub">Days</span>
            </div>
            <div className="balance-mini-box">
              <span className="balance-mini-box-label">Annual Leave</span>
              <span className="balance-mini-icon">🏖️</span>
              <span className="balance-mini-box-val">12</span>
              <span className="balance-mini-box-sub">Days</span>
            </div>
            <div className="balance-mini-box">
              <span className="balance-mini-box-label">Comp Off</span>
              <span className="balance-mini-icon">⛵</span>
              <span className="balance-mini-box-val">01</span>
              <span className="balance-mini-box-sub">Days</span>
            </div>
          </div>
        </div>

        {/* My Attendance Card (Matching Reference Image 5) */}
        <div className="attendance-card">
          <div className="attendance-card-header">
            <h3>My attendance</h3>
            <div className="attendance-radio-group">
              <label className="attendance-radio-label">
                <input 
                  type="radio" 
                  name="attStatus" 
                  checked={isCheckedIn} 
                  onChange={() => setIsCheckedIn(true)}
                />
                <span style={{ color: '#10b981', fontWeight: 700 }}>● Check In</span>
              </label>
              <label className="attendance-radio-label">
                <input 
                  type="radio" 
                  name="attStatus" 
                  checked={!isCheckedIn} 
                  onChange={() => setIsCheckedIn(false)}
                />
                <span style={{ color: '#ea580c', fontWeight: 700 }}>● Check Out</span>
              </label>
            </div>
          </div>

          {/* Timeline slider representation */}
          <div className="timeline-track-container">
            <div className="timeline-line"></div>
            <div className="timeline-marker-dots">
              <span className="timeline-marker-dot" title="9.59 AM"></span>
              <span className="timeline-marker-dot" title="11.30 AM"></span>
              <span className="timeline-marker-dot" title="11.45 AM"></span>
              <span className="timeline-marker-dot" title="02.00 PM"></span>
              <span className="timeline-marker-dot" title="03.00 PM"></span>
              <span className="timeline-marker-dot" title="07.30 PM"></span>
            </div>
            <div className="timeline-marker-labels">
              <span>9.59 AM</span>
              <span>11.30 AM</span>
              <span>11.45 AM</span>
              <span>02.00 PM</span>
              <span>03.00 PM</span>
              <span>07.30 PM</span>
            </div>
          </div>

          {/* Total In & Late By Stat Chips */}
          <div className="attendance-stat-chips-row">
            <div className="att-stat-chip total-in">
              <FaClock style={{ fontSize: '1.6rem' }} />
              <div className="att-stat-text">
                <h5>Total in</h5>
                <p>8h 3m 4s</p>
              </div>
            </div>
            <div className="att-stat-chip late-by">
              <span style={{ fontSize: '1.6rem' }}>🐢</span>
              <div className="att-stat-text">
                <h5>Late by</h5>
                <p>30mins</p>
              </div>
            </div>
          </div>
        </div>

      </div>

      {/* Leave Requests Table Card (Matching Reference Image 5) */}
      <div className="leave-requests-card">
        <h3 className="requests-card-title">Leave Requests</h3>
        <div style={{ overflowX: 'auto' }}>
          <table className="requests-custom-table">
            <thead>
              <tr>
                <th>Start date</th>
                <th>End date</th>
                <th>Leave type</th>
                <th>Applied by</th>
                <th>Approval status</th>
                <th>Count</th>
                <th>Lop days</th>
                <th>Applied date</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>11 Aug 2026</td>
                <td>11 Aug 2026</td>
                <td><span style={{ fontWeight: 600, color: '#0284c7' }}>LOP</span></td>
                <td>Steve wilson</td>
                <td>
                  <span style={{ color: '#10b981', fontWeight: 700 }}>
                    <span className="status-dot-active"></span>Approved
                  </span>
                </td>
                <td>1 day(s)</td>
                <td>1</td>
                <td>10 Aug 2026</td>
              </tr>
              <tr>
                <td>20 Oct 2026</td>
                <td>24 Oct 2026</td>
                <td><span style={{ fontWeight: 600, color: '#0284c7' }}>Casual Leave</span></td>
                <td>{employeeName}</td>
                <td>
                  <span style={{ color: '#f59e0b', fontWeight: 700 }}>
                    <span className="status-dot-active" style={{ background: '#f59e0b' }}></span>Pending
                  </span>
                </td>
                <td>5 day(s)</td>
                <td>0</td>
                <td>13 Oct 2026</td>
              </tr>
              <tr>
                <td>15 Oct 2026</td>
                <td>16 Oct 2026</td>
                <td><span style={{ fontWeight: 600, color: '#0284c7' }}>Sick Leave</span></td>
                <td>Sam Taylor</td>
                <td>
                  <span style={{ color: '#10b981', fontWeight: 700 }}>
                    <span className="status-dot-active"></span>Approved
                  </span>
                </td>
                <td>2 day(s)</td>
                <td>0</td>
                <td>12 Oct 2026</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Payslip Modal (Encrypted Document Storage) */}
      {showPayslipModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(15, 23, 42, 0.7)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '20px'
        }}>
          <div style={{
            background: '#ffffff',
            borderRadius: '20px',
            maxWidth: '520px',
            width: '100%',
            padding: '28px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.25)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FaFileInvoiceDollar style={{ color: '#0284c7' }} /> Encrypted Payslip Vault
              </h3>
              <button 
                onClick={() => setShowPayslipModal(false)}
                style={{ background: 'none', border: 'none', fontSize: '1.4rem', cursor: 'pointer', color: '#94a3b8' }}
              >
                ✕
              </button>
            </div>
            <p style={{ color: '#64748b', fontSize: '0.88rem', marginBottom: '20px' }}>
              Access password-protected monthly salary statements and tax deduction breakdowns.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {["September 2026 (Latest)", "August 2026", "July 2026"].map((m, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 14px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <FaFileAlt style={{ color: '#0284c7' }} />
                    <span style={{ fontSize: '0.9rem', fontWeight: 600, color: '#1e293b' }}>Payslip - {m}</span>
                  </div>
                  <button 
                    onClick={() => alert(`Downloading 256-bit AES encrypted PDF for ${m}`)}
                    style={{ background: '#0284c7', color: '#fff', border: 'none', padding: '6px 12px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 600, cursor: 'pointer' }}
                  >
                    Download PDF
                  </button>
                </div>
              ))}
            </div>

            <div style={{ marginTop: '24px', textAlign: 'right' }}>
              <button 
                onClick={() => setShowPayslipModal(false)}
                style={{ padding: '8px 18px', background: '#e2e8f0', color: '#475569', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default SelfServicePortal;
