import React from 'react';
import { FaEnvelope, FaMapMarkerAlt, FaPhone } from 'react-icons/fa';
import './About.css';

const About = () => {
    return (
        <div className="about-container">
            <div className="about-header">
                <h1 className="about-title">About This App</h1>
                <p className="about-subtitle">A streamlined solution for managing employee leave requests.</p>
            </div>

            <div className="about-content">
                <div className="about-section">
                    <h2 className="section-title">Key Features</h2>
                    <ul className="features-list">
                        <li>
                            <strong>Dashboard:</strong> A comprehensive overview of leave statistics, including total, approved, and rejected leaves.
                        </li>
                        <li>
                            <strong>Leave Management:</strong> Easily add, view, update, and delete leave requests with an intuitive interface.
                        </li>
                        <li>
                            <strong>User Authentication:</strong> Secure login and signup functionality to protect user data.
                        </li>
                        <li>
                            <strong>Modern UI:</strong> A clean and modern user interface for a seamless user experience.
                        </li>
                    </ul>
                </div>

                <div className="about-section">
                    <h2 className="section-title">About Me</h2>
                    <div className="contact-details">
                        <div className="contact-item">
                            <FaEnvelope className="contact-icon" />
                            <a href="mailto:ponmadhan1122@gmail.com">ponmadhan1122@gmail.com</a>
                        </div>
                        <div className="contact-item">
                            <FaMapMarkerAlt className="contact-icon" />
                            <span>Coimbatore</span>
                        </div>
                        <div className="contact-item">
                            <FaPhone className="contact-icon" />
                            <a href="tel:9943244480">9943244480</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default About;