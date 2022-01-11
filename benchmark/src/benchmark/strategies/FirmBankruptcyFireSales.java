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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import benchmark.StaticValues;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Households;
import jmab.agents.AbstractFirm;
import jmab.agents.CreditSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.AdaptiveMarkUpOnAC;
import jmab.strategies.BankruptcyStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class FirmBankruptcyFireSales extends AbstractStrategy implements
		BankruptcyStrategy {
	
	private double haircut;
	
	
	/**
	 * @return the haircut
	 */
	public double getHaircut() {
		return haircut;
	}


	/**
	 * @param haircut the haircut to set
	 */
	public void setHaircut(double haircut) {
		this.haircut = haircut;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.BankruptcyStrategy#bankrupt()
	 */
	@Override
	public void bankrupt() {

		AbstractFirm firm = (AbstractFirm) this.agent;

		// 1. Move all money on one deposit if there were two of them
		List<Item> deposits = firm.getItemsStockMatrix(true, StaticValues.SM_DEP);
		Deposit deposit = (Deposit) deposits.get(0);
		Deposit res = (Deposit) firm.getItemStockMatrix(true, StaticValues.SM_RESERVES);

		Cash cash = (Cash) firm.getItemStockMatrix(true, StaticValues.SM_CASH);

		// Get the paying stocks
		List<Item> payingStocks = firm.getPayingStocks(StaticValues.MKT_LABOR, deposit);

		// Calculate amount to be reallocated
		double reallocateAmount = 0;
		for (Item item : payingStocks) {
			reallocateAmount += item.getValue();
		}
		// Reallocate
		firm.reallocateLiquidity(reallocateAmount, payingStocks, deposit);

		//3. Compute total liquidity to be distributed to creditors
		double liquidity=deposit.getValue();
		

		//4. Compute each creditor's share of debt
		List<Item> loans=firm.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double[] debts = new double[loans.size()];
		double[] banksLosses = new double[loans.size()];
		double totalDebt=0;
		double totalBanksLoss=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			debts[i]=loan.getValue();
			banksLosses[i]=loan.getValue();
			totalDebt+=loan.getValue();
			totalBanksLoss+=loan.getValue();
		}
		
		if (totalDebt!=0){
			//5. Distribute liquidity according to the share of debt of each creditor
			for(int i=0;i<loans.size();i++){
				Loan loan = (Loan) loans.get(i);
				double amountToPay=liquidity*(debts[i])/totalDebt;
				if (liquidity>=totalDebt){
					amountToPay=debts[i];
				}
				//lendingBank.setCurrentNonPerformingLoans(lendingBank.getCurrentNonPerformingLoans()+(debts[i]-amountToPay)); 
				deposit.setValue(deposit.getValue()-amountToPay);
				if(loan.getAssetHolder()!=deposit.getLiabilityHolder()){
					Item lBankRes = loan.getAssetHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
					lBankRes.setValue(lBankRes.getValue()+amountToPay);
					Item dBankRes = deposit.getLiabilityHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
					dBankRes.setValue(dBankRes.getValue()-amountToPay);
				}
				loan.setValue(loan.getValue()-amountToPay);
				banksLosses[i]-=amountToPay;
				totalBanksLoss-=amountToPay;
				//loan.setValue(0);
			}
			deposit.setValue(0.0);
			
			//compute the value of capital to be sold
			if (firm instanceof ConsumptionFirm){
				double capitalValue=0;
				List<Item> capital=firm.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
				for (Item i:capital){
					capitalValue+=i.getValue()*haircut;
				}
				double ownersDisbursment=0;
				if (capitalValue>totalBanksLoss){
					
					ownersDisbursment=totalBanksLoss;
				}
				else{
					ownersDisbursment=capitalValue;
				}
				// Allowance of inventory and reduction of excess capacities
				SimulationController controller = (SimulationController)firm.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				
				//Value adjustment of inventories
				
				Population cFirms = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
				
				double unitCostSum = 0;
				
				for(Agent h:cFirms.getAgents()){
					ConsumptionFirm cFirm = (ConsumptionFirm) h;
					ConsumptionGood inventories = (ConsumptionGood)cFirm.getItemStockMatrix(true, cFirm.getProductionStockId());
					unitCostSum += inventories.getUnitCost();
				}
				double avUnitCost = unitCostSum/cFirms.getSize();
				double valueAdjustmentAmount = 0;
				
				List<Item> consGoods=firm.getItemsStockMatrix(true, StaticValues.SM_CONSGOOD);
				for (Item i:consGoods){
					ConsumptionGood cGood = (ConsumptionGood) i;
					valueAdjustmentAmount += cGood.getQuantity()*cGood.getUnitCost()-cGood.getQuantity()*avUnitCost;
					cGood.setUnitCost(avUnitCost);
				}
				//ownersDisbursment+=valueAdjustmentAmount;
				
				// Reduce excess capacities
				
				InvestmentCapacityOperatingCashFlowExpected strategy1=(InvestmentCapacityOperatingCashFlowExpected) firm.getStrategy(StaticValues.STRATEGY_INVESTMENT);
				double targetCapacityUtilization = strategy1.getTargetCapacityUtlization();
				
				double targetCapacity = firm.getPassedValue(StaticValues.LAG_PRODUCTION, 0)/targetCapacityUtilization;
				double tempCapacity = 0;
				
				// Sort capital by age
				
				TreeMap<Integer,ArrayList<CapitalGood>> sortedCapital = new TreeMap<Integer,ArrayList<CapitalGood>>();
				
				for (Item i:capital){
					CapitalGood kGood = (CapitalGood) i;
					if(kGood.getAge()>=0&&kGood.getAge()<kGood.getCapitalDuration()) {
						if(sortedCapital.containsKey(kGood.getAge())){
							ArrayList<CapitalGood> list = sortedCapital.get(kGood.getAge());
							list.add(kGood);
						}else{
							ArrayList<CapitalGood> list = new ArrayList<CapitalGood>();
							list.add(kGood);
							sortedCapital.put(kGood.getAge(), list);
						}

					}
				}

				for (Integer key:sortedCapital.keySet()){
					for(CapitalGood kGood:sortedCapital.get(key)){
						if(tempCapacity > targetCapacity) {
							kGood.setObsolete(true);
						}else{
							tempCapacity+=kGood.getQuantity()*kGood.getProductivity();
						}
					}
					}
				
				firm.setBailoutCost(ownersDisbursment);
				Population households = macroPop.getPopulation(StaticValues.HOUSEHOLDS_ID);
				double totalHouseholdsWealth=0;
				for(Agent h:households.getAgents()){
					totalHouseholdsWealth+=((MacroAgent)h).getNetWealth();
				}
				for (Agent h:households.getAgents()){
					MacroAgent hh = (MacroAgent) h;
					for(int i=0;i<loans.size();i++){
						Loan loan = (Loan) loans.get(i);
						//each owner (household contribute according to his share of net-wealth, each creditor is refunded according to his share of credit.
						double amountToPay=ownersDisbursment*hh.getNetWealth()/totalHouseholdsWealth*(banksLosses[i])/totalBanksLoss;
						CreditSupplier lendingBank= (CreditSupplier) loan.getAssetHolder();
						Deposit depositHH =(Deposit)hh.getItemStockMatrix(true, StaticValues.SM_DEP);
						Households hhs = (Households) hh;
						hhs.reallocateLiquidity(amountToPay, hhs.getPayingStocks(0, depositHH), depositHH);
						if (depositHH.getLiabilityHolder()==lendingBank){
							depositHH.setValue(depositHH.getValue()-amountToPay);
						}
						else{
							depositHH.setValue(depositHH.getValue()-amountToPay);
							Item dBankReserve= (Item) depositHH.getLiabilityHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
							dBankReserve.setValue(dBankReserve.getValue()-amountToPay);
							Item lendingBankReserves= (Item) lendingBank.getItemStockMatrix(true, StaticValues.SM_RESERVES);
							lendingBankReserves.setValue(lendingBankReserves.getValue()+amountToPay);
						}
						loan.setValue(loan.getValue()-amountToPay);
						banksLosses[i]-=amountToPay;
						totalBanksLoss-=amountToPay;
					}
				}
				
			} 
			//all banks recover the same share of their outstanding credit as the total available funds are residualDeposits plus K
			//discounted value and this sum is distributed across loans on the base of their weight on total outstanding loans. 
			//Abstracting from residual deposits (which in most cases would be negligible) the share recovered would be Kvalue/totLoans
			for(int i=0;i<loans.size();i++){
				Loan loan = (Loan) loans.get(i);
				CreditSupplier lendingBank= (CreditSupplier) loan.getAssetHolder();
				lendingBank.setCurrentNonPerformingLoans(StaticValues.SM_LOAN,lendingBank.getCurrentNonPerformingLoans(StaticValues.SM_LOAN)+banksLosses[i]);
				loan.setValue(0);
			}
			
			// Recapitalize firm if nW is below average
			
			SimulationController controller = (SimulationController)firm.getScheduler();
			MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
			
			if (firm instanceof ConsumptionFirm){
			
			Population cFirms = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
			
			double averageNetWealthSum = 0;
			
			List<Item> consGoods=firm.getItemsStockMatrix(true, StaticValues.SM_CONSGOOD);
			
			for(Agent h:cFirms.getAgents()){
				ConsumptionFirm cFirm = (ConsumptionFirm) h;
				averageNetWealthSum += cFirm.getPassedValue(StaticValues.LAG_NETWEALTH, 1);
			}
			
			double firmAssets = 0;
			
			// Calculate firms assets

			for (Item i:consGoods){
				firmAssets += i.getValue();
			}
			List<Item> kGoods=firm.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
			for (Item i:kGoods){
				CapitalGood kGood = (CapitalGood) i;
				if(kGood.isObsolete()==false) {
					firmAssets += i.getValue();
				}
			}
			List<Item> cFirmPayingStocks = firm.getPayingStocks(StaticValues.MKT_LABOR,null);
		
			for (Item i:cFirmPayingStocks){
				firmAssets += i.getValue();
			}
			
			// Calculate firms current net wealth
			double firmLoansSum = 0;
			
			for (Item i:loans){
				firmLoansSum += i.getValue();
			}
			
			double firmNW = firmAssets-firmLoansSum;
			
			double averageNetWealth = averageNetWealthSum/cFirms.getSize();
			
			Population households = macroPop.getPopulation(StaticValues.HOUSEHOLDS_ID);
			
			double totalNW = 0;
			for (Agent receiver : households.getAgents()) {
				totalNW += ((MacroAgent) receiver).getNetWealth();
			}
			
			if(firmNW<averageNetWealth) {
				double nwDelta = averageNetWealth-firmNW;
				
				Item targetStock = firm.getPayableStock(0);
				
				for (Agent rec : households.getAgents()) {
					Households receiver = (Households) rec;
					double hhnw = receiver.getNetWealth();
					double toPay = hhnw * (nwDelta) / totalNW;

					Item payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
					List<Item> hhPayingStocks = receiver.getPayingStocks(0, payablestock);
					receiver.reallocateLiquidity(toPay, hhPayingStocks, payablestock);

					LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();

					libHolder.transfer(payablestock, targetStock, toPay);
				}
				
			}
			}else if(firm instanceof CapitalFirm){
				
				Population kFirms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
				
				double averageNetWealthSum = 0;
				
				for(Agent h:kFirms.getAgents()){
					CapitalFirm cFirm = (CapitalFirm) h;
					averageNetWealthSum += cFirm.getPassedValue(StaticValues.LAG_NETWEALTH, 1);
				}
				
				double firmAssets = 0;
				
				// Calculate firms assets

				List<Item> kGoods=firm.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
				for (Item i:kGoods){
					CapitalGood kGood = (CapitalGood) i;
					if(kGood.isObsolete()==false) {
						firmAssets += i.getValue();
					}
				}
				List<Item> kFirmPayingStocks = firm.getPayingStocks(StaticValues.MKT_LABOR,null);

				for (Item i:kFirmPayingStocks){
					firmAssets += i.getValue();
				}
				
				// Calculate firms current net wealth
				double firmLoansSum = 0;
				
				for (Item i:loans){
					firmLoansSum += i.getValue();
				}
				
				double firmNW = firmAssets-firmLoansSum;
				
				double averageNetWealth = averageNetWealthSum/kFirms.getSize();
				
				Population households = macroPop.getPopulation(StaticValues.HOUSEHOLDS_ID);
				
				double totalNW = 0;
				for (Agent receiver : households.getAgents()) {
					totalNW += ((MacroAgent) receiver).getNetWealth();
				}
				
				if(firmNW<averageNetWealth) {
					double nwDelta = averageNetWealth-firmNW;
					
					Item targetStock = firm.getPayableStock(0);
					
					for (Agent rec : households.getAgents()) {
						Households receiver = (Households) rec;
						double hhnw = receiver.getNetWealth();
						double toPay = hhnw * (nwDelta) / totalNW;

						Item payablestock = receiver.getPayableStock(StaticValues.MKT_LABOR);
						List<Item> hhPayingStocks = receiver.getPayingStocks(0, payablestock);
						receiver.reallocateLiquidity(toPay, hhPayingStocks, payablestock);

						LiabilitySupplier libHolder = (LiabilitySupplier) payablestock.getLiabilityHolder();

						libHolder.transfer(payablestock, targetStock, toPay);
					}
					
				}
				
				
			}
			
			// Set firm's mark up to an average level
			
			if (firm instanceof ConsumptionFirm){
				Population pop = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
				
				double markupSum = 0;
				for (Agent i:pop.getAgents()){
					ConsumptionFirm cFirm= (ConsumptionFirm) i;
					AdaptiveMarkUpOnAC tempStrategy = (AdaptiveMarkUpOnAC )cFirm.getStrategy(StaticValues.STRATEGY_PRICING);
					markupSum += tempStrategy.getMarkUp();
				}
				
				AdaptiveMarkUpOnAC strategy = (AdaptiveMarkUpOnAC )firm.getStrategy(StaticValues.STRATEGY_PRICING);
				double avMarkup = markupSum/pop.getSize();
				strategy.setMarkUp(avMarkup);	
			}
			else if (firm instanceof CapitalFirm){
				Population pop = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
				
				double markupSum = 0;
				for (Agent i:pop.getAgents()){
					CapitalFirm kFirm= (CapitalFirm) i;
					AdaptiveMarkUpOnAC tempStrategy = (AdaptiveMarkUpOnAC )kFirm.getStrategy(StaticValues.STRATEGY_PRICING);
					markupSum += tempStrategy.getMarkUp();
				}
				
				AdaptiveMarkUpOnAC strategy = (AdaptiveMarkUpOnAC )firm.getStrategy(StaticValues.STRATEGY_PRICING);
				double avMarkup = markupSum/pop.getSize();
				strategy.setMarkUp(avMarkup);	
			}		
			
		}
	}
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [haircut]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putDouble(this.haircut);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [haircut]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.haircut = buf.getDouble();
	}

}
