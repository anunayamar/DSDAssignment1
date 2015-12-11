package com.customer.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.util.Date;

import com.config.Configuration;
import com.service.ICustomerOperations;

/**
 * This is a customer client interface through which the customer opens a bank
 * account and requests for loan.
 * 
 * @author Anunay
 * 
 */
public class CustomerClient {

	public static void main(String[] args) {

		System.setSecurityManager(new RMISecurityManager());
		CustomerClient client = new CustomerClient();
		client.performOperations();

	}

	/**
	 * This method is used to choose the bank.
	 */
	public void performOperations() {

		try {
			System.out.println("Welcome to RMI DLMS\n__________________\n");

			while (true) {

				System.out.println("\n  BANKS\n_______");
				System.out.println("1. RBC\n2. TD\n3. BMO\n4. Exit");
				System.out.println("\n Select your bank:");

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(System.in));
				int input = Integer.parseInt(reader.readLine());

				switch (input) {
				case 1:
					customerOperations("rmi://"
							+ Configuration.RMI_SERVER_1_NAME + ":"
							+ Configuration.RMI_SERVER_1_PORT + "/"
							+ Configuration.RMI_SERVER_1_ADDRESS, "RBC");
					break;
				case 2:
					customerOperations("rmi://"
							+ Configuration.RMI_SERVER_1_NAME + ":"
							+ Configuration.RMI_SERVER_1_PORT + "/"
							+ Configuration.RMI_SERVER_2_ADDRESS, "TD");
					break;
				case 3:
					customerOperations("rmi://"
							+ Configuration.RMI_SERVER_1_NAME + ":"
							+ Configuration.RMI_SERVER_1_PORT + "/"
							+ Configuration.RMI_SERVER_3_ADDRESS, "BMO");
					break;
				case 4:
					System.exit(0);
				default:
					System.out.println("Invalid input!!");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to select the type of operations available for the
	 * client.
	 * 
	 * @param rmiURL
	 * @param bankName
	 */
	public void customerOperations(String rmiURL, String bankName) {
		boolean exit = false;
		try {
			while (true) {
				ICustomerOperations clientOperation = (ICustomerOperations) Naming
						.lookup(rmiURL);

				System.out.println("   MENU\n____________");
				System.out.println("\n1. Open an account");
				System.out.println("\n2. Get a loan");
				System.out.println("\n3. Exit");

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(System.in));
				int input = Integer.parseInt(reader.readLine());

				switch (input) {
				case 1:
					System.out.println("Enter the following details:");

					System.out.println("First Name:");
					String firstName = reader.readLine();

					System.out.println("Last Name:");
					String lastName = reader.readLine();

					System.out.println("Email Id:");
					String emailId = reader.readLine();

					System.out.println("Phone Number:");
					String phoneNumber = reader.readLine();

					System.out.println("Password:");
					String password = reader.readLine();

					if (password.length() < 6) {
						System.out
								.println("Error!!\nPassword should atleast be of 6 characters!!");
						break;
					}

					System.out.println("Opening an account");
					String accountNumber = clientOperation.openAccount(
							bankName, firstName, lastName, emailId,
							phoneNumber, password);

					if (accountNumber != null) {
						logInformation(accountNumber, firstName + " "
								+ lastName + " has created an account, account number:"+accountNumber,
								bankName);
						System.out
								.println("Successfully open your account!!. \n Your accountNumber is "
										+ accountNumber);
					} else {
						System.out.println("Something went wrong, try again!!");
					}

					break;
				case 2:
					System.out.println("Enter the following details:");

					System.out.println("Account Number:");
					String accNumber = reader.readLine();

					System.out.println("Password:");
					String pwd = reader.readLine();

					System.out.println("Loan Amount:");
					double loanAmount = Double.parseDouble(reader.readLine());

					System.out.println("Getting a loan!!");
					logInformation(accNumber,
							"Customer has initiated a loan request for the amount"
									+ loanAmount + "$", bankName);
					boolean result = clientOperation.getLoan(accNumber, pwd,
							loanAmount);

					if (result) {
						logInformation(accNumber,
								"Customer's request for the loan of amount"
										+ loanAmount + "$ has been approved",
								bankName);
						System.out
								.println("Congrats, Your loan request has been approved!!");
					} else {
						logInformation(accNumber,
								"Customer's request for the loan of amount"
										+ loanAmount + "$ has been rejected",
								bankName);
						System.out
								.println("Sorry, you request was rejected!! Try again later.");
					}

					break;
				case 3:
					exit = true;
					break;
				default:
					System.out.println("Invalid input!!");
				}

				if (exit) {
					break;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

	}

	public void logInformation(String accountId, String message, String bankName) {
		try {

			File file = new File("Log-" + bankName + "-" + accountId + ".txt");
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));

			String logMessage = "" + (new Date()).toString() + " : " + message;

			out.println(logMessage);
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
