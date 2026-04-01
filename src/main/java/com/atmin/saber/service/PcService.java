package com.atmin.saber.service;

import com.atmin.saber.model.PC;

import java.util.List;
import java.util.Optional;

public interface PcService {
    List<PC> getAllActive();

    List<PC> getAllActiveByRoomId(int roomId);

    Optional<PC> getById(int pcId);

    void add(PC pc);

    void update(PC pc);

    void delete(int pcId);
}

