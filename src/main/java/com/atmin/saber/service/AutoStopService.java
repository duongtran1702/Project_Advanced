package com.atmin.saber.service;

/**
 * Background service that periodically checks ACTIVE sessions and auto-stops them
 * when the wallet can no longer pay for the next minute.
 */
public interface AutoStopService {
    void start();
}

