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
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.CentralBank;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.GovernmentAntiCyclical;
import benchmark.agents.Households;
import jmab.agents.AbstractFirm;
import jmab.agents.LiabilitySupplier;
import jmab.agents.AbstractBank;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.SingleStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 *
 * This strategy distributes the central bank profits to households and firms equally i.e. every agent receives the same share of the profits
 *
 */
@SuppressWarnings("serial")
public class CentralBankProfitDistributionEqual extends AbstractStrategy implements CentralBankProfitDistributionStrategy {

	/* (non-Javadoc)
	 * @see jmab.strategies.DividendsStrategy#payDividends()
	 */
	public void distributeCBProfits() {
		GovernmentAntiCyclical government = (GovernmentAntiCyclical)this.agent;
		
		Population hhs = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.HOUSEHOLDS_ID);
		Population cFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		Population kFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CAPITALFIRMS_ID);
		
		// Distribute CB profits based on reserve holdings of households and firms
		
		Population cbpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CB_ID);
		double amountToDistribute = 0;

		for(Agent rec:cbpop.getAgents()){
			CentralBank cb = (CentralBank) rec;
			amountToDistribute = government.getProfitsFromCB() - cb.getBondInterestsReceived();
			break;
		}
		
		double size = hhs.getAgents().size()+cFirms.getAgents().size()+kFirms.getAgents().size();
		
		double shareOfCBProfits = amountToDistribute/size;
		
		Item targetStock = government.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
		
		for(Agent rec:hhs.getAgents()){
			Households receiver =(Households) rec; 
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
			payingSupplier.transfer(targetStock, Payablestock,shareOfCBProfits);
		}
		for(Agent rec:cFirms.getAgents()){
			ConsumptionFirm receiver =(ConsumptionFirm) rec; 		
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_CONSGOOD);	
			payingSupplier.transfer(targetStock, Payablestock,shareOfCBProfits);
		}
		
		for(Agent rec:kFirms.getAgents()){
			CapitalFirm receiver =(CapitalFirm) rec; 
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_CAPGOOD);
			payingSupplier.transfer(targetStock, Payablestock,shareOfCBProfits);
		}
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [profitShare][profitsLagId][receiversId][depositId][reservesId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(24);

		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [profitShare][profitsLagId][receiversId][depositId][reservesId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
	}
	
}
