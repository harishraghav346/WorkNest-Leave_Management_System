import React, { useState, useEffect } from 'react';
import { getAllLeaves } from '../services/leaveService';
import { FaCalendarAlt, FaCheckCircle, FaTimesCircle, FaHourglassHalf, FaAward } from 'react-icons/fa';
import { useAuth } from './AuthContext';
import { Link } from 'react-router-dom';
import './Dashboard.css';

const Dashboard = () => {
    const { user, isAdmin } = useAuth();
    const [stats, setStats] = useState({
        total: 0,
        approved: 0,
        pending: 0,
        rejected: 0,
        quotaTotal: 20,
        quotaRemaining: 20
    });
    const [allLeaves, setAllLeaves] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAllLeaves = async () => {
            setLoading(true);
            try {
                let leaves = [];
                let currentPage = 0;
                let totalPages = 1;

                while (currentPage < totalPages && currentPage < 10) {
                    const response = await getAllLeaves(currentPage, 20);
                    const data = response.data;
                    if (data && data.content) {
                        leaves = leaves.concat(data.content);
                        totalPages = data.totalPages || 1;
                    } else if (Array.isArray(data)) {
                        leaves = data;
                        break;
                    } else {
                        break;
                    }
                    currentPage++;
                }

                // If regular employee, filter down to their leaves
                const userLeaves = isAdmin() || !user ? leaves : leaves.filter(l => 
                    l.employeeName?.toLowerCase() === (user.username || user.name || "").toLowerCase()
                );

                const total = userLeaves.length;
                const approved = userLeaves.filter(leave => leave.status === 'Approved').length;
                const pending = userLeaves.filter(leave => leave.status === 'Pending').length;
                const rejected = userLeaves.filter(leave => leave.status === 'Rejected').length;

                // Simple quota remaining estimation (1 day per approved leave)
                const quotaRemaining = Math.max(0, 20 - approved);

                setStats({ total, approved, pending, rejected, quotaTotal: 20, quotaRemaining });

                const sortedLeaves = [...userLeaves].sort((a, b) => new Date(b.startDate) - new Date(a.startDate));
                setAllLeaves(sortedLeaves);

            } catch (error) {
                console.error("Error fetching leaves:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchAllLeaves();
    }, [user, isAdmin]);

    if (loading) {
        return <div className="loading-message">Loading your dashboard...</div>;
    }

    return (
        <div className="dashboard-container blurry-background">
            <ul className="background">
                <li></li><li></li><li></li><li></li><li></li>
                <li></li><li></li><li></li><li></li><li></li>
            </ul>

            <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
                <div>
                    <h2 className="dashboard-title">
                        {user ? `${user.name || user.username}'s Leave Center` : 'Your Leave Dashboard'}
                    </h2>
                    <p className="dashboard-subtitle">Real-time overview of your leave requests, balance, and status.</p>
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                    <Link to="/add-leave" className="btn-add-leave" style={{ textDecoration: 'none' }}>
                        + Apply Leave
                    </Link>
                </div>
            </div>

            {/* Quota Banner */}
            <div style={{
                background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%)',
                border: '1px solid rgba(56, 189, 248, 0.3)',
                borderRadius: '12px',
                padding: '16px 24px',
                marginBottom: '24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '16px'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <div style={{ background: '#0284c7', padding: '12px', borderRadius: '10px', color: '#fff', fontSize: '1.4rem' }}>
                        <FaAward />
                    </div>
                    <div>
                        <div style={{ color: '#94a3b8', fontSize: '0.85rem' }}>Annual Leave Allowance</div>
                        <div style={{ color: '#f8fafc', fontSize: '1.25rem', fontWeight: 'bold' }}>
                            {stats.quotaRemaining} Days Remaining <span style={{ color: '#64748b', fontSize: '0.9rem', fontWeight: 'normal' }}>of {stats.quotaTotal} Days</span>
                        </div>
                    </div>
                </div>
                <div style={{ color: '#38bdf8', fontSize: '0.88rem' }}>
                    💡 Tip: Ask our AI Assistant below to check eligibility or book time off instantly.
                </div>
            </div>

            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon total"><FaCalendarAlt /></div>
                    <div className="stat-info">
                        <h3>Total Requests</h3>
                        <p>{stats.total}</p>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon approved"><FaCheckCircle /></div>
                    <div className="stat-info">
                        <h3>Approved</h3>
                        <p>{stats.approved}</p>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon pending"><FaHourglassHalf /></div>
                    <div className="stat-info">
                        <h3>Pending</h3>
                        <p>{stats.pending}</p>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon rejected"><FaTimesCircle /></div>
                    <div className="stat-info">
                        <h3>Rejected</h3>
                        <p>{stats.rejected}</p>
                    </div>
                </div>
            </div>

            {allLeaves.length > 0 ? (
                <div className="upcoming-leaves">
                    <h3 className="upcoming-leaves-title">Recent Leave Activity</h3>
                    <ul className="leaves-list">
                        {allLeaves.map(leave => (
                            <li key={leave.id} className="leave-item">
                                <div className="leave-details">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <p className="leave-employee-name">{leave.employeeName}</p>
                                        <span style={{ fontSize: '0.75rem', color: '#38bdf8', background: 'rgba(56, 189, 248, 0.1)', padding: '2px 6px', borderRadius: '4px' }}>
                                            {leave.leaveType || 'Leave'}
                                        </span>
                                    </div>
                                    <p className="leave-reason">{leave.reason}</p>
                                    <p className="leave-dates">
                                        {new Date(leave.startDate).toLocaleDateString()} – {new Date(leave.endDate).toLocaleDateString()}
                                    </p>
                                </div>
                                <span className={`leave-status status-${leave.status ? leave.status.toLowerCase() : 'pending'}`}>
                                    {leave.status || 'Pending'}
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>
            ) : (
                <div style={{ textAlign: 'center', padding: '40px 20px', color: '#94a3b8' }}>
                    <p>No leave records found for your profile. Click "+ Apply Leave" to request time off.</p>
                </div>
            )}
        </div>
    );
};

export default Dashboard;