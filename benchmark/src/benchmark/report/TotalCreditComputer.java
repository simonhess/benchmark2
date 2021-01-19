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

import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 * Generates the network of balance sheet connections among the selected populations for the selected assets. Each NxN matrix is represented 
 * as one 1x(N+2) row, where the two first fields (with key "-1" and "-2") contain 1 the number of agents (i.e. N) and 2 the selection of assets (where each 
 * number is separated by a hyphen "-". Each cell of the row is given by the concatenation of the following information (separated by 
 * "-"): 1. agent type, 2 list of all the issuers' id of the assets belonging to assetsId.
 */
public class TotalCreditComputer  implements MacroVariableComputer {

	private int banksId;
	private int loansId;


	/* (non-Javadoc)
	 * @see jmab.report.MicroMultipleVariablesComputer#computeVariables(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {

		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population banks = macroPop.getPopulation(banksId);
		double totalCredit = 0;
		for (Agent i:banks.getAgents()){
			MacroAgent agent=(MacroAgent) i;
			if (!agent.isDead()){
				List<Item> loans = agent.getItemsStockMatrix(true, loansId);
				for(Item loan:loans){
					totalCredit+=loan.getValue();
				}
			}
		}
		return totalCredit;
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

	
	
}
