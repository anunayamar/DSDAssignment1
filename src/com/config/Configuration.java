package com.config;
public class Configuration {
	
	/*
	Configuration for RMI project
	
	Note that your server shall be registered as (for example SERVER 1)
		rmi://RMI_SERVER_1_NAME:RMI_SERVER_1_PORT/RMI_SERVER_1_ADDRESS
	*/
	public static final String RMI_SERVER_1_NAME = "localhost";
	public static final String RMI_SERVER_2_NAME = "localhost";
	public static final String RMI_SERVER_3_NAME = "localhost";
	
	public static final String RMI_SERVER_1_ADDRESS = "rbc";
	public static final String RMI_SERVER_2_ADDRESS = "td";
	public static final String RMI_SERVER_3_ADDRESS = "bmo";
	
	public static final int RMI_SERVER_1_PORT = 2020;
	public static final int RMI_SERVER_2_PORT = 2020;
	public static final int RMI_SERVER_3_PORT = 2020;
	
	public static final int UDP_SERVER_1_PORT = 3131;
	public static final int UDP_SERVER_2_PORT = 3132;
	public static final int UDP_SERVER_3_PORT = 3133;
	
}