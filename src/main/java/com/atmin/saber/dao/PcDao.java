package com.atmin.saber.dao;

import com.atmin.saber.model.PC;

import java.util.List;
import java.util.Optional;

public interface PcDao {
    List<PC> findAllActive();

    List<PC> findAllActiveByRoomId(int roomId);

    Optional<PC> findById(int pcId);

    void insert(PC pc);

    void update(PC pc);

    void softDelete(int pcId);
}

