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

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab2.agents.BondSupplier;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.strategies.BondDemandStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class FullBondDemandStrategyReserves extends AbstractStrategy implements BondDemandStrategy{

	/* (non-Javadoc)
	 * @see jmab2.strategies.BondDemandStrategy#BondDemand(double)
	 */
	@Override
	public long bondDemand(BondSupplier supplier) {
		Bank bank = (Bank) getAgent();
		SimulationController controller = (SimulationController)bank.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(bank.getPopulationId());
		double totalReserves=0;
		for(Agent b:banks.getAgents()){
			MacroAgent tempB = (MacroAgent) b;
			double bankRes =tempB.getItemStockMatrix(true, StaticValues.SM_RESERVES).getValue();
			if(bankRes >0) {
			totalReserves+=bankRes;
			}
		}
		double thisBankReserves = bank.getItemStockMatrix(true, StaticValues.SM_RESERVES).getValue();
		if(totalReserves>0) {
			if(thisBankReserves>0) {
				return (long) Math.rint(supplier.getBondSupply()*thisBankReserves/totalReserves);
			}else {
				return 0;
			}
		}else {
			return (long) Math.ceil(supplier.getBondSupply()/(double)banks.getSize());
		}
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return new byte[1];//TODO
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#populateFromBytes(byte[], jmab2.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {}

}
