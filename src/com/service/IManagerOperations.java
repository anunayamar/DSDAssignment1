package com.service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

public interface IManagerOperations extends Remote{
	public boolean delayPayment(String bank, String loanId, Date currentDueDate, Date newDueDate) throws RemoteException;
	public String printCustomerInfo(String bank) throws RemoteException;
}
