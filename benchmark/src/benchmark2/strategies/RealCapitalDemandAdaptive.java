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
import java.util.List;

import jmab2.agents.InvestmentAgent;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.CapitalGood;
import jmab2.stockmatrix.Item;
import jmab2.strategies.RealCapitalDemandStrategy;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class RealCapitalDemandAdaptive extends AbstractStrategy implements
		RealCapitalDemandStrategy {

	int productionStockId;
	int capitalGoodId;
	double investment;

	/* (non-Javadoc)
	 * @see jmab2.strategies.RealCapitalDemandStrategy#computeRealCapitalDemand(jmab2.agents.MacroAgent)
	 */
	@Override
	public double computeRealCapitalDemand(MacroAgent seller) {
		
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		
		if(round==1) {
			List<Item> capStock=((InvestmentAgent)this.getAgent()).getItemsStockMatrix(true, capitalGoodId);
			int investment=0;
			for (Item j:capStock){
				CapitalGood good= (CapitalGood)j;
				if(good.getAge()==0)
					investment+=good.getQuantity();
				}
			this.investment =investment;
		}
		
		InvestmentAgent investor= (InvestmentAgent) this.getAgent();
		CapitalGood newCapital= (CapitalGood)seller.getItemStockMatrix(true, productionStockId);
		List<Item> capitalStock=investor.getItemsStockMatrix(true, capitalGoodId);
		double currentCapacity=0;
		double residualCapacity=0;
		for (Item i:capitalStock){
			CapitalGood oldCapital=(CapitalGood) i;
			currentCapacity+=oldCapital.getProductivity()*oldCapital.getQuantity();
			if (oldCapital.getCapitalDuration()- oldCapital.getAge()>1){ //i.e. the capital is still working in the next period
					residualCapacity+=oldCapital.getProductivity()*oldCapital.getQuantity();
				}
			}
		double desiredCapacity= (1+investor.getDesiredCapacityGrowth())*currentCapacity;
		
		if (desiredCapacity>currentCapacity){
			//this.investment+=0.2;
			this.investment+=1;
		}else if(desiredCapacity<currentCapacity){
			this.investment-=1;
			//this.investment-=0.2;
		}
//		if(investor.getAgentId()==8153) {
//			System.out.println("Test");
//		}
		
		int newCapitalRealDemand = (int) Math.round(this.investment);
		
		return newCapitalRealDemand;
	}
	
	/**
	 * @return the productionStockId
	 */
	public int getProductionStockId() {
		return productionStockId;
	}



	/**
	 * @param productionStockId the productionStockId to set
	 */
	public void setProductionStockId(int productionStockId) {
		this.productionStockId = productionStockId;
	}



	/**
	 * @return the capitalGoodId
	 */
	public int getCapitalGoodId() {
		return capitalGoodId;
	}



	/**
	 * @param capitalGoodId the capitalGoodId to set
	 */
	public void setCapitalGoodId(int capitalGoodId) {
		this.capitalGoodId = capitalGoodId;
	}



	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [productionStockId][capitalGoodId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putInt(this.productionStockId);
		buf.putInt(this.capitalGoodId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [productionStockId][capitalGoodId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.productionStockId = buf.getInt();
		this.capitalGoodId = buf.getInt();
	}
	
	

}
