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
package benchmark.strategies;

import java.nio.ByteBuffer;

import benchmark.StaticValues;
import benchmark.agents.GovernmentAntiCyclicalWithInvestment;
import jmab.agents.AbstractHousehold;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Deposit;
import jmab.strategies.ConsumptionStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess: With this strategy the government invests its profits from central bank into consumption goods
 *
 */
@SuppressWarnings("serial")
public class GovernmentCBProfitConsumptionStrategy extends AbstractStrategy
		implements ConsumptionStrategy {
	
	/* (non-Javadoc)
	 * @see jmab.strategies.ConsumptionStrategy#computeRealConsumptionDemand()
	 */
	@Override
	public double computeRealConsumptionDemand() {
		GovernmentAntiCyclicalWithInvestment gov= (GovernmentAntiCyclicalWithInvestment) this.getAgent();
		double demand = 0;
		Deposit deposit = (Deposit) gov.getItemsStockMatrix(true, StaticValues.SM_RESERVES).get(1);
		if(deposit.getValue()>0) {
	    demand=deposit.getValue();
		}
		return demand;
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub
		
	}
	
}
