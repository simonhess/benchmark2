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

import java.nio.ByteBuffer;

import benchmark.agents.Bank;
import jmab.agents.AbstractBank;
import jmab.agents.CreditDemander;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Item;
import jmab.strategies.DefaultProbilityComputer;
import jmab.strategies.SpecificCreditSupplyStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 * 
 * This strategy makes the credit decision based the expected cashflows and the expected credit loss of the requested loan. 
 * Both values, the cashflow and the loss, are discounted with the central bank advances rate.
 *
 */
@SuppressWarnings("serial")
public class ExpectedReturnCreditSupplyWithECFAndECL extends AbstractStrategy implements
		SpecificCreditSupplyStrategy {

	private DefaultProbilityComputer defaultComputer;
	private int loansId;	
	private int capitalId;
	private double haircut;
	private boolean binaryDecision;

	/* (non-Javadoc)
	 * @see jmab.strategies.SpecificCreditSupplyStrategy#computeSpecificSupply(jmab.agents.MacroAgent, double)
	 */
	@Override
	public double computeSpecificSupply(MacroAgent creditDemander,
			double required) {
		double expectedShareRecovered=0;
		
		double defaulted= creditDemander.getPassedValue(38,1);
		
		if(defaulted ==1) {
			return required;
		}
		
		Bank creditSupplier= (Bank) this.getAgent();
		double totCurrentDebt=0;
		double capitalValue=0;
		for (Item loan:creditDemander.getItemsStockMatrix(false, loansId)){
			totCurrentDebt+=loan.getValue();
			}
		for (Item capital:creditDemander.getItemsStockMatrix(true, capitalId) ){
				capitalValue+=capital.getValue()*haircut;
			}
		expectedShareRecovered=Math.min(1, capitalValue/totCurrentDebt);
		int duration=((CreditDemander)creditDemander).decideLoanLength(loansId);
		double interest=creditSupplier.getInterestRate(loansId, creditDemander, required, duration);
		double advancesInterestRate = creditSupplier.getAdvancesInterestRate();
		double amount=0;
		for(int i=0; i<101; i++){
			//if(required>0) return required;
			amount=required*(1-(double)i/100);

			double probability = defaultComputer.getDefaultProbability(creditDemander, creditSupplier, amount);
			
			double expectedReturn=0;
			
			double residualDebt = amount;
			double periodicRePayment=amount*(double) 1/ (double) duration;
			double ECF = 0;
			double ECL = 0;
			
			for (int t=1; t<=duration; t++){
				ECF += (1-probability)*(periodicRePayment+residualDebt*interest)/Math.pow((1+advancesInterestRate), t);
	
				ECL+=probability*(residualDebt*(1-expectedShareRecovered)/Math.pow((1+advancesInterestRate), t));
				residualDebt-=periodicRePayment;
			}
			
			expectedReturn=ECF-amount-ECL;
			
			if (binaryDecision==true){
				
				if (expectedReturn>=0){
					return required;
				}
				else{
					return 0;
				}
			}
			else{
				if (expectedReturn>=0){
					return amount;
				}
			}	
		}
		return 0;
	}
	/**
	 * @return the defaultComputer
	 */
	public DefaultProbilityComputer getDefaultComputer() {
		return defaultComputer;
	}
	/**
	 * @param defaultComputer the defaultComputer to set
	 */
	public void setDefaultComputer(DefaultProbilityComputer defaultComputer) {
		this.defaultComputer = defaultComputer;
	}
	/**
	 * @return the loansId
	 */
	public int getLoansId() {
		return loansId;
	}
	/**
	 * @param loansId the loansId to set
	 */
	public void setLoansId(int loansId) {
		this.loansId = loansId;
	}
	/**
	 * @return the capitalId
	 */
	public int getCapitalId() {
		return capitalId;
	}
	/**
	 * @param capitalId the capitalId to set
	 */
	public void setCapitalId(int capitalId) {
		this.capitalId = capitalId;
	}
	/**
	 * @return the haircut
	 */
	public double getHaircut() {
		return haircut;
	}
	/**
	 * @param haircut the haircut to set
	 */
	public void setHaircut(double haircut) {
		this.haircut = haircut;
	}
	/**
	 * @return the binaryDecision
	 */
	public boolean isBinaryDecision() {
		return binaryDecision;
	}
	/**
	 * @param binaryDecision the binaryDecision to set
	 */
	public void setBinaryDecision(boolean binaryDecision) {
		this.binaryDecision = binaryDecision;
	}
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [haircut][loansId][capitalId][binaryDecision][computerSize][computerStructure]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		byte[] computerStructure = this.defaultComputer.getBytes();
		ByteBuffer buf = ByteBuffer.allocate(21+computerStructure.length);
		buf.putDouble(haircut);
		buf.putInt(loansId);
		buf.putInt(capitalId);
		if(binaryDecision)
			buf.put((byte)1);
		else
			buf.put((byte)0);
		buf.putInt(computerStructure.length);
		buf.put(computerStructure);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [haircut][loansId][capitalId][binaryDecision][computerSize][computerStructure]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.haircut = buf.getDouble();
		this.loansId = buf.getInt();
		this.capitalId = buf.getInt();
		this.binaryDecision=buf.get()==(byte)1;
		int sizeComputer = buf.getInt();
		byte[] computerStructure = new byte[sizeComputer];
		buf.get(computerStructure);
		this.defaultComputer.populateFromBytes(computerStructure, pop);
	}

	
}
