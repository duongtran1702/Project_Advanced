package com.atmin.saber.service;

import com.atmin.saber.model.PC;
import java.util.List;
import java.util.Map;

public interface BookingService {
    List<PC> getAvailablePCsByZone();

    Map<String, List<PC>> getAvailablePCsGroupedByZone();
}
