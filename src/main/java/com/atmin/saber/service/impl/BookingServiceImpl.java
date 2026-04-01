package com.atmin.saber.service.impl;

import com.atmin.saber.dao.PcDao;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.service.BookingService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingServiceImpl implements BookingService {

    private final PcDao pcDao;

    public BookingServiceImpl(PcDao pcDao) {
        this.pcDao = pcDao;
    }

    @Override
    public List<PC> getAvailablePCsByZone() {
        return pcDao.findAllActive().stream()
                .filter(pc -> pc.getStatus() == PCStatus.AVAILABLE)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Map<String, List<PC>> getAvailablePCsGroupedByZone() {
        List<PC> availablePCs = getAvailablePCsByZone();
        Map<String, List<PC>> groupedPCs = new LinkedHashMap<>();

        for (PC pc : availablePCs) {
            groupedPCs.computeIfAbsent(pc.getRoomName(), k -> new java.util.ArrayList<>()).add(pc);
        }

        return groupedPCs;
    }
}
