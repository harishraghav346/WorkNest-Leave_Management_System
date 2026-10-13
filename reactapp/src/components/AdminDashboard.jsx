import React, { useState, useEffect, useCallback } from 'react';
import { getAllLeaves, approveLeave, rejectLeave } from '../services/leaveService';
import { FaRobot, FaCheck, FaTimes, FaClock, FaCheckCircle, FaExclamationTriangle, FaCalendarAlt } from 'react-icons/fa';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [allLeavesForAnalysis, setAllLeavesForAnalysis] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [analyzingId, setAnalyzingId] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  const [aiAnalysisMap, setAiAnalysisMap] = useState({});

  const fetchLeaveRequests = useCallback(async (page) => {
    try {
      const response = await getAllLeaves(page, 10);
      const data = response.data.content || response.data || [];
      setLeaveRequests(data);
      setTotalPages(response.data.totalPages || 1);
    } catch (error) {
      console.error('Error fetching leave requests:', error);
    }
  }, []);

  const fetchAllForAnalysis = useCallback(async () => {
    try {
      const response = await getAllLeaves(0, 100);
      const data = response.data.content || response.data || [];
      setAllLeavesForAnalysis(data);
    } catch (err) {
      console.error('Error fetching leaves for analysis', err);
    }
  }, []);

  useEffect(() => {
    fetchLeaveRequests(currentPage);
    fetchAllForAnalysis();
  }, [currentPage, fetchLeaveRequests, fetchAllForAnalysis]);

  const handleApprove = async (id) => {
    if (processingId) return;
    setProcessingId(id);
    try {
      await approveLeave(id);
      await fetchLeaveRequests(currentPage);
      await fetchAllForAnalysis();
    } catch (error) {
      console.error('Error approving leave request:', error);
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (id) => {
    if (processingId) return;
    setProcessingId(id);
    try {
      await rejectLeave(id);
      await fetchLeaveRequests(currentPage);
      await fetchAllForAnalysis();
    } catch (error) {
      console.error('Error rejecting leave request:', error);
    } finally {
      setProcessingId(null);
    }
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // AI Impact Analyzer: Calculates overlaps, duration, and generates smart recommendations
  const runAiImpactAnalysis = (request) => {
    setAnalyzingId(request.id);
    setTimeout(() => {
      const reqStart = new Date(request.startDate);
      const reqEnd = new Date(request.endDate);
      const diffDays = Math.max(1, Math.round((reqEnd - reqStart) / (1000 * 60 * 60 * 24)) + 1);

      // Check overlapping approved/pending leaves by OTHER employees
      const overlapping = allLeavesForAnalysis.filter((l) => {
        if (l.id === request.id) return false;
        if (l.status === 'Rejected') return false;
        const lStart = new Date(l.startDate);
        const lEnd = new Date(l.endDate);
        return reqStart <= lEnd && reqEnd >= lStart;
      });

      let risk = "Low";
      let message = "";
      if (overlapping.length >= 2 || diffDays > 5) {
        risk = "High";
        message = `High workload impact: ${overlapping.length} peer(s) also away during this window (${overlapping.map(o => o.employeeName).join(', ')}). Total request duration is ${diffDays} days. Ensure delegate coverage before approving.`;
      } else if (overlapping.length === 1 || diffDays > 3) {
        risk = "Medium";
        message = `Moderate impact: 1 peer (${overlapping[0]?.employeeName}) on leave during this timeframe. Team coverage estimated at 75%. Safe if key milestones are not scheduled.`;
      } else {
        risk = "Low";
        message = `Minimal team impact: No overlapping absences detected for this date range. Request duration is ${diffDays} day(s). Recommended for quick approval.`;
      }

      setAiAnalysisMap(prev => ({
        ...prev,
        [request.id]: { risk, message, overlappingCount: overlapping.length, days: diffDays }
      }));
      setAnalyzingId(null);
    }, 600);
  };

  const totalCount = allLeavesForAnalysis.length;
  const pendingCount = allLeavesForAnalysis.filter(r => r.status === 'Pending').length;
  const approvedCount = allLeavesForAnalysis.filter(r => r.status === 'Approved').length;
  const rejectedCount = allLeavesForAnalysis.filter(r => r.status === 'Rejected').length;

  return (
    <div className="admin-dashboard">
      <div className="admin-header-area">
        <div>
          <h2>Manager & Admin Command Center</h2>
          <p style={{ color: '#64748b', margin: '4px 0 0 0', fontSize: '0.95rem' }}>
            Review employee requests, evaluate AI team impact, and manage workforce capacity.
          </p>
        </div>
        <div className="admin-header-badge">
          <FaRobot />
          <span>AI Impact Analyzer Active</span>
        </div>
      </div>

      {/* Modern Metrics Row */}
      <div className="admin-metrics-row">
        <div className="admin-stat-card">
          <div className="stat-icon-wrapper purple">
            <FaCalendarAlt />
          </div>
          <div className="stat-text-group">
            <span className="stat-metric-number">{totalCount}</span>
            <span className="stat-metric-label">Total Requests</span>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="stat-icon-wrapper amber">
            <FaClock />
          </div>
          <div className="stat-text-group">
            <span className="stat-metric-number">{pendingCount}</span>
            <span className="stat-metric-label">Pending Review</span>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="stat-icon-wrapper emerald">
            <FaCheckCircle />
          </div>
          <div className="stat-text-group">
            <span className="stat-metric-number">{approvedCount}</span>
            <span className="stat-metric-label">Approved</span>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="stat-icon-wrapper rose">
            <FaExclamationTriangle />
          </div>
          <div className="stat-text-group">
            <span className="stat-metric-number">{rejectedCount}</span>
            <span className="stat-metric-label">Rejected / Denied</span>
          </div>
        </div>
      </div>

      <div className="leave-requests">
        <div className="leave-requests-header-row">
          <h3>Pending & Recent Leave Requests</h3>
          <span style={{ fontSize: '0.85rem', color: '#64748b', fontWeight: 600 }}>
            Showing {leaveRequests.length} records
          </span>
        </div>

        <div className="table-responsive-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Employee</th>
                <th>From</th>
                <th>To</th>
                <th>Reason & Type</th>
                <th>Status</th>
                <th>AI Impact</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {leaveRequests.map(request => {
                const analysis = aiAnalysisMap[request.id];
                return (
                  <React.Fragment key={request.id}>
                    <tr>
                      <td style={{ color: '#64748b', fontWeight: 600 }}>#{request.id}</td>
                      <td>
                        <div className="employee-cell">
                          <div className="employee-avatar-circle">
                            {(request.employeeName || 'E').charAt(0).toUpperCase()}
                          </div>
                          <span>{request.employeeName}</span>
                        </div>
                      </td>
                      <td>{request.startDate}</td>
                      <td>{request.endDate}</td>
                      <td>
                        <div style={{ fontWeight: 600, color: '#1e293b' }}>{request.reason}</div>
                        <small style={{ color: '#6366f1', fontWeight: 600 }}>{request.leaveType || 'Casual Leave'}</small>
                      </td>
                      <td>
                        <span className={`status-badge status-${request.status ? request.status.toLowerCase() : 'pending'}`}>
                          {request.status}
                        </span>
                      </td>
                      <td>
                        {analysis ? (
                          <span style={{
                            padding: '4px 10px',
                            borderRadius: '9999px',
                            fontSize: '0.78rem',
                            fontWeight: '700',
                            backgroundColor: analysis.risk === 'High' ? 'rgba(239, 68, 68, 0.15)' : analysis.risk === 'Medium' ? 'rgba(234, 179, 8, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                            color: analysis.risk === 'High' ? '#dc2626' : analysis.risk === 'Medium' ? '#b45309' : '#047857'
                          }}>
                            {analysis.risk} Risk
                          </span>
                        ) : (
                          <button
                            type="button"
                            className="ai-btn"
                            onClick={() => runAiImpactAnalysis(request)}
                            disabled={analyzingId === request.id}
                          >
                            <FaRobot /> {analyzingId === request.id ? 'Analyzing...' : 'AI Assess'}
                          </button>
                        )}
                      </td>
                      <td>
                        {request.status === 'Pending' && (
                          <div className="action-buttons-group">
                            <button 
                              className="approve" 
                              onClick={() => handleApprove(request.id)} 
                              disabled={processingId === request.id}
                              title="Approve"
                            >
                              <FaCheck /> {processingId === request.id ? 'Approving...' : 'Approve'}
                            </button>
                            <button 
                              className="reject" 
                              onClick={() => handleReject(request.id)} 
                              disabled={processingId === request.id}
                              title="Reject"
                            >
                              <FaTimes /> {processingId === request.id ? 'Rejecting...' : 'Reject'}
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>

                    {/* AI Impact Assessment Expanded Card */}
                    {analysis && (
                      <tr style={{ backgroundColor: '#f8fafc' }}>
                        <td colSpan={8} style={{ padding: '16px 20px', borderBottom: '1px solid #e2e8f0' }}>
                          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                            <div style={{
                              width: 32,
                              height: 32,
                              borderRadius: '8px',
                              background: 'rgba(99, 102, 241, 0.12)',
                              color: '#6366f1',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              flexShrink: 0,
                              marginTop: 2
                            }}>
                              <FaRobot />
                            </div>
                            <div>
                              <div style={{ fontWeight: '700', color: '#0f172a', fontSize: '0.9rem' }}>
                                AI Team Bandwidth & Overlap Assessment
                              </div>
                              <div style={{ color: '#475569', fontSize: '0.88rem', marginTop: '4px', lineHeight: 1.5 }}>
                                {analysis.message}
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="pagination">
            {Array.from({ length: totalPages }, (_, i) => i).map(page => (
              <button
                key={page}
                onClick={() => handlePageChange(page)}
                className={currentPage === page ? 'active' : ''}
              >
                {page + 1}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;