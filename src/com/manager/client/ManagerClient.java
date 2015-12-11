package com.manager.client;

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
import com.service.IManagerOperations;

/**
 * This is manager client interface through which manager performs its opertions.
 * @author Anunay
 *
 */
public class ManagerClient {

	public static void main(String[] args) {

		System.setSecurityManager(new RMISecurityManager());
		ManagerClient client = new ManagerClient();
		client.performOperations();

	}

	/**
	 * Through this manager can choose the bank.
	 */
	public void performOperations() {

		try {
			System.out
					.println("Welcome to RMI DLMS for Managers\n__________________\n");

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
	 * This provides the list of available operations.
	 * @param rmiURL
	 * @param bankName
	 */
	@SuppressWarnings("deprecation")	
	public void customerOperations(String rmiURL, String bankName) {
		boolean exit = false;
		try {
			while (true) {
				IManagerOperations managerOperation = (IManagerOperations) Naming
						.lookup(rmiURL);

				System.out.println("   MENU\n____________");
				System.out.println("\n1. Delay Loan Payment");
				System.out.println("\n2. Print customer information");
				System.out.println("\n3. Exit");

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(System.in));
				int input = Integer.parseInt(reader.readLine());

				switch (input) {
				case 1:
					System.out.println("Enter the following details:");

					System.out.println("Loan Id:");
					String loanId = reader.readLine();

					try {
						System.out.println("Current due date (dd-mm-yyyy):");
						String currentDueDate = reader.readLine();

						String strCurrentDate[] = currentDueDate.split("-");

						int currentDD = Integer.parseInt(strCurrentDate[0]);
						int currentMM = Integer.parseInt(strCurrentDate[1]);
						int currentYYYY = Integer.parseInt(strCurrentDate[2]);

						Date currentDate = null;
						if (currentDD < 31 && currentDD > 0 && currentMM <= 12
								&& currentMM > 0 && currentYYYY > 1900) {
							currentDate = new Date(currentYYYY - 1900,
									currentMM - 1, currentDD);

						} else {
							throw new NumberFormatException();
						}

						new Date(115, 9, 12);

						System.out.println("New due date (dd-mm-yyyy):");
						String newDueDate = reader.readLine();

						String strNewDate[] = newDueDate.split("-");

						int newDD = Integer.parseInt(strNewDate[0]);
						int newMM = Integer.parseInt(strNewDate[1]);
						int newYYYY = Integer.parseInt(strNewDate[2]);

						Date newDate = null;
						if (newDD < 31 && newDD > 0 && newMM <= 12 && newMM > 0
								&& newYYYY > 1900) {
							newDate = new Date(newYYYY - 1900, newMM - 1, newDD);

						} else {
							throw new NumberFormatException();
						}

						System.out.println("Delaying the loan payment");
						
						logInformation("Manager has initated a delay payment request for the Loan ID: "+loanId, bankName);
						boolean result = managerOperation.delayPayment(
								bankName, loanId, currentDate, newDate);
						
						if (result) {
							logInformation("Manager has successfully delayed payment request for the Loan ID: "+loanId, bankName);
							System.out
									.println("Successfully delay the payment due date.");
						} else {
							logInformation("Manager's request for delay payment for the Loan ID: "+loanId+" has failed.", bankName);
							System.out
									.println("Something went wrong, try again!!");
						}

						break;

					} catch (NumberFormatException e) {
						System.out.println("Invalid date format!!");
						break;
					}

				case 2:
					logInformation("Manager has initated a request to print customer and loan information", bankName);
					System.out.println(managerOperation.printCustomerInfo(bankName));
					logInformation("Manager request to print customer and loan information has succeed.", bankName);
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
	
	public void logInformation(String message, String bankName) {
		try {

			File file = new File("Log-" + bankName + "-Manager.txt");
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
