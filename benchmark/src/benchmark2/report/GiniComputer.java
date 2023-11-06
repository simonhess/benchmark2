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

import benchmark2.agents.Households;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class GiniComputer implements MacroVariableComputer {
	private int householdId;

	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(householdId);

		ArrayList<Households> households = new ArrayList();
		for (Agent i:pop.getAgents()){
			Households household= (Households) i;
			 households.add(household);
		}
		
		double[] values = new double[households.size()];
		
		for(int i = 0; i<households.size(); i++) {
			values[i]=households.get(i).getNetWealth();
		}
		
		return calcGiniCoefficient(values);
	}
	
	// Calculate Gini Index based on https://github.com/ybtuteng/GiniMap/blob/master/src/gini/GiniCompute.java
	
	public double calcGiniCoefficient(double[] values)
    {
        if (values.length < 1) return 0;  //not computable
        if (values.length == 1) return 0;
        double relVars = 0;

        float descMean = this.Stat(values);
        if (descMean == 0.0) return 0; // only possible if all data is zero
        for (int i = 0; i < values.length; i++)
        {
            for (int j = 0; j < values.length; j++)
            {
                if (i == j) continue;
                relVars += (Math.abs(values[i] - values[j]));
            }
        }
        relVars = relVars / (2.0 * values.length * values.length);
        return (relVars / descMean); // return gini value
    }
	
	public float Stat(double[] seriesNumber){
        float sum = 0;
        for(int index = 0;index < seriesNumber.length;index++){
            sum+=seriesNumber[index];
        }
        float mean;
        mean = sum / seriesNumber.length;
        return mean;
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
