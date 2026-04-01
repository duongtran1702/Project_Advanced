package com.atmin.saber;

import com.atmin.saber.controller.AuthController;
import com.atmin.saber.dao.UserDao;
import com.atmin.saber.dao.impl.BookingDaoImpl;
import com.atmin.saber.dao.impl.PcDaoImpl;
import com.atmin.saber.dao.impl.TransactionDaoImpl;
import com.atmin.saber.dao.impl.UserDaoImpl;

import com.atmin.saber.service.AutoStopService;
import com.atmin.saber.service.AuthService;
import com.atmin.saber.service.SessionBillingService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.service.impl.AutoStopServiceImpl;
import com.atmin.saber.service.impl.AuthServiceImpl;
import com.atmin.saber.service.impl.SessionBillingServiceImpl;
import com.atmin.saber.service.impl.WalletServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		DBConnection db = DBConnection.getInstance();

		// Background worker: auto-stop ACTIVE sessions when wallet can't pay next
		// minute (Policy B)
		try {
			WalletService walletService = new WalletServiceImpl(new UserDaoImpl(db), new TransactionDaoImpl(db), db);
			SessionBillingService sessionBillingService = new SessionBillingServiceImpl(new BookingDaoImpl(db),
					new PcDaoImpl(db), walletService);
			AutoStopService autoStopService = new AutoStopServiceImpl(new BookingDaoImpl(db), sessionBillingService);
			autoStopService.start();
		} catch (RuntimeException ex) {
			System.err.println("[WARN] Auto-stop service not started: " + ex.getMessage());
		}
		UserDao userDao = new UserDaoImpl(db);
		AuthService authService = new AuthServiceImpl(userDao);
		Scanner sc = new Scanner(System.in);

		new AuthController(authService, sc).start();
	}
}
