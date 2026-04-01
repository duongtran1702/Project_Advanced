package com.atmin.saber.service.impl;

import com.atmin.saber.dao.StatisticsDao;
import com.atmin.saber.service.StatisticsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsDao statisticsDao;

    public StatisticsServiceImpl(StatisticsDao statisticsDao) {
        this.statisticsDao = Objects.requireNonNull(statisticsDao, "statisticsDao must not be null");
    }

    @Override
    public List<DailyRow> dailyReport(YearMonth month) {
        Objects.requireNonNull(month, "month must not be null");

        Map<LocalDate, BigDecimal> session = statisticsDao.sessionRevenueByDay(month);
        Map<LocalDate, BigDecimal> fnb = statisticsDao.fnbRevenueByDay(month);
        Map<LocalDate, BigDecimal> topup = statisticsDao.topupByDay(month);
        Map<LocalDate, BigDecimal> payment = statisticsDao.paymentByDay(month);
        Map<LocalDate, BigDecimal> refund = statisticsDao.refundByDay(month);

        List<DailyRow> rows = new ArrayList<>();
        int days = month.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            LocalDate date = month.atDay(d);
            rows.add(new DailyRow(
                    date,
                    session.getOrDefault(date, BigDecimal.ZERO),
                    fnb.getOrDefault(date, BigDecimal.ZERO),
                    topup.getOrDefault(date, BigDecimal.ZERO),
                    payment.getOrDefault(date, BigDecimal.ZERO),
                    refund.getOrDefault(date, BigDecimal.ZERO)
            ));
        }
        return rows;
    }
}

