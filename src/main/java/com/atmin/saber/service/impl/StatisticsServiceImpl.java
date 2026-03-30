package com.atmin.saber.service.impl;

import com.atmin.saber.dao.StatisticsDao;
import com.atmin.saber.service.StatisticsService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsDao statisticsDao;

    public StatisticsServiceImpl(StatisticsDao statisticsDao) {
        this.statisticsDao = Objects.requireNonNull(statisticsDao, "statisticsDao must not be null");
    }

    @Override
    public List<MonthlyRow> monthlyReport(int year) {
        Map<YearMonth, BigDecimal> session = statisticsDao.sessionRevenueByMonth(year);
        Map<YearMonth, BigDecimal> fnb = statisticsDao.fnbRevenueByMonth(year);
        Map<YearMonth, BigDecimal> topup = statisticsDao.topupByMonth(year);
        Map<YearMonth, BigDecimal> payment = statisticsDao.paymentByMonth(year);

        Set<YearMonth> months = new HashSet<>();
        months.addAll(session.keySet());
        months.addAll(fnb.keySet());
        months.addAll(topup.keySet());
        months.addAll(payment.keySet());

        // Always show all 12 months for requested year
        for (int m = 1; m <= 12; m++) {
            months.add(YearMonth.of(year, m));
        }

        List<YearMonth> sorted = new ArrayList<>(months);
        sorted.sort(YearMonth::compareTo);

        List<MonthlyRow> rows = new ArrayList<>();
        for (YearMonth ym : sorted) {
            if (ym.getYear() != year) continue;
            rows.add(new MonthlyRow(
                    ym,
                    session.getOrDefault(ym, BigDecimal.ZERO),
                    fnb.getOrDefault(ym, BigDecimal.ZERO),
                    topup.getOrDefault(ym, BigDecimal.ZERO),
                    payment.getOrDefault(ym, BigDecimal.ZERO)
            ));
        }
        return rows;
    }
}

