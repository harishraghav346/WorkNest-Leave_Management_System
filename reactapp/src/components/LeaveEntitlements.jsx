import React, { useState } from "react";
import { Link } from "react-router-dom";
import { 
  FaCalendarAlt, 
  FaChevronLeft, 
  FaChevronRight, 
  FaThLarge, 
  FaList, 
  FaPlus, 
  FaSun, 
  FaUmbrellaBeach, 
  FaShip, 
  FaSuitcaseRolling, 
  FaBaby, 
  FaHeartbeat, 
  FaInfoCircle,
  FaMapMarkerAlt
} from "react-icons/fa";
import "./LeaveEntitlements.css";

const LeaveEntitlements = () => {
  const [currentYear, setCurrentYear] = useState(2024);
  const [viewMode, setViewMode] = useState("grid");
  const [selectedLocation, setSelectedLocation] = useState("Bangalore");

  const entitlements = [
    {
      id: "annual",
      title: "Annual leave",
      icon: <FaSun />,
      theme: "sky",
      available: 8,
      booked: 2,
      unit: "Days",
      policyCode: "AL"
    },
    {
      id: "casual",
      title: "Casual Leave",
      icon: <FaUmbrellaBeach />,
      theme: "blue",
      available: 11,
      booked: 1,
      unit: "Days",
      policyCode: "CL"
    },
    {
      id: "compoff",
      title: "Compensatory off",
      icon: <FaShip />,
      theme: "green",
      available: 20,
      booked: 0,
      unit: "Days",
      policyCode: "COMP"
    },
    {
      id: "deductible",
      title: "Deductible holiday",
      icon: <FaSuitcaseRolling />,
      theme: "peach",
      allowance: 25,
      holidays: 3,
      availableBooking: 22,
      availableBalance: 22,
      previouslyBooked: 0,
      unit: "Days",
      isComplex: true,
      policyCode: "DH"
    },
    {
      id: "maternity",
      title: "Maternity Leave",
      icon: <FaBaby />,
      theme: "rose",
      available: 0,
      booked: 0,
      unit: "Days",
      policyCode: "ML"
    },
    {
      id: "maternity_grant",
      title: "Maternity Leave - grant...",
      icon: <FaBaby />,
      theme: "sky",
      available: 0,
      booked: 0,
      unit: "Days",
      policyCode: "ML-G"
    },
    {
      id: "sick",
      title: "Sick & Medical Leave",
      icon: <FaHeartbeat />,
      theme: "purple",
      available: 10,
      booked: 2,
      unit: "Days",
      policyCode: "SL"
    }
  ];

  return (
    <div className="entitlements-page">
      {/* Top Controls Bar */}
      <div className="entitlements-top-bar">
        {/* Date Range Stepper */}
        <div className="date-range-stepper">
          <button 
            className="stepper-nav-btn" 
            onClick={() => setCurrentYear(prev => prev - 1)}
            aria-label="Previous Year"
          >
            <FaChevronLeft />
          </button>
          <FaCalendarAlt style={{ color: '#0284c7', fontSize: '0.95rem' }} />
          <button 
            className="stepper-nav-btn" 
            onClick={() => setCurrentYear(prev => prev + 1)}
            aria-label="Next Year"
          >
            <FaChevronRight />
          </button>
          <span className="stepper-range-text">
            01-Jan-{currentYear} - 31-Dec-{currentYear}
          </span>
        </div>

        {/* Location & Action Tools */}
        <div className="entitlements-actions">
          {/* Location-based Leave selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <FaMapMarkerAlt style={{ color: '#64748b', fontSize: '0.9rem' }} />
            <select
              value={selectedLocation}
              onChange={(e) => setSelectedLocation(e.target.value)}
              className="location-filter-select"
            >
              <option value="Bangalore">Location: Bangalore (India)</option>
              <option value="NewYork">Location: New York (US)</option>
              <option value="London">Location: London (UK)</option>
              <option value="Remote">Location: Global Remote</option>
            </select>
          </div>

          {/* View Toggles */}
          <div className="view-mode-toggle-group">
            <button 
              className={`toggle-view-btn ${viewMode === "grid" ? "active" : ""}`}
              onClick={() => setViewMode("grid")}
              title="Grid View"
            >
              <FaThLarge />
            </button>
            <button 
              className={`toggle-view-btn ${viewMode === "list" ? "active" : ""}`}
              onClick={() => setViewMode("list")}
              title="List View"
            >
              <FaList />
            </button>
          </div>

          <Link to="/add-leave" className="btn-apply-leave-blue">
            <FaPlus /> Apply Leave
          </Link>

          <Link to="/leave-policy-wizard" className="btn-more-options" title="Configure Custom Policies">
            Custom Policies
          </Link>
        </div>
      </div>

      {/* Entitlement Cards Grid */}
      <div className="entitlements-grid">
        {entitlements.map((item) => (
          <div key={item.id} className="entitlement-card">
            <div className="entitlement-card-header">
              <span className="entitlement-card-title">{item.title}</span>
              <div className={`entitlement-icon-box ${item.theme}`}>
                {item.icon}
              </div>
            </div>

            <div className="entitlement-metrics-list">
              {item.isComplex ? (
                <>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">Allowance</span>
                    <span className="metric-number-val">{item.allowance}</span>
                  </div>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">Holidays</span>
                    <span className="metric-number-val">{item.holidays}</span>
                  </div>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">Available for booking</span>
                    <span className="metric-number-val available-highlight">{item.availableBooking}</span>
                  </div>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">Available Balance</span>
                    <span className="metric-number-val available-highlight">{item.availableBalance}</span>
                  </div>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">
                      Previously booked
                      <FaInfoCircle className="metric-info-icon" title="Leaves utilized in prior quarters" />
                    </span>
                    <span className="metric-number-val">{item.previouslyBooked}</span>
                  </div>
                </>
              ) : (
                <>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">Available</span>
                    <span className="metric-number-val available-highlight">
                      {item.available}
                    </span>
                  </div>
                  <div className="entitlement-metric-row">
                    <span className="metric-label-group">
                      Booked
                      <FaInfoCircle className="metric-info-icon" title="Leaves booked or approved for this period" />
                    </span>
                    <span className="metric-number-val">{item.booked}</span>
                  </div>
                </>
              )}
            </div>

            <div className="entitlement-card-footer">
              <span style={{ fontSize: '0.78rem', color: '#94a3b8' }}>
                Code: <strong>{item.policyCode}</strong> ({item.unit})
              </span>
              <Link to="/add-leave" className="policy-badge-link">
                Request {item.policyCode} &rarr;
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default LeaveEntitlements;
