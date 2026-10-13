import React from 'react';
import { Link } from 'react-router-dom';
import { FaFacebook, FaTwitter, FaLinkedin, FaGithub } from 'react-icons/fa';
import './Footer.css';

const Footer = () => {
  return (
    <footer className="app-footer">
      <div className="footer-content">
        <h2 className="footer-logo">WorkNest</h2>

        <div className="footer-links">
          <Link to="/" className="footer-link">Home</Link>
          <Link to="/dashboard" className="footer-link">Dashboard</Link>
          <Link to="/view-leaves" className="footer-link">My Leaves</Link>
          <Link to="/add-leave" className="footer-link">Apply Leave</Link>
          <Link to="/about" className="footer-link">About</Link>
        </div>

        <div className="footer-socials">
          <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="social-icon" aria-label="Facebook">
            <FaFacebook />
          </a>
          <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="social-icon" aria-label="Twitter">
            <FaTwitter />
          </a>
          <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" className="social-icon" aria-label="LinkedIn">
            <FaLinkedin />
          </a>
          <a href="https://github.com" target="_blank" rel="noopener noreferrer" className="social-icon" aria-label="GitHub">
            <FaGithub />
          </a>
        </div>

        <p className="footer-copy">
          &copy; {new Date().getFullYear()} WorkNest HR Workforce Suite. All Rights Reserved.
        </p>
      </div>
    </footer>
  );
};

export default Footer;