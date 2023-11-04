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

import java.util.Map;
import java.util.TreeMap;

import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import jmab2.population.MacroPopulation;
import jmab2.report.AbstractMicroComputer;
import jmab2.report.MicroMultipleVariablesComputer;
import jmab2.simulations.MacroSimulation;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 * This computer computes consumtpion firms' labor productivity as the ratio between production and workers employed.
 */
public class MicroProfitsComputer extends AbstractMicroComputer implements
		MicroMultipleVariablesComputer {
	
	private int agentId;
	private int profitId;

	/**
	 * @return the profitId
	 */
	public int getProfitId() {
		return profitId;
	}

	/**
	 * @param profitId the profitId to set
	 */
	public void setProfitId(int profitId) {
		this.profitId = profitId;
	}

	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(agentId);
		TreeMap<Long,Double> result = new TreeMap<Long,Double>();
		for (Agent i:pop.getAgents()){
			if(i instanceof Bank){
				Bank bank= (Bank) i;
				if (!bank.isDead()){
				result.put(bank.getAgentId(), bank.getPassedValue(profitId, 0));
				}
				else{
					result.put(bank.getAgentId(), Double.NaN);
				}
			}else if(i instanceof CapitalFirm){
				CapitalFirm firm= (CapitalFirm) i;
				if (!firm.isDead()){
				result.put(firm.getAgentId(), firm.getPassedValue(profitId, 0));
				}
				else{
					result.put(firm.getAgentId(), Double.NaN);
				}
			}else if(i instanceof ConsumptionFirm){
				ConsumptionFirm firm= (ConsumptionFirm) i;
				if (!firm.isDead()){
				result.put(firm.getAgentId(), firm.getPassedValue(profitId, 0));
				}			
				else{
					result.put(firm.getAgentId(), Double.NaN);
				}
			}
		}
		return result;
	}

	/**
	 * @return the consumptionFirmsId
	 */
	public int getAgentId() {
		return agentId;
	}

	/**
	 * @param agentId the consumptionFirmsId to set
	 */
	public void setAgentId(int agentId) {
		this.agentId = agentId;
	}

	
	
}
