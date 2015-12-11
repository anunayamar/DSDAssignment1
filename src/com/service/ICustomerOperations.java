package com.service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICustomerOperations extends Remote{
	public String openAccount(String bank, String firstName, String lastName, String
			emailId, String phoneNumber, String password) throws RemoteException;
	
	public boolean getLoan(String accountNumber, String password,
			double loanAmount) throws RemoteException;
}
