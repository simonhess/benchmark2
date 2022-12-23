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
package benchmark.agents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import benchmark.StaticValues;
import benchmark.expectations.NoExpectation;
import benchmark.strategies.InvestmentCapacityOperatingCashFlowExpected;
import jmab.agents.AbstractFirm;
import jmab.agents.CreditDemander;
import jmab.agents.DepositDemander;
import jmab.agents.FinanceAgent;
import jmab.agents.GoodDemander;
import jmab.agents.GoodSupplier;
import jmab.agents.InvestmentAgent;
import jmab.agents.LaborDemander;
import jmab.agents.LaborSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.agents.PriceSetterWithTargets;
import jmab.agents.ProfitsTaxPayer;
import jmab.events.MacroTicEvent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.simulations.TwoStepMarketSimulation;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.AdaptiveMarkUpOnAC;
import jmab.strategies.BankruptcyStrategy;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.FinanceStrategy;
import jmab.strategies.InvestmentStrategy;
import jmab.strategies.PricingStrategy;
import jmab.strategies.ProductionStrategy;
import jmab.strategies.RealCapitalDemandStrategy;
import jmab.strategies.SelectDepositSupplierStrategy;
import jmab.strategies.SelectLenderStrategy;
import jmab.strategies.SelectSellerStrategy;
import jmab.strategies.SelectWorkerStrategy;
import jmab.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ConsumptionFirm extends AbstractFirm implements GoodSupplier, GoodDemander, CreditDemander, 
LaborDemander, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent, InvestmentAgent {

	protected double creditDemanded;
	protected double targetStock;
	protected double desiredCapacityGrowth;
	protected ArrayList<Agent> selectedCapitalGoodSuppliers;
	protected double desiredRealCapitalDemand;
	protected double[][] debtPayments;
	protected double debtBurden;
	protected double debtInterests;
	protected double interestReceived;
	protected double reservesInterestsReceived;
	protected double turnoverLabor;
	protected double expectedVariableCosts;
	protected double preferredDepositRatio;
	protected double preferredCashRatio;
	protected double preferredReserveRatio;
	private double cashAmount;
	private double depositAmount;
	private double reservesAmount;
	

//TODO check whether targetStock and its getters/setters are really used.
	/**
	 * @return the targetStock
	 */
	public double getTargetStock() {
		return targetStock;
	}

	/**
	 * @param targetStock the targetStock to set
	 */
	public void setTargetStock(double targetStock) {
		this.targetStock = targetStock;
	}
	
	
	

	/**
	 * @param debtPayments the debtPayments to set
	 */
	public void setDebtPayments(double[][] debtPayments) {
		this.debtPayments = debtPayments;
	}

	/**
	 * @return the debtBurden
	 */
	public double getDebtBurden() {
		return debtBurden;
	}

	/**
	 * @param debtBurden the debtBurden to set
	 */
	public void setDebtBurden(double debtBurden) {
		this.debtBurden = debtBurden;
	}

	/**
	 * @return the debtInterests
	 */
	public double getDebtInterests() {
		return debtInterests;
	}

	/**
	 * @param debtInterests the debtInterests to set
	 */
	public void setDebtInterests(double debtInterests) {
		this.debtInterests = debtInterests;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.MacroAgent#onRoundFinished(net.sourceforge.jabm.event.RoundFinishedEvent)
	 */
	@Override
	public void onRoundFinished(RoundFinishedEvent event) {
		// TODO Auto-generated method stub

	}
    
	/* (non-Javadoc)
	 * @see jmab.agents.MacroAgent#initialiseCounterpart(net.sourceforge.jabm.agent.Agent, int)
	 */
	@Override
	public void initialiseCounterpart(Agent counterpart, int marketID) {
		// TODO Auto-generated method stub
	
	}

	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			this.defaulted=false;
			computeExpectations();
			determineOutput();
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
	protected void updateExpectations() {
		Item inventories =this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
		double nW=this.getNetWealth();
		this.addValue(StaticValues.LAG_NETWEALTH,nW);
		inventories.setAge(-2);
		this.cleanSM();
		if(this.defaulted==true) {
			// Set lagged defaulted value to 1 indicating that this firm was defaulted in the last round
			this.addValue(StaticValues.LAG_DEFAULTED,1);
		}else {
			this.addValue(StaticValues.LAG_DEFAULTED,0);
		}
	}

	@Override
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_CAPGOOD:
			TwoStepMarketSimulation sim = (TwoStepMarketSimulation)macroSim.getActiveMarket();
			if(sim.isFirstStep()){				
				this.selectedCapitalGoodSuppliers=event.getObjects();
			}else if(sim.isSecondStep()){
				if(this.selectedCapitalGoodSuppliers.isEmpty()) this.selectedCapitalGoodSuppliers=event.getObjects();
				
				int nbSellers = this.selectedCapitalGoodSuppliers.size()+1;//There are nbSellers+1 options for the firm to invest
				for(int i=0; i<nbSellers&&this.desiredRealCapitalDemand>0&&this.selectedCapitalGoodSuppliers.size()>0;i++){
					SelectSellerStrategy buyingStrategy = (SelectSellerStrategy) this.getStrategy(StaticValues.STRATEGY_BUYING);
					MacroAgent selSupplier = buyingStrategy.selectGoodSupplier(this.selectedCapitalGoodSuppliers, 0.0, true);
					//computeDesiredInvestment(selSupplier);
					macroSim.getActiveMarket().commit(this, selSupplier,marketID);
					this.selectedCapitalGoodSuppliers.remove(selSupplier);
				}
			}
			break;
		case StaticValues.MKT_CREDIT:
			SelectLenderStrategy borrowingStrategy = (SelectLenderStrategy) this.getStrategy(StaticValues.STRATEGY_BORROWING);
			MacroAgent lender= (MacroAgent)borrowingStrategy.selectLender(event.getObjects(), this.getLoanRequirement(StaticValues.SM_LOAN), this.getLoanLength());
			macroSim.getActiveMarket().commit(this, lender,marketID);
			break;
		case StaticValues.MKT_DEPOSIT:
			SelectDepositSupplierStrategy depStrategy = (SelectDepositSupplierStrategy) this.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			MacroAgent depSupplier= (MacroAgent)depStrategy.selectDepositSupplier(event.getObjects(), this.getDepositAmount());
			macroSim.getActiveMarket().commit(this, depSupplier,marketID);
			break;
		case StaticValues.MKT_LABOR:
			SelectWorkerStrategy strategy = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent worker= (MacroAgent)strategy.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, worker,marketID);
			break;
		}
	}
	
	/**
	 * Determines output using a strategy
	 */
	protected void determineOutput() {
		ProductionStrategy strategy = (ProductionStrategy)this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		this.setDesiredOutput(strategy.computeOutput());
		this.setActive(true, StaticValues.MKT_CAPGOOD); //TODO CHECK IF THIS IS THE RIGHT PLACE TO ACTIVATE C ON THE CAPITAL MKT
		
	}

	/**
	 * @return the number of workers needed to produce desired output
	 * To compute the amount of workers required the firm first sort the capital stocks according to their productivity 
	 * (from less to more productive capital goods) creating a TreeMap with keys=productivity levels and values=the corresponding capital objects.
	 * Then, it compute the amount of goods than can be produced with the most productive capital (extracted from the TreeMap using 
	 * pollLastEntry()). If this amount<residualOuput it computes the amount of workers required to use the whole stock of this capital, then 
	 * updates the residualOutput and passes to consider the second most productive capital and so on. When the amount producible with a certain capital
	 * stock is more than the residualOutput, the firm compute the amount of capital it will use to produce this latter and then the amount of workers 
	 * required. NB The firm may be constraint by its productive capacity.
	 */
	protected int getRequiredWorkers() {
		List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
		for (Item item:currentCapitalStock){
			CapitalGood capital=(CapitalGood)item;
			double prod = capital.getProductivity();
			if(orderedCapital.containsKey(prod)){
				ArrayList<CapitalGood> list = orderedCapital.get(prod);
				list.add(capital);
			}else{
				ArrayList<CapitalGood> list = new ArrayList<CapitalGood>();
				list.add(capital);
				orderedCapital.put(prod, list);
			}
				
		}
		double residualOutput=this.getDesiredOutput();
		double requiredWorkers=0;
		for (Double key:orderedCapital.descendingKeySet()){
			for(CapitalGood capital:orderedCapital.get(key)){	
				if (residualOutput>capital.getProductivity()*capital.getQuantity()){
					requiredWorkers+=capital.getQuantity()/capital.getCapitalLaborRatio();
					residualOutput-=capital.getProductivity()*capital.getQuantity();
				}
				else{
					double requiredCapital=residualOutput/capital.getProductivity();
					requiredWorkers+=requiredCapital/capital.getCapitalLaborRatio();
					residualOutput-=requiredCapital*capital.getProductivity();
				}
			}
		}
		return (int) Math.round(requiredWorkers);
	}

	/**
	 * Compute the labor demand by the firm. First it determine the total amount of workers required to produce
	 * the desiredOutput through the method getRequiredWorkers: if smaller than the number of current employees the firm 
	 * fires the last it had hired, otherwise it hires new workers. 
	 */
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
			this.setActive(false, StaticValues.MKT_LABOR);
			emplPop = new AgentList();
			for(MacroAgent ag : this.employees)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentWorkers-nbWorkers;i++){
				fireAgent((MacroAgent)emplPop.get(i));
			}
		}
		if (laborDemand>0){
			this.setActive(true, StaticValues.MKT_LABOR);
		}
		if(employees.size()>0){
			//Prepare the re-allocation of funds
			//1 Get the payable stock
			Item payableStock = this.getPayableStock(StaticValues.MKT_CONSGOOD);
			//2 Get the paying stocks
			List<Item> payingStocks = this.getPayingStocks(StaticValues.MKT_LABOR,payableStock);
			//3 Get the first occurrence of an item of the same sort than the payable stock within the paying stocks
			Item targetStock=null;
			for(Item item:payingStocks){
				if(item.getSMId()==payableStock.getSMId()){
					targetStock=item;
					break;
				}
			}
			// Calculate amount to be reallocated
			double reallocateAmount = 0;
			for(Item item:payingStocks){
				reallocateAmount += item.getValue();
			}
			// Reallocate
			reallocateLiquidity(reallocateAmount, payingStocks, targetStock);
			payWages(targetStock,StaticValues.MKT_LABOR);
		}
		cleanEmployeeList();
	}

	/**
	 * 
	 */
	protected void computeDebtPayments() {
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double[][] payments = new double[loans.size()][3];
		double toPay=0;
		double totInterests=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			if(loan.getAge()>0){
				double iRate=loan.getInterestRate();
				double amount=loan.getInitialAmount();
				int length = loan.getLength();
				double interests=iRate*loan.getValue();
				payments[i][0]=interests;
				double principal=0.0;
				switch(loan.getAmortization()){
				case Loan.FIXED_AMOUNT:
					double amortization = amount*(iRate*Math.pow(1+iRate, length))/(Math.pow(1+iRate, length)-1);
					principal=amortization-interests;
					break;
				case Loan.FIXED_CAPITAL:
					principal=amount/length;
					break;
				case Loan.ONLY_INTERESTS:
					if(length==loan.getAge())
						principal=amount;
					break;
				}
				if(loan.getAge()==loan.getLength()) { payments[i][1]=loan.getValue();
				}else {
				payments[i][1]=principal;	
				}
				payments[i][2]=loan.getValue();
				toPay+=principal+interests;
				totInterests +=interests;
				
			}else{
				payments[i][0]=0;
				payments[i][1]=0;
				payments[i][2]=loan.getValue();
			}
		}
		this.debtPayments = payments;
		this.debtBurden = toPay;
		this.debtInterests = totInterests;
	}
	
	/**
	 *
	 */
	protected void computeCreditDemand() {
		this.computeDebtPayments();
		FinanceStrategy strategy =(FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		double totalFinancialRequirement=0;
		int nbWorkers = this.getRequiredWorkers();
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		double expWages = expectation.getExpectation();
		double[][] bs = this.getNumericBalanceSheet();
		totalFinancialRequirement=(nbWorkers*expWages)+
				this.desiredRealCapitalDemand*((CapitalFirm)this.selectedCapitalGoodSuppliers.get(0)).getPrice()+
				this.debtBurden;
		this.creditDemanded=strategy.computeCreditDemand(totalFinancialRequirement);
		if(creditDemanded>0){
			this.setActive(true, StaticValues.MKT_CREDIT);
		}
	}

	/**
	 * 
	 */
	protected void computeDesiredInvestment(MacroAgent selectedCapitalGoodSupplier) {
		RealCapitalDemandStrategy strategy2 = (RealCapitalDemandStrategy) this.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND);
		this.desiredRealCapitalDemand=strategy2.computeRealCapitalDemand(selectedCapitalGoodSupplier);
		if(desiredRealCapitalDemand>0){
			this.setActive(true,StaticValues.MKT_CAPGOOD);
		}
	}
	
	

	/**
	 * Produces all output possible, given the level of employees, using first the most productive capital vintages. 
	 * First the firm sorts the capital stocks according to their productivity using a TreeMap. Then takes the most productive 
	 * one using pollLastEntry. If the number of workers required to use this stock of capital at full capacity is lower than
	 * the number of employees, all the capital is used to produce and the firm passes to the second most productive capital. 
	 * When instead the amount of labor required to use the capital at full capacity is lower than the number of employees not already 
	 * employed in the more productive capital goods, it uses only the amount compatible with the number of employees wtill availbale.
	 * 
	 */
	protected void produce() {
		double outputQty=0;
		double capacity=0;
		double capValue=0;
		if(this.employees.size()>0){
			List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
			TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
			double amortisationCosts=0;
			for (Item item:currentCapitalStock){
				CapitalGood capital=(CapitalGood)item;
				double prod = capital.getProductivity();
				capacity+=capital.getQuantity()*prod;
				capValue+=capital.getValue();
				amortisationCosts+=capital.getPrice()*capital.getQuantity()/capital.getCapitalAmortization();
				if(orderedCapital.containsKey(prod)){
					ArrayList<CapitalGood> list = orderedCapital.get(prod);
					list.add(capital);
				}else{
					ArrayList<CapitalGood> list = new ArrayList<CapitalGood>();
					list.add(capital);
					orderedCapital.put(prod, list);
				}

			}
			double residualWorkers=this.employees.size();
			for (Double key:orderedCapital.descendingKeySet()){
				for(CapitalGood capital:orderedCapital.get(key)){
					double employedWorkers = capital.getQuantity()/capital.getCapitalLaborRatio();
					if (employedWorkers<residualWorkers){
						outputQty+=capital.getProductivity()*capital.getQuantity();
						residualWorkers-=employedWorkers;
					}
					else{
						outputQty+=capital.getCapitalLaborRatio()*residualWorkers*capital.getProductivity();
						residualWorkers=0;
						//we are assuming that we can use a fraction of capital in the production process. Otherwise we 
						//have set outputQty+= Math.floor (capital.getCapitalLaborRatio()*residualWorkers)*capital.getProductivity();
					} 
				}
			}
			ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
			inventories.setQuantity(inventories.getQuantity()+outputQty);
			inventories.setUnitCost((amortisationCosts+this.getWageBill()+this.debtInterests)/outputQty);
		}
		else{
			List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
//			TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
			for (Item item:currentCapitalStock){
				CapitalGood capital=(CapitalGood)item;
				double prod = capital.getProductivity();
				capacity+=capital.getQuantity()*prod;
				capValue+=capital.getValue();
			}
		}
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		if (inventories.getQuantity()>0){
			this.setActive(true, StaticValues.MKT_CONSGOOD);
		}
		else{
			this.setActive(false, StaticValues.MKT_CONSGOOD);
		}
		
		this.addValue(StaticValues.LAG_PRODUCTION, outputQty);
		this.addValue(StaticValues.LAG_CAPACITY, capacity);
		this.addValue(StaticValues.LAG_CAPITALFINANCIALVALUE,capValue);
	}

	/**
	 * Manages the bankruptcy of the firm.
	 */
	protected void bankruptcy() {
		this.defaulted=true;
		BankruptcyStrategy strategy = (BankruptcyStrategy)this.getStrategy(StaticValues.STRATEGY_BANKRUPTCY);
		strategy.bankrupt();
		
		//Remove the agent from all events and set the agent as dead
		//this.unsubscribeFromEvents();
		//this.dead=true;
	}
	
	/**
	 * 
	 */
	public void payTaxes(Item account) {
		this.updatePreTaxProfits();
		TaxPayerStrategy strategy = (TaxPayerStrategy)this.getStrategy(StaticValues.STRATEGY_TAXES);
		double taxes=0;
		if (!this.defaulted){
		taxes=strategy.computeTaxes();
		}
		else{
			taxes=0;
		}
		// Prepare the re-allocation of funds
		// 1 Get the payable stock
		Item payableStock = this.getPayableStock(StaticValues.MKT_CONSGOOD);
		// 2 Get the paying stocks
		List<Item> payingStocks = this.getPayingStocks(StaticValues.MKT_LABOR, payableStock);
		// 3 Get the first occurrence of an item of the same sort than the payable stock
		// within the paying stocks
		Item targetStock = null;
		for (Item item : payingStocks) {
			if (item.getSMId() == payableStock.getSMId()) {
				targetStock = item;
				break;
			}
		}
		// Reallocate
		reallocateLiquidity(taxes, payingStocks, targetStock);
		updateAfterTaxProfits(taxes);
		if(Math.round(taxes)>Math.round(targetStock.getValue())){
			System.out.println("Default "+ this.getAgentId()+ " due to taxes");
			bankruptcy();
		    this.addValue(StaticValues.LAG_TAXES, 0);
		    }
		else{
			this.addValue(StaticValues.LAG_TAXES, taxes);
			LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
			payingSupplier.transfer(targetStock, account, taxes);
		}
	}
	
	/**
	 * @param taxes
	 */
	private void updateAfterTaxProfits(double taxes) {
		double profitsAfterTaxes= this.getPassedValue(StaticValues.LAG_PROFITPRETAX,0)- taxes;
		this.addValue(StaticValues.LAG_PROFITAFTERTAX, profitsAfterTaxes);
		double operatingNetCashFlow=0;
		List<Item> capStocks = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		double capitalAmortization = 0;
		for(Item c:capStocks){
			CapitalGood cap = (CapitalGood)c;
			if(cap.getAge()>=0)
				capitalAmortization+=cap.getQuantity()*cap.getPrice()/cap.getCapitalAmortization();
		}
		double inv=(double) this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 0);
		double inv1=(double)this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		double varInv=inv-inv1;
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double principal=0;
		for(int i=0;i<loans.size();i++){
			principal+=debtPayments[i][1];
		}
		operatingNetCashFlow=profitsAfterTaxes+capitalAmortization-varInv-principal;
		this.addValue(StaticValues.LAG_OPERATINGCASHFLOW,operatingNetCashFlow);
	}

	/**
	 * 
	 */
	protected void updatePreTaxProfits() {
		double lInv = this.getPassedValue(StaticValues.LAG_INVENTORIES, 1);
		double lNomInv=(double)this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
		double inv = inventories.getQuantity();
		double nomInv= inventories.getValue();
		this.addValue(StaticValues.LAG_INVENTORIES, inv);
		this.addValue(StaticValues.LAG_NOMINALINVENTORIES, nomInv);
		double production = this.getPassedValue(StaticValues.LAG_PRODUCTION, 0);
		double[] sales = new double[1];
		double[] rSales = new double[1];
		double realSales = (production-inv+lInv); 
		if (Double.isNaN(realSales)|| Double.isNaN(this.getPrice())){
			System.out.println("Error");
		}
		sales[0] = realSales*this.getPrice();
		rSales[0] = realSales;
		this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES).addObservation(sales);
		this.getExpectation(StaticValues.EXPECTATIONS_REALSALES).addObservation(rSales);
		this.addValue(StaticValues.LAG_REALSALES, realSales);
		this.addValue(StaticValues.LAG_NOMINALSALES, sales[0]);
		double wagebill = 0;
		for(MacroAgent employee:this.employees){
			wagebill+=((LaborSupplier)employee).getWage();
		}
		double[] avWage = new double[1];
		if(this.employees.size()>0)
			avWage[0]=wagebill/this.employees.size();
		else
			avWage[0]=this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();
		this.getExpectation(StaticValues.EXPECTATIONS_WAGES).addObservation(avWage);
		List<Item> capStocks = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		double capitalAmortization = 0;
		for(Item c:capStocks){
			CapitalGood cap = (CapitalGood)c;
			if(cap.getAge()>=0)	
				capitalAmortization+=cap.getQuantity()*cap.getPrice()/cap.getCapitalAmortization();
		}
		this.addValue(StaticValues.LAG_CAPITALAMORTIZATION,capitalAmortization);
		double profits = sales[0]-wagebill-this.debtInterests-capitalAmortization+this.interestReceived+this.reservesInterestsReceived+nomInv-lNomInv;
		this.addValue(StaticValues.LAG_PROFITPRETAX, profits);
	}

	/**
	 * Pays interests and part of capital, depending on the structure of the loan. If the amount of liquidity of the firm
	 * is not high enough to meet its interests and capital payments, it defaults. If the firm defaults, it liquid assets
	 * are distributed to creditors, proportionally to the amount of debt held by each creditor.
	 */
	protected void payInterests() {
		// First, we need to determine the total amount to be paid (interests+capital)
		this.computeDebtPayments();
		// Then determine the amount of liquid assets the firm has
		List<Item> deposits = this.getItemsStockMatrix(true, StaticValues.SM_DEP);
		// 1. If more than one deposit account (at max 2)
		Deposit deposit = (Deposit) deposits.get(0);

		List<Item> loans = this.getItemsStockMatrix(false, StaticValues.SM_LOAN);

		// Prepare the re-allocation of funds
		// 1 Get the payable stock
		Item payableStock = deposit;
		// 2 Get the paying stocks
		List<Item> payingStocks = this.getPayingStocks(StaticValues.MKT_LABOR, payableStock);
		// 3 Get the first occurrence of an item of the same sort than the payable stock
		// within the paying stocks
		Item targetStock = null;
		for (Item item : payingStocks) {
			if (item.getSMId() == payableStock.getSMId()) {
				targetStock = item;
				break;
			}
		}
		// Reallocate
		reallocateLiquidity(this.debtBurden, payingStocks, targetStock);

		// If enough liquidity, all is well
		if (Math.round(targetStock.getValue()) >= Math.round(this.debtBurden)) {
			for (int i = 0; i < loans.size(); i++) {
				Loan loan = (Loan) loans.get(i);
				double amountToPay = this.debtPayments[i][0] + this.debtPayments[i][1];
				deposit.setValue(deposit.getValue() - amountToPay);
				if (loan.getAssetHolder() != deposit.getLiabilityHolder()) {
					Item lBankRes = loan.getAssetHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
					lBankRes.setValue(lBankRes.getValue() + amountToPay);
					Item dBankRes = deposit.getLiabilityHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
					dBankRes.setValue(dBankRes.getValue() - amountToPay);
				}
				loan.setValue(loan.getValue() - this.debtPayments[i][1]);
				Bank bank = (Bank)loan.getAssetHolder();
				bank.setTotInterestsLoans(bank.getTotInterestsLoans()+this.debtPayments[i][0]);
			}
			// Else, the firm defaults
		} else {
			System.out.println("Default " + this.getAgentId() + " due to debt service");
			this.bankruptcy();
		}
	}
	
	
	/**
	 * Computes the price of the capital good using a strategy
	 */
	protected void computePrice() {
		
		if (this.getExpectation(StaticValues.EXPECTATIONS_WAGES) instanceof NoExpectation) {
			double avWage = 0;
			for (Agent a : employees) {
				Households hh = (Households) a;
				avWage += hh.getWage();
			}
			avWage /= employees.size();
			NoExpectation exp = (NoExpectation) this
					.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			if (Double.isNaN(avWage)) {

			} else {
				exp.setExpectation(avWage);
			}
		}
	
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		if(inventories==null)
			inventories=new ConsumptionGood(0, 0,this, this, 0,(int)Double.POSITIVE_INFINITY);
		PricingStrategy strategy = (PricingStrategy )this.getStrategy(StaticValues.STRATEGY_PRICING);
		inventories.setPrice(strategy.computePrice());
		this.addValue(StaticValues.LAG_PRICE, inventories.getPrice());
	}
	/**
	 * 
	 */
	protected void computeLiquidAssetsAmounts() {
		double liquidAssets=0;
		List<Item> deposits=this.getItemsStockMatrix(true, StaticValues.SM_DEP);
		List<Item> cash=this.getItemsStockMatrix(true, StaticValues.SM_CASH);
		List<Item> reserves=this.getItemsStockMatrix(true, StaticValues.SM_RESERVES);
		for (Item i: deposits){
			liquidAssets+=i.getValue();
		}
		for (Item i: cash){
			liquidAssets+=i.getValue();
		}
		for (Item i: reserves){
			liquidAssets+=i.getValue();
		}
		this.setDepositAmount(this.preferredDepositRatio*liquidAssets);
		this.setCashAmount(this.preferredCashRatio*liquidAssets);
		this.setReservesAmount(this.preferredReserveRatio*liquidAssets);
		this.setActive(true, StaticValues.MKT_DEPOSIT);
	}
	
	public void reallocateLiquidity(double amount, List<Item> payingStocks, Item targetStock){
		//The amount raised is equal to what is already on the target stock
		double amountRaised=targetStock.getValue();
		for(int i=0;i<payingStocks.size()&&amountRaised<amount;i++){
			//For each item in the list
			Item payingStock = payingStocks.get(i);
			//If the payingStock is not the target stock (otherwise, there's nothing to do).
			if(payingStock!=targetStock){
				//compute different amounts
				double thisAmount=payingStock.getValue();
				double remAmount=Math.max(0,thisAmount+amountRaised-amount);
				double transferAmount=thisAmount-remAmount;
				amountRaised+=transferAmount;
				//who is the supplier of the paying stock?
				LiabilitySupplier payingSupplier = (LiabilitySupplier) payingStock.getLiabilityHolder();
				// Do the transfer
				payingSupplier.transfer(payingStock, targetStock, transferAmount);
			}
		}
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodDemander#getPayingStocks(int, jmab.goods.Item)
	 */
	@Override
	public List<Item> getPayingStocks(int idMarket, Item payableStock) {
		switch(idMarket){
		case StaticValues.MKT_LABOR:
			List<Item> payingStocksLab=new ArrayList<Item>();
			payingStocksLab.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
			payingStocksLab.add(payingStocksLab.size()-1, this.getItemStockMatrix(true,StaticValues.SM_CASH));
			payingStocksLab.add(payingStocksLab.size()-1, this.getItemStockMatrix(true,StaticValues.SM_RESERVES));
			return payingStocksLab;
		case StaticValues.MKT_CAPGOOD:
			List<Item> payingStocksCap=new ArrayList<Item>();
			payingStocksCap.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
			payingStocksCap.add(payingStocksCap.size()-1, this.getItemStockMatrix(true,StaticValues.SM_CASH));
			payingStocksCap.add(payingStocksCap.size()-1, this.getItemStockMatrix(true,StaticValues.SM_RESERVES));
			return payingStocksCap;
		}
		return null;
	}

	/** 
	 * Consumption firms want to be paid in cash.
	 */
	@Override
	public Item getPayableStock(int idMarket) {
		switch(idMarket){
		case StaticValues.MKT_CONSGOOD:
			
		}
		// Determine most preferred payment method based on liquidity preferences
		double preferredPayableStock = Math.max(Math.max(preferredDepositRatio, preferredCashRatio),
				preferredReserveRatio);
		if (preferredPayableStock == preferredDepositRatio) {
			return this.getItemStockMatrix(true, StaticValues.SM_DEP);
		} else if (preferredPayableStock == preferredCashRatio) {
			return this.getItemStockMatrix(true, StaticValues.SM_CASH);
		} else if (preferredPayableStock == preferredReserveRatio) {
			return this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		} else
			return null;
	}

	/** (non-Javadoc)
	 *
	 */
	@Override
	public double getDepositAmount() {
		return this.depositAmount;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.DepositDemander#getCashAmount()
	 */
	
	@Override
	public double getCashAmount() {
		return this.cashAmount;
	}

	public double getPreferredDepositRatio() {
		return preferredDepositRatio;
	}

	public void setPreferredDepositRatio(double preferredDepositRatio) {
		this.preferredDepositRatio = preferredDepositRatio;
	}

	public double getPreferredCashRatio() {
		return preferredCashRatio;
	}

	public void setPreferredCashRatio(double preferredCashRatio) {
		this.preferredCashRatio = preferredCashRatio;
	}

	public double getPreferredReserveRatio() {
		return preferredReserveRatio;
	}

	public void setPreferredReserveRatio(double preferredReserveRatio) {
		this.preferredReserveRatio = preferredReserveRatio;
	}

	public double getReservesAmount() {
		return reservesAmount;
	}

	public void setReservesAmount(double reservesAmount) {
		this.reservesAmount = reservesAmount;
	}

	public void setCashAmount(double cashAmount) {
		this.cashAmount = cashAmount;
	}

	public void setDepositAmount(double depositAmount) {
		this.depositAmount = depositAmount;
	}
	

	/* (non-Javadoc)
	 * @see jmab.agents.CreditDemander#getLoanRequirement()
	 */
	@Override
	public double getLoanRequirement(int idLoanSM) {
		return this.creditDemanded;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.CreditDemander#setLoanRequirement(double)
	 */
	@Override
	public void setLoanRequirement(int idLoanSM, double amount) {
		this.creditDemanded=amount;
	
	}


	/**
	 * This method determines the real demand for capital good  
	 */
	@Override
	public double getDemand(int idGood) {
		return this.desiredRealCapitalDemand;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodDemander#setDemand(double)
	 */
	@Override
	public void setDemand(double d, int idGood) {
		this.desiredRealCapitalDemand=d;
	}

	/**
	 * Returns the price of the inventories to be sold. Remember that we assume that all that is produced is stored as inventories to be 
	 * sold within the same period.
	 */
	@Override
	public double getPrice(GoodDemander buyer, double demand) {
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		return inventories.getPrice();
	}

	

	/* (non-Javadoc)
	 * @see jmab.agents.PriceSetterWithTargets#getPrice()
	 * Needed to implement the PriceSetterWithTargets Interface which is necessary to use the 
	 * AdpativePriceOnAC pricing strategy.
	 */
	@Override
	public double getPrice() {
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		return inventories.getPrice();
	}

	/* (non-Javadoc)
	 * @see jmab.agents.PriceSetterWithTargets#getPriceLowerBound()
	 * Needed to implement the PriceSetterWithTargets Interface which is necessary to use the 
	 * AdpativePriceOnAC pricing strategy.
	 */
	@Override
	public double getPriceLowerBound() {
		
		// Get reference interest rate for loans (average interest rate of last period)
		
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		
		double inter=0;
		double n=(double) banks.getSize();
		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			if (bank.getNumericBalanceSheet()[0][StaticValues.SM_LOAN]!=0&&bank.getNetWealth()>0){
				inter+=bank.getPassedValue(StaticValues.LAG_LOANINTEREST, 1);
			}
			else{
				n-=1;
			}
			}
		
		double avInterest=inter/n;
		
		// Calc DebtRatio
		
		List<Item> loans = this.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double loansValue = 0;
		double totInterests = 0;
		for (int i = 0; i < loans.size(); i++) {
			Loan loan = (Loan) loans.get(i);
			if (loan.getAge() > 0) {
				double iRate = loan.getInterestRate();
				double interests = iRate * loan.getValue();
				totInterests += interests;
				loansValue+=loan.getValue();
			}
		}
		
		double debtRatio;
		
		if(loansValue>0&&this.getNetWealth()>0) {
			debtRatio = loansValue/(loansValue+this.getNetWealth());
		}else {
			debtRatio = 0;
		}

		// Get deposits as loan ratio
		
		double depositRatio = 1;
		
		if (this instanceof ConsumptionFirmWagesEnd) {
			depositRatio = ((ConsumptionFirmWagesEnd) this).getShareOfExpIncomeAsDeposit();
		}
		
		// Get capital information
		
		List<Item> capitalStock= this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		double amortisationCosts=0;
		double capitalAmount=0;
		double capacity=0;
		double capLaborRatio = 0;
		for (Item i: capitalStock){
			CapitalGood capital= (CapitalGood)i;
			amortisationCosts+=capital.getPrice()*capital.getQuantity()/capital.getCapitalAmortization();
			capitalAmount+=capital.getQuantity();
			double prod = capital.getProductivity();
			capacity+=capital.getQuantity()*prod;
			capLaborRatio = capital.getCapitalLaborRatio();
		}
		
		if(capitalAmount==0) {
			return 0;
		}else {
		
		
		InvestmentCapacityOperatingCashFlowExpected strategy1=(InvestmentCapacityOperatingCashFlowExpected) this.getStrategy(StaticValues.STRATEGY_INVESTMENT);
		
		double targetCapacityUtlization = strategy1.getTargetCapacityUtlization();
		
		double normalOutput = capacity*targetCapacityUtlization;
		
		double normalLaborCosts =this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*targetCapacityUtlization*(capitalAmount/capLaborRatio);
		
		// Calculate capital costs
		
		double normalCapitalCosts = (normalLaborCosts*depositRatio*avInterest+capitalAmount*avInterest)*debtRatio;
		
		// Calculate normal unit costs
		
		double normalUnitCosts = (normalLaborCosts+normalCapitalCosts+amortisationCosts)/normalOutput;
		
		
		return normalUnitCosts;
		}
		
	}

	/* (non-Javadoc)
	 * @see jmab.agents.PriceSetterWithTargets#getReferenceVariableForPrice()
	 * Needed to implement the PriceSetterWithTargets Interface which is necessary to use the 
	 * AdpativePriceOnAC pricing strategy.
	 */
	@Override
	public double getReferenceVariableForPrice() {
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		double pastSales=this.getPassedValue(StaticValues.LAG_REALSALES, 1);
		return inventories.getQuantity()/pastSales;
	}

	

	/* (non-Javadoc)
	 * @see jmab.agents.ProfitsTaxPayer#getProfits()
	 */
	@Override
	public double getPreTaxProfits() {
		if(!dead)
			return this.getPassedValue(StaticValues.LAG_PROFITPRETAX, 0);
		else
			return Double.NaN;
	}
	
	/* (non-Javadoc)
	 * @see jmab.agents.FinanceAgent#getReferenceVariableForFinance()
	 */
	@Override
	public double getReferenceVariableForFinance() {
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		double pastSales=this.getPassedValue(StaticValues.LAG_REALSALES, 1);
		return inventories.getQuantity()/pastSales;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.InvestmentAgent#getDesiredCapacityGrowth()
	 */
	@Override
	public double getDesiredCapacityGrowth() {
		return this.desiredCapacityGrowth;
	}
	
	/* (non-Javadoc)
	 * @see jmab.agents.DepositDemander#interestPaid(double)
	 */
	@Override
	public void interestPaid(double interests) {
		this.interestReceived=interests;
		
	}
	
	public double getInterestReceived(){
		return this.interestReceived;
	}

	/**
	 * @return the turnoverLabor
	 */
	public double getTurnoverLabor() {
		return turnoverLabor;
	}

	/**
	 * @param turnoverLabor the turnoverLabor to set
	 */
	public void setTurnoverLabor(double turnoverLabor) {
		this.turnoverLabor = turnoverLabor;
	}
	
	protected void payDividends(){
		if (!this.defaulted){
		DividendsStrategy strategy=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		strategy.payDividends();
		}
	}
	
	
	
	/**
	 * @return the expectedVariableCosts
	 */
	public double getExpectedVariableCosts() {
		return expectedVariableCosts;
	}

	/**
	 * @param expectedVariableCosts the expectedVariableCosts to set
	 */
	public void setExpectedVariableCosts(double expectedVariableCosts) {
		this.expectedVariableCosts = expectedVariableCosts;
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
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][sizeDebtPayments][debtPayments]
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
			ByteBuffer buf = ByteBuffer.allocate(72+3*debtPayments.length*8+this.selectedCapitalGoodSuppliers.size()*12);
			buf.putDouble(targetStock);
			buf.putDouble(creditDemanded);
			buf.putDouble(desiredCapacityGrowth);
			buf.putDouble(desiredRealCapitalDemand);
			buf.putDouble(debtBurden);
			buf.putDouble(debtInterests);
			buf.putDouble(interestReceived);
			buf.putDouble(turnoverLabor);			
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

	/**
	 * Populates the stockMatrix with the byte array content. The structure of the stock matrix is the following:
	 * [nbStockTypes]
	 * for each type of stocks
	 * 	[IdStock][nbItems]
	 * 		for each Item
	 * 			[itemSize][itemStructure]
	 * 		end for
	 * end for 	
	 */
	@Override
	public void populateStockMatrixBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		int nbStockTypes = buf.getInt();
		for(int i = 0 ; i < nbStockTypes ; i++){
			int stockId = buf.getInt();
			int nbStocks = buf.getInt();
			for(int j = 0 ; j < nbStocks ; j++){
				int itemSize = buf.getInt();
				byte[] itemData = new byte[itemSize];
				buf.get(itemData);
				Item it;
				switch(stockId){
				case StaticValues.SM_CAPGOOD:
					it = new CapitalGood(itemData, pop, this);
					break;
				case StaticValues.SM_CONSGOOD:
					it = new ConsumptionGood(itemData, pop, this);
					break;
				case StaticValues.SM_DEP:
					it = new Deposit(itemData, pop, this);
					MacroAgent depHolder = it.getLiabilityHolder();
					if(depHolder.getAgentId()!=this.agentId)
						depHolder.addItemStockMatrix(it, false, stockId);
					break;
				default:
					it = new Cash(itemData, pop, this);
					MacroAgent cashHolder = it.getLiabilityHolder();
					if(cashHolder.getAgentId()!=this.agentId)
						cashHolder.addItemStockMatrix(it, false, stockId);
					break;
				}
				this.addItemStockMatrix(it, true, stockId);
				
			}
		}	
		
	}

	@Override
	public void setLaborActive(boolean active) {
		this.setActive(active, StaticValues.MKT_LABOR);
		
	}
	
	@Override
	public void reservesInterestPaid(double interests) {
		// TODO Auto-generated method stub
		this.reservesInterestsReceived = interests;
	}
	
	public double getReservesInterestReceived(){
		return this.reservesInterestsReceived;
	}
	
}
