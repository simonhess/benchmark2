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

import jmab2.agents.AbstractFirm;
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
public class MicroOperatingCashFlowComputer extends AbstractMicroComputer implements
MicroMultipleVariablesComputer {

	private int firmsId;
	private int oCFId;
	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */


	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		TreeMap<Long,Double> result = new TreeMap<Long,Double>();
		Population pop = macroPop.getPopulation(firmsId);
		for (Agent i:pop.getAgents()){
			AbstractFirm firm= (AbstractFirm) i;
			if (!firm.isDead()){
				double ocf = firm.getPassedValue(oCFId, 0);
				result.put(firm.getAgentId(),ocf);
			}
			else{
				result.put(firm.getAgentId(), Double.NaN);
			}
		}
		return result;
	}

	/**
	 * @return the firmsId
	 */
	public int getFirmsId() {
		return firmsId;
	}


	/**
	 * @param firmsId the firmsId to set
	 */
	public void setFirmsId(int firmsId) {
		this.firmsId = firmsId;
	}


	/**
	 * @return the oCFId
	 */
	public int getoCFId() {
		return oCFId;
	}


	/**
	 * @param oCFId the oCFId to set
	 */
	public void setoCFId(int oCFId) {
		this.oCFId = oCFId;
	}

	
	
}
