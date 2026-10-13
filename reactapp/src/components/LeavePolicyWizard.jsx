import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FaSun, FaCheck, FaArrowRight, FaArrowLeft, FaCheckCircle } from "react-icons/fa";
import "./LeavePolicyWizard.css";

const LeavePolicyWizard = () => {
  const [currentStep, setCurrentStep] = useState(1);
  const [savedSuccess, setSavedSuccess] = useState(false);
  const navigate = useNavigate();

  // Policy Form State
  const [policyData, setPolicyData] = useState({
    name: "Casual Leave",
    color: "#38bdf8",
    code: "CL",
    type: "Paid",
    unit: "Days",
    creditAmount: 12,
    creditFrequency: "yearly",
    creditDate: "1st of Jan",
    resetEnabled: true,
    resetFrequency: "yearly",
    resetDate: "31st of Dec",
    carryForwardEnabled: false,
    carryForwardLimit: 0,
    encashEnabled: false,
    encashLimit: 0,
    // Step 2 Applicability
    locations: ["Bangalore", "NewYork", "London", "Remote"],
    departments: ["All"],
    // Step 3 Restrictions
    noticePeriodDays: 2,
    maxConsecutiveDays: 5,
    excludeWeekends: true
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setPolicyData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleNext = () => {
    if (currentStep < 3) {
      setCurrentStep(prev => prev + 1);
    } else {
      // Final step submit
      setSavedSuccess(true);
      setTimeout(() => {
        navigate("/leave-entitlements");
      }, 1500);
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(prev => prev - 1);
    }
  };

  return (
    <div className="policy-wizard-page">
      <div className="wizard-card-container">
        
        {/* Left Stepper Sidebar (Matching Reference Image 1 & 3) */}
        <div className="wizard-stepper-sidebar">
          <div className="stepper-steps-list">
            
            <div 
              className={`stepper-step-item ${currentStep === 1 ? 'active' : ''} ${currentStep > 1 ? 'completed active-or-done' : ''}`}
              onClick={() => setCurrentStep(1)}
            >
              <div className="step-number-circle">
                {currentStep > 1 ? <FaCheck /> : "1"}
              </div>
              <span className="step-label-text">Details & Entitlement</span>
            </div>

            <div 
              className={`stepper-step-item ${currentStep === 2 ? 'active' : ''} ${currentStep > 2 ? 'completed active-or-done' : ''}`}
              onClick={() => setCurrentStep(2)}
            >
              <div className="step-number-circle">
                {currentStep > 2 ? <FaCheck /> : "2"}
              </div>
              <span className="step-label-text">Applicability</span>
            </div>

            <div 
              className={`stepper-step-item ${currentStep === 3 ? 'active' : ''}`}
              onClick={() => setCurrentStep(3)}
            >
              <div className="step-number-circle">3</div>
              <span className="step-label-text">Restrictions</span>
            </div>

          </div>

          <div style={{ marginTop: 'auto', paddingTop: '2rem' }}>
            <span style={{ fontSize: '0.78rem', color: '#94a3b8' }}>
              WorkNest Leave Framework v2.4 • Dynamic Policy Engine
            </span>
          </div>
        </div>

        {/* Right Form Content */}
        <div className="wizard-form-content">
          <div>
            {savedSuccess ? (
              <div style={{ textAlign: 'center', padding: '4rem 1rem' }}>
                <FaCheckCircle style={{ fontSize: '3.5rem', color: '#10b981', marginBottom: '1.25rem' }} />
                <h2 style={{ fontSize: '1.75rem', color: '#0f172a', marginBottom: '8px' }}>
                  Leave Policy Activated!
                </h2>
                <p style={{ color: '#64748b', fontSize: '1rem' }}>
                  Policy <strong>{policyData.name} ({policyData.code})</strong> has been saved with customized entitlements and applicability rules. Redirecting to balances...
                </p>
              </div>
            ) : (
              <>
                {/* Step 1: Details & Entitlement (Images 1 & 3) */}
                {currentStep === 1 && (
                  <>
                    <div className="wizard-section-header">
                      <h2 className="wizard-section-title">Details & Entitlement</h2>
                      <p className="wizard-section-subtitle">
                        Enter the basic details and set the entitlement options of this policy
                      </p>
                    </div>

                    <div className="wizard-form-body">
                      {/* Name with Color Swatch & Icon */}
                      <div className="wizard-form-row">
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <label className="wizard-label">Name <span className="req">*</span></label>
                          <div style={{ display: 'flex', gap: '20px', fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>
                            <span>Color</span>
                            <span>Image</span>
                          </div>
                        </div>
                        <div className="name-color-picker-row">
                          <input 
                            type="text" 
                            name="name" 
                            value={policyData.name} 
                            onChange={handleChange}
                            className="wizard-input-text" 
                            placeholder="e.g. Casual Leave"
                            required
                          />
                          <div className="color-swatch-box" style={{ background: policyData.color }}></div>
                          <div className="image-icon-picker-btn">
                            <FaSun />
                          </div>
                        </div>
                      </div>

                      {/* Code */}
                      <div className="wizard-form-row">
                        <label className="wizard-label">Code</label>
                        <input 
                          type="text" 
                          name="code" 
                          value={policyData.code} 
                          onChange={handleChange}
                          className="wizard-input-text" 
                          placeholder="e.g. CL"
                        />
                      </div>

                      {/* Type */}
                      <div className="wizard-form-row">
                        <label className="wizard-label">Type <span className="req">*</span></label>
                        <select 
                          name="type" 
                          value={policyData.type} 
                          onChange={handleChange}
                          className="wizard-select"
                        >
                          <option value="Paid">Paid</option>
                          <option value="Unpaid">Unpaid</option>
                        </select>
                      </div>

                      {/* Unit */}
                      <div className="wizard-form-row">
                        <label className="wizard-label">Unit <span className="req">*</span></label>
                        <div className="unit-radio-group">
                          <label className="unit-radio-label">
                            <input 
                              type="radio" 
                              name="unit" 
                              value="Days" 
                              checked={policyData.unit === "Days"} 
                              onChange={handleChange}
                            />
                            Days
                          </label>
                          <label className="unit-radio-label">
                            <input 
                              type="radio" 
                              name="unit" 
                              value="Hours" 
                              checked={policyData.unit === "Hours"} 
                              onChange={handleChange}
                            />
                            Hours
                          </label>
                        </div>
                      </div>

                      {/* Entitlement Settings */}
                      <div className="wizard-form-row">
                        <label className="wizard-label">Entitlement</label>
                        <div className="entitlement-credit-box">
                          <div className="credit-inline-row">
                            <span>Credit</span>
                            <input 
                              type="number" 
                              name="creditAmount" 
                              value={policyData.creditAmount} 
                              onChange={handleChange}
                              className="credit-num-input"
                            />
                            <span>days</span>
                            <select 
                              name="creditFrequency" 
                              value={policyData.creditFrequency} 
                              onChange={handleChange}
                              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                            >
                              <option value="yearly">yearly</option>
                              <option value="monthly">monthly</option>
                              <option value="quarterly">quarterly</option>
                            </select>
                            <span>on 1st of Jan</span>
                          </div>

                          <label className="checkbox-option-row">
                            <input 
                              type="checkbox" 
                              name="resetEnabled" 
                              checked={policyData.resetEnabled} 
                              onChange={handleChange}
                            />
                            <span>Reset <strong>yearly</strong> on 31st of Dec</span>
                          </label>

                          <label className="checkbox-option-row">
                            <input 
                              type="checkbox" 
                              name="carryForwardEnabled" 
                              checked={policyData.carryForwardEnabled} 
                              onChange={handleChange}
                            />
                            <span>Carry forward unused leave upto </span>
                            <input 
                              type="number" 
                              name="carryForwardLimit" 
                              value={policyData.carryForwardLimit} 
                              onChange={handleChange}
                              style={{ width: 50, padding: 4, borderRadius: 4, border: '1px solid #cbd5e1', textAlign: 'center' }}
                            />
                            <span>unit</span>
                          </label>

                          <label className="checkbox-option-row">
                            <input 
                              type="checkbox" 
                              name="encashEnabled" 
                              checked={policyData.encashEnabled} 
                              onChange={handleChange}
                            />
                            <span>Encash unused leave upto </span>
                            <input 
                              type="number" 
                              name="encashLimit" 
                              value={policyData.encashLimit} 
                              onChange={handleChange}
                              style={{ width: 50, padding: 4, borderRadius: 4, border: '1px solid #cbd5e1', textAlign: 'center' }}
                            />
                            <span>unit</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </>
                )}

                {/* Step 2: Applicability */}
                {currentStep === 2 && (
                  <>
                    <div className="wizard-section-header">
                      <h2 className="wizard-section-title">Applicability & Location Rules</h2>
                      <p className="wizard-section-subtitle">
                        Define which global offices and employee departments this policy applies to
                      </p>
                    </div>

                    <div className="wizard-form-body">
                      <div className="wizard-form-row">
                        <label className="wizard-label">Office Locations</label>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                          {["Bangalore (HQ)", "New York Office", "London Tech Center", "Global Remote"].map((loc) => (
                            <label key={loc} className="checkbox-option-row" style={{ padding: '10px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                              <input type="checkbox" defaultChecked />
                              <span>{loc}</span>
                            </label>
                          ))}
                        </div>
                      </div>

                      <div className="wizard-form-row">
                        <label className="wizard-label">Departments</label>
                        <select className="wizard-select">
                          <option>All Departments</option>
                          <option>Engineering & Product</option>
                          <option>Human Resources</option>
                          <option>Finance & Operations</option>
                        </select>
                      </div>

                      <div className="wizard-form-row">
                        <label className="wizard-label">Eligibility by Tenure</label>
                        <p style={{ fontSize: '0.88rem', color: '#64748b', margin: 0 }}>
                          Applicable from Day 1 of employee joining date (including probationary period).
                        </p>
                      </div>
                    </div>
                  </>
                )}

                {/* Step 3: Restrictions */}
                {currentStep === 3 && (
                  <>
                    <div className="wizard-section-header">
                      <h2 className="wizard-section-title">Leave Restrictions & Approval Flows</h2>
                      <p className="wizard-section-subtitle">
                        Configure booking windows, consecutive limits, and instant approval rules
                      </p>
                    </div>

                    <div className="wizard-form-body">
                      <div className="wizard-form-row">
                        <label className="wizard-label">Prior Notice Period Required</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <input 
                            type="number" 
                            name="noticePeriodDays" 
                            value={policyData.noticePeriodDays} 
                            onChange={handleChange}
                            className="credit-num-input"
                          />
                          <span style={{ fontSize: '0.92rem', color: '#64748b' }}>days in advance of leave start</span>
                        </div>
                      </div>

                      <div className="wizard-form-row">
                        <label className="wizard-label">Maximum Consecutive Days</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <input 
                            type="number" 
                            name="maxConsecutiveDays" 
                            value={policyData.maxConsecutiveDays} 
                            onChange={handleChange}
                            className="credit-num-input"
                          />
                          <span style={{ fontSize: '0.92rem', color: '#64748b' }}>days permitted in a single request</span>
                        </div>
                      </div>

                      <div className="wizard-form-row">
                        <label className="wizard-label">Instant Approval Automation</label>
                        <label className="checkbox-option-row">
                          <input type="checkbox" defaultChecked />
                          <span>Enable <strong>Instant AI Approval</strong> for requests under 2 days with zero team conflict</span>
                        </label>
                      </div>
                    </div>
                  </>
                )}
              </>
            )}
          </div>

          {/* Wizard Footer Controls */}
          {!savedSuccess && (
            <div className="wizard-footer-buttons">
              {currentStep > 1 ? (
                <button type="button" onClick={handleBack} className="btn-wizard-back">
                  <FaArrowLeft style={{ marginRight: '6px' }} /> Back
                </button>
              ) : <div></div>}

              <button type="button" onClick={handleNext} className="btn-wizard-next">
                <span>{currentStep === 3 ? "Save & Activate Policy" : "Next Step"}</span>
                <FaArrowRight />
              </button>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default LeavePolicyWizard;
