package com.atmin.saber.service;

import java.time.YearMonth;
import java.util.List;

public interface StatisticsService {

    record DailyRow(java.time.LocalDate date,
                    java.math.BigDecimal sessionRevenue,
                    java.math.BigDecimal fnbRevenue,
                    java.math.BigDecimal topup,
                    java.math.BigDecimal payment,
                    java.math.BigDecimal refund) {
    }

    /** Daily report for a specific month (all days in that month). */
    List<DailyRow> dailyReport(YearMonth month);
}

