import React, { createContext, useState, useContext, useEffect, useCallback } from "react";
import apiClient from "../services/apiClient";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);

    const logout = useCallback(() => {
        setUser(null);
        localStorage.removeItem("user");
        localStorage.removeItem("token");
    }, []);

    const login = useCallback((userData, token) => {
        setUser(userData);
        localStorage.setItem("user", JSON.stringify(userData));
        if (token) {
            localStorage.setItem("token", token);
        }
    }, []);

    const refreshUser = useCallback(async () => {
        const token = localStorage.getItem("token");
        if (!token) return null;
        try {
            const response = await apiClient.get("/auth/me");
            if (response.data) {
                setUser(prev => {
                    if (prev && JSON.stringify(prev) === JSON.stringify(response.data)) {
                        return prev;
                    }
                    return response.data;
                });
                localStorage.setItem("user", JSON.stringify(response.data));
                return response.data;
            }
        } catch (e) {
            console.error("Failed to refresh user profile from /auth/me", e);
            if (e.response && (e.response.status === 401 || e.response.status === 403)) {
                logout();
            }
        }
        return null;
    }, [logout]);

    const isAdmin = useCallback(() => {
        return Boolean(user && (user.role === "admin" || user.role === "ADMIN"));
    }, [user]);

    const isEmployee = useCallback(() => {
        return Boolean(user && (user.role === "employee" || user.role === "EMPLOYEE"));
    }, [user]);

    useEffect(() => {
        const savedUser = localStorage.getItem("user");
        if (savedUser) {
            try {
                setUser(JSON.parse(savedUser));
            } catch (e) {
                console.error("Failed to parse stored user", e);
            }
        }
        // If authenticated token exists, sync live profile & leaveBalance from backend
        const token = localStorage.getItem("token");
        if (token) {
            refreshUser();
        }
    }, [refreshUser]);

    return (
        <AuthContext.Provider value={{ user, login, logout, isAdmin, isEmployee, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        return {
            user: null,
            login: () => {},
            logout: () => {},
            isAdmin: () => false,
            isEmployee: () => false,
            refreshUser: async () => null
        };
    }
    return context;
};