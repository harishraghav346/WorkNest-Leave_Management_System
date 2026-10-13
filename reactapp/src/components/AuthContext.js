import React, { createContext, useState, useContext, useEffect } from "react";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);

    const login = (userData, token) => {
        setUser(userData);
        localStorage.setItem("user", JSON.stringify(userData));
        if (token) {
            localStorage.setItem("token", token);
        }
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem("user");
        localStorage.removeItem("token");
    };

    const isAdmin = () => {
        return user && (user.role === "admin" || user.role === "ADMIN");
    };

    const isEmployee = () => {
        return user && (user.role === "employee" || user.role === "EMPLOYEE");
    };

    useEffect(() => {
        const savedUser = localStorage.getItem("user");
        if (savedUser) {
            try {
                setUser(JSON.parse(savedUser));
            } catch (e) {
                console.error("Failed to parse stored user", e);
            }
        }
    }, []);

    return (
        <AuthContext.Provider value={{ user, login, logout, isAdmin, isEmployee }}>
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
            isEmployee: () => false
        };
    }
    return context;
};