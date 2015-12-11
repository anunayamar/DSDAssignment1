package com.beans;

import java.util.Date;

public class Loan {
	private String loanId;
	private Customer account;
	private double loanAmount;
	private Date loanDueDate;
	
	public String getLoanId() {
		return loanId;
	}
	public void setLoanId(String loanId) {
		this.loanId = loanId;
	}
	public Customer getAccount() {
		return account;
	}
	public void setAccount(Customer account) {
		this.account = account;
	}
	public double getLoanAmount() {
		return loanAmount;
	}
	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
	}
	public Date getLoanDueDate() {
		return loanDueDate;
	}
	public void setLoanDueDate(Date loanDueDate) {
		this.loanDueDate = loanDueDate;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof Loan){
			Loan loan=(Loan)obj;
			if(loanId.equals(loan.getLoanId())){
				return true;
			}
		}
		return false;
	}
	
	public String toString(){
		/*return "loanId:"+ loanId + " account:"+account+" loanAmount:"+loanAmount+" loanDueDate:"+loanDueDate+"\n";*/
		String message= "loan ID: "+ loanId + ", Account Number: "+account+", Loan Amount: "+loanAmount+", Loan Due Date: "+loanDueDate;
		return message;
	}
}
