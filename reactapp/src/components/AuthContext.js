import React, { createContext, useState, useContext, useEffect, useCallback } from "react";
import apiClient from "../services/apiClient";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [organization, setOrganization] = useState(null);
    const [tenantRole, setTenantRole] = useState(null);
    const [memberships, setMemberships] = useState([]);

    const logout = useCallback(() => {
        setUser(null);
        setOrganization(null);
        setTenantRole(null);
        setMemberships([]);
        localStorage.removeItem("user");
        localStorage.removeItem("token");
        localStorage.removeItem("organization");
        localStorage.removeItem("tenantRole");
        localStorage.removeItem("memberships");
        // Dispatch event so any open tables/caches clear their data
        window.dispatchEvent(new Event("worknest-logout"));
    }, []);

    const login = useCallback((userData, token) => {
        setUser(userData);
        localStorage.setItem("user", JSON.stringify(userData));
        if (token) {
            localStorage.setItem("token", token);
        }
    }, []);

    const selectTenant = useCallback((org, role, token) => {
        setOrganization(org);
        setTenantRole(role);
        if (org) {
            localStorage.setItem("organization", JSON.stringify(org));
        } else {
            localStorage.removeItem("organization");
        }
        if (role) {
            localStorage.setItem("tenantRole", role);
        } else {
            localStorage.removeItem("tenantRole");
        }
        if (token) {
            localStorage.setItem("token", token);
        }
        // Dispatch event to clear tenant-specific caches across the app
        window.dispatchEvent(new CustomEvent("worknest-tenant-changed", { detail: { organization: org, role } }));
    }, []);

    const switchTenant = useCallback(async (orgId) => {
        try {
            const response = await apiClient.post("/auth/select-tenant", { organizationId: orgId });
            if (response.data && response.data.token) {
                selectTenant(response.data.organization, response.data.role, response.data.token);
                return response.data;
            }
        } catch (e) {
            console.error("Failed to switch tenant", e);
            throw e;
        }
    }, [selectTenant]);

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

    const isPlatformAdmin = useCallback(() => {
        return Boolean(user && (
            user.role === "PLATFORM_ADMIN" ||
            user.role === "ROLE_PLATFORM_ADMIN"
        ));
    }, [user]);

    const isOrgAdmin = useCallback(() => {
        return Boolean(tenantRole === "ORG_ADMIN" || (user && user.role === "ORG_ADMIN"));
    }, [tenantRole, user]);

    const isHrAdmin = useCallback(() => {
        return Boolean(tenantRole === "HR_ADMIN" || isOrgAdmin());
    }, [tenantRole, isOrgAdmin]);

    const isManager = useCallback(() => {
        return Boolean(tenantRole === "MANAGER" || isHrAdmin());
    }, [tenantRole, isHrAdmin]);

    const isAdmin = useCallback(() => {
        return Boolean(
            isOrgAdmin() ||
            isHrAdmin() ||
            isPlatformAdmin() ||
            (user && (user.role === "admin" || user.role === "ADMIN"))
        );
    }, [isOrgAdmin, isHrAdmin, isPlatformAdmin, user]);

    const isEmployee = useCallback(() => {
        return Boolean(user);
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

        const savedOrg = localStorage.getItem("organization");
        if (savedOrg) {
            try {
                setOrganization(JSON.parse(savedOrg));
            } catch (e) {
                console.error("Failed to parse stored organization", e);
            }
        }

        const savedRole = localStorage.getItem("tenantRole");
        if (savedRole) {
            setTenantRole(savedRole);
        }

        const savedMems = localStorage.getItem("memberships");
        if (savedMems) {
            try {
                setMemberships(JSON.parse(savedMems));
            } catch (e) {
                console.error("Failed to parse stored memberships", e);
            }
        }

        const token = localStorage.getItem("token");
        if (token) {
            refreshUser();
        }
    }, [refreshUser]);

    return (
        <AuthContext.Provider value={{
            user,
            organization,
            tenantRole,
            memberships,
            setMemberships,
            login,
            logout,
            selectTenant,
            switchTenant,
            isAdmin,
            isOrgAdmin,
            isHrAdmin,
            isManager,
            isEmployee,
            isPlatformAdmin,
            refreshUser
        }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        return {
            user: null,
            organization: null,
            tenantRole: null,
            memberships: [],
            setMemberships: () => {},
            login: () => {},
            logout: () => {},
            selectTenant: () => {},
            switchTenant: async () => {},
            isAdmin: () => false,
            isOrgAdmin: () => false,
            isHrAdmin: () => false,
            isManager: () => false,
            isEmployee: () => false,
            isPlatformAdmin: () => false,
            refreshUser: async () => null
        };
    }
    return context;
};