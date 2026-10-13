import React from "react";
import { NavLink } from "react-router-dom";
import { 
  FaHome, 
  FaUserCheck, 
  FaUmbrellaBeach, 
  FaCogs, 
  FaThLarge, 
  FaTasks, 
  FaShieldAlt, 
  FaUserTie 
} from "react-icons/fa";
import { useAuth } from "./AuthContext";
import "./Sidebar.css";

const Sidebar = () => {
  const { user, isAdmin } = useAuth();

  if (!user) return null;

  return (
    <aside className="app-sidebar-rail">
      <nav className="sidebar-nav-list">
        <NavLink 
          to="/" 
          end
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Home Dashboard"
        >
          <FaHome className="sidebar-icon" />
          <span>Home</span>
        </NavLink>

        <NavLink 
          to="/self-service" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Employee Self-Service (ESS)"
        >
          <FaUserCheck className="sidebar-icon" />
          <span>Self-Service</span>
        </NavLink>

        <NavLink 
          to="/leave-entitlements" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Leave Balances & Entitlements"
        >
          <FaUmbrellaBeach className="sidebar-icon" />
          <span>Leave</span>
        </NavLink>

        <NavLink 
          to="/leave-policy-wizard" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Customized Leave Policies"
        >
          <FaCogs className="sidebar-icon" />
          <span>Policies</span>
        </NavLink>

        <NavLink 
          to="/workforce-hub" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Unified Workforce Management"
        >
          <FaThLarge className="sidebar-icon" />
          <span>Workforce</span>
        </NavLink>

        <NavLink 
          to="/office-operations" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Streamlined Office Operations"
        >
          <FaTasks className="sidebar-icon" />
          <span>Operations</span>
        </NavLink>

        <NavLink 
          to="/security-privacy" 
          className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
          title="Advanced Security & Privacy"
        >
          <FaShieldAlt className="sidebar-icon" />
          <span>Security</span>
        </NavLink>

        {isAdmin() && (
          <NavLink 
            to="/admin/dashboard" 
            className={({ isActive }) => `sidebar-nav-item ${isActive ? "active" : ""}`}
            title="Admin Command Center"
          >
            <FaUserTie className="sidebar-icon" />
            <span>Admin</span>
          </NavLink>
        )}
      </nav>
    </aside>
  );
};

export default Sidebar;
