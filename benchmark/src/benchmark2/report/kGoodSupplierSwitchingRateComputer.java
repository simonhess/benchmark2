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

import java.util.ArrayList;
import java.util.TreeMap;

import benchmark2.StaticValues;
import benchmark2.agents.ConsumptionFirm;
import benchmark2.agents.Households;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.strategies.BestQualityPriceCapitalSupplierWithSwitching;
import jmab2.strategies.CheapestGoodSupplierWithSwitching;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class kGoodSupplierSwitchingRateComputer implements MacroVariableComputer {
	private int populationId;



	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(populationId);

		double switchingCount = 0;
		
		for (Agent a:pop.getAgents()){
			ConsumptionFirm c= (ConsumptionFirm) a;
			 
			if(c.getStrategy(StaticValues.STRATEGY_BUYING) instanceof BestQualityPriceCapitalSupplierWithSwitching) {
				BestQualityPriceCapitalSupplierWithSwitching strategy = (BestQualityPriceCapitalSupplierWithSwitching) c.getStrategy(StaticValues.STRATEGY_BUYING);
			 
			 if(strategy.isSwitched()) {
				 switchingCount++;
			 }
			}
		}
		
		double switchingRate = switchingCount/pop.getSize();
		
		return switchingRate;
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

}
