import React from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
  useLocation,
} from "react-router-dom";
import { AuthProvider, useAuth } from "./components/AuthContext";
import Header from "./components/Header";
import Sidebar from "./components/Sidebar";
import Footer from "./components/Footer";
import Home from "./components/Home";
import About from "./components/About";
import Login from "./components/Login";
import Signup from "./components/Signup";
import Dashboard from "./components/Dashboard";
import AddLeave from "./components/AddLeave";
import ViewLeaves from "./components/ViewLeaves";
import UpdateLeave from "./components/UpdateLeave";
import Notifications from "./components/Notifications";
import AdminDashboard from "./components/AdminDashboard";
import AiAssistant from "./components/AiAssistant";
import LeaveEntitlements from "./components/LeaveEntitlements";
import LeavePolicyWizard from "./components/LeavePolicyWizard";
import SelfServicePortal from "./components/SelfServicePortal";
import WorkforceHub from "./components/WorkforceHub";
import SecurityPrivacy from "./components/SecurityPrivacy";
import OfficeOperations from "./components/OfficeOperations";
import TeamCalendar from "./components/TeamCalendar";
import PublicWebsite from "./components/PublicWebsite";
import TenantSelection from "./components/TenantSelection";
import OrganizationOnboarding from "./components/OrganizationOnboarding";
import InvitationActivation from "./components/InvitationActivation";
import OrganizationVerification from "./components/OrganizationVerification";

const App = () => {
  return (
    <AuthProvider>
      <Router>
        <AppContent />
      </Router>
    </AuthProvider>
  );
};

const RootRoute = () => {
  const { user } = useAuth();
  return user ? <Home /> : <PublicWebsite />;
};

const AppContent = () => {
  const { user } = useAuth();
  const location = useLocation();

  const isPublicStandalone = 
    location.pathname === '/login' || 
    location.pathname === '/signup' || 
    location.pathname === '/onboard' || 
    location.pathname === '/activate' || 
    location.pathname === '/verify-organization' ||
    location.pathname === '/verify-org' ||
    location.pathname === '/verification-pending' ||
    location.pathname === '/select-tenant' ||
    (!user && location.pathname === '/');

  return (
    <div className="app-container">
      {!isPublicStandalone && <Header />}
      
      <div style={{ display: 'flex', minHeight: 'calc(100vh - 70px)', flex: 1 }}>
        {!isPublicStandalone && user && <Sidebar />}
        
        <main style={{ flex: 1, minWidth: 0 }}>
          <Routes>
            {/* Public or Home Root */}
            <Route path="/" element={<RootRoute />} />

            {/* Public Auth & Onboarding Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/onboard" element={<OrganizationOnboarding />} />
            <Route path="/activate" element={<InvitationActivation />} />
            <Route path="/verify-organization" element={<OrganizationVerification />} />
            <Route path="/verify-org" element={<OrganizationVerification />} />
            <Route path="/verification-pending" element={<OrganizationVerification isPendingScreen={true} />} />

            {/* Tenant Selection */}
            <Route
              path="/select-tenant"
              element={<ProtectedRoute component={TenantSelection} />}
            />
            
            {/* WorkNest / Modern Modules */}
            <Route
              path="/leave-entitlements"
              element={<ProtectedRoute component={LeaveEntitlements} />}
            />
            <Route
              path="/leave-policy-wizard"
              element={<ProtectedRoute component={LeavePolicyWizard} />}
            />
            <Route
              path="/self-service"
              element={<ProtectedRoute component={SelfServicePortal} />}
            />
            <Route
              path="/workforce-hub"
              element={<ProtectedRoute component={WorkforceHub} />}
            />
            <Route
              path="/security-privacy"
              element={<ProtectedRoute component={SecurityPrivacy} />}
            />
            <Route
              path="/office-operations"
              element={<ProtectedRoute component={OfficeOperations} />}
            />
            <Route
              path="/calendar"
              element={<ProtectedRoute component={TeamCalendar} />}
            />

            {/* Core Routes Preserved */}
            <Route
              path="/about"
              element={<ProtectedRoute component={About} />}
            />
            <Route
              path="/dashboard"
              element={<ProtectedRoute component={Dashboard} />}
            />
            <Route
              path="/add-leave"
              element={<ProtectedRoute component={AddLeave} />}
            />
            <Route path="/add" element={<Navigate to="/add-leave" replace />} />
            <Route path="/view" element={<Navigate to="/view-leaves" replace />} />

            <Route
              path="/view-leaves"
              element={<ProtectedRoute component={ViewLeaves} />}
            />
            <Route
              path="/update-leave/:id"
              element={<ProtectedRoute component={UpdateLeave} />}
            />
            <Route
              path="/notifications"
              element={<ProtectedRoute component={Notifications} />}
            />
            <Route
              path="/admin/dashboard"
              element={<AdminRoute component={AdminDashboard} />}
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>

      {user && !isPublicStandalone && <AiAssistant />}
      {!isPublicStandalone && <Footer />}
    </div>
  );
};

const ProtectedRoute = ({ component: Component }) => {
  const { user } = useAuth();
  return user ? <Component /> : <Navigate to="/login" />;
};

const AdminRoute = ({ component: Component }) => {
  const { user, isAdmin } = useAuth();
  return user && isAdmin() ? <Component /> : <Navigate to="/" />;
};

export default App;