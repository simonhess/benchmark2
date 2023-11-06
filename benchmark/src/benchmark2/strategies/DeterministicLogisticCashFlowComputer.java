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

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import jmab2.agents.CreditDemander;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.strategies.DefaultProbilityComputer;



/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class DeterministicLogisticCashFlowComputer implements
		DefaultProbilityComputer {
	
	private int loansId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.DefaultProbilityComputer#getDefaultProbability(jmab2.agents.MacroAgent, jmab2.agents.MacroAgent)
	 */
	@Override
	public double getDefaultProbability(MacroAgent creditDemander,
			MacroAgent creditSupplier, double demanded) {
		double operatingCashFlow=creditDemander.getPassedValue(StaticValues.LAG_OPERATINGCASHFLOW, 1);
		Bank creditSupplier1= (Bank) creditSupplier;
		CreditDemander creditDemander1= (CreditDemander) creditDemander;
		double demandedLoanInterestPaymentPerPeriod=creditSupplier1.getInterestRate(loansId, creditDemander, demanded, creditDemander1.decideLoanLength(StaticValues.SM_LOAN))*demanded;
		double demandedLoanPaymentsPerPeriod=demandedLoanInterestPaymentPerPeriod+demanded/creditDemander1.decideLoanLength(StaticValues.SM_LOAN);
		double bankRiskAversion=creditSupplier1.getRiskAversion(creditDemander);
		double probability=1/(1+Math.exp((operatingCashFlow-bankRiskAversion*demandedLoanPaymentsPerPeriod)/demandedLoanPaymentsPerPeriod));
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
