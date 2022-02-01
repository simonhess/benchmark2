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
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Households;
import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.BankruptcyStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class BankBankruptcyBailin extends AbstractStrategy implements
		BankruptcyStrategy {
	
//	private int numberBailouts; 
	private int depositId;
	private int depositExpectationId;
	protected RandomEngine prng;

	/**
	 * @return the depositId
	 */
	public int getDepositId() {
		return depositId;
	}


	/**
	 * @param depositId the depositId to set
	 */
	public void setDepositId(int depositId) {
		this.depositId = depositId;
	}


	/**
	 * 
	 */
	public BankBankruptcyBailin() {
		super();
//		this. numberBailouts=0;
	}


	/**
	 * @return the depositExpectationId
	 */
	public int getDepositExpectationId() {
		return depositExpectationId;
	}


	/**
	 * @param depositExpectationId the depositExpectationId to set
	 */
	public void setDepositExpectationId(int depositExpectationId) {
		this.depositExpectationId = depositExpectationId;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.BankruptcyStrategy#bankrupt()
	 */
	@Override
	public void bankrupt() {
		Bank bank = (Bank) getAgent();
		Population banks = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.BANKS_ID);
		double tot=0;
		for (Agent b:banks.getAgents()){
			Bank bank1 = (Bank) b;
			if (bank1.getAgentId()!=bank.getAgentId())
			tot+=bank1.getCapitalRatio();
			}
		Uniform distribution = new Uniform(0,0.1,prng);
		double car=tot/(banks.getSize()-1)+distribution.nextDouble();
		List<Item> loans=bank.getItemsStockMatrix(true, StaticValues.SM_LOAN);
		double loansValue=0;
		for (Item a:loans){
			Loan loan= (Loan)a;
			loansValue+=loan.getValue();
		}
		double targetNW=car*loansValue;
		double nw=bank.getNetWealth();
		bank.setBailoutCost(targetNW-nw);
		double totDeposits= bank.getNumericBalanceSheet()[1][depositId];
		
		Population hhs = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.HOUSEHOLDS_ID);
		Population cFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		Population kFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CAPITALFIRMS_ID);
		
		// If bank does not hold enough deposits, get bailed out from all households and firms. Recapitalize bank by all households.
		if (-nw> totDeposits) {
			
			// Calculate net wealth of households and firms and raise taxes that net wealth of bank reaches 0
			double totalNW = 0;
			for (Agent receiver : hhs.getAgents()) {
				totalNW += ((MacroAgent) receiver).getNetWealth();
			}
			for (Agent receiver : cFirms.getAgents()) {
				totalNW += ((MacroAgent) receiver).getNetWealth();
			}
			for (Agent receiver : kFirms.getAgents()) {
				totalNW += ((MacroAgent) receiver).getNetWealth();
			}

			Item targetStock = bank.getItemStockMatrix(true, StaticValues.SM_RESERVES);

			for (Agent rec : hhs.getAgents()) {
				Households receiver = (Households) rec;
				double hhnw = receiver.getNetWealth();
				double toPay = hhnw * (nw) / totalNW * -1;

				Item payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
				List<Item> payingStocks = receiver.getPayingStocks(0, payablestock);
				receiver.reallocateLiquidity(toPay, payingStocks, payablestock);

				LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();
				
				receiver.setBailoutcost(toPay);
				libHolder.transfer(payablestock, targetStock, toPay);
			}
			
			for (Agent rec : cFirms.getAgents()) {
				ConsumptionFirm receiver = (ConsumptionFirm) rec;
				double hhnw = receiver.getNetWealth();
				double toPay = hhnw * (nw) / totalNW * -1;

				Item payablestock = receiver.getPayableStock(StaticValues.MKT_CONSGOOD);
				List<Item> payingStocks = receiver.getPayingStocks(StaticValues.MKT_LABOR, null);
				receiver.reallocateLiquidity(toPay, payingStocks, payablestock);

				LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();

				libHolder.transfer(payablestock, targetStock, toPay);
			}
			
			for (Agent rec : kFirms.getAgents()) {
				CapitalFirm receiver = (CapitalFirm) rec;
				double hhnw = receiver.getNetWealth();
				double toPay = hhnw * (nw) / totalNW * -1;

				Item payablestock = receiver.getPayableStock(StaticValues.MKT_CAPGOOD);
				List<Item> payingStocks = receiver.getPayingStocks(StaticValues.MKT_LABOR, null);
				receiver.reallocateLiquidity(toPay, payingStocks, payablestock);

				LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();

				libHolder.transfer(payablestock, targetStock, toPay);
			}
			
			// Recapitalize banks by households
			
			totalNW = 0;
			for (Agent receiver : hhs.getAgents()) {
				totalNW += ((MacroAgent) receiver).getNetWealth();
			}

			for (Agent rec : hhs.getAgents()) {
				Households receiver = (Households) rec;
				double hhnw = receiver.getNetWealth();
				double toPay = hhnw * (targetNW) / totalNW;

				Item payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
				List<Item> payingStocks = receiver.getPayingStocks(0, payablestock);
				receiver.reallocateLiquidity(toPay, payingStocks, payablestock);

				LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();

				libHolder.transfer(payablestock, targetStock, toPay);
			}
		// If bank has enough deposits get bail in from depositors. Recapitalize bank by all households.
		}else {
//		numberBailouts+=1;
		for (Item deposit:bank.getItemsStockMatrix(false, depositId)){
			double bailoutcost = -(deposit.getValue()*(nw)/totDeposits);
			deposit.setValue(deposit.getValue()+(deposit.getValue()*(nw)/totDeposits));
			if(deposit.getAssetHolder() instanceof Households) {
				Households hh = (Households)deposit.getAssetHolder();
				hh.setBailoutcost(bailoutcost);
			}
		}
		
		Item targetStock = bank.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		
		double totalNW = 0;
		for(Agent receiver:hhs.getAgents()){
			totalNW+=((MacroAgent)receiver).getNetWealth();
		}
		
		for(Agent rec:hhs.getAgents()){
			Households receiver =(Households) rec; 
			double hhnw = receiver.getNetWealth();
			double toPay= hhnw *(targetNW)/totalNW;
			
			Item payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
			List<Item> payingStocks = receiver.getPayingStocks(0, payablestock);
			receiver.reallocateLiquidity(toPay, payingStocks, payablestock);
			
			LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();
			
			libHolder.transfer(payablestock, targetStock,toPay);
		}
		}
		totDeposits= bank.getNumericBalanceSheet()[1][depositId];
		
		Expectation exp =bank.getExpectation(depositExpectationId);
		double[][] expData = exp.getPassedValues();
		for(int j = 0; j<expData.length; j++){
			expData[j][0]=totDeposits;
			expData[j][1]=totDeposits;
		}
		exp.setPassedValues(expData);
		
		System.out.println("bank "+ bank.getAgentId() +" defaulted");
		//System.out.println(numberBailouts);
		
	}
	

	public RandomEngine getPrng() {
		return prng;
	}


	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}


	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [depositId][depositExpectationId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putInt(this.depositId);
		buf.putInt(this.depositExpectationId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [depositId][depositExpectationId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.depositId = buf.getInt();
		this.depositExpectationId = buf.getInt();
	}

}
