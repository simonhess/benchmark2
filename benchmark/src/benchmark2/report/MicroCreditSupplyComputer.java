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
public class MicroCreditSupplyComputer extends AbstractMicroComputer implements
		MicroMultipleVariablesComputer {
	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(StaticValues.BANKS_ID);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		for (Agent i:pop.getAgents()){
			Bank bank= (Bank) i;
			if (!bank.isDead()){
				result.put(bank.getAgentId(), (bank.getPassedValue(StaticValues.LAG_BANKTOTLOANSUPPLY, 0)));
			}
			else{
				result.put(bank.getAgentId(), Double.NaN);
			}
		}
		return result;
	}


}
