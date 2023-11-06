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
package benchmark2.strategies;

import java.nio.ByteBuffer;

import benchmark2.StaticValues;
import benchmark2.agents.GovernmentAntiCyclicalWithInvestment;
import jmab2.agents.AbstractHousehold;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Deposit;
import jmab2.strategies.ConsumptionStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess: With this strategy the government invests its profits from central bank into consumption goods
 *
 */
@SuppressWarnings("serial")
public class GovernmentCBProfitConsumptionStrategy extends AbstractStrategy
		implements ConsumptionStrategy {
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.ConsumptionStrategy#computeRealConsumptionDemand()
	 */
	@Override
	public double computeRealConsumptionDemand() {
		GovernmentAntiCyclicalWithInvestment gov= (GovernmentAntiCyclicalWithInvestment) this.getAgent();
		double demand = 0;
		Deposit deposit = (Deposit) gov.getItemsStockMatrix(true, StaticValues.SM_RESERVES).get(1);
		double previousCPrice = gov.getAggregateValue(StaticValues.LAG_CPRICE, 1);
		if(deposit.getValue()>0) {
	    demand=deposit.getValue();
		}
		return demand/previousCPrice;
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
