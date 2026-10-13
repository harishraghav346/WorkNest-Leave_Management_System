import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getLeaveById, updateLeave, getAllLeaves } from "../services/leaveService";
import { useAuth } from "./AuthContext";
import "./LeaveForm.css";

const UpdateLeave = () => {
  const { isAdmin } = useAuth();
  const [form, setForm] = useState({
    employeeName: "",
    reason: "",
    startDate: "",
    endDate: "",
    status: "Pending",
    leaveType: "Casual Leave"
  });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");
  const { id } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchLeave = async () => {
      try {
        let leaveData = null;
        try {
          const res = await getLeaveById(id);
          leaveData = res?.data;
        } catch (e) {
          // fallback
        }
        if (!leaveData) {
          try {
            const allRes = await getAllLeaves();
            const list = allRes?.data?.content || allRes?.data || [];
            leaveData = list.find(l => String(l.id || l.leaveId) === String(id));
          } catch (e) {}
        }

        if (leaveData) {
          setForm({
            employeeName: leaveData.employeeName || "",
            reason: leaveData.reason || "",
            startDate: leaveData.startDate ? new Date(leaveData.startDate).toISOString().split("T")[0] : "",
            endDate: leaveData.endDate ? new Date(leaveData.endDate).toISOString().split("T")[0] : "",
            status: leaveData.status || "Pending",
            leaveType: leaveData.leaveType || "Casual Leave"
          });
        }
      } catch (err) {
        console.error("Error fetching leave:", err);
        setError("Could not load leave record.");
      } finally {
        setFetching(false);
      }
    };
    fetchLeave();
  }, [id]);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await updateLeave(id, form);
      navigate("/view-leaves");
    } catch (err) {
      console.error("Error updating leave:", err);
      setError("Failed to update leave request.");
    } finally {
      setLoading(false);
    }
  };

  if (fetching) {
    return <div className="leave-form-container"><p>Loading request details...</p></div>;
  }

  return (
    <div className="leave-form-container">
      <form className="leave-form" onSubmit={handleSubmit}>
        <h2 className="form-title">Update Leave Request</h2>
        {error && <div className="form-error-banner" style={{ color: '#ef4444', marginBottom: '15px' }}>{error}</div>}

        <div className="form-group full-width">
          <label htmlFor="employeeName">Employee</label>
          <input
            type="text"
            id="employeeName"
            name="employeeName"
            value={form.employeeName}
            onChange={handleChange}
            readOnly={!isAdmin()}
            className={!isAdmin() ? "readonly-input" : ""}
          />
        </div>

        <div className="form-group full-width">
          <label htmlFor="reason">Reason for Leave</label>
          <textarea
            id="reason"
            name="reason"
            value={form.reason}
            onChange={handleChange}
            placeholder="Reason for leave"
            required
          />
        </div>

        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="startDate">Start Date</label>
            <input
              type="date"
              id="startDate"
              name="startDate"
              value={form.startDate}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="endDate">End Date</label>
            <input
              type="date"
              id="endDate"
              name="endDate"
              value={form.endDate}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        <div className="form-group full-width">
          <label htmlFor="status">Status</label>
          {isAdmin() ? (
            <select
              id="status"
              name="status"
              value={form.status}
              onChange={handleChange}
              required
            >
              <option value="Pending">Pending</option>
              <option value="Approved">Approved</option>
              <option value="Rejected">Rejected</option>
            </select>
          ) : (
            <input
              type="text"
              id="status"
              name="status"
              value={form.status}
              readOnly
              className="readonly-input"
            />
          )}
        </div>

        <button type="submit" className="btn-submit" disabled={loading}>
          {loading ? "Updating..." : "Update Request"}
        </button>
      </form>
    </div>
  );
};

export default UpdateLeave;