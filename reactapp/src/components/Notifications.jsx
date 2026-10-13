import React, { useState, useEffect } from 'react';
import apiClient from '../services/apiClient';
import './Notifications.css';

const Notifications = () => {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchNotifications = async () => {
            try {
                const response = await apiClient.get('/notifications');
                setNotifications(response.data || []);
            } catch (error) {
                console.error('Error fetching notifications:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchNotifications();
    }, []);

    return (
        <div className="notifications-container">
            <h1 className="notifications-title">Notifications</h1>
            {loading ? (
                <p className="no-notifications">Loading notifications...</p>
            ) : notifications.length > 0 ? (
                <ul className="notifications-list">
                    {notifications.map((notification) => (
                        <li key={notification.id} className="notification-item">
                            <p className="notification-message">{notification.message}</p>
                            <span className="notification-date">
                                {notification.date ? new Date(notification.date).toLocaleString() : 'Just now'}
                            </span>
                        </li>
                    ))}
                </ul>
            ) : (
                <p className="no-notifications">No new notifications.</p>
            )}
        </div>
    );
};

export default Notifications;