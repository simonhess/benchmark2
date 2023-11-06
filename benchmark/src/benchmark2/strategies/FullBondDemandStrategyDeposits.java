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

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import benchmark2.agents.Government;
import jmab2.agents.BondSupplier;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Item;
import jmab2.strategies.BondDemandStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 *
 */
@SuppressWarnings("serial")
public class FullBondDemandStrategyDeposits extends AbstractStrategy implements BondDemandStrategy{

	/* (non-Javadoc)
	 * @see jmab2.strategies.BondDemandStrategy#BondDemand(double)
	 */
	@Override
	public long bondDemand(BondSupplier supplier) {
		Bank bank = (Bank) getAgent();
		SimulationController controller = (SimulationController)bank.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(bank.getPopulationId());
		double totalDeposits = 0;
		for(Agent b:banks.getAgents()){
			MacroAgent tempB = (MacroAgent) b;
			totalDeposits+=tempB.getPassedValue(StaticValues.LAG_DEPOSITS, 1);
		}
		Population govPop = macroPop.getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) govPop.getAgentList().get(0);
		double nonBankMoneySupply = gov.getAggregateValue(StaticValues.LAG_NONBANKMONEYSUPPLY, 1);
		
		double thisBankDeposits =bank.getPassedValue(StaticValues.LAG_DEPOSITS, 1);
		
		double bondDemand = (supplier.getBondSupply()*thisBankDeposits/totalDeposits*totalDeposits/nonBankMoneySupply)
				+(supplier.getBondSupply()*(1-(totalDeposits/nonBankMoneySupply))/(double)banks.getSize());
		
		return (long) Math.ceil(bondDemand);

//		if(totalReserves>0) {
//			if(thisBankReserves>0) {
//				return (long) Math.rint(supplier.getBondSupply()*thisBankReserves/totalReserves);
//			}else {
//				return 0;
//			}
//		}else {
//			return (long) Math.ceil(supplier.getBondSupply()/(double)banks.getSize());
//		}
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
