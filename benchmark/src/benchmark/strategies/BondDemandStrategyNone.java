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



import jmab.agents.BondSupplier;
import jmab.population.MacroPopulation;
import jmab.strategies.BondDemandStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class BondDemandStrategyNone extends AbstractStrategy implements BondDemandStrategy{
	

	/* (non-Javadoc)
	 * @see jmab.strategies.BondDemandStrategy#BondDemand(double)
	 */
	@Override
	public long bondDemand(BondSupplier supplier) {
		
			return 0;
	}
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [liquidityRatio]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		return new byte[0];
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [liquidityRatio]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
	}

}
