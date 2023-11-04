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
package benchmark.report;

import java.util.List;

import benchmark.agents.Bank;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.InterestBearingItem;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Simon Hess
 * 
 * Computer of the average interbank interest rate. If there are no interbank loans the average ask or bid is chosen.
 * 
 */
public class AverageInterbankInterestRateComputer implements MacroVariableComputer {
	private int banksId;
	private int stockId;

	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population banks=macroPop.getPopulation(banksId);
		double avInterests=0;
		double totInterloans=0;
		for (Agent i:banks.getAgents()){
			MacroAgent bank= (MacroAgent) i;
			totInterloans+=bank.getNumericBalanceSheet()[1][stockId];
		}
		for (Agent i:banks.getAgents()){
			MacroAgent bank= (MacroAgent) i;
			List<Item> interloans=bank.getItemsStockMatrix(false, stockId);
			for(Item h:interloans){
				InterestBearingItem interloan= (InterestBearingItem) h;
				avInterests+=(interloan.getValue()/totInterloans)*interloan.getInterestRate();	
			}
		}
		if(totInterloans==0) {
			avInterests=0;
			for (Agent i:banks.getAgents()){
				Bank bank= (Bank) i;
				avInterests+=bank.getInterbankAsk();
			}
			avInterests/=banks.getSize();
		}
		return avInterests;
	}

	/**
	 * @return the banksId
	 */
	public int getBanksId() {
		return banksId;
	}

	/**
	 * @param banksId the banksId to set
	 */
	public void setBanksId(int banksId) {
		this.banksId = banksId;
	}

	/**
	 * @return the stockId
	 */
	public int getStockId() {
		return stockId;
	}

	/**
	 * @param stockId the stockId to set
	 */
	public void setStockId(int stockId) {
		this.stockId = stockId;
	}


	
	
	

}
