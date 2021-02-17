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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;

import benchmark.StaticValues;
import benchmark.agents.CentralBank;
import benchmark.agents.Government;
import benchmark.agents.Households;
import jmab.agents.IncomeTaxPayer;
import jmab.agents.LiabilitySupplier;
import jmab.agents.SimpleAbstractAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Item;
import jmab.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 * This strategy calculates tax of households and reduces them by seigniorage gains from the last period
 */
@SuppressWarnings("serial")
public class IncomeWealthTaxStrategySeigniorage extends IncomeWealthTaxStrategy implements TaxPayerStrategy {

	private double wealthTaxRate;
	private double incomeTaxRate;
	private double maxIncomeTaxRate;
	private double minIncomeTaxRate;
	
	/* (non-Javadoc)
	 * @see jmab.strategies.TaxPayerStrategy#computeTaxes()
	 */
	@Override
	public double computeTaxes() {
		IncomeTaxPayer taxPayer = (IncomeTaxPayer)this.getAgent();
		double income = taxPayer.getGrossIncome();
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
		
		Households hh = (Households) agent;
		
		double taxDue = Math.max(wealthTaxRate*wealth+incomeTaxRate*income, 0);
		Item depositGov = gov.getItemsStockMatrix(true, StaticValues.SM_RESERVES).get(0);
		CentralBank cb = (CentralBank) depositSeign.getLiabilityHolder();
		
		
		if(taxDue>=seigniorageShare) {
			// If tax due is larger than seigniorage share of agent reduce tax due by seigniorage share
			cb.transfer(depositSeign, depositGov, seigniorageShare);
			return taxDue-seigniorageShare;
		}else {
			// If seigniorage share is larger than tax due reduce tax due to 0 and refund remaining seigniorage share
			cb.transfer(depositSeign, depositGov, taxDue);
			cb.transfer(depositSeign, hh.getPayableStock(0), seigniorageShare-taxDue);
			return 0;
		}
		
		
		
		// return Math.max(wealthTaxRate*wealth+incomeTaxRate*income-taxRefund, 0);
	}
	
	/* (non-Javadoc)
	 * @see jmab.strategies.TaxPayerStrategy#updateRates(double)
	 */
	@Override
	public void updateRates(double multiplier) {
		this.wealthTaxRate = this.wealthTaxRate*multiplier;
		if (this.incomeTaxRate >= this.maxIncomeTaxRate) {
			this.incomeTaxRate = this.maxIncomeTaxRate;
					}
		else if (this.incomeTaxRate <= minIncomeTaxRate){
			this.incomeTaxRate = this.minIncomeTaxRate;
		}
		else {
			this.incomeTaxRate = this.incomeTaxRate*multiplier;
		}
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

	/**
	 * @return the incomeTaxRate
	 */
	public double getIncomeTaxRate() {
		return incomeTaxRate;
	}

	/**
	 * @param incomeTaxRate the incomeTaxRate to set
	 */
	public void setIncomeTaxRate(double incomeTaxRate) {
		this.incomeTaxRate = incomeTaxRate;
	}

	public double getMaxIncomeTaxRate() {
		return maxIncomeTaxRate;
	}

	public void setMaxIncomeTaxRate(double maxIncomeTaxRate) {
		this.maxIncomeTaxRate = maxIncomeTaxRate;
	}

	public double getMinIncomeTaxRate() {
		return minIncomeTaxRate;
	}

	public void setMinIncomeTaxRate(double minIncomeTaxRate) {
		this.minIncomeTaxRate = minIncomeTaxRate;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][incomeTaxRate]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putDouble(wealthTaxRate);
		buf.putDouble(incomeTaxRate);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][incomeTaxRate]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.wealthTaxRate = buf.getDouble();
		this.incomeTaxRate = buf.getDouble();
	}
	
}
