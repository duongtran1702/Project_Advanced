package com.atmin.saber.service.impl;

import com.atmin.saber.dao.PcDao;
import com.atmin.saber.model.PC;
import com.atmin.saber.service.PcService;

import java.util.List;
import java.util.Optional;

public class PcServiceImpl implements PcService {

    private final PcDao pcDao;

    public PcServiceImpl(PcDao pcDao) {
        this.pcDao = pcDao;
    }

    @Override
    public List<PC> getAllActive() {
        return pcDao.findAllActive();
    }

    @Override
    public List<PC> getAllActiveByRoomId(int roomId) {
        return pcDao.findAllActiveByRoomId(roomId);
    }

    @Override
    public Optional<PC> getById(int pcId) {
        return pcDao.findById(pcId);
    }

    @Override
    public void add(PC pc) {
        pcDao.insert(pc);
    }

    @Override
    public void update(PC pc) {
        pcDao.update(pc);
    }

    @Override
    public void delete(int pcId) {
        pcDao.softDelete(pcId);
    }
}

