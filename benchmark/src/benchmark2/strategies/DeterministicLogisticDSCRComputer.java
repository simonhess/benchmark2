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
package benchmark2.strategies;

import java.util.List;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import benchmark2.agents.ConsumptionFirm;
import benchmark2.agents.Government;
import benchmark2.expectations.AdaptiveExpectationDoubleExponentialSmoothing;
import jmab2.agents.CreditDemander;
import jmab2.agents.MacroAgent;
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.DefaultProbilityComputer;
import net.sourceforge.jabm.SimulationController;



/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class DeterministicLogisticDSCRComputer implements
		DefaultProbilityComputer {
	
	private int loansId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.DefaultProbilityComputer#getDefaultProbability(jmab2.agents.MacroAgent, jmab2.agents.MacroAgent)
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

		AdaptiveExpectationDoubleExponentialSmoothing exp=null;
		double expEBITDA=0;
		Bank creditSupplier1= (Bank) creditSupplier;
		CreditDemander creditDemander1= (CreditDemander) creditDemander;
		if(creditDemander1 instanceof ConsumptionFirm) {
			exp = (AdaptiveExpectationDoubleExponentialSmoothing) creditDemander.getExpectation(StaticValues.EXPECTATIONS_EBITDAMINUSCAPEX);
			expEBITDA=exp.getExpectation();	
		}else {
			exp = (AdaptiveExpectationDoubleExponentialSmoothing) creditDemander.getExpectation(StaticValues.EXPECTATIONS_EBITDA);
			expEBITDA=exp.getExpectation();
		}
		
	
		double demandedLoanInterestPaymentPerPeriod=creditSupplier1.getInterestRate(loansId, creditDemander, demanded, creditDemander1.decideLoanLength(StaticValues.SM_LOAN))*demanded;
		double demandedLoanPaymentsPerPeriod=demandedLoanInterestPaymentPerPeriod+demanded/creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
		double bankRiskAversion=creditSupplier1.getRiskAversion(creditDemander);
		
		if(creditDemander1 instanceof ConsumptionFirm) {
			if(creditDemander1.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND) instanceof RealLumpyCapitalDemandAdaptiveNPV) {
				
				double avDebtService = 0;
				double avExpEBITDA = 0;
				double residualDebt = demanded;
				double periodicRePayment=demanded*(double) 1/ (double) creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
				for (int t=1; t<=creditDemander1.decideLoanLength(StaticValues.SM_LOAN); t++){
					avDebtService += toPay+(periodicRePayment+residualDebt*creditSupplier1.getInterestRate(loansId, creditDemander, demanded, creditDemander1.decideLoanLength(StaticValues.SM_LOAN)));
					residualDebt-=periodicRePayment;
					avExpEBITDA+= exp.getExpectation(t);
				}
				avDebtService/=creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
				avExpEBITDA/=creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
				
				double probability=1/(1+Math.exp(bankRiskAversion*avExpEBITDA/avDebtService-5));
				
				return probability;
			}
		}
		
		double totalDebtService = demandedLoanPaymentsPerPeriod+toPay;
		
		double probability=1/(1+Math.exp(bankRiskAversion*expEBITDA/totalDebtService-5));

		return probability;
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.DefaultProbilityComputer#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return new byte[1];//TODO
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.DefaultProbilityComputer#populateFromBytes(byte[], jmab2.population.MacroPopulation)
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
