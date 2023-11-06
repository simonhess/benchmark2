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

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import benchmark2.agents.CapitalFirm;
import benchmark2.agents.ConsumptionFirm;
import benchmark2.expectations.AdaptiveExpectationDoubleExponentialSmoothing;
import benchmark2.strategies.AdaptiveMarkupOnAdvancesRate;
import benchmark2.strategies.AdaptiveMarkupOnAdvancesRateDF;
import benchmark2.strategies.AdaptiveMarkupOnAdvancesRateProfitGrowth;
import jmab2.agents.AbstractFirm;
import jmab2.agents.MacroAgent;
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.report.AbstractMicroComputer;
import jmab2.report.MicroMultipleVariablesComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.strategies.MarkupPricingStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Simon Hess
 *
 */
public class MicroExpectationComputer extends AbstractMicroComputer implements MicroMultipleVariablesComputer {
	private int populationId;
	private int expId;

	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(populationId);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		
		for (Agent i:pop.getAgents()){
			MacroAgent a = (MacroAgent)i;
			
			if (!a.isDead()){
			Expectation exp = a.getExpectation(expId);
			result.put(a.getAgentId(), exp.getExpectation());
		    }else{
					result.put(a.getAgentId(), Double.NaN);
				}
			}
		
		return result;
	}

	/**
	 * @return the populationId
	 */
	public int getPopulationId() {
		return populationId;
	}

	/**
	 * @param populationId the populationId to set
	 */
	public void setPopulationId(int populationId) {
		this.populationId = populationId;
	}

	public int getExpId() {
		return expId;
	}

	public void setExpId(int expId) {
		this.expId = expId;
	}

}
