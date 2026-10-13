import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getAllLeaves, deleteLeave } from "../services/leaveService";
import { useAuth } from "./AuthContext";
import { FaCalendarAlt, FaEnvelopeOpenText, FaTrash, FaEdit, FaPlus, FaCheck, FaCopy } from "react-icons/fa";
import "./ViewLeaves.css";

const ViewLeaves = () => {
  const { user, isAdmin } = useAuth();
  const [leaves, setLeaves] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [selectedOooLeave, setSelectedOooLeave] = useState(null);
  const [oooText, setOooText] = useState("");
  const [copySuccess, setCopySuccess] = useState(false);

  useEffect(() => {
    fetchLeaves();
  }, []);

  const fetchLeaves = async () => {
    setIsLoading(true);
    let allLeaves = [];
    let page = 0;
    let hasMore = true;

    try {
      while (hasMore) {
        const response = await getAllLeaves(page, 20);
        const data = response.data.content || response.data || [];
        allLeaves = [...allLeaves, ...data];
        hasMore = response.data.last !== undefined ? !response.data.last : false;
        page++;
        if (page > 10) break;
      }
      setLeaves(allLeaves);
    } catch (error) {
      console.error("Error fetching leaves:", error);
      setLeaves([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to cancel this leave request?")) {
      try {
        await deleteLeave(id);
        fetchLeaves();
      } catch (error) {
        console.error("Failed to delete leave:", error);
      }
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      return new Date(dateString).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
      return dateString;
    }
  };

  // Filter leaves: if not admin, show user's leaves
  const displayedLeaves = leaves.filter((leave) => {
    const matchesUser = isAdmin() || !user || !leave.employeeName || 
      leave.employeeName.toLowerCase() === (user.username || user.name || "").toLowerCase();
    
    const matchesStatus = filterStatus === "ALL" || 
      leave.status?.toUpperCase() === filterStatus;

    return matchesUser && matchesStatus;
  });

  // Calculate quota numbers
  const approvedDays = displayedLeaves
    .filter(l => l.status === 'Approved')
    .reduce((sum, l) => {
      const start = new Date(l.startDate);
      const end = new Date(l.endDate);
      const days = Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1);
      return sum + (l.daysCount || days);
    }, 0);

  const pendingDays = displayedLeaves
    .filter(l => l.status === 'Pending')
    .reduce((sum, l) => {
      const start = new Date(l.startDate);
      const end = new Date(l.endDate);
      const days = Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1);
      return sum + (l.daysCount || days);
    }, 0);

  const totalQuota = 20;
  const remainingDays = Math.max(0, totalQuota - approvedDays);

  const handleGenerateOoo = (leave) => {
    setSelectedOooLeave(leave);
    const startDateStr = formatDate(leave.startDate);
    const endDateStr = formatDate(leave.endDate);
    const name = leave.employeeName || user?.name || "Team Member";
    
    const generated = `Subject: Out of Office: ${name} (${startDateStr} - ${endDateStr})

Hello,

Thank you for reaching out. I am currently out of the office on approved ${leave.leaveType || 'leave'} from ${startDateStr} until ${endDateStr} with limited access to email.

For any urgent matters regarding ongoing projects, please contact our team lead or project manager. Otherwise, I will respond to your email promptly upon my return on ${endDateStr}.

Best regards,
${name}
LeaveTracker Automated Workforce Notification`;

    setOooText(generated);
    setCopySuccess(false);
  };

  const handleCopyOoo = () => {
    navigator.clipboard.writeText(oooText);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2500);
  };

  return (
    <div className="view-leaves-container">
      <div className="view-leaves-header">
        <div>
          <h1 className="view-leaves-title">
            {isAdmin() ? "Organization Leave Records" : "My Leave History"}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.95rem', margin: '4px 0 0 0' }}>
            Monitor approval status, date windows, and generate automated out-of-office handoffs.
          </p>
        </div>
        <div className="view-leaves-header-actions">
          <Link to="/add-leave" className="btn-add-leave">
            <FaPlus /> Apply for Leave
          </Link>
        </div>
      </div>

      {/* Quota Summary Strip */}
      <div className="quota-summary-strip">
        <div className="quota-mini-card">
          <span className="quota-mini-label">Annual Quota</span>
          <span className="quota-mini-val">{totalQuota} Days</span>
        </div>
        <div className="quota-mini-card">
          <span className="quota-mini-label">Approved & Used</span>
          <span className="quota-mini-val" style={{ color: '#059669' }}>{approvedDays} Days</span>
        </div>
        <div className="quota-mini-card">
          <span className="quota-mini-label">Pending Approval</span>
          <span className="quota-mini-val" style={{ color: '#d97706' }}>{pendingDays} Days</span>
        </div>
        <div className="quota-mini-card">
          <span className="quota-mini-label">Remaining Balance</span>
          <span className="quota-mini-val" style={{ color: '#6366f1' }}>{remainingDays} Days</span>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="leaves-filter-bar">
        {["ALL", "PENDING", "APPROVED", "REJECTED"].map((status) => (
          <button
            key={status}
            onClick={() => setFilterStatus(status)}
            className={`filter-pill-btn ${filterStatus === status ? "active" : ""}`}
          >
            {status === "ALL" ? "All Requests" : status.charAt(0) + status.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="no-leaves-container">
          <h2>Loading Leave Records...</h2>
          <p>Connecting to backend database.</p>
        </div>
      ) : displayedLeaves.length > 0 ? (
        <div className="leaves-grid">
          {displayedLeaves.map((leave) => (
            <div key={leave.id} className="leave-card">
              <div className="leave-card-header">
                <span className="leave-type-tag">
                  {leave.leaveType || 'Casual Leave'}
                </span>
                <span className={`status-badge status-${leave.status ? leave.status.toLowerCase() : 'pending'}`}>
                  {leave.status || 'Pending'}
                </span>
              </div>

              <div className="leave-card-body">
                <h3 className="leave-employee-label">{leave.employeeName}</h3>
                
                <div className="leave-dates-row">
                  <FaCalendarAlt style={{ color: '#6366f1' }} />
                  <span>{formatDate(leave.startDate)} – {formatDate(leave.endDate)}</span>
                </div>

                <div className="leave-reason-text">
                  "{leave.reason}"
                </div>

                {leave.status === 'Approved' && (
                  <button
                    type="button"
                    onClick={() => handleGenerateOoo(leave)}
                    className="btn-action btn-ooo-ai"
                    style={{ alignSelf: 'flex-start', marginTop: 4 }}
                  >
                    <FaEnvelopeOpenText /> AI Out-of-Office Draft
                  </button>
                )}
              </div>

              <div className="leave-card-footer">
                <Link to={`/update-leave/${leave.id}`} className="btn-action btn-edit">
                  <FaEdit /> Edit
                </Link>
                <button onClick={() => handleDelete(leave.id)} className="btn-action btn-delete">
                  <FaTrash /> Cancel
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="no-leaves-container">
          <h2>No Leave Requests Found</h2>
          <p>There are no leave requests matching the selected filter.</p>
          <Link to="/add-leave" className="btn-add-leave">Create a New Request</Link>
        </div>
      )}

      {/* AI OOO Modal */}
      {selectedOooLeave && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.75)',
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
            maxWidth: '580px',
            width: '100%',
            padding: '28px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            border: '1px solid #e2e8f0'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.25rem', fontWeight: 800 }}>
                <FaEnvelopeOpenText style={{ color: '#10b981' }} /> Generated Out-of-Office Email
              </h3>
              <button
                onClick={() => setSelectedOooLeave(null)}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', fontSize: '1.4rem', cursor: 'pointer', lineHeight: 1 }}
              >
                ✕
              </button>
            </div>
            
            <p style={{ color: '#64748b', fontSize: '0.9rem', margin: '0 0 16px 0', lineHeight: 1.5 }}>
              Your personalized out-of-office message has been drafted based on your approved dates:
            </p>

            <textarea
              value={oooText}
              onChange={(e) => setOooText(e.target.value)}
              rows={9}
              style={{
                width: '100%',
                background: '#f8fafc',
                color: '#1e293b',
                border: '1px solid #cbd5e1',
                borderRadius: '12px',
                padding: '14px',
                fontSize: '0.88rem',
                lineHeight: '1.6',
                fontFamily: 'monospace',
                resize: 'vertical',
                boxSizing: 'border-box'
              }}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
              <button
                onClick={() => setSelectedOooLeave(null)}
                style={{ padding: '9px 18px', background: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 600 }}
              >
                Close
              </button>
              <button
                onClick={handleCopyOoo}
                style={{
                  padding: '9px 20px',
                  background: copySuccess ? '#10b981' : '#4f46e5',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
              >
                {copySuccess ? <><FaCheck /> Copied to Clipboard!</> : <><FaCopy /> Copy Auto-Responder</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ViewLeaves;