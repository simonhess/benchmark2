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

import jmab2.agents.AbstractFirm;
import jmab2.population.MacroPopulation;
import jmab2.report.MacroVariableComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.AbstractGood;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class AveragePriceAllProducersComputer implements MacroVariableComputer {
	private int[] populationIds; 
	private int[] goodIds;
	private int[] salesIds;

	/* (non-Javadoc)
	 * @see jmab2.report.VariableComputer#computeVariable(jmab2.simulations.MacroSimulation)
	 */
	@Override
	public double computeVariable(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop;
		double totalSales=0;
		double averagePrice=0;
		for(int i = 0; i < populationIds.length; i++) {
		pop = macroPop.getPopulation(populationIds[i]);
		for (Agent a:pop.getAgents()){
			AbstractFirm firm= (AbstractFirm) a;
			totalSales+=firm.getPassedValue(salesIds[i], 0);
		}
		}
		for(int i = 0; i < populationIds.length; i++) {
			pop = macroPop.getPopulation(populationIds[i]);
			for (Agent a:pop.getAgents()){
				AbstractFirm firm= (AbstractFirm) a;
				AbstractGood good = (AbstractGood)firm.getItemStockMatrix(true, goodIds[i]);
				averagePrice+=good.getPrice()*(firm.getPassedValue(salesIds[i],0)/totalSales);
			}
			}
		return averagePrice;
	}

	/**
	 * @return the populationId
	 */
	public int[] getPopulationIds() {
		return populationIds;
	}

	/**
	 * @param populationId the populationId to set
	 */
	public void setPopulationIds(int[] populationId) {
		this.populationIds = populationId;
	}

	/**
	 * @return the goodId
	 */
	public int[] getGoodIds() {
		return goodIds;
	}

	/**
	 * @param goodId the goodId to set
	 */
	public void setGoodIds(int[] goodIds) {
		this.goodIds = goodIds;
	}

	/**
	 * @return the salesId
	 */
	public int[] getSalesIds() {
		return salesIds;
	}

	/**
	 * @param salesId the salesId to set
	 */
	public void setSalesIds(int[] salesIds) {
		this.salesIds = salesIds;
	}
	

}
