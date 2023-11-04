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
import benchmark.agents.CentralBank;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani, Antoine Godin and Simon Hess
 * 
 * With this strategy the interest rate on bonds follows the advances rate set by the central bank
 *
 */
@SuppressWarnings("serial")
public class BondInterestRateStrategyAdvRate extends AbstractStrategy implements
		InterestRateStrategy {

	private int cbId;
	

	/* (non-Javadoc)
	 * @see jmab2.strategies.InterestRateStrategy#computeInterestRate(jmab2.agents.MacroAgent, double, int)
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount,
			int length) {
		Population cbpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(cbId);
		CentralBank centralbank = (CentralBank) cbpop.getAgentList().get(0);
		return centralbank.getAdvancesInterestRate();
	}
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [interestRate]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [interestRate]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
	}

	public int getCbId() {
		return cbId;
	}

	public void setCbId(int cbId) {
		this.cbId = cbId;
	}

}
