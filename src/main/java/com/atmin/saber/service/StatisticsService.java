package com.atmin.saber.service;

import java.time.YearMonth;
import java.util.List;

public interface StatisticsService {

    record MonthlyRow(YearMonth month,
                      java.math.BigDecimal sessionRevenue,
                      java.math.BigDecimal fnbRevenue,
                      java.math.BigDecimal topup,
                      java.math.BigDecimal payment) {
    }

    List<MonthlyRow> monthlyReport(int year);
}

