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

import benchmark.agents.Households;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import jmab.population.MacroPopulation;
import jmab.report.MacroVariableComputer;
import jmab.simulations.MacroSimulation;

/**
 * @author Simon Hess
 * 
 * This computer computes the weighted standard deviation of households bailout costs
 *
 */
public class HhBailoutCostStdDevComputer implements MacroVariableComputer {
	private int householdId;

	/* (non-Javadoc)
	 * @see jmab.report.VariableComputer#computeVariable(jmab.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(householdId);

		double totalLiquidAssetsSum = 0;
		
		ArrayList<Households> households = new ArrayList();
		for (Agent i:pop.getAgents()){
			Households household= (Households) i;
			 households.add(household);
			 totalLiquidAssetsSum += household.getLiquidAssetsSum();
		}
		
		double[] values = new double[households.size()];
		
		for(int i = 0; i<households.size(); i++) {
			values[i]=households.get(i).getBailoutcost()/households.get(i).getLiquidAssetsSum();
		}
		
		// The weighted mean average
		double wMean = 0.0;
		for (int i = 0; i < values.length; i++) {
			double weight = households.get(i).getLiquidAssetsSum()/totalLiquidAssetsSum;
			wMean += values[i]*weight;
		}

		// The weighted variance
		double wVariance = 0;
		for (int i = 0; i < values.length; i++) {
			double weight = households.get(i).getLiquidAssetsSum()/totalLiquidAssetsSum;
			wVariance += weight*Math.pow(values[i] - wMean, 2);
		}
		
		return Math.sqrt(wVariance);
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
