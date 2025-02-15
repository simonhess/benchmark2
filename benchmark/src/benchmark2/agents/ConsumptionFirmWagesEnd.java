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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import benchmark2.StaticValues;
import benchmark2.strategies.ProfitsWealthTaxStrategy;
import cern.jet.random.engine.RandomEngine;
import jmab2.agents.CreditDemander;
import jmab2.agents.DepositDemander;
import jmab2.agents.FinanceAgent;
import jmab2.agents.GoodDemander;
import jmab2.agents.GoodSupplier;
import jmab2.agents.InvestmentAgent;
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
import jmab2.stockmatrix.ConsumptionGood;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.Item;
import jmab2.strategies.DividendsStrategy;
import jmab2.strategies.FinanceStrategy;
import jmab2.strategies.InvestmentStrategy;
import jmab2.strategies.SelectSellerStrategy;
import jmab2.strategies.TargetExpectedInventoriesOutputStrategy;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ConsumptionFirmWagesEnd extends ConsumptionFirm implements GoodSupplier, GoodDemander, CreditDemander, 
LaborDemander, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent, InvestmentAgent {



	private double minWageDiscount;
	private double shareOfExpIncomeAsDeposit;


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
		case StaticValues.TIC_CONSUMPTIONPRICE:
			computePrice();
			break;
		case StaticValues.TIC_INVESTMENTDEMAND:
			InvestmentStrategy strategy1=(InvestmentStrategy) this.getStrategy(StaticValues.STRATEGY_INVESTMENT);
			this.desiredCapacityGrowth=strategy1.computeDesiredGrowth();
			SelectSellerStrategy buyingStrategy = (SelectSellerStrategy) this.getStrategy(StaticValues.STRATEGY_BUYING);
			computeDesiredInvestment(buyingStrategy.selectGoodSupplier(this.selectedCapitalGoodSuppliers, 0.0, true));
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
			Item targetStock = this.getPayableStock(StaticValues.MKT_CONSGOOD);
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
				//System.out.println("discount");
				neededDiscount = targetStock.getValue()/wageBill;
			}
			if(neededDiscount<this.minWageDiscount){
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
				System.out.println("Default "+ this.getAgentId() + " due to wages");
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
	 * Compute the labor demand by the firm. First it determine the total amount of workers required to produce
	 * the desiredOutput through the method getRequiredWorkers: if smaller than the number of current employees the firm 
	 * fires the last it had hired, otherwise it hires new workers. 
	 */
	@Override
	protected void computeLaborDemand() {

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

		int nbWorkers= this.getRequiredWorkers();	
		if(nbWorkers>currentWorkers){
			this.laborDemand=nbWorkers-currentWorkers;
		}else{
			this.laborDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.employees)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			this.setActive(false, StaticValues.MKT_LABOR);
			for(int i=0;i<currentWorkers-nbWorkers;i++){
				fireAgent((MacroAgent)emplPop.get(i));
			}
		}
		if (laborDemand>0){
			this.setActive(true, StaticValues.MKT_LABOR);
		}
		cleanEmployeeList();
	}

	/**
	 *
	 */
	@Override
	protected void computeCreditDemand() {
		this.computeDebtPayments();
		Expectation nomSalesExp=this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
		Expectation realSalesExp=this.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		double lNomInv=(double)this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		double expRealSales=realSalesExp.getExpectation();
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
		//double uc=inventories.getUnitCost();
		//double uc2=this.getPriceLowerBound();
		//int inv = (int)inventories.getQuantity();
		//double expRevenues=nomSalesExp.getExpectation();
		double expRevenues = expRealSales*this.getPrice();
		int nbWorkers = this.getRequiredWorkers();
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		double expWages = expectation.getExpectation();
		DividendsStrategy strategyDiv=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		double profitShare=strategyDiv.getProfitShare();
		TargetExpectedInventoriesOutputStrategy strategyProd= (TargetExpectedInventoriesOutputStrategy) this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		ProfitsWealthTaxStrategy taxStrategy= (ProfitsWealthTaxStrategy) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double profitTaxRate=taxStrategy.getProfitTaxRate();
		double shareInvenstories=strategyProd.getInventoryShare();
		List<Item> capStocks = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		double capitalAmortization = 0;
		for(Item c:capStocks){
			CapitalGood cap = (CapitalGood)c;
			if(cap.getAge()>=0 && cap.getAge()<cap.getCapitalAmortization())
				capitalAmortization+=cap.getQuantity()*cap.getPrice()/cap.getCapitalAmortization();
		}
		
		double uc = (capitalAmortization+this.getWageBill()+this.debtInterests)/desiredOutput;
		
		double expectedProfits=expRevenues-(nbWorkers*expWages)+this.interestReceived+this.reservesInterestsReceived-this.debtInterests+(shareInvenstories*expRealSales)*uc-lNomInv-capitalAmortization;
		double expectedTaxes=Math.max(0, expectedProfits*profitTaxRate);
		double expectedDividends=Math.max(0,expectedProfits*(1-profitTaxRate)*profitShare);
		double investment=this.desiredRealCapitalDemand*((CapitalFirm)this.selectedCapitalGoodSuppliers.get(0)).getPrice();
		double expectedProfitsAfterTaxes = expectedProfits-expectedTaxes;

		double expectedOCF = expRevenues+this.interestReceived + this.reservesInterestsReceived-(nbWorkers*expWages)-this.debtInterests-expectedTaxes;
		double totalFinancialRequirement=investment+expectedDividends+this.shareOfExpIncomeAsDeposit*(nbWorkers*expWages)+(this.debtBurden-this.debtInterests)-expectedOCF;

		totalFinancialRequirement=Math.max(totalFinancialRequirement,Math.ceil(nbWorkers*expWages));
		FinanceStrategy strategy =(FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		this.creditDemanded=strategy.computeCreditDemand(totalFinancialRequirement);
		if(creditDemanded>0){
			this.setActive(true, StaticValues.MKT_CREDIT);
		}
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
	 *  [sizeMacroAgentStructure][MacroAgentStructure][targetStock][creditdDemanded][desiredCapacityGrowth][desiredRealCapitalDemand]
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][sizeDebtPayments][debtPayments]
	 * [sizeSuppliers][suppliersPopId and suppliersId][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public void populateAgent(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		byte[] macroBytes = new byte[buf.getInt()];
		buf.get(macroBytes);
		super.populateCharacteristics(macroBytes, pop);
		targetStock = buf.getDouble();
		creditDemanded = buf.getDouble();
		desiredCapacityGrowth = buf.getDouble();
		desiredRealCapitalDemand = buf.getDouble();
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
		int nbSuppliers = buf.getInt();
		this.selectedCapitalGoodSuppliers = new ArrayList<Agent>();
		for(int i = 0 ; i < nbSuppliers ; i++){
			Collection<Agent> aHolders = pop.getPopulation(buf.getInt()).getAgents();
			long selSupplierId = buf.getLong(); 
			for(Agent a:aHolders){
				MacroAgent pot = (MacroAgent) a;
				if(pot.getAgentId()==selSupplierId){
					this.selectedCapitalGoodSuppliers.add(pot);
				}
			}
		}
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
	 * Generates the byte array containing all relevant informations regarding the consumption firm agent. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][targetStock][creditdDemanded][desiredCapacityGrowth][desiredRealCapitalDemand]
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][minWageDiscount][shareOfExpIncomeAsDeposit][sizeDebtPayments][debtPayments]
	 * [sizeSuppliers][suppliersPopId and suppliersId][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
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
			buf.putDouble(creditDemanded);
			buf.putDouble(desiredCapacityGrowth);
			buf.putDouble(desiredRealCapitalDemand);
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
			buf.putInt(this.selectedCapitalGoodSuppliers.size());
			for(Agent supplier:selectedCapitalGoodSuppliers){
				buf.putInt(((MacroAgent)supplier).getPopulationId());
				buf.putLong(((MacroAgent)supplier).getAgentId());
			}
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
