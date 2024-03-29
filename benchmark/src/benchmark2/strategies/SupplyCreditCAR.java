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
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.InterestBearingItem;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.SupplyCreditStrategy;
import jmab2.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 *
 */
@SuppressWarnings("serial")
public class SupplyCreditCAR extends AbstractStrategy implements
		SupplyCreditStrategy {

	/* (non-Javadoc)
	 * @see jmab2.strategies.SupplyCreditStrategy#computeCreditSupply()
	 */
	@Override
	public double computeCreditSupply() {
		
		Bank b = (Bank) this.getAgent();
		
		// Calculate principal payment
		
		List<Item> loans=b.getItemsStockMatrix(true, StaticValues.SM_LOAN);
		double principalSum=0;
		double loanSum=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			if(loan.getAge()>0){
				double iRate=loan.getInterestRate();
				double amount=loan.getInitialAmount();
				int length = loan.getLength();
				double interests=iRate*loan.getValue();
				
				double principal=0.0;
				switch(loan.getAmortization()){
				case Loan.FIXED_AMOUNT:
					double amortization = amount*(iRate*Math.pow(1+iRate, length))/(Math.pow(1+iRate, length)-1);
					principal=amortization-interests;
					break;
				case Loan.FIXED_CAPITAL:
					principal=amount/length;
					break;
				case Loan.ONLY_INTERESTS:
					if(length==loan.getAge())
						principal=amount;
					break;
				}
				principalSum+=principal;
				loanSum+=loan.getValue();
			}
		}	
		
		List<List<Item>> assets=b.getAssetStock();
		List<List<Item>> liabilities=b.getLiabilityStock();
		
		double expInterestRecieved = 0;
		double expInterestPaid = 0;
		
		for(List<Item> list: assets) {
			for(Item item: list) {
				if(item instanceof InterestBearingItem) {
				InterestBearingItem iItem = (InterestBearingItem) item;
				expInterestRecieved+=iItem.getValue()*iItem.getInterestRate();
				}
			}
		}
		
		for(List<Item> list: liabilities) {
			for(Item item: list) {
				if(item instanceof InterestBearingItem) {
				InterestBearingItem iItem = (InterestBearingItem) item;
				expInterestPaid+=iItem.getValue()*iItem.getInterestRate();
				}
			}
		}
		
		double expProfits = expInterestRecieved-expInterestPaid;
		ProfitsWealthTaxStrategy strategy = (ProfitsWealthTaxStrategy) b.getStrategy(StaticValues.STRATEGY_TAXES);
		double expAfterTaxProfits = expProfits-Math.max(0, expProfits*strategy.getProfitTaxRate());
		
		double expNW = b.getPassedValue(StaticValues.LAG_NETWEALTH, 1)+expAfterTaxProfits;
		
		double maxLoans = expNW/b.getTargetedCapitalAdequacyRatio();
		double maxLoansSupply=maxLoans-loanSum+principalSum;
		
		return maxLoansSupply;
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return new byte[1];//TODO
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#populateFromBytes(byte[], jmab2.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {}

}