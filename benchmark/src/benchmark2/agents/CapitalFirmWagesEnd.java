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
package benchmark2.agents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import benchmark2.StaticValues;
import benchmark2.strategies.ProfitsWealthTaxStrategy;
import cern.jet.random.engine.RandomEngine;
import jmab2.agents.CreditDemander;
import jmab2.agents.DepositDemander;
import jmab2.agents.FinanceAgent;
import jmab2.agents.GoodSupplier;
import jmab2.agents.LaborDemander;
import jmab2.agents.LaborSupplier;
import jmab2.agents.LiabilitySupplier;
import jmab2.agents.MacroAgent;
import jmab2.agents.PriceSetterWithTargets;
import jmab2.agents.ProfitsTaxPayer;
import jmab2.events.MacroTicEvent;
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.CapitalGood;
import jmab2.stockmatrix.Cash;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.Item;
import jmab2.strategies.DividendsStrategy;
import jmab2.strategies.FinanceStrategy;
import jmab2.strategies.TargetExpectedInventoriesOutputStrategy;
import net.sourceforge.jabm.agent.AgentList;

/**
 * Class representing Capital Producers 
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class CapitalFirmWagesEnd extends CapitalFirm implements GoodSupplier,
		CreditDemander, LaborDemander, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent {

	private double minWageDiscount;
	private double shareOfExpIncomeAsDeposit;

	/* (non-Javadoc)
	 * @see jmab2.agents.SimpleAbstractAgent#onTicArrived(AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {

		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			bailoutCost=0;
			this.defaulted=false;
			computeExpectations();
			determineOutput();
			computeDebtPayments();
			break;
		case StaticValues.TIC_CAPITALPRICE:
			computePrice();
			break;
		case StaticValues.TIC_RDDECISION:
			researchDecision();
			break;
		case StaticValues.TIC_CREDITDEMAND:
			computeCreditDemand();
			break;
		case StaticValues.TIC_LABORDEMAND:
			computeLaborDemand();
			break;
		case StaticValues.TIC_PRODUCTION:
			produce();
			break;
		case StaticValues.TIC_RDOUTCOME:
			researchOutcome();
			break;
		case StaticValues.TIC_WAGEPAYMENT:
			payWages();
			break;
		case StaticValues.TIC_CREDINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_DIVIDENDS:
			payDividends();
			break;
		case StaticValues.TIC_DEPOSITDEMAND:
			computeLiquidAssetsAmounts();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			updateExpectations();
		}


	}

	/**
	 * 
	 */
	private void payWages() {
		//If there are wages to pay
		if(employees.size()>0){
			//Prepare the re-allocation of funds
			//1 Get the payable stock
			Item targetStock = this.getPayableStock(StaticValues.MKT_CAPGOOD);
			//2 Get the paying stocks
			List<Item> payingStocks = this.getPayingStocks(StaticValues.MKT_LABOR,targetStock);
			
			// Calculate amount to be reallocated
			double reallocateAmount = 0;
			for(Item item:payingStocks){
				reallocateAmount += item.getValue();
			}
			// Reallocate
			reallocateLiquidity(reallocateAmount, payingStocks, targetStock);
			
			double wageBill = this.getWageBill();
			double neededDiscount = 1;
			if(wageBill>targetStock.getValue()){
				neededDiscount = targetStock.getValue()/wageBill;
			}
			if(Math.round(neededDiscount)<Math.round(this.minWageDiscount)){
				int currentWorkers = this.employees.size();
				AgentList emplPop = new AgentList();
				for(MacroAgent ag : this.employees)
					emplPop.add(ag);
				emplPop.shuffle(prng);
				for(int i=0;i<currentWorkers;i++){
					
					LaborSupplier employee = (LaborSupplier) emplPop.get(i);
					Item payableStock = employee.getPayableStock(StaticValues.MKT_LABOR);
					LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
					payingSupplier.transfer(targetStock, payableStock, wageBill*neededDiscount/employees.size());
				}
				targetStock.setValue(0);
				this.bankruptcy();
			}else{
				//3. Pay wages
				int currentWorkers = this.employees.size();
				AgentList emplPop = new AgentList();
				for(MacroAgent ag : this.employees)
					emplPop.add(ag);
				emplPop.shuffle(prng);
				for(int i=0;i<currentWorkers;i++){
					LaborSupplier employee = (LaborSupplier) emplPop.get(i);
					double wage = employee.getWage();
					if(wage<targetStock.getValue()){
						Item payableStock = employee.getPayableStock(StaticValues.MKT_LABOR);
						LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
						payingSupplier.transfer(targetStock, payableStock, wage*neededDiscount);
					}
				}
			}
		}
		
	}

	/**
	 * Computes labor demand. In this case, labor demand is equal to the quantity of workers needed to produced
	 * desired output plus quantity of workers corresponding the desired level of investment in R&D.
	 */
	@Override
	protected void computeLaborDemand() {
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);

		int currentWorkers = this.employees.size();
		AgentList emplPop = new AgentList();
		for(MacroAgent ag : this.employees)
			emplPop.add(ag);
		emplPop.shuffle(prng);
		for(int i=0;i<this.turnoverLabor*currentWorkers;i++){
			fireAgent((MacroAgent)emplPop.get(i));
		}
		cleanEmployeeList();
		currentWorkers = this.employees.size();
		double expWages = expectation.getExpectation();
		int nbWorkers = this.getRequiredWorkers()+(int)Math.round(this.amountResearch/expWages);

		if(nbWorkers>currentWorkers){
			this.laborDemand=nbWorkers-currentWorkers;
		}else{
			this.setActive(false, StaticValues.MKT_LABOR);
			this.laborDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.employees)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentWorkers-nbWorkers;i++){
				fireAgent((MacroAgent)emplPop.get(i));
			}
		}
		cleanEmployeeList();
		if(this.laborDemand>0)
			this.setActive(true, StaticValues.MKT_LABOR);
	}

	/**
	 * Computes the amount of credit needed to finance production and R&D
	 */
	@Override
	protected void computeCreditDemand() {
		computeDebtPayments();
		FinanceStrategy strategy = (FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		Expectation wageExp = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		Expectation nomSalesExp = this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
		double lNomInv=(double)this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		Expectation realSalesExp=this.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		double expRealSales=realSalesExp.getExpectation();
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
		//double uc=inventories.getUnitCost();
		//double uc2=this.getPriceLowerBound();
		double uc=(this.getWageBill()+this.debtInterests)/this.desiredOutput;
		int inv = (int)inventories.getQuantity();
		int nbWorkers = this.getRequiredWorkers();
		double expWages = wageExp.getExpectation();
		DividendsStrategy strategyDiv=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		TargetExpectedInventoriesOutputStrategy strategyProd= (TargetExpectedInventoriesOutputStrategy) this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		ProfitsWealthTaxStrategy taxStrategy= (ProfitsWealthTaxStrategy) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double profitTaxRate=taxStrategy.getProfitTaxRate();
		double shareInvenstories=strategyProd.getInventoryShare();
		double profitShare=strategyDiv.getProfitShare();
		//double expRevenues = nomSalesExp.getExpectation();
		double expRevenues = expRealSales*this.getPrice();
		double expectedProfits=expRevenues-(nbWorkers*expWages)+this.interestReceived-this.debtInterests+(shareInvenstories*expRealSales)*uc-lNomInv;
		double expectedTaxes=Math.max(0, expectedProfits*profitTaxRate);
		double expectedDividends=Math.max(0,expectedProfits*(1-profitTaxRate)*profitShare);
		double expectedFinancialRequirement=(nbWorkers*expWages)+(Math.floor(this.amountResearch/expWages))*expWages +
				this.debtBurden - this.interestReceived + expectedDividends+expectedTaxes-expRevenues+ this.shareOfExpIncomeAsDeposit*(nbWorkers*expWages);
		expectedFinancialRequirement=Math.max(expectedFinancialRequirement,Math.ceil(nbWorkers*expWages));
		this.creditDemanded = strategy.computeCreditDemand(expectedFinancialRequirement);
		if(creditDemanded>0)
			this.setActive(true, StaticValues.MKT_CREDIT);
	}

	/**
	 * @return the minWageDiscount
	 */
	public double getMinWageDiscount() {
		return minWageDiscount;
	}

	/**
	 * @param minWageDiscount the minWageDiscount to set
	 */
	public void setMinWageDiscount(double minWageDiscount) {
		this.minWageDiscount = minWageDiscount;
	}

	/**
	 * @return the shareOfExpIncomeAsDeposit
	 */
	public double getShareOfExpIncomeAsDeposit() {
		return shareOfExpIncomeAsDeposit;
	}

	/**
	 * @param shareOfExpIncomeAsDeposit the shareOfExpIncomeAsDeposit to set
	 */
	public void setShareOfExpIncomeAsDeposit(double shareOfExpIncomeAsDeposit) {
		this.shareOfExpIncomeAsDeposit = shareOfExpIncomeAsDeposit;
	}
	
	/**
	 * Populates the agent characteristics using the byte array content. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][targetStock][amountResearch][creditdDemanded][capitalProductivity][capitalLaborRatio]
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][minWageDiscount][shareOfExpIncomeAsDeposit]
	 * [sizeDebtPayments][debtPayments][payableStockId][laborProductvity][capitalDuration][capitalAmortization]
	 * [matrixSize][stockMatrixStructure][expSize][ExpectationStructure][passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public void populateAgent(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		byte[] macroBytes = new byte[buf.getInt()];
		buf.get(macroBytes);
		super.populateCharacteristics(macroBytes, pop);
		targetStock = buf.getDouble();
		amountResearch = buf.getDouble();
		creditDemanded = buf.getDouble();
		capitalProductivity = buf.getDouble();
		capitalLaborRatio = buf.getDouble();
		debtBurden = buf.getDouble();
		debtInterests = buf.getDouble();
		interestReceived = buf.getDouble();
		turnoverLabor = buf.getDouble();
		minWageDiscount = buf.getDouble();
		shareOfExpIncomeAsDeposit = buf.getDouble();
		int lengthDebtPayments = buf.getInt();
		debtPayments = new double[lengthDebtPayments][3];
		for(int i = 0 ; i < debtPayments.length ; i++){
			debtPayments[i][0] = buf.getDouble();
			debtPayments[i][1] = buf.getDouble();
			debtPayments[i][2] = buf.getDouble();
		}
		payableStockId = buf.getInt();
		laborProductivity = buf.getInt();
		capitalDuration = buf.getInt();
		capitalAmortization = buf.getInt();
		int matSize = buf.getInt();
		if(matSize>0){
			byte[] smBytes = new byte[matSize];
			buf.get(smBytes);
			this.populateStockMatrixBytes(smBytes, pop);
		}
		int expSize = buf.getInt();
		if(expSize>0){
			byte[] expBytes = new byte[expSize];
			buf.get(expBytes);
			this.populateExpectationsBytes(expBytes);
		}
		int lagSize = buf.getInt();
		if(lagSize>0){
			byte[] lagBytes = new byte[lagSize];
			buf.get(lagBytes);
			this.populatePassedValuesBytes(lagBytes);
		}
		int stratSize = buf.getInt();
		if(stratSize>0){
			byte[] stratBytes = new byte[stratSize];
			buf.get(stratBytes);
			this.populateStrategies(stratBytes, pop);
		}
	}
	
	/**
	 * Generates the byte array containing all relevant informations regarding the capital firm agent. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][targetStock][amountResearch][creditdDemanded][capitalProductivity][capitalLaborRatio]
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][minWageDiscount][shareOfExpIncomeAsDeposit]
	 * [sizeDebtPayments][debtPayments][payableStockId][laborProductvity][capitalDuration][capitalAmortization]
	 * [matrixSize][stockMatrixStructure][expSize][ExpectationStructure][passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate((9+3*debtPayments.length)*8+16);
			buf.putDouble(targetStock);
			buf.putDouble(amountResearch);
			buf.putDouble(creditDemanded);
			buf.putDouble(capitalProductivity);
			buf.putDouble(capitalLaborRatio);
			buf.putDouble(debtBurden);
			buf.putDouble(debtInterests);
			buf.putDouble(interestReceived);
			buf.putDouble(turnoverLabor);	
			buf.putDouble(minWageDiscount);
			buf.putDouble(shareOfExpIncomeAsDeposit);			
			buf.putInt(debtPayments.length);
			for(int i = 0 ; i < debtPayments.length ; i++){
				buf.putDouble(debtPayments[i][0]);
				buf.putDouble(debtPayments[i][1]);
				buf.putDouble(debtPayments[i][2]);
			}
			buf.putInt(payableStockId);
			buf.putDouble(laborProductivity);
			buf.putInt(capitalDuration);
			buf.putInt(capitalAmortization);
			out.write(buf.array());
			byte[] smBytes = super.getStockMatrixBytes();
			out.write(ByteBuffer.allocate(4).putInt(smBytes.length).array());
			out.write(smBytes);
			byte[] expBytes = super.getExpectationsBytes();
			out.write(ByteBuffer.allocate(4).putInt(expBytes.length).array());
			out.write(expBytes);
			byte[] passedValBytes = super.getPassedValuesBytes();
			out.write(ByteBuffer.allocate(4).putInt(passedValBytes.length).array());
			out.write(passedValBytes);
			byte[] stratsBytes = super.getStrategiesBytes();
			out.write(ByteBuffer.allocate(4).putInt(stratsBytes.length).array());
			out.write(stratsBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}
}
