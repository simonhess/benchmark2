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
import java.util.ArrayList;
import java.util.List;

import jmab2.agents.AbstractBank;
import jmab2.agents.ProfitsTaxPayer;
import jmab2.agents.SimpleAbstractAgent;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Item;
import jmab2.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ProfitsWealthTaxStrategy extends AbstractStrategy implements TaxPayerStrategy {

	private double wealthTaxRate;
	private double profitTaxRate;
	private double maxProfitTaxRate;
	private double minProfitTaxRate;
	private int[] liquidAssetsId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.TaxPayerStrategy#computeTaxes()
	 */
	@Override
	public double computeTaxes() {
		ProfitsTaxPayer taxPayer = (ProfitsTaxPayer)this.getAgent();
		double profits = taxPayer.getPreTaxProfits();
		double wealth=taxPayer.getNetWealth();
		if (taxPayer instanceof AbstractBank){ 
			return Math.max(wealthTaxRate*wealth+profitTaxRate*profits, 0);
		}
		else{
			SimpleAbstractAgent agent = (SimpleAbstractAgent) this.getAgent();
			List<Item> payingStocks=new ArrayList<Item>();
			payingStocks.addAll(agent.getItemsStockMatrix(true, liquidAssetsId[0]));
			payingStocks.add(payingStocks.size()-1, agent.getItemStockMatrix(true,liquidAssetsId[1]));
			payingStocks.add(payingStocks.size()-1, agent.getItemStockMatrix(true,liquidAssetsId[2]));
			
			double liquidity = 0;
			
			for(Item stock: payingStocks) {
				liquidity += stock.getValue();
			}
			
			if (wealthTaxRate*wealth+profitTaxRate*profits>liquidity){
				return 0;
			}
			else{
				return Math.max(wealthTaxRate*wealth+profitTaxRate*profits, 0);
			}
		}
		
	}
	
	public int[] getLiquidAssetsId() {
		return liquidAssetsId;
	}

	public void setLiquidAssetsId(int[] liquidAssetsId) {
		this.liquidAssetsId = liquidAssetsId;
	}

	/**
	 * @return the wealthTaxRate
	 */
	public double getWealthTaxRate() {
		return wealthTaxRate;
	}

	/**
	 * @param wealthTaxRate the wealthTaxRate to set
	 */
	public void setWealthTaxRate(double wealthTaxRate) {
		this.wealthTaxRate = wealthTaxRate;
	}

	public double getMaxProfitTaxRate() {
		return maxProfitTaxRate;
	}

	public void setMaxProfitTaxRate(double maxProfitTaxRate) {
		this.maxProfitTaxRate = maxProfitTaxRate;
	}

	public double getMinProfitTaxRate() {
		return minProfitTaxRate;
	}

	public void setMinProfitTaxRate(double minProfitTaxRate) {
		this.minProfitTaxRate = minProfitTaxRate;
	}

	/**
	 * @return the profitTaxRate
	 */
	public double getProfitTaxRate() {
		return profitTaxRate;
	}

	/**
	 * @param profitTaxRate the profitTaxRate to set
	 */
	public void setProfitTaxRate(double profitTaxRate) {
		this.profitTaxRate = profitTaxRate;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][profitTaxRate][depositId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(20);
		buf.putDouble(wealthTaxRate);
		buf.putDouble(profitTaxRate);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][profitTaxRate][depositId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.wealthTaxRate = buf.getDouble();
		this.profitTaxRate = buf.getDouble();
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.TaxPayerStrategy#updateRates(double)
	 */
	@Override
	public void updateRates(double multiplier) {
		this.wealthTaxRate = this.wealthTaxRate*multiplier;
		if (this.profitTaxRate >= this.maxProfitTaxRate) {
			this.profitTaxRate = this.maxProfitTaxRate;
					}
		else if (this.profitTaxRate <= minProfitTaxRate){
			this.profitTaxRate = this.minProfitTaxRate;
		}
		else {
			this.profitTaxRate = this.profitTaxRate*multiplier;
		}
	}
	
	
}
