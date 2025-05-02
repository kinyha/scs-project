package com.pharmacy.scs.dto;

import lombok.Data;

@Data
public class PerformanceResultDTO {
    private String queryName;
    private String querySql;
    private int rowCount;
    private long executionTimeMs;
    private String explainPlan;
    private String error;
}
