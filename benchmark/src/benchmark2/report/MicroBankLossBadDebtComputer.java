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
package benchmark2.report;

import java.util.Map;
import java.util.TreeMap;

import benchmark2.agents.Bank;
import jmab2.population.MacroPopulation;
import jmab2.report.AbstractMicroComputer;
import jmab2.report.MicroMultipleVariablesComputer;
import jmab2.simulations.MacroSimulation;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class MicroBankLossBadDebtComputer extends AbstractMicroComputer implements
MicroMultipleVariablesComputer {
	private int banksId;
	private int loansId;
	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(banksId);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		for (Agent i:pop.getAgents()){
			Bank bank= (Bank) i;
			result.put(bank.getAgentId(), bank.getCurrentNonPerformingLoans(loansId));
		}
		return result;
	}

	public int getLoansId() {
		return loansId;
	}

	public void setLoansId(int loansId) {
		this.loansId = loansId;
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


}
