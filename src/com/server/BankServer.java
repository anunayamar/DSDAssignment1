package com.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.beans.Customer;
import com.beans.Loan;
import com.beans.Manager;
import com.config.Configuration;
import com.log.Logger;
import com.service.ICustomerOperations;
import com.service.IManagerOperations;

/**
 * This is a muli-threaded bank server that creates 3 bank servers
 * 
 * @author Anunay
 * 
 */
public class BankServer implements Runnable, ICustomerOperations,
		IManagerOperations {

	private volatile AtomicInteger counter = new AtomicInteger(10000);
	private volatile AtomicInteger loanCounter = new AtomicInteger(200000);

	private HashMap<Character, List<Customer>> accountMap = new HashMap<Character, List<Customer>>();
	private HashMap<Character, List<Loan>> loanMap = new HashMap<Character, List<Loan>>();
	private List<Manager> managerList = new ArrayList<Manager>();

	private String bankName;
	private boolean udpServer;
	private BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(50);

	// This is a ReadWrite lock for customer account
	ReadWriteLock accountReadWriteLock = new ReentrantReadWriteLock();
	// This is a ReadWrite lock for customer loan
	ReadWriteLock loanReadWriteLock = new ReentrantReadWriteLock();

	public BankServer(String bankName) {
		super();
		this.bankName = bankName;
	}

	public void run() {

		try {
			Thread loggerThread = new Thread(
					new Logger(blockingQueue, bankName));
			loggerThread.start();

			for (int i = 1; i <= 10; i++) {
				createCustomerAccount(i);
			}

			for (int i = 1; i < 5; i++) {
				createCustomerLoan(i);
			}

			for (int i = 1; i < 2; i++) {
				createManager(i);
			}
			// System.out.println("Account:" + accountMap);
			// System.out.println("Loan:" + loanMap);

			exportServer(bankName.toLowerCase());
			System.out.println(bankName + " bank server is up and running");
			bankUDPListener();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This creates a bank UDP server. This UDP server listens for the incoming
	 * request from the client.
	 */
	public void bankUDPListener() {
		int port = 0;
		if (bankName.equals("RBC")) {
			port = Configuration.UDP_SERVER_1_PORT;
		} else if (bankName.equals("TD")) {
			port = Configuration.UDP_SERVER_2_PORT;
		} else if (bankName.equals("BMO")) {
			port = Configuration.UDP_SERVER_3_PORT;
		}

		// UDP Server is on
		udpServer = true;

		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket(port);
			byte[] buffer = new byte[1000];

			while (true) {
				DatagramPacket request = new DatagramPacket(buffer,
						buffer.length);

				socket.receive(request);

				String message = new String(request.getData(), 0,
						request.getLength());
				// System.out.println("Server:" + message);

				String messagePart[] = message.split(",");
				String replyMessage = isCreditOK(messagePart[0],
						messagePart[1], messagePart[2]).toString();

				DatagramPacket reply = new DatagramPacket(
						replyMessage.getBytes(),
						replyMessage.getBytes().length, request.getAddress(),
						request.getPort());
				socket.send(reply);
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}

	}

	/**
	 * This method check whether the customer has good credit standing.
	 * 
	 * @param firstName
	 * @param lastName
	 * @param phoneNumber
	 * @return True or False for success and failure respectively
	 */
	public Boolean isCreditOK(String firstName, String lastName,
			String phoneNumber) {
		List<Loan> loanList = loanMap.get(firstName.charAt(0));
		if (loanList != null) {
			Iterator<Loan> iterator = loanList.iterator();

			while (iterator.hasNext()) {
				Loan loan = iterator.next();
				Customer account = loan.getAccount();

				if (firstName.equals(account.getFirstName())
						&& lastName.equals(account.getLastName())
						&& phoneNumber.equals(account.getPhoneNumber())) {
					if (account.getCreditLimit() > 0) {
						return true;
					} else {
						return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * This method checks whether the customer has good credits with other banks
	 * 
	 * @param firstName
	 * @param lastName
	 * @param phoneNumber
	 */
	public String creditCheckUDP(String firstName, String lastName,
			String phoneNumber, int portNumber) {
		int port = portNumber;

		String replyMessage = "false";
		String message = firstName + "," + lastName + "," + phoneNumber;

		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			// byte[] m = "hello".getBytes();
			byte[] m = message.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			int serverPort = port;

			DatagramPacket request = new DatagramPacket(m, m.length, host,
					serverPort);
			socket.send(request);

			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

			socket.receive(reply);
			replyMessage = new String(reply.getData(), 0, reply.getLength());

			// System.out.println(bankName+" Reply:" + replyMessage);

			return replyMessage;
		} catch (SocketException e) {
			e.printStackTrace();
			return replyMessage;
		} catch (IOException e) {
			e.printStackTrace();
			return replyMessage;
		} finally {
			if (socket != null) {
				socket.close();
			}

		}

	}

	/**
	 * This method creates customer account for testing purpose. It is only
	 * accessible to system user.
	 * 
	 * @param i
	 */
	public void createCustomerAccount(int i) {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(bankName + "UserGenerator.properties");

			prop.load(input);
			String inputString = prop.getProperty("user" + i);
			String str[] = inputString.split("\\s");

			String firstName = str[0];
			String lastName = str[1];
			String emailId = str[2];
			String phoneNumber = str[3];
			String password = str[4];

			openAccount(bankName, firstName, lastName, emailId, phoneNumber,
					password);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * This method creates manager for testing purpose. It is only accessible to
	 * system user.
	 * 
	 * @param i
	 */
	public void createManager(int i) {

		String firstName = "fname" + bankName.toLowerCase() + "" + i;
		String lastName = "lname" + bankName.toLowerCase() + "" + i;
		String emailId = bankName.toLowerCase() + "" + i + "@gmail.com";
		String phoneNumber = "514-699-401" + (10 + i);
		String password = "Manager";
		String userName = "Manager";

		Manager manager = new Manager();

		manager.setFirstName(firstName);
		manager.setLastName(lastName);
		manager.setEmailId(emailId);
		manager.setPassword(password);
		manager.setPhoneNumber(phoneNumber);
		manager.setUserName(userName);

		managerList.add(manager);
	}

	/**
	 * This method creates customer loan for testing purpose. It is only
	 * accessible to the system user.
	 * 
	 * @param i
	 */
	public void createCustomerLoan(int i) {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(bankName + "UserGenerator.properties");

			prop.load(input);
			String inputString = prop.getProperty("user" + i);
			String str[] = inputString.split("\\s");

			String firstName = str[0];

			getLoan(firstName.charAt(0) + "" + (10000 + i), bankName + "User"
					+ i, 300);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static void main(String arg[]) throws Exception {

		Thread server1 = new Thread(new BankServer("RBC"), "RBC Thread");
		Thread server2 = new Thread(new BankServer("TD"), "TD Thread");
		Thread server3 = new Thread(new BankServer("BMO"), "BMO Thread");

		server1.start();
		server2.start();
		server3.start();

	}

	/**
	 * This method export the remote object and store it into RMI registry.
	 * 
	 * @param name
	 * @throws Exception
	 */
	public void exportServer(String name) throws Exception {

		blockingQueue.put(new Date() + ": " + bankName
				+ " server entered exportServer");

		Remote remoteObject = null;
		Registry registry = null;

		try {

			if (bankName.equals("RBC")) {
				remoteObject = UnicastRemoteObject.exportObject(this,
						Configuration.RMI_SERVER_1_PORT);
				registry = LocateRegistry
						.createRegistry(Configuration.RMI_SERVER_1_PORT);
			} else if (bankName.equals("TD")) {
				remoteObject = UnicastRemoteObject.exportObject(this,
						Configuration.RMI_SERVER_2_PORT);
				registry = LocateRegistry
						.createRegistry(Configuration.RMI_SERVER_2_PORT);
			} else if (bankName.equals("BMO")) {
				remoteObject = UnicastRemoteObject.exportObject(this,
						Configuration.RMI_SERVER_3_PORT);
				registry = LocateRegistry
						.createRegistry(Configuration.RMI_SERVER_3_PORT);
			}

			registry.bind(name, remoteObject);
		} catch (Exception e) {
			if (e.getMessage().contains("internal error: ObjID already in use")) {

				if (bankName.equals("RBC")) {
					registry = LocateRegistry
							.getRegistry(Configuration.RMI_SERVER_1_PORT);
				} else if (bankName.equals("TD")) {
					registry = LocateRegistry
							.getRegistry(Configuration.RMI_SERVER_2_PORT);
				} else if (bankName.equals("BMO")) {
					registry = LocateRegistry
							.getRegistry(Configuration.RMI_SERVER_3_PORT);
				}

				registry.bind(name, remoteObject);
			} else {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method opens a bank account
	 * 
	 * @param bankName
	 * @param firstName
	 * @param lastName
	 * @param emailId
	 * @param phoneNumber
	 * @param password
	 * 
	 * @return accountNumber
	 */
	@Override
	public String openAccount(String bank, String firstName, String lastName,
			String emailId, String phoneNumber, String password)
			throws RemoteException {

		try {
			blockingQueue.put(new Date() + ": " + firstName + " " + lastName
					+ " has initiated a request to open an account at " + bank);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Customer account = new Customer();

		account.setFirstName(firstName);
		account.setLastName(lastName);
		account.setEmailId(emailId);
		account.setPhoneNumber(phoneNumber);
		account.setPassword(password);
		account.setCreditLimit(1000);

		String accountNumber = null;
		synchronized (counter) {
			int accountSequence = counter.incrementAndGet();

			accountNumber = firstName.charAt(0) + "" + accountSequence;
			// counter.set(new AtomicInteger(accountSequence));
			account.setAccountNumber(accountNumber);
		}

		accountReadWriteLock.writeLock().lock();
		List<Customer> accountList = accountMap.get(accountNumber.charAt(0));
		if (accountList == null) {

			accountList = new ArrayList<Customer>();
		} else {
			Iterator<Customer> accountIterator = accountList.iterator();

			while (accountIterator.hasNext()) {
				Customer acct = accountIterator.next();

				if ((acct.getFirstName()).equals(firstName)
						&& (acct.getLastName().equals(lastName))
						&& (acct.getEmailId()).equals(emailId)) {
					accountReadWriteLock.writeLock().unlock();
					return null;
				}

			}

		}

		accountList.add(account);
		accountMap.put(firstName.charAt(0), accountList);
		accountReadWriteLock.writeLock().unlock();

		return accountNumber;
	}

	/**
	 * This method approves or rejects a loan request.
	 * 
	 * @param accountNumber
	 * @param password
	 * @param loanAmount
	 * 
	 * @return success/failure
	 */
	@Override
	public boolean getLoan(String accountNumber, String password,
			double loanAmount) throws RemoteException {

		// This add log information to the log file
		try {
			blockingQueue.put(new Date() + ": AccountNumber:" + accountNumber
					+ " has initiated a loan request for " + loanAmount
					+ " at " + bankName);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		accountReadWriteLock.writeLock().lock();
		Customer customerAccount = getAccount(accountNumber, password);
		String loanId = null;
		int port1 = 0;
		int port2 = 0;

		if (customerAccount == null) {
			accountReadWriteLock.writeLock().unlock();
			return false;
		}

		if (udpServer) {
			if (bankName.equals("RBC")) {
				port1 = Configuration.UDP_SERVER_2_PORT;
				port2 = Configuration.UDP_SERVER_3_PORT;
			} else if (bankName.equals("TD")) {
				port1 = Configuration.UDP_SERVER_1_PORT;
				port2 = Configuration.UDP_SERVER_3_PORT;
			} else if (bankName.equals("BMO")) {
				port1 = Configuration.UDP_SERVER_1_PORT;
				port2 = Configuration.UDP_SERVER_2_PORT;
			}

			boolean isCreditGood = Boolean.parseBoolean(creditCheckUDP(
					customerAccount.getFirstName(),
					customerAccount.getLastName(),
					customerAccount.getPhoneNumber(), port1))
					&& Boolean.parseBoolean(creditCheckUDP(
							customerAccount.getFirstName(),
							customerAccount.getLastName(),
							customerAccount.getPhoneNumber(), port2));

			if (!isCreditGood) {
				accountReadWriteLock.writeLock().unlock();
				return false;
			}
		}

		if (customerAccount != null) {

			if (customerAccount.getCreditLimit() >= loanAmount) {
				// Updating the new credit limit for customer

				customerAccount.setCreditLimit(customerAccount.getCreditLimit()
						- loanAmount);

				Loan loan = new Loan();

				synchronized (loanCounter) {
					loanId = accountNumber.charAt(0) + ""
							+ loanCounter.incrementAndGet();
					loan.setLoanId(loanId);
				}

				loan.setLoanAmount(loanAmount);

				Date date = new Date();
				if (loanAmount < 2000) {
					date.setMonth(date.getMonth() + 12);
				} else {
					date.setMonth(date.getMonth() + 24);
				}
				loan.setLoanDueDate(date);
				loan.setAccount(customerAccount);

				// Store the loan in loanMap

				loanReadWriteLock.writeLock().lock();
				saveLoan(loanId, loan);
				loanReadWriteLock.writeLock().unlock();

				updateAccountMap(customerAccount);

				/* System.out.println(accountMap); */
				accountReadWriteLock.writeLock().unlock();
				return true;
			}
		}

		// System.out.println(accountMap);
		accountReadWriteLock.writeLock().unlock();
		return false;
	}

	/**
	 * This method takes accountNumner and password and returns customer
	 * account.
	 * 
	 * @param accountNumber
	 * @param password
	 * @return CustomerAccount
	 */
	public Customer getAccount(String accountNumber, String password) {
		List<Customer> accountList = accountMap.get(accountNumber.charAt(0));

		if (accountList != null) {
			Iterator<Customer> iterator = accountList.iterator();

			while (iterator.hasNext()) {
				Customer account = iterator.next();

				if (account.getAccountNumber().equals(accountNumber)
						&& account.getPassword().equals(password)) {

					return account;
				}
			}
			return null;

		} else {
			return null;
		}

	}

	/**
	 * It updates the accountMap with an update account object.
	 * 
	 * @param account
	 * @return
	 */

	public boolean updateAccountMap(Customer account) {
		List<Customer> accountList = accountMap.get(account.getAccountNumber()
				.charAt(0));
		boolean accountPresent = false;

		if (accountList != null) {
			Iterator<Customer> iterator = accountList.iterator();

			while (iterator.hasNext()) {
				Customer customerAccount = iterator.next();

				if (account == customerAccount) {
					accountPresent = true;
					break;
				}
			}

		}

		if (accountPresent) {
			accountList.remove(account);
			accountList.add(account);
			accountMap.put(account.getAccountNumber().charAt(0), accountList);
			return true;
		}

		return false;
	}

	/**
	 * It stores the loan object into a loanMap.
	 * 
	 * @param loanId
	 * @param loan
	 */
	public void saveLoan(String loanId, Loan loan) {
		List<Loan> loanList = loanMap.get(loanId.charAt(0));
		if (loanList == null) {
			loanList = new ArrayList<Loan>();
		}
		loanList.add(loan);

		loanMap.put(loanId.charAt(0), loanList);
	}

	/**
	 * This method is used by manager to change the due date of customer loan
	 * 
	 * @param bank
	 * @param loanId
	 * @param currentDueDate
	 * @param newDueDate
	 * 
	 * @return True or False for success and failure respectively
	 */
	@Override
	public boolean delayPayment(String bank, String loanId,
			Date currentDueDate, Date newDueDate) throws RemoteException {

		// This add log information to the log file
		try {
			blockingQueue.put(new Date()
					+ ": Manager has initiated a request to delay payment for "
					+ loanId + " at " + bank);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		loanReadWriteLock.writeLock().lock();
		// System.out.println(loanMap);
		List<Loan> loanList = loanMap.get(loanId.charAt(0));

		if (loanList != null) {
			Iterator<Loan> loanIterator = loanList.iterator();

			Loan loan = null;
			boolean loanFound = false;

			while (loanIterator.hasNext()) {
				loan = loanIterator.next();

				if (loan.getLoanId().equals(loanId)) {
					loanFound = true;
					break;
				}
			}

			if (loanFound) {

				loanList.remove(loan);
				loanMap.remove(loanId.charAt(0));

				loan.setLoanDueDate(newDueDate);
				loanList.add(loan);

				loanMap.put(loanId.charAt(0), loanList);

				// System.out.println(loanMap);

				loanReadWriteLock.writeLock().unlock();
				return true;
			}
		}
		loanReadWriteLock.writeLock().unlock();
		return false;

	}

	/**
	 * This method prints the customer information and loan information. This
	 * method is only accessible to managers.
	 * 
	 * @param bankName
	 * 
	 */
	@Override
	public String printCustomerInfo(String bank) throws RemoteException {
		// This add log information to the log file
		
		String resultset="";
		
		try {
			blockingQueue
					.put(new Date()
							+ ": Manager has initiated a request for printing customer information at "
							+ bank);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		accountReadWriteLock.readLock().lock();

		List<List<Customer>> accountList = new ArrayList<List<Customer>>(
				accountMap.values());
		Iterator<List<Customer>> iterator = accountList.iterator();

		System.out.println("\n CUSTOMER ACCOUNT INFORMATION - "
				+ bankName.toUpperCase()
				+ "\n__________________________________________________");
		resultset=resultset.concat("\n\n CUSTOMER ACCOUNT INFORMATION - "
				+ bankName.toUpperCase()
				+ "\n__________________________________________________");
		while (iterator.hasNext()) {
			List<Customer> accList = iterator.next();

			Iterator<Customer> itr = accList.iterator();

			while (itr.hasNext()) {
				Customer cust=itr.next();
				System.out.println(cust);
				resultset=resultset.concat("\n"+cust.toString());
			}
		}

		accountReadWriteLock.readLock().unlock();

		loanReadWriteLock.readLock().lock();

		List<List<Loan>> loanList = new ArrayList<List<Loan>>(loanMap.values());
		Iterator<List<Loan>> loanIterator = loanList.iterator();

		System.out.println("\n CUSTOMER LOAN INFORMATION - "
				+ bankName.toUpperCase()
				+ "\n__________________________________________________");
		resultset=resultset.concat("\n\n CUSTOMER LOAN INFORMATION - "
				+ bankName.toUpperCase()
				+ "\n__________________________________________________");

		while (loanIterator.hasNext()) {
			List<Loan> lnList = loanIterator.next();

			Iterator<Loan> itr = lnList.iterator();

			while (itr.hasNext()) {
				Loan ln=itr.next();
				System.out.println(ln);
				resultset=resultset.concat("\n"+ln.toString());
			}
		}

		loanReadWriteLock.readLock().unlock();
		return resultset;

	}

}