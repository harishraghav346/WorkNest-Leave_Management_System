import React, { useState, useEffect } from 'react';
import { getAllLeaves, deleteLeave } from '../services/leaveService';
import { Link } from 'react-router-dom';

const ViewLeave = () => {
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLeaves();
  }, []);

  const fetchLeaves = async () => {
    try {
      setLoading(true);
      const response = await getAllLeaves();
      const data = response.data?.content || response.data || [];
      setLeaves(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching leaves:', error);
      setLeaves([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteLeave(id);
      fetchLeaves();
    } catch (error) {
      console.error('Error deleting leave:', error);
    }
  };

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <h2>Leave Records</h2>
      <div style={{ marginBottom: '15px' }}>
        <Link to="/add-leave" style={{ padding: '8px 16px', background: '#0284c7', color: '#fff', textDecoration: 'none', borderRadius: '6px' }}>
          Add Leave
        </Link>
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
        <thead>
          <tr style={{ background: '#f1f5f9', textAlign: 'left' }}>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>Employee Name</th>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>Start Date</th>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>End Date</th>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>Reason</th>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>Status</th>
            <th style={{ padding: '10px', border: '1px solid #cbd5e1' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {leaves.map((leave, index) => (
            <tr key={leave.id || leave.leaveId || index} style={{ borderBottom: '1px solid #e2e8f0' }}>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>{leave.employeeName}</td>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>{leave.startDate}</td>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>{leave.endDate}</td>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>{leave.reason}</td>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>{leave.status}</td>
              <td style={{ padding: '10px', border: '1px solid #cbd5e1' }}>
                <Link to={`/update-leave/${leave.id || leave.leaveId}`} style={{ marginRight: '10px', color: '#0284c7' }}>
                  Edit
                </Link>
                <button 
                  onClick={() => handleDelete(leave.id || leave.leaveId)}
                  style={{ background: '#ef4444', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ViewLeave;
