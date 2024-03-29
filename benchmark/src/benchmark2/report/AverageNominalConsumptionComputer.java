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

import java.util.List;
import java.util.TreeMap;

import benchmark2.StaticValues;
import benchmark2.agents.Households;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.ConsumptionGood;
import jmab2.stockmatrix.Item;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class AverageNominalConsumptionComputer implements MacroVariableComputer {
	private int householdId;

	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(householdId);
		double nominalConsumption=0;
		for (Agent i:pop.getAgents()){
			Households household= (Households) i;
			List<Item>loans=household.getItemsStockMatrix(true, StaticValues.SM_CONSGOOD);
			double consumption=0;
			for (Item j:loans){
				ConsumptionGood good= (ConsumptionGood)j;
				consumption+=good.getValue();
				}
			nominalConsumption+=consumption;
		}
		return nominalConsumption/pop.getSize();
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