package com.atmin.saber.dao;

import java.math.BigDecimal;
import java.util.Map;

public interface StatisticsDao {

    Map<java.time.LocalDate, BigDecimal> sessionRevenueByDay(java.time.YearMonth month);

    Map<java.time.LocalDate, BigDecimal> fnbRevenueByDay(java.time.YearMonth month);

    Map<java.time.LocalDate, BigDecimal> topupByDay(java.time.YearMonth month);

    Map<java.time.LocalDate, BigDecimal> paymentByDay(java.time.YearMonth month);

    Map<java.time.LocalDate, BigDecimal> refundByDay(java.time.YearMonth month);
}

