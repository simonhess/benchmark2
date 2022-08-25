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

import java.util.Map;
import java.util.TreeMap;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.strategies.AdaptiveMarkupOnAdvancesRate;
import benchmark.strategies.AdaptiveMarkupOnAdvancesRateDF;
import jmab.agents.AbstractFirm;
import jmab.population.MacroPopulation;
import jmab.report.AbstractMicroComputer;
import jmab.report.MicroMultipleVariablesComputer;
import jmab.simulations.MacroSimulation;
import jmab.strategies.MarkupPricingStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class MicroMarkupComputer extends AbstractMicroComputer implements MicroMultipleVariablesComputer {
	private int populationId;

	/* (non-Javadoc)
	 * @see jmab.report.MicroMultipleVariablesComputer#computeVariables(jmab.simulations.MacroSimulation)
	 */
	@Override
	public Map<Long, Double> computeVariables(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(populationId);
		TreeMap<Long,Double> result=new TreeMap<Long,Double>();
		
		for (Agent i:pop.getAgents()){
			if(i instanceof Bank){
				Bank bank= (Bank) i;
				if (!bank.isDead()){
					double markup = 0;
					if(bank.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof AdaptiveMarkupOnAdvancesRate){
						AdaptiveMarkupOnAdvancesRate banksBankSpecificLoanInterestStrategy = (AdaptiveMarkupOnAdvancesRate) bank.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
						markup = banksBankSpecificLoanInterestStrategy.getMarkup();
					}else if(bank.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof AdaptiveMarkupOnAdvancesRateDF){
						AdaptiveMarkupOnAdvancesRateDF banksBankSpecificLoanInterestStrategy = (AdaptiveMarkupOnAdvancesRateDF) bank.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
						markup = banksBankSpecificLoanInterestStrategy.getMarkup();
					}
				result.put(bank.getAgentId(), markup);
				}
				else{
					result.put(bank.getAgentId(), Double.NaN);
				}
			}else if(i instanceof AbstractFirm){
				AbstractFirm firm= (AbstractFirm) i;
				if (!firm.isDead()){
					if(firm.getStrategy(benchmark.StaticValues.STRATEGY_PRICING) instanceof MarkupPricingStrategy) {
						MarkupPricingStrategy pricingStrategy = (MarkupPricingStrategy) firm.getStrategy(benchmark.StaticValues.STRATEGY_PRICING);
						double markup = pricingStrategy.getMarkUp();
						result.put(firm.getAgentId(), markup);
						}
				}
				else{
					result.put(firm.getAgentId(), Double.NaN);
				}
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

}
