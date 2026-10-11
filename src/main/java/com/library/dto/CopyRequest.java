package com.library.dto;

public class CopyRequest {
    private String accessionNumber;
    private String condition;
    private String status;

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
