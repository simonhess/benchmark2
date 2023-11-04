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

import java.util.ArrayList;
import java.util.List;

import benchmark.StaticValues;
import jmab2.agents.AbstractFirm;
import jmab2.agents.CreditSupplier;
import jmab2.agents.LaborSupplier;
import jmab2.agents.LiabilitySupplier;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Cash;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.BankruptcyStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class FirmBankruptcy extends AbstractStrategy implements
		BankruptcyStrategy {
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.BankruptcyStrategy#bankrupt()
	 */
	@Override
	public void bankrupt() {

		AbstractFirm firm = (AbstractFirm) this.agent;

		//1. Move all money on one deposit if there were two of them
		List<Item> deposits = firm.getItemsStockMatrix(true, StaticValues.SM_DEP);
		Deposit deposit = (Deposit)deposits.get(0);
		Deposit res = (Deposit)firm.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		
		Cash cash = (Cash)firm.getItemStockMatrix(true, StaticValues.SM_CASH);
		
		// Get the paying stocks
		List<Item> payingStocks = firm.getPayingStocks(StaticValues.MKT_LABOR,deposit);
		
		// Calculate amount to be reallocated
		double reallocateAmount = 0;
		for(Item item:payingStocks){
			reallocateAmount += item.getValue();
		}
		// Reallocate
		firm.reallocateLiquidity(reallocateAmount, payingStocks, deposit);

		//3. Compute total liquidity to be distributed to creditors
		double liquidity=deposit.getValue();

		//4. Compute each creditor's share of debt
		List<Item> loans=firm.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double[] debts = new double[loans.size()];
		double totalDebt=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			debts[i]=loan.getValue();
			totalDebt+=loan.getValue();
		}

		//5. Distribute liquidity according to the share of debt of each creditor
		for(int i=0;i<loans.size();i++){
			Loan loan = (Loan) loans.get(i);
			double amountToPay=liquidity*(debts[i])/totalDebt;
			CreditSupplier lendingBank= (CreditSupplier) loan.getAssetHolder();
			lendingBank.setCurrentNonPerformingLoans(StaticValues.SM_LOAN,lendingBank.getCurrentNonPerformingLoans(StaticValues.SM_LOAN)+(debts[i]-amountToPay)); 
			deposit.setValue(deposit.getValue()-amountToPay);
			if(loan.getAssetHolder()!=deposit.getLiabilityHolder()){
				Item lBankRes = loan.getAssetHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
				lBankRes.setValue(lBankRes.getValue()+amountToPay);
				Item dBankRes = deposit.getLiabilityHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
				dBankRes.setValue(dBankRes.getValue()-amountToPay);
			}
			loan.setValue(0);
		}
				
		//6. fire all employees
		for(MacroAgent employee:firm.getEmployees()){
			if(((LaborSupplier) employee).getEmployer()!=null){
				firm.fireAgent(employee);
			}
		}
		firm.cleanEmployeeList();
		
		//7. Removing deposit and cash from liability holder stock matrix		
		deposit.getLiabilityHolder().removeItemStockMatrix(deposit, false, deposit.getSMId());
		cash.getLiabilityHolder().removeItemStockMatrix(cash, false, cash.getSMId());
		res.getLiabilityHolder().removeItemStockMatrix(res, false, res.getSMId());

		
		//8. Create new set of asset
		List<List<Item>> assets = new ArrayList<List<Item>>();
		for(int i=0;i<firm.getAssetStock().size();i++){
			assets.add(new ArrayList<Item>());
		}
		firm.setAssetStock(assets);  
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
