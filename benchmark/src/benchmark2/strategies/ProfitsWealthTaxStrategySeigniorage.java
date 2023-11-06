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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import benchmark2.StaticValues;
import benchmark2.agents.CentralBank;
import benchmark2.agents.Government;
import benchmark2.agents.Households;
import jmab2.agents.AbstractBank;
import jmab2.agents.AbstractFirm;
import jmab2.agents.ProfitsTaxPayer;
import jmab2.agents.SimpleAbstractAgent;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Item;
import jmab2.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 * This strategy calculates tax of firms and banks and reduces them by seigniorage gains from the last period
 */
@SuppressWarnings("serial")
public class ProfitsWealthTaxStrategySeigniorage extends ProfitsWealthTaxStrategy implements TaxPayerStrategy {

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
		
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population gpop = macroPop.getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) gpop.getAgentList().get(0);
		double tG = gov.getAggregateValue(StaticValues.LAG_GOVTAX, 1);
		Item depositSeign = gov.getItemsStockMatrix(true, StaticValues.SM_RESERVES).get(1);
		double seigniorage = depositSeign.getValue();
		SimpleAbstractAgent abstractAgent = (SimpleAbstractAgent)this.getAgent();
		double tAgent = abstractAgent.getPassedValue(StaticValues.LAG_TAXES, 1);
		double seigniorageShare = seigniorage* tAgent/tG;
		// round seigniorage share
	    BigDecimal bd = new BigDecimal(seigniorageShare).setScale(4, RoundingMode.DOWN);
	    seigniorageShare = bd.doubleValue();
		
		double taxDue = Math.max(wealthTaxRate*wealth+profitTaxRate*profits, 0);
		Item depositGov = gov.getItemsStockMatrix(true, StaticValues.SM_RESERVES).get(0);
		CentralBank cb = (CentralBank) depositSeign.getLiabilityHolder();
		
		
		if (taxPayer instanceof AbstractBank){ 
			if(taxDue>=seigniorageShare) {
				// If tax due is larger than seigniorage share of agent reduce tax due by seigniorage share
				cb.transfer(depositSeign, depositGov, seigniorageShare);
				return taxDue-seigniorageShare;
			}else {
				// If seigniorage share is larger than tax due reduce tax due to 0 and refund remaining seigniorage share
				cb.transfer(depositSeign, depositGov, taxDue);
				cb.transfer(depositSeign, taxPayer.getItemStockMatrix(true, StaticValues.SM_RESERVES), seigniorageShare-taxDue);
				return 0;
			}
		}
		else{
			AbstractFirm agent = (AbstractFirm) this.getAgent();
			List<Item> payingStocks=new ArrayList<Item>();
			payingStocks.addAll(agent.getItemsStockMatrix(true, liquidAssetsId[0]));
			payingStocks.add(payingStocks.size()-1, agent.getItemStockMatrix(true,liquidAssetsId[1]));
			payingStocks.add(payingStocks.size()-1, agent.getItemStockMatrix(true,liquidAssetsId[2]));
			
			double liquidity = 0;
			
			for(Item stock: payingStocks) {
				liquidity += stock.getValue();
			}
			
			if(taxDue>=seigniorageShare) {
				// If tax due is larger than seigniorage share of agent reduce tax due by seigniorage share
				cb.transfer(depositSeign, depositGov, seigniorageShare);
				double remaningTax = taxDue-seigniorageShare;
				if(remaningTax>liquidity) {
					return 0;
				}else {
					return remaningTax;
				}
			}else {
				// If seigniorage share is larger than tax due reduce tax due to 0 and refund remaining seigniorage share
				cb.transfer(depositSeign, depositGov, taxDue);
				cb.transfer(depositSeign, agent.getPayableStock(0), seigniorageShare-taxDue);
				return 0;
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
