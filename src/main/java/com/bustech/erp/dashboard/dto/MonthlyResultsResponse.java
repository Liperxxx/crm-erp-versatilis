package com.bustech.erp.dashboard.dto;

import java.util.List;

public record MonthlyResultsResponse(
    String companyAName,
    String companyBName,
    List<MonthlyResultsPoint> points
) {}