package com.examly.springapp.model;

public class AiChatResponse {
    private String reply;
    private String intent; // "LEAVE_BOOKING", "BALANCE_INQUIRY", "POLICY_QUESTION", "GENERAL"
    private Leave suggestedLeave;

    public AiChatResponse() {
    }

    public AiChatResponse(String reply, String intent, Leave suggestedLeave) {
        this.reply = reply;
        this.intent = intent;
        this.suggestedLeave = suggestedLeave;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Leave getSuggestedLeave() {
        return suggestedLeave;
    }

    public void setSuggestedLeave(Leave suggestedLeave) {
        this.suggestedLeave = suggestedLeave;
    }
}
