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

    record DailyRow(java.time.LocalDate date,
                    java.math.BigDecimal sessionRevenue,
                    java.math.BigDecimal fnbRevenue,
                    java.math.BigDecimal topup,
                    java.math.BigDecimal payment) {
    }

    /** Daily report for a specific month (all days in that month). */
    List<DailyRow> dailyReport(YearMonth month);
}

