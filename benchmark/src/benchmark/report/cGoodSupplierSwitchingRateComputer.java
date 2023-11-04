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

import java.util.ArrayList;
import java.util.TreeMap;

import benchmark.StaticValues;
import benchmark.agents.Households;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.strategies.CheapestGoodSupplierWithSwitching;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class cGoodSupplierSwitchingRateComputer implements MacroVariableComputer {
	private int householdId;

	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(householdId);

		double switchingCount = 0;
		
		for (Agent a:pop.getAgents()){
			Households household= (Households) a;
			 
			if(household.getStrategy(StaticValues.STRATEGY_BUYING) instanceof CheapestGoodSupplierWithSwitching) {
			 CheapestGoodSupplierWithSwitching strategy = (CheapestGoodSupplierWithSwitching) household.getStrategy(StaticValues.STRATEGY_BUYING);
			 
			 if(strategy.switched) {
				 switchingCount++;
			 }
			}
		}
		
		double switchingRate = switchingCount/pop.getSize();
		
		return switchingRate;
	}
	
	/**
	 * @return the householdId
	 */
	public int getHouseholdId() {
		return householdId;
	}

	/**
	 * @param householdId the householdId to set
	 */
	public void setHouseholdId(int householdId) {
		this.householdId = householdId;
	}

}
