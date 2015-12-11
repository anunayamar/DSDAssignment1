package com.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;

public class Logger implements Runnable {

	private BlockingQueue<String> queue = null;
	private String bankName;

	public Logger(BlockingQueue<String> queue, String bankName) {
		this.queue = queue;
		this.bankName=bankName;
	}

	public void run() {

		try {

			while (true) {
				File file = new File("Log-"+bankName+".txt");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new FileWriter(file, true)));

				String message = queue.take();

				out.println(message);
				out.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
