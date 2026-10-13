import React, { useState } from "react";
import { Link } from "react-router-dom";
import { 
  FaUserFriends, 
  FaUserCheck, 
  FaChartLine, 
  FaHandshake, 
  FaCalendarCheck, 
  FaBookOpen, 
  FaClock, 
  FaComments,
  FaCheckCircle,
  FaArrowRight
} from "react-icons/fa";
import "./WorkforceHub.css";

const WorkforceHub = () => {
  const [selectedModule, setSelectedModule] = useState(null);

  const modules = [
    {
      id: "database",
      title: "Employee Database Management",
      desc: "Central repository of employee records, profiles, emergency contacts & documents.",
      theme: "pastel-blue",
      icon: <FaUserFriends style={{ color: '#0284c7' }} />,
      stats: "128 Active Employees across 4 global locations"
    },
    {
      id: "attendance",
      title: "Attendance Tracker",
      desc: "Geo-fenced clock-in, biometric syncing, and daily working hour timeline tracking.",
      theme: "pastel-green",
      icon: <FaUserCheck style={{ color: '#16a34a' }} />,
      stats: "96.4% On-Time rate this month"
    },
    {
      id: "performance",
      title: "Performance Management",
      desc: "Continuous 360 feedback, quarterly OKRs, appraisal cycles and goal tracking.",
      theme: "pastel-peach",
      icon: <FaChartLine style={{ color: '#d97706' }} />,
      stats: "Q4 Appraisal Cycle scheduled for next week"
    },
    {
      id: "onboarding",
      title: "Employee Onboarding",
      desc: "Automated digital workflows, equipment provisioning & welcome checklists.",
      theme: "pastel-pink",
      icon: <FaHandshake style={{ color: '#e11d48' }} />,
      stats: "3 New team members joining this Monday"
    },
    {
      id: "timesheets",
      title: "Timesheets",
      desc: "Project-level time logging, billable hour allocation & manager approvals.",
      theme: "pastel-yellow",
      icon: <FaClock style={{ color: '#ca8a04' }} />,
      stats: "98% Timesheets submitted on schedule"
    },
    {
      id: "lms",
      title: "Learning Management",
      desc: "Interactive skill courses, compliance certifications and career pathing.",
      theme: "pastel-rose",
      icon: <FaBookOpen style={{ color: '#db2777' }} />,
      stats: "18 Active training modules available"
    },
    {
      id: "scheduling",
      title: "Shift Scheduling",
      desc: "Roster planning, night shift differentials, shift swaps and availability.",
      theme: "pastel-cyan",
      icon: <FaCalendarCheck style={{ color: '#0284c7' }} />,
      stats: "Coverage optimal: 0 unfilled shifts"
    },
    {
      id: "cases",
      title: "Case management",
      desc: "Confidential HR ticketing, employee grievances, inquiries & SLA tracking.",
      theme: "pastel-mint",
      icon: <FaComments style={{ color: '#059669' }} />,
      stats: "Avg resolution time: under 4 business hours"
    }
  ];

  return (
    <div className="workforce-hub-page">
      <div className="workforce-header-area">
        <div>
          <h1 className="workforce-title">Unified Workforce Management</h1>
          <p className="workforce-subtitle">
            Brings employee records, documents, attendance, and task information into one connected environment.
          </p>
        </div>
        <div>
          <Link to="/self-service" style={{
            padding: '10px 20px',
            background: '#0284c7',
            color: '#fff',
            borderRadius: '10px',
            textDecoration: 'none',
            fontWeight: 700,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            Employee Self-Service &rarr;
          </Link>
        </div>
      </div>

      {/* 8-Card Feature Grid Matching Reference Image 2 */}
      <div className="workforce-grid">
        {modules.map((m) => (
          <div 
            key={m.id} 
            className={`workforce-card ${m.theme}`}
            onClick={() => setSelectedModule(m)}
          >
            <div className="workforce-icon-illustration">
              {m.icon}
            </div>
            <h3 className="workforce-card-title">{m.title}</h3>
            <p className="workforce-card-desc">{m.desc}</p>
          </div>
        ))}
      </div>

      {/* Connected Detail Modal */}
      {selectedModule && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(15, 23, 42, 0.75)',
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
            maxWidth: '540px',
            width: '100%',
            padding: '28px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.25)',
            border: '1px solid #e2e8f0'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{ width: 44, height: 44, borderRadius: 12, background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem' }}>
                  {selectedModule.icon}
                </div>
                <h3 style={{ margin: 0, color: '#0f172a', fontSize: '1.25rem', fontWeight: 800 }}>
                  {selectedModule.title}
                </h3>
              </div>
              <button 
                onClick={() => setSelectedModule(null)}
                style={{ background: 'none', border: 'none', fontSize: '1.4rem', cursor: 'pointer', color: '#94a3b8' }}
              >
                ✕
              </button>
            </div>

            <p style={{ color: '#475569', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '20px' }}>
              {selectedModule.desc}
            </p>

            <div style={{ padding: '14px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', marginBottom: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#16a34a', fontWeight: 700, fontSize: '0.88rem' }}>
                <FaCheckCircle /> Live Telemetry
              </div>
              <p style={{ margin: '6px 0 0 0', color: '#334155', fontSize: '0.92rem', fontWeight: 600 }}>
                {selectedModule.stats}
              </p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button 
                onClick={() => setSelectedModule(null)}
                style={{ padding: '9px 18px', background: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 600 }}
              >
                Close
              </button>
              <Link 
                to="/leave-entitlements"
                onClick={() => setSelectedModule(null)}
                style={{ padding: '9px 20px', background: '#0284c7', color: '#ffffff', borderRadius: '10px', textDecoration: 'none', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <span>Open in Workplace</span>
                <FaArrowRight />
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WorkforceHub;
