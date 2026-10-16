import React, { useState, useEffect, useCallback, useMemo } from "react";
import { 
  FaCalendarAlt, 
  FaChevronLeft, 
  FaChevronRight, 
  FaSyncAlt, 
  FaInfoCircle, 
  FaTimes
} from "react-icons/fa";
import { getTeamCalendar } from "../services/leaveService";
import { useAuth } from "./AuthContext";
import "./TeamCalendar.css";

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

const DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

const pad2 = (n) => String(n).padStart(2, "0");
const formatIso = (y, m, d) => `${y}-${pad2(m + 1)}-${pad2(d)}`;

const TeamCalendar = () => {
  const { isAdmin } = useAuth();
  
  const today = useMemo(() => new Date(), []);
  const [currentYear, setCurrentYear] = useState(today.getFullYear());
  const [currentMonth, setCurrentMonth] = useState(today.getMonth()); // 0-indexed
  
  const [absences, setAbsences] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAbsence, setSelectedAbsence] = useState(null);

  // Build grid cells for the current visible calendar month
  const gridCells = useMemo(() => {
    const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
    const daysInCurrentMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
    const daysInPrevMonth = new Date(currentYear, currentMonth, 0).getDate();

    // Monday as 0, Sunday as 6
    const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7;

    const cells = [];

    // 1. Previous month trailing days
    for (let i = startDayOfWeek - 1; i >= 0; i--) {
      const d = daysInPrevMonth - i;
      const m = currentMonth === 0 ? 11 : currentMonth - 1;
      const y = currentMonth === 0 ? currentYear - 1 : currentYear;
      const dateStr = formatIso(y, m, d);
      const dayOfWeek = (new Date(y, m, d).getDay() + 6) % 7;
      cells.push({
        year: y,
        month: m,
        day: d,
        dateStr,
        isCurrentMonth: false,
        isWeekend: dayOfWeek >= 5
      });
    }

    // 2. Current month days
    for (let d = 1; d <= daysInCurrentMonth; d++) {
      const dateStr = formatIso(currentYear, currentMonth, d);
      const dayOfWeek = (new Date(currentYear, currentMonth, d).getDay() + 6) % 7;
      cells.push({
        year: currentYear,
        month: currentMonth,
        day: d,
        dateStr,
        isCurrentMonth: true,
        isWeekend: dayOfWeek >= 5
      });
    }

    // 3. Next month leading days to complete grid rows
    const totalCells = Math.ceil(cells.length / 7) * 7;
    const remaining = totalCells - cells.length;
    for (let d = 1; d <= remaining; d++) {
      const m = currentMonth === 11 ? 0 : currentMonth + 1;
      const y = currentMonth === 11 ? currentYear + 1 : currentYear;
      const dateStr = formatIso(y, m, d);
      const dayOfWeek = (new Date(y, m, d).getDay() + 6) % 7;
      cells.push({
        year: y,
        month: m,
        day: d,
        dateStr,
        isCurrentMonth: false,
        isWeekend: dayOfWeek >= 5
      });
    }

    return cells;
  }, [currentYear, currentMonth]);

  // Fetch absences for visible date range
  const fetchCalendar = useCallback(async () => {
    if (gridCells.length === 0) return;
    setLoading(true);
    setError(null);

    const rangeStart = gridCells[0].dateStr;
    const rangeEnd = gridCells[gridCells.length - 1].dateStr;
    const statusParam = statusFilter === "ALL" ? null : statusFilter;

    try {
      const response = await getTeamCalendar(rangeStart, rangeEnd, statusParam);
      const data = response?.data || [];
      setAbsences(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Error loading calendar leaves:", err);
      setError(err?.response?.data?.message || err?.message || "Failed to load team availability calendar.");
    } finally {
      setLoading(false);
    }
  }, [gridCells, statusFilter]);

  useEffect(() => {
    fetchCalendar();
  }, [fetchCalendar]);

  const handlePrevMonth = () => {
    if (currentMonth === 0) {
      setCurrentYear(prev => prev - 1);
      setCurrentMonth(11);
    } else {
      setCurrentMonth(prev => prev - 1);
    }
  };

  const handleNextMonth = () => {
    if (currentMonth === 11) {
      setCurrentYear(prev => prev + 1);
      setCurrentMonth(0);
    } else {
      setCurrentMonth(prev => prev + 1);
    }
  };

  const handleToday = () => {
    setCurrentYear(today.getFullYear());
    setCurrentMonth(today.getMonth());
  };

  const todayIso = formatIso(today.getFullYear(), today.getMonth(), today.getDate());

  // Filter absences per cell
  const getAbsencesForDate = useCallback((dateStr) => {
    return absences.filter(a => {
      if (searchQuery && !a.employeeName?.toLowerCase().includes(searchQuery.toLowerCase())) {
        return false;
      }
      return a.startDate <= dateStr && a.endDate >= dateStr;
    });
  }, [absences, searchQuery]);

  // Summary counts for current month
  const monthAbsencesCount = useMemo(() => {
    const curMonthPrefix = `${currentYear}-${pad2(currentMonth + 1)}`;
    const distinctUsers = new Set();
    let totalRequests = 0;
    absences.forEach(a => {
      if (a.startDate.startsWith(curMonthPrefix) || a.endDate.startsWith(curMonthPrefix)) {
        totalRequests++;
        if (a.employeeName) distinctUsers.add(a.employeeName.toLowerCase());
      }
    });
    return {
      requests: totalRequests,
      uniquePeople: distinctUsers.size
    };
  }, [absences, currentYear, currentMonth]);

  return (
    <div className="team-calendar-page">
      {/* Top Banner */}
      <div className="calendar-header-banner">
        <div className="calendar-title-group">
          <h1>
            <FaCalendarAlt style={{ color: "var(--primary-600)" }} />
            Team Availability Calendar
          </h1>
          <p>
            Real-time workforce availability, team absences, and scheduling insights across WorkNest.
          </p>
        </div>

        <div className="calendar-stats-row">
          <div className="calendar-stat-pill">
            <span className="stat-num">{monthAbsencesCount.requests}</span>
            <span className="stat-label">Scheduled Absences</span>
          </div>
          <div className="calendar-stat-pill">
            <span className="stat-num">{monthAbsencesCount.uniquePeople}</span>
            <span className="stat-label">Team Members Away</span>
          </div>
        </div>
      </div>

      {/* Calendar Controls Bar */}
      <div className="calendar-controls-bar">
        <div className="calendar-nav-controls">
          <button 
            type="button" 
            className="cal-btn" 
            onClick={handlePrevMonth}
            title="Previous Month"
          >
            <FaChevronLeft />
          </button>

          <span className="cal-current-label">
            {MONTH_NAMES[currentMonth]} {currentYear}
          </span>

          <button 
            type="button" 
            className="cal-btn" 
            onClick={handleNextMonth}
            title="Next Month"
          >
            <FaChevronRight />
          </button>

          <button 
            type="button" 
            className="cal-btn" 
            onClick={handleToday}
            style={{ marginLeft: '4px' }}
          >
            Today
          </button>

          <button 
            type="button" 
            className="cal-btn" 
            onClick={fetchCalendar} 
            title="Refresh availability"
          >
            <FaSyncAlt className={loading ? "fa-spin" : ""} />
          </button>
        </div>

        <div className="calendar-filter-controls">
          <input
            type="text"
            className="cal-search-input"
            placeholder="Search employee..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />

          <select
            className="cal-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="Approved">Approved Only</option>
            {(isAdmin() || absences.some(a => a.status === "Pending")) && (
              <option value="Pending">Pending Only</option>
            )}
          </select>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div style={{
          background: "#fef2f2",
          border: "1px solid #fecaca",
          borderRadius: "12px",
          padding: "1rem 1.25rem",
          color: "#991b1b",
          marginBottom: "1.5rem",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}>
          <div>
            <strong>Unable to load calendar:</strong> {error}
          </div>
          <button type="button" className="cal-btn" onClick={fetchCalendar}>
            Retry
          </button>
        </div>
      )}

      {/* Calendar Month Grid */}
      <div className="calendar-grid-container">
        {/* Days Header */}
        <div className="calendar-days-header">
          {DAY_LABELS.map((dayName, idx) => (
            <div 
              key={dayName} 
              className={`calendar-day-header-cell ${idx >= 5 ? "weekend" : ""}`}
            >
              {dayName}
            </div>
          ))}
        </div>

        {/* Date Tiles */}
        <div className="calendar-month-grid">
          {gridCells.map((cell) => {
            const isToday = cell.dateStr === todayIso;
            const dateAbsences = getAbsencesForDate(cell.dateStr);

            return (
              <div
                key={cell.dateStr}
                className={`calendar-cell ${!cell.isCurrentMonth ? "outside-month" : ""} ${cell.isWeekend ? "weekend" : ""} ${isToday ? "today" : ""}`}
              >
                <div className="calendar-cell-top">
                  <span className="calendar-date-number">
                    {cell.day}
                  </span>
                  {dateAbsences.length > 2 && (
                    <span style={{ fontSize: "0.7rem", color: "var(--slate-400)", fontWeight: 700 }}>
                      +{dateAbsences.length - 2} more
                    </span>
                  )}
                </div>

                <div className="calendar-absence-list">
                  {dateAbsences.slice(0, 3).map((item) => {
                    const isApproved = item.status === "Approved";
                    return (
                      <div
                        key={`${item.id}-${cell.dateStr}`}
                        className={`calendar-absence-badge ${isApproved ? "approved" : "pending"}`}
                        onClick={() => setSelectedAbsence(item)}
                        title={`${item.employeeName} (${item.displayLabel || "Out of Office"}) - ${item.startDate} to ${item.endDate}`}
                      >
                        <span className="badge-emp-name">
                          {item.employeeName}
                        </span>
                        <span className="badge-status-dot" />
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Legend & Privacy Information Footer */}
      <div className="calendar-legend-bar">
        <div className="calendar-legend-items">
          <div className="legend-item">
            <span className="legend-swatch approved" />
            <span>Approved Leave</span>
          </div>
          <div className="legend-item">
            <span className="legend-swatch pending" />
            <span>Pending Request</span>
          </div>
          <div className="legend-item">
            <span className="legend-swatch weekend" />
            <span>Weekend (Non-Working)</span>
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "var(--slate-500)", fontSize: "0.8rem" }}>
          <FaInfoCircle />
          <span>Absences are masked with generic labels to protect colleague personal health and private reasons.</span>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedAbsence && (
        <div className="cal-modal-backdrop" onClick={() => setSelectedAbsence(null)}>
          <div className="cal-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="cal-modal-header">
              <h3>Absence Details</h3>
              <button 
                type="button" 
                className="cal-close-btn" 
                onClick={() => setSelectedAbsence(null)}
              >
                <FaTimes />
              </button>
            </div>

            <div className="cal-modal-detail-row">
              <span className="label">Team Member:</span>
              <span className="val">{selectedAbsence.employeeName}</span>
            </div>

            <div className="cal-modal-detail-row">
              <span className="label">Absence Type:</span>
              <span className="val">{selectedAbsence.displayLabel || "Out of Office"}</span>
            </div>

            <div className="cal-modal-detail-row">
              <span className="label">Date Range:</span>
              <span className="val">{selectedAbsence.startDate} to {selectedAbsence.endDate}</span>
            </div>

            <div className="cal-modal-detail-row">
              <span className="label">Working Duration:</span>
              <span className="val">{selectedAbsence.daysCount} working day(s)</span>
            </div>

            <div className="cal-modal-detail-row">
              <span className="label">Status:</span>
              <span className="val" style={{
                color: selectedAbsence.status === "Approved" ? "#059669" : "#d97706"
              }}>
                {selectedAbsence.status}
              </span>
            </div>

            <div style={{
              marginTop: "1.25rem",
              padding: "0.75rem",
              background: "var(--slate-50)",
              borderRadius: "8px",
              fontSize: "0.8rem",
              color: "var(--slate-500)"
            }}>
              Private leave reasons and medical details are withheld under WorkNest workforce privacy policy.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TeamCalendar;
