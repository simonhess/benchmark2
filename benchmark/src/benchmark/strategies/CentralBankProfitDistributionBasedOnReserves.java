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
 * This strategy distributes the central bank profits to households and firms based on their reserve holdings
 *
 */
@SuppressWarnings("serial")
public class CentralBankProfitDistributionBasedOnReserves extends AbstractStrategy implements CentralBankProfitDistributionStrategy {

	int profitsLagId;
	double profitShare;
	int receiversId;
	int depositId;
	int reservesId;


	/* (non-Javadoc)
	 * @see jmab.strategies.DividendsStrategy#payDividends()
	 */
	public void distributeCBProfits() {
		GovernmentAntiCyclical government = (GovernmentAntiCyclical)this.agent;
		
		Population hhs = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.HOUSEHOLDS_ID);
		Population cFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		Population kFirms = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.CAPITALFIRMS_ID);
		
		// Distribute CB profits based on reserve holdings of households and firms
		
		double totalRes = 0;
		for(Agent rec:hhs.getAgents()){
			Households receiver =(Households) rec; 
			Item hhRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			totalRes+=hhRes.getValue();
		}
		for(Agent rec:cFirms.getAgents()){
			ConsumptionFirm receiver =(ConsumptionFirm) rec; 
			Item cFirmRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			totalRes+=cFirmRes.getValue();
		}
		for(Agent rec:kFirms.getAgents()){
			CapitalFirm receiver =(CapitalFirm) rec;
			Item kFirmRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			totalRes+=kFirmRes.getValue();
		}
		
		Item targetStock = government.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
		
		for(Agent rec:hhs.getAgents()){
			Households receiver =(Households) rec; 
			Item hhRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			double toPay=government.getProfitsFromCB()*hhRes.getValue()/totalRes;
			
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
			
			payingSupplier.transfer(targetStock, Payablestock,toPay);
		}
		for(Agent rec:cFirms.getAgents()){
			ConsumptionFirm receiver =(ConsumptionFirm) rec; 
			Item cFirmRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			double toPay=government.getProfitsFromCB()*cFirmRes.getValue()/totalRes;
			
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_CONSGOOD);
			
			payingSupplier.transfer(targetStock, Payablestock,toPay);
		}
		
		for(Agent rec:kFirms.getAgents()){
			CapitalFirm receiver =(CapitalFirm) rec; 
			Item kFirmRes = receiver.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			double toPay=government.getProfitsFromCB()*kFirmRes.getValue()/totalRes;
			
			Item Payablestock = receiver.getPayableStock(StaticValues.MKT_CAPGOOD);
			
			payingSupplier.transfer(targetStock, Payablestock,toPay);
		}
	}


	/**
	 * @return the reservesId
	 */
	public int getReservesId() {
		return reservesId;
	}


	/**
	 * @param reservesId the reservesId to set
	 */
	public void setReservesId(int reservesId) {
		this.reservesId = reservesId;
	}


	/**
	 * @return the profitsLagId
	 */
	public int getProfitsLagId() {
		return profitsLagId;
	}

	/**
	 * @param profitsLagId the profitsLagId to set
	 */
	public void setProfitsLagId(int profitsLagId) {
		this.profitsLagId = profitsLagId;
	}

	/**
	 * @return the profitShare
	 */
	public double getProfitShare() {
		return profitShare;
	}

	/**
	 * @param profitShare the profitShare to set
	 */
	public void setProfitShare(double profitShare) {
		this.profitShare = profitShare;
	}

	/**
	 * @return the receiversId
	 */
	public int getReceiversId() {
		return receiversId;
	}

	/**
	 * @param receiversId the receiversId to set
	 */
	public void setReceiversId(int receiversId) {
		this.receiversId = receiversId;
	}

	/**
	 * @return the depositId
	 */
	public int getDepositId() {
		return depositId;
	}

	/**
	 * @param depositId the depositId to set
	 */
	public void setDepositId(int depositId) {
		this.depositId = depositId;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [profitShare][profitsLagId][receiversId][depositId][reservesId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.putDouble(this.profitShare);
		buf.putInt(this.profitsLagId);
		buf.putInt(this.receiversId);
		buf.putInt(this.depositId);
		buf.putInt(this.reservesId);
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
		this.profitShare = buf.getDouble();
		this.profitsLagId = buf.getInt();
		this.receiversId = buf.getInt();
		this.depositId = buf.getInt();
		this.reservesId = buf.getInt();
	}
	
}
