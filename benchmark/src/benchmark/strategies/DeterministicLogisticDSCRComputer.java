/*
 * JMAB - Java Macroeconomic Agent Based Modeling Toolkit
 * Copyright (C) 2013 Alessandro Caiani and Antoine Godin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package benchmark.strategies;

import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Government;
import jmab.agents.CreditDemander;
import jmab.agents.MacroAgent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.DefaultProbilityComputer;
import net.sourceforge.jabm.SimulationController;



/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class DeterministicLogisticDSCRComputer implements
		DefaultProbilityComputer {
	
	private int loansId;

	/* (non-Javadoc)
	 * @see jmab.strategies.DefaultProbilityComputer#getDefaultProbability(jmab.agents.MacroAgent, jmab.agents.MacroAgent)
	 */
	@Override
	public double getDefaultProbability(MacroAgent creditDemander,
			MacroAgent creditSupplier, double demanded) {
		
		// Compute future debt service
		
		List<Item> loans=creditDemander.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double toPay=0;
		double totInterests=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			if(loan.getAge()<loan.getLength()){
				double iRate=loan.getInterestRate();
				double amount=loan.getInitialAmount();
				int length = loan.getLength();
				double interests=0.0;

				double principal=0.0;
				switch(loan.getAmortization()){
				case Loan.FIXED_AMOUNT:
					interests=iRate*loan.getValue();
					double amortization = amount*(iRate*Math.pow(1+iRate, length))/(Math.pow(1+iRate, length)-1);
					if(loan.getAge()==0) {
						interests=iRate*loan.getValue();
					}else {
					interests=iRate*(loan.getValue()-amortization);
					}
					principal=amortization-interests;
					
					break;
				case Loan.FIXED_CAPITAL:
					principal=amount/length;
					if(loan.getAge()==0) {
						interests=iRate*loan.getValue();
					}else {
					interests=iRate*(loan.getValue()-principal);
					}
					break;
				case Loan.ONLY_INTERESTS:
					interests=iRate*loan.getValue();
					if(length==loan.getAge()-1)
						principal=amount;
					break;
				}
				toPay+=principal+interests;
				totInterests +=interests;
		}
			
		}

		Expectation exp = creditDemander.getExpectation(StaticValues.EXPECTATIONS_EBITDA);
		double expEBITDA=exp.getExpectation();

		Bank creditSupplier1= (Bank) creditSupplier;
		CreditDemander creditDemander1= (CreditDemander) creditDemander;
		double demandedLoanInterestPaymentPerPeriod=creditSupplier1.getInterestRate(loansId, creditDemander, demanded, creditDemander1.decideLoanLength(StaticValues.SM_LOAN))*demanded;
		double demandedLoanPaymentsPerPeriod=demandedLoanInterestPaymentPerPeriod+demanded/creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
		double bankRiskAversion=creditSupplier1.getRiskAversion(creditDemander);
		double totalDebtService = demandedLoanPaymentsPerPeriod+toPay;
		double probability=1/(1+Math.exp(bankRiskAversion*expEBITDA/totalDebtService-5));

		return probability;
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.DefaultProbilityComputer#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return new byte[1];//TODO
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.DefaultProbilityComputer#populateFromBytes(byte[], jmab.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {}

	public int getLoansId() {
		return loansId;
	}

	public void setLoansId(int loansId) {
		this.loansId = loansId;
	}
	

}
