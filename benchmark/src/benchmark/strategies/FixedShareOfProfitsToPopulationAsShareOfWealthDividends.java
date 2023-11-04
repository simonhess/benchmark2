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
import java.util.HashMap;
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Households;
import jmab2.agents.AbstractBank;
import jmab2.agents.AbstractFirm;
import jmab2.agents.LiabilitySupplier;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.Item;
import jmab2.strategies.DividendsStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class FixedShareOfProfitsToPopulationAsShareOfWealthDividends extends AbstractStrategy implements
DividendsStrategy {

	int profitsLagId;
	double profitShare;
	int receiversId;
	int depositId;
	int reservesId;
	static int currentRound;
	static double receiversTotalNW;
	static HashMap<Long, Double> receiversNW = new HashMap<Long, Double>();

	/* (non-Javadoc)
	 * @see jmab2.strategies.DividendsStrategy#payDividends()
	 */
	@Override
	public void payDividends() {
		MacroAgent dividendPayer = (MacroAgent)this.agent;
		double profits = dividendPayer.getPassedValue(profitsLagId, 0);	
		if (profits>0){
			Population receivers = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(receiversId);
			double totalNW = 0;
			int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
			// Calculate net worth of all receivers if not done yet and save net wealth of each receiver to speed up dividend payments
			if(currentRound!=round) {
				currentRound=round;
				for(Agent receiver:receivers.getAgents()){
					double agentNW = ((MacroAgent)receiver).getNetWealth();
					totalNW+= agentNW;
					receiversNW.put(((MacroAgent)receiver).getAgentId(), agentNW);
				}
				receiversTotalNW = totalNW;
			}else {
				totalNW=receiversTotalNW;
			}
			
			if (dividendPayer instanceof Bank){
				Deposit payerDep = (Deposit)dividendPayer.getItemStockMatrix(true, reservesId);
				LiabilitySupplier payingSupplier = (LiabilitySupplier) payerDep.getLiabilityHolder();
				//if(profits>payerDep.getValue()){
					//profits=payerDep.getValue();
				//}
				Bank bank= (Bank) dividendPayer;
				bank.setDividends(profits*profitShare);
				for(Agent rec:receivers.getAgents()){
					Households receiver =(Households) rec; 
					double nw = receiversNW.get(receiver.getAgentId());
					double toPay=profits*profitShare*nw/totalNW;
					
					Item Payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
					
					payingSupplier.transfer(payerDep, Payablestock,toPay);
					receiver.setDividendsReceived(receiver.getDividendsReceived()+toPay);
				}
			}
			else{
				// Calculate liquid assets of dividend payer
				
				AbstractFirm firm= (AbstractFirm) dividendPayer;

				List<Item> payingStocks = firm.getPayingStocks(StaticValues.MKT_LABOR, null);
				
				double liquidity = 0;
				for(Item item:payingStocks){
					liquidity += item.getValue();
				}
				
				if (liquidity>profits*profitShare){
					Item targetStock = null;
					if (firm instanceof ConsumptionFirm) {
						targetStock = firm.getPayableStock(StaticValues.MKT_CONSGOOD);
					}else if(firm instanceof CapitalFirm) {
						targetStock = firm.getPayableStock(StaticValues.MKT_CAPGOOD);
					}
					firm.reallocateLiquidity(liquidity, payingStocks, targetStock);
					
					if(profits>targetStock.getValue()){
						profits=targetStock.getValue();
					}
					firm.setDividends(profits*profitShare);
					LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
					for(Agent rec:receivers.getAgents()){
						Households receiver =(Households) rec; 
						double nw = receiversNW.get(receiver.getAgentId());
						double toPay=profits*profitShare*nw/totalNW;
						
						Item Payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
						
						payingSupplier.transfer(targetStock, Payablestock,toPay);
						receiver.setDividendsReceived(receiver.getDividendsReceived()+toPay);
					}
				}
				
			}
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
