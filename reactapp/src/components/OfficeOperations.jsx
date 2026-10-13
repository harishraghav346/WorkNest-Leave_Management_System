import React, { useState } from "react";
import { FaRobot, FaBullhorn } from "react-icons/fa";

const OfficeOperations = () => {
  const [autoApprove1Day, setAutoApprove1Day] = useState(true);
  const [delegateAlerts, setDelegateAlerts] = useState(true);
  const [announcementText, setAnnouncementText] = useState("");
  const [postedAnnouncement, setPostedAnnouncement] = useState(null);

  const handlePostAnnouncement = (e) => {
    e.preventDefault();
    if (announcementText.trim()) {
      setPostedAnnouncement({
        text: announcementText,
        time: "Just now",
        author: "HR Operations"
      });
      setAnnouncementText("");
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '2.5rem 2rem 5rem' }}>
      <div style={{ marginBottom: '2.5rem' }}>
        <h1 style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--slate-900)', margin: 0 }}>
          Streamlined Office Operations & Automation
        </h1>
        <p style={{ color: 'var(--slate-500)', fontSize: '0.95rem', marginTop: '4px' }}>
          Automates repetitive tasks, schedules handover reminders, and organizes workplace communication efficiently.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
        
        {/* Workflow Automation Rules */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '20px', padding: '2rem', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.5rem' }}>
            <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(99, 102, 241, 0.1)', color: '#6366f1', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.3rem' }}>
              <FaRobot />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: '#0f172a' }}>
                Automated Operational Workflows
              </h3>
              <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Zero-friction routine process rules</span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            
            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong style={{ fontSize: '0.92rem', color: '#1e293b' }}>Instant AI Approval for 1-Day Leaves</strong>
                <p style={{ margin: '4px 0 0 0', fontSize: '0.8rem', color: '#64748b' }}>
                  Auto-approves single-day casual leaves if no peer overlaps exist.
                </p>
              </div>
              <input 
                type="checkbox" 
                checked={autoApprove1Day} 
                onChange={(e) => setAutoApprove1Day(e.target.checked)}
                style={{ width: 20, height: 20, cursor: 'pointer', accentColor: '#6366f1' }}
              />
            </div>

            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong style={{ fontSize: '0.92rem', color: '#1e293b' }}>Automated Handover & Delegate Alerts</strong>
                <p style={{ margin: '4px 0 0 0', fontSize: '0.8rem', color: '#64748b' }}>
                  Sends team notification 24 hours prior to employee absence.
                </p>
              </div>
              <input 
                type="checkbox" 
                checked={delegateAlerts} 
                onChange={(e) => setDelegateAlerts(e.target.checked)}
                style={{ width: 20, height: 20, cursor: 'pointer', accentColor: '#6366f1' }}
              />
            </div>

            <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong style={{ fontSize: '0.92rem', color: '#1e293b' }}>Monthly Balance Recalibration</strong>
                <p style={{ margin: '4px 0 0 0', fontSize: '0.8rem', color: '#64748b' }}>
                  Automatically credits monthly prorated days on the 1st of every month.
                </p>
              </div>
              <span style={{ color: '#10b981', fontWeight: 700, fontSize: '0.84rem' }}>Active ✓</span>
            </div>

          </div>
        </div>

        {/* Office Announcement Broadcast Publisher */}
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '20px', padding: '2rem', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.5rem' }}>
            <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(2, 132, 199, 0.1)', color: '#0284c7', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.3rem' }}>
              <FaBullhorn />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: '#0f172a' }}>
                Broadcast Announcement
              </h3>
              <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Notify all team members across offices</span>
            </div>
          </div>

          <form onSubmit={handlePostAnnouncement}>
            <textarea
              value={announcementText}
              onChange={(e) => setAnnouncementText(e.target.value)}
              placeholder="Type office-wide announcement, holiday schedule, or team engagement event..."
              rows={4}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '12px',
                border: '1px solid #cbd5e1',
                fontSize: '0.92rem',
                fontFamily: 'inherit',
                boxSizing: 'border-box',
                resize: 'vertical',
                marginBottom: '14px'
              }}
              required
            />
            <button
              type="submit"
              style={{
                width: '100%',
                padding: '11px',
                background: '#0284c7',
                color: '#fff',
                border: 'none',
                borderRadius: '10px',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              Publish to Workplace
            </button>
          </form>

          {postedAnnouncement && (
            <div style={{ marginTop: '16px', padding: '12px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '10px', color: '#166534', fontSize: '0.88rem' }}>
              ✓ Published: "{postedAnnouncement.text}"
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default OfficeOperations;
