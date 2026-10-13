import React, { useState } from 'react';
import { FaRobot, FaPaperPlane, FaTimes, FaComments, FaCalendarCheck, FaLightbulb } from 'react-icons/fa';
import { useAuth } from './AuthContext';
import { addLeave } from '../services/leaveService';
import { useNavigate } from 'react-router-dom';
import './AiAssistant.css';

const AiAssistant = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        {
            sender: 'ai',
            text: `Hello ${user?.name || 'there'}! I'm your AI HR & Leave Assistant. You can ask me about leave balances, company policies, or tell me in plain English: "Book sick leave tomorrow for a dental checkup".`,
            draft: null
        }
    ]);
    const [input, setInput] = useState('');
    const [isTyping, setIsTyping] = useState(false);

    // Natural Language Parser
    const parseIntent = (text) => {
        const lower = text.toLowerCase();

        // Check for leave booking intent
        const isBooking = lower.includes('book') || lower.includes('apply') || lower.includes('take') || lower.includes('request');
        
        if (isBooking) {
            let leaveType = "Casual Leave";
            if (lower.includes('sick') || lower.includes('doctor') || lower.includes('fever') || lower.includes('medical')) {
                leaveType = "Sick Leave";
            } else if (lower.includes('vacation') || lower.includes('annual') || lower.includes('holiday')) {
                leaveType = "Annual Leave";
            } else if (lower.includes('emergency') || lower.includes('urgent')) {
                leaveType = "Emergency Leave";
            }

            // Simple date heuristics for demo/assistant
            const today = new Date();
            let startDate = new Date(today);
            let endDate = new Date(today);

            if (lower.includes('tomorrow')) {
                startDate.setDate(today.getDate() + 1);
                endDate.setDate(today.getDate() + 1);
            } else if (lower.includes('next monday')) {
                const daysUntilMon = ((1 + 7 - today.getDay()) % 7) || 7;
                startDate.setDate(today.getDate() + daysUntilMon);
                endDate.setDate(today.getDate() + daysUntilMon);
            } else if (lower.includes('friday')) {
                const daysUntilFri = ((5 + 7 - today.getDay()) % 7) || 7;
                startDate.setDate(today.getDate() + daysUntilFri);
                endDate.setDate(today.getDate() + daysUntilFri);
            } else {
                startDate.setDate(today.getDate() + 2);
                endDate.setDate(today.getDate() + 2);
            }

            const startStr = startDate.toISOString().split('T')[0];
            const endStr = endDate.toISOString().split('T')[0];

            return {
                type: 'booking_draft',
                draft: {
                    employeeName: user?.name || user?.username || 'Employee',
                    startDate: startStr,
                    endDate: endStr,
                    leaveType,
                    reason: text,
                    status: 'Pending'
                },
                reply: `I've prepared your leave request for **${leaveType}** from **${startStr}** to **${endStr}**. Would you like me to submit it for manager approval?`
            };
        }

        // Self-Service & Payslips
        if (lower.includes('payslip') || lower.includes('salary') || lower.includes('attendance') || lower.includes('profile')) {
            return {
                type: 'info',
                reply: `You can access your **Personal Profile, Encrypted Payslip Vault**, and daily **Attendance Timeline** directly in the [Employee Self-Service Portal](/self-service). Check in/out with 1-click and download AES-256 protected salary slips anytime!`
            };
        }

        // Policy & Entitlement Wizard
        if (lower.includes('custom') || lower.includes('wizard') || lower.includes('create policy') || lower.includes('encash')) {
            return {
                type: 'info',
                reply: `To configure custom leave entitlement rules, credit dates (e.g. 1st Jan), yearly resets (31st Dec), and carry-forward limits, visit the [Customized Leave Policy Wizard](/leave-policy-wizard).`
            };
        }

        // Workforce Hub
        if (lower.includes('workforce') || lower.includes('timesheet') || lower.includes('lms') || lower.includes('shift') || lower.includes('onboarding')) {
            return {
                type: 'info',
                reply: `The **Unified Workforce Management Hub** connects Employee Records, Timesheets, Shift Scheduling, LMS, and Case Management. Explore it at [Workforce Hub](/workforce-hub).`
            };
        }

        // Security & MFA
        if (lower.includes('security') || lower.includes('mfa') || lower.includes('encrypt') || lower.includes('privacy')) {
            return {
                type: 'info',
                reply: `Our platform enforces **Multi-Factor Authentication (MFA)**, hardware-backed 256-bit AES encrypted document vaults, and strict Role-Based Access Control (RBAC). Visit [Security & Privacy Governance](/security-privacy) to review settings.`
            };
        }

        // Policy / Balance inquiries
        if (lower.includes('balance') || lower.includes('quota') || lower.includes('how many days') || lower.includes('entitlement')) {
            return {
                type: 'info',
                reply: `Your current entitlements include:\n• **Annual Leave:** 8 Available (2 Booked)\n• **Casual Leave:** 11 Available (1 Booked)\n• **Compensatory Off:** 20 Available\n• **Deductible Holiday:** 22 Available Balance\n• **Sick Leave:** 10 Days\nView all breakdowns in [Leave Balances & Entitlements](/leave-entitlements).`
            };
        }

        if (lower.includes('policy') || lower.includes('notice') || lower.includes('rules')) {
            return {
                type: 'info',
                reply: `**Zylker Modern Leave Guidelines:**\n• **Instant Approvals:** 1-day leaves with zero team overlap are auto-approved instantly.\n• **Planned Leaves** (>2 days): Require 2 business days advance notice.\n• **Location Rules:** Location-specific public holidays apply to Bangalore, New York, London, and Remote.\n• **Carryover:** Up to 5 unused leave days can be carried over annually.`
            };
        }

        // Default intelligent fallback
        return {
            type: 'info',
            reply: `I understand you asked: "${text}". I can help you apply for time off, check leave balance quotas, access payslips, or review team coverage. Try asking: *"Show my leave balances"*, *"Where can I download my payslip?"*, or *"Apply for casual leave tomorrow"*.`
        };
    };

    const handleSend = () => {
        if (!input.trim()) return;
        const userMsg = input.trim();
        setInput('');

        setMessages(prev => [...prev, { sender: 'user', text: userMsg }]);
        setIsTyping(true);

        setTimeout(() => {
            const result = parseIntent(userMsg);
            setMessages(prev => [
                ...prev,
                { sender: 'ai', text: result.reply, draft: result.draft }
            ]);
            setIsTyping(false);
        }, 500);
    };

    const handleConfirmBooking = async (draft) => {
        try {
            await addLeave(draft);
            setMessages(prev => [
                ...prev,
                { sender: 'ai', text: `🎉 Your **${draft.leaveType}** request for **${draft.startDate}** to **${draft.endDate}** has been successfully submitted! Your manager has been notified.` }
            ]);
            setTimeout(() => {
                navigate('/view-leaves');
            }, 1200);
        } catch (err) {
            setMessages(prev => [
                ...prev,
                { sender: 'ai', text: `⚠️ Unable to submit leave request automatically. Please check network connection or use the standard form.` }
            ]);
        }
    };

    return (
        <div className="ai-assistant-wrapper">
            {!isOpen && (
                <button
                    className="ai-assistant-toggle"
                    onClick={() => setIsOpen(true)}
                    title="Open AI Leave Assistant"
                >
                    <FaRobot className="robot-icon" />
                    <span className="toggle-label">AI HR Assistant</span>
                </button>
            )}

            {isOpen && (
                <div className="ai-chat-window">
                    <div className="ai-chat-header">
                        <div className="ai-header-left">
                            <div className="ai-status-circle"></div>
                            <div>
                                <h4>AI Leave Assistant</h4>
                                <span className="ai-sub">Powered by Gemini & Antigravity</span>
                            </div>
                        </div>
                        <button className="ai-close-btn" onClick={() => setIsOpen(false)}>
                            <FaTimes />
                        </button>
                    </div>

                    <div className="ai-chat-messages">
                        {messages.map((m, idx) => (
                            <div key={idx} className={`ai-message-row ${m.sender}`}>
                                <div className={`ai-bubble ${m.sender}`}>
                                    {m.text}
                                    {m.draft && (
                                        <div className="ai-draft-card">
                                            <div className="draft-header">
                                                <FaCalendarCheck /> Leave Request Ready
                                            </div>
                                            <div className="draft-body">
                                                <div><strong>Type:</strong> {m.draft.leaveType}</div>
                                                <div><strong>From:</strong> {m.draft.startDate} <strong>To:</strong> {m.draft.endDate}</div>
                                                <div><strong>Reason:</strong> {m.draft.reason}</div>
                                            </div>
                                            <button
                                                className="btn-confirm-ai"
                                                onClick={() => handleConfirmBooking(m.draft)}
                                            >
                                                Confirm & Submit Leave
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        {isTyping && (
                            <div className="ai-message-row ai">
                                <div className="ai-bubble ai typing">
                                    <span className="dot"></span>
                                    <span className="dot"></span>
                                    <span className="dot"></span>
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="ai-chat-quick-chips">
                        <button onClick={() => setInput("How many leaves do I have left?")}>
                            <FaLightbulb /> Leave Balance
                        </button>
                        <button onClick={() => setInput("Book sick leave tomorrow")}>
                            <FaCalendarCheck /> Sick Leave Tomorrow
                        </button>
                        <button onClick={() => setInput("What is the leave policy?")}>
                            <FaComments /> Leave Policy
                        </button>
                    </div>

                    <div className="ai-chat-input-row">
                        <input
                            type="text"
                            placeholder="Type a request (e.g. 'Take Friday off')..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                        />
                        <button onClick={handleSend} className="ai-send-btn">
                            <FaPaperPlane />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AiAssistant;
