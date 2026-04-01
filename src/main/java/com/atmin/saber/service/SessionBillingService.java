package com.atmin.saber.service;

import com.atmin.saber.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionBillingService {

    /**
     * Staff: start a booking (PENDING -> ACTIVE) and set PC status to IN_USE.
     */
    void startBooking(String customerId, int pcId, LocalDateTime startTime);

    /**
     * Staff: stop customer's ACTIVE booking, complete it, and charge wallet.
     * If wallet is insufficient, the booking is NOT completed.
     *
     * @return charged amount if completed.
     */
    Optional<BigDecimal> stopActiveBookingAndCharge(String customerId);

    /**
     * Customer: view currently active booking.
     */
    Optional<Booking> getActiveBookingOfCustomer(String customerId);

    /**
     * Policy B (auto-stop): if a customer has an ACTIVE session and the wallet can no longer pay
     * for the next minute, the system stops the session at the maximum payable minute and charges
     * exactly that amount.
     */
    void autoStopIfOutOfBalance(String customerId);

    /**
     * Helper calculation: how much debt the customer currently owes for PC usage in their active session.
     */
    BigDecimal calculateCurrentDebt(String customerId);
}

