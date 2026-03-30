package com.atmin.saber.dao;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public interface StatisticsDao {
    Map<YearMonth, BigDecimal> sessionRevenueByMonth(int year);

    Map<YearMonth, BigDecimal> fnbRevenueByMonth(int year);

    Map<YearMonth, BigDecimal> topupByMonth(int year);

    Map<YearMonth, BigDecimal> paymentByMonth(int year);
}

