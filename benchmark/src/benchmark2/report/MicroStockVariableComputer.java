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
import java.util.Map;
import java.util.TreeMap;

import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.report.AbstractMicroComputer;
import jmab2.report.MicroMultipleVariablesComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class MicroStockVariableComputer extends AbstractMicroComputer implements
		MicroMultipleVariablesComputer {
	private int stockId;
	private boolean asset;
	private int populationId; //typically banksId
	
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



	/* (non-Javadoc)
	 * @see jmab2.report.MicroMultipleVariablesComputer#computeVariables(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop=macroPop.getPopulation(populationId);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		for (Agent i:pop.getAgents()){
			MacroAgent agent= (MacroAgent) i;
			if (!agent.isDead()){
			double variableSum = 0;
			List<Item> items=agent.getItemsStockMatrix(asset, stockId);
			for(Item stockItem:items){
				variableSum+=stockItem.getValue();
			}	
			
			result.put(agent.getAgentId(), variableSum);
			}
			else{
				result.put(agent.getAgentId(), Double.NaN);
			}
		}
		return result;
	}



	public boolean isAsset() {
		return asset;
	}



	public void setAsset(boolean asset) {
		this.asset = asset;
	}



	public int getStockId() {
		return stockId;
	}



	public void setStockId(int stockId) {
		this.stockId = stockId;
	}

}
