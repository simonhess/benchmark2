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
import java.util.List;

import benchmark.StaticValues;
import benchmark.expectations.NoExpectation;
import jmab.agents.AbstractFirm;
import jmab.agents.CreditDemander;
import jmab.agents.DepositDemander;
import jmab.agents.FinanceAgent;
import jmab.agents.GoodDemander;
import jmab.agents.GoodSupplier;
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
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.BankruptcyStrategy;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.FinanceStrategy;
import jmab.strategies.PricingStrategy;
import jmab.strategies.ProductionStrategy;
import jmab.strategies.RandDInvestment;
import jmab.strategies.RandDOutcome;
import jmab.strategies.SelectDepositSupplierStrategy;
import jmab.strategies.SelectLenderStrategy;
import jmab.strategies.SelectWorkerStrategy;
import jmab.strategies.TaxPayerStrategy;
import jmab.strategies.UpdateInventoriesProductivityStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;

/**
 * Class representing Capital Producers 
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class CapitalFirm extends AbstractFirm implements GoodSupplier,
		CreditDemander, LaborDemander, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent {
	
	protected int payableStockId;
	protected double targetStock;
	protected double amountResearch;
	protected double creditDemanded;
	protected double laborProductivity;
	protected double capitalProductivity;
	protected double capitalLaborRatio;
	protected int capitalDuration;
	protected int capitalAmortization;
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
	
	
	/**
	 * @return the capitalLaborRatio
	 */
	public double getCapitalLaborRatio() {
		return capitalLaborRatio;
	}

	/**
	 * @param capitalLaborRatio the capitalLaborRatio to set
	 */
	public void setCapitalLaborRatio(double capitalLaborRatio) {
		this.capitalLaborRatio = capitalLaborRatio;
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

	/**
	 * @return the payableStockId
	 */
	public int getPayableStockId() {
		return payableStockId;
	}

	/**
	 * @param payableStockId the payableStockId to set
	 */
	public void setPayableStockId(int payableStockId) {
		this.payableStockId = payableStockId;
	}

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

	/* (non-Javadoc)
	 * @see jmab.agents.CreditDemander#getLoanRequirement()
	 */
	@Override
	public double getLoanRequirement(int idLoanSM) {
		return this.creditDemanded;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodSupplier#getPrice(jmab.agents.GoodDemander, double)
	 */
	@Override
	public double getPrice(GoodDemander agent, double demand) {
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
		return inventories.getPrice();
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodSupplier#getPayableStock(int)
	 */
	@Override
	public Item getPayableStock(int idGood) {
		switch(idGood){
		case StaticValues.MKT_CAPGOOD:
			
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
	
	/* (non-Javadoc)
	 * @see jmab.agents.SimpleAbstractAgent#onTicArrived(AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {

		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			this.defaulted=false;
			computeExpectations();
			determineOutput();
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
		case StaticValues.TIC_CREDINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_DEPOSITDEMAND:
			computeLiquidAssetsAmounts();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			updateExpectations();
		}


	}

	/**
	 * Updates expectations
	 */
	protected void updateExpectations() {
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
		double nW=this.getNetWealth();
		this.addValue(StaticValues.LAG_NETWEALTH,nW);
		inventories.setAge(-1);
		this.cleanSM();
		if(this.defaulted==true) {
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
	 * TODO
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
			bankruptcy();
			this.defaulted = true;
		}
	}

	
	
	/**
	 * Determines the increase in productivity due to R&D activities. Still TODO since we need to determine the 
	 * amount spent from the quantity of workers hired to do R&D. For now there are no distinction between output
	 * workers and R&D workers, thus there are no direct costs for R&D as any worker (and thus its wage) could work
	 * in R&D. Two simple solutions: pick randomly (or not) the workers and sum their wages or multiply the number of workers
	 * by average wage. 
	 * The other radical solution is to have two labor markets   
	 */
	protected void researchOutcome() {
		if(this.employees.size()>0){
			double expWages = this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();
			int researchLaborDemand=(int)Math.floor(this.amountResearch/expWages);
			int actualResearchLabor=0;
			if(this.laborDemand>0){//Constrained case
				int outputLaborDemand=this.getRequiredWorkers();
				int totalLaborDesired=outputLaborDemand+researchLaborDemand;
				int actualLabor=this.employees.size();
				int actualOutputLabor=Math.round(outputLaborDemand*actualLabor/totalLaborDesired);
				actualResearchLabor=actualLabor-actualOutputLabor;
			}else{			
				actualResearchLabor=(int)Math.floor(this.amountResearch/expWages);
			}
			RandDOutcome strategy= (RandDOutcome) this.getStrategy(StaticValues.STRATEGY_RANDDEVELOPMENTOUTCOME);
			double productivityGain= strategy.computeRandDOutcome(actualResearchLabor);
			this.setCapitalProductivity(this.getCapitalProductivity()+productivityGain);
			updateInventoriesAfterInnovation(this.capitalProductivity);
		}
	}

	/**
	 * 
	 */
	protected void updateInventoriesAfterInnovation(double capitalProductivity) {
		UpdateInventoriesProductivityStrategy strategy= (UpdateInventoriesProductivityStrategy)this.getStrategy(StaticValues.STRATEGY_UPDATEINVPRODUCTIVITY);
		strategy.updateInventoriesProductivity(capitalProductivity);
	}

	/**
	 * Produces all output possible, given the level of employees. If the quantity of hired employee is lower than 
	 * the quantity of desired employee, then constraints apply both for production and R&D, proportionally to their
	 * demand for labor.
	 */
	protected void produce() {
		double outputQty=0;
		if(this.employees.size()>0){
			int outputLaborDemand=this.getRequiredWorkers();
			if(this.laborDemand>0){//Constrained case
				Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
				double expWages = expectation.getExpectation();
				int researchDemandLabor=(int)Math.floor(this.amountResearch/expWages);
				int totalDemandLabor=outputLaborDemand+researchDemandLabor;
				int ActualLabor=this.employees.size();
				int ActualOutputLabor=Math.round(outputLaborDemand/totalDemandLabor*ActualLabor);//labor dedicated to production is 
				//actual total workers hired multiplied by the share of desired workers for production purpose over total desired workers
				outputQty=ActualOutputLabor*this.laborProductivity;
			}else{			
				outputQty=outputLaborDemand*this.laborProductivity;
			}
			CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
			inventories.setQuantity(inventories.getQuantity()+outputQty);
			inventories.setUnitCost((this.getWageBill()+this.debtInterests)/outputQty);
		}
		this.addValue(StaticValues.LAG_PRODUCTION, outputQty);
	}

	/**
	 * Computes labor demand. In this case, labor demand is equal to the quantity of workers needed to produced
	 * desired output plus quantity of workers corresponding the desired level of investment in R&D.
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
		
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		double expWages = expectation.getExpectation();
		int nbWorkers = this.getRequiredWorkers()+(int)Math.floor(this.amountResearch/expWages);
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
		if(this.laborDemand>0)
			this.setActive(true, StaticValues.MKT_LABOR);
		cleanEmployeeList();
		if(employees.size()>0){
			//Prepare the re-allocation of funds
			//1 Get the payable stock
			Item payableStock = this.getPayableStock(StaticValues.MKT_CAPGOOD);
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
	 * Computes the amount of credit needed to finance production and R&D
	 */
	protected void computeCreditDemand() {
		computeDebtPayments();
		FinanceStrategy strategy = (FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		int nbWorkers = this.getRequiredWorkers();
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		double expWages = expectation.getExpectation();
		double[][] bs = this.getNumericBalanceSheet();
		double expectedFinancialRequirement=(nbWorkers+Math.floor(this.amountResearch/expWages))*expWages +
				this.debtBurden;
		this.creditDemanded = strategy.computeCreditDemand(expectedFinancialRequirement);
		if(creditDemanded>0)
			this.setActive(true, StaticValues.MKT_CREDIT);
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
				payments[i][1]=principal;
				payments[i][2]=loan.getValue();
				toPay+=principal+interests;
				totInterests+=interests;
			}else{
				payments[i][0]=0;
				payments[i][1]=0;
				payments[i][2]=loan.getValue();
			}
		}
		this.debtPayments=payments;
		this.debtBurden = toPay;
		this.debtInterests = totInterests;
	}

	/**
	 * @return the number of wrokers needed to produce desired output
	 */
	protected int getRequiredWorkers() {
		return (int) Math.round(this.desiredOutput/this.laborProductivity) ;
	}

	/**
	 * Computes the amount of money to be invested in R&D as a share of past sales.
	 */
	protected void researchDecision() {
		RandDInvestment strategy=(RandDInvestment) this.getStrategy(StaticValues.STRATEGY_RANDDEVELOPMENTINVESTMENT);
		this.amountResearch=strategy.computeRandDInvestment();
	}

	/**
	 * Determines output using a strategy
	 */
	protected void determineOutput() {
		ProductionStrategy strategy = (ProductionStrategy)this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		this.setDesiredOutput(strategy.computeOutput());
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
		if (inventories.getQuantity()+Math.floor(this.desiredOutput)>0){
			this.setActive(true, StaticValues.MKT_CAPGOOD);
		}
		else{
			this.setActive(false, StaticValues.MKT_CAPGOOD);
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
			NoExpectation exp = (NoExpectation) this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			if (Double.isNaN(avWage)) {

			} else {
				exp.setExpectation(avWage);
			}
		}
		
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
		if(inventories==null)
			inventories=new CapitalGood(0, 0, this,this, 0,this.capitalProductivity,(int)Double.POSITIVE_INFINITY, 
					this.capitalAmortization, this.capitalLaborRatio);
		PricingStrategy strategy = (PricingStrategy )this.getStrategy(StaticValues.STRATEGY_PRICING);
		inventories.setPrice(strategy.computePrice());
		this.addValue(StaticValues.LAG_PRICE, inventories.getPrice());
		
	}

	/* (non-Javadoc)
	 * @see jmab.agents.LaborDemander#getPayingStocks(int, jmab.goods.Item)
	 */
	@Override
	public List<Item> getPayingStocks(int idMarket, Item payableStock) {
		switch(idMarket){
		case StaticValues.MKT_LABOR:
			List<Item> payingStocksLab= new ArrayList<Item>();
			payingStocksLab.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
			payingStocksLab.add(payingStocksLab.size()-1, this.getItemStockMatrix(true,StaticValues.SM_CASH));
			payingStocksLab.add(payingStocksLab.size()-1, this.getItemStockMatrix(true,StaticValues.SM_RESERVES));
			return payingStocksLab;
		}
		return null;
	}

	

	/* (non-Javadoc)
	 * @see jmab.agents.TaxPayer#payTaxes(jmab.goods.Item)
	 */
	@Override
	public void payTaxes(Item account) {
		updatePreTaxProfits();
		TaxPayerStrategy strategy = (TaxPayerStrategy) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double taxes=0;
		if (!this.defaulted){
			taxes=strategy.computeTaxes(); 
		}
		else{
			taxes=0;
		}
		//Prepare the re-allocation of funds
		//1 Get the payable stock
		Item payableStock = this.getPayableStock(StaticValues.MKT_CAPGOOD);
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
		// Reallocate
		reallocateLiquidity(taxes, payingStocks, targetStock);
		updateAfterTaxProfits(taxes);
		if(Math.round(taxes)>Math.round(targetStock.getValue())){
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
		double varInv=this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 0)-this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double principal=0;
		for(int i=0;i<loans.size();i++){
			principal+=debtPayments[i][1];
		}
		operatingNetCashFlow=profitsAfterTaxes-varInv-principal;
		this.addValue(StaticValues.LAG_OPERATINGCASHFLOW,operatingNetCashFlow);
	}

	/**
	 * 
	 */
	protected void updatePreTaxProfits() {
		int lInv = (int)this.getPassedValue(StaticValues.LAG_INVENTORIES, 1);
		double lNomInv= this.getPassedValue(StaticValues.LAG_NOMINALINVENTORIES, 1);
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
		int inv = (int)inventories.getQuantity();
		double nomInv= inventories.getValue();
		this.addValue(StaticValues.LAG_INVENTORIES, inv);
		this.addValue(StaticValues.LAG_NOMINALINVENTORIES, nomInv);
		int production = (int)this.getPassedValue(StaticValues.LAG_PRODUCTION, 0);
		double[] sales = new double[1];
		double[] rSales = new double[1];
		double realSales = (production-inv+lInv); 
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
		double profits = sales[0]-wagebill-this.debtInterests+this.interestReceived+this.reservesInterestsReceived+nomInv-lNomInv;
		this.addValue(StaticValues.LAG_PROFITPRETAX, profits);
		
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

	/**
	 * @return the laborProductivity
	 */
	public double getLaborProductivity() {
		return laborProductivity;
	}

	/**
	 * @param laborProductivity the laborProductivity to set
	 */
	public void setLaborProductivity(double laborProductivity) {
		this.laborProductivity = laborProductivity;
	}

	/**
	 * @return the productivity
	 */
	public double getCapitalProductivity() {
		return capitalProductivity;
	}

	/**
	 * @param productivity the productivity to set
	 */
	public void setCapitalProductivity(double productivity) {
		this.capitalProductivity = productivity;
	}

	/**
	 * @return the capitalDuration
	 */
	public int getCapitalDuration() {
		return capitalDuration;
	}

	/**
	 * @param capitalDuration the capitalDuration to set
	 */
	public void setCapitalDuration(int capitalDuration) {
		this.capitalDuration = capitalDuration;
	}

	/**
	 * @return the capitalAmortization
	 */
	public int getCapitalAmortization() {
		return capitalAmortization;
	}

	/**
	 * @param capitalAmortization the capitalAmortization to set
	 */
	public void setCapitalAmortization(int capitalAmortization) {
		this.capitalAmortization = capitalAmortization;
	}
	
	protected void payDividends(){
		if (!this.defaulted){
		DividendsStrategy strategy=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		strategy.payDividends();
		}
	}
	/* (non-Javadoc)
	 * @see jmab.agents.CreditDemander#setLoanRequirement(double)
	 */
	@Override
	public void setLoanRequirement(int idLoanSMm, double amount) {
		this.creditDemanded=amount;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.PriceSetterWithTargets#getPrice()
	 * Needed to implement the PriceSetterWithTargets Interface which is necessary to use the 
	 * AdpativePriceOnAC pricing strategy.
	 */
	@Override
	public double getPrice() {
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
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
				
		double normalUnitCosts = 0;
		
		
		double expectedVariableCosts = this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()
				/ this.getLaborProductivity();
		double expectedAverageCosts = 0;
		double expectedFixedCosts = 0;
		// Calculate funding costs/ fixed costs
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
		
		// Get deposits as loan ratio

		double depositRatio = 1;

		if (this instanceof CapitalFirmWagesEnd) {
			depositRatio = ((CapitalFirmWagesEnd) this).getShareOfExpIncomeAsDeposit();
		}
		
		double debtRatio;
		
		if(loansValue>0&&this.getNetWealth()>0) {
			debtRatio = loansValue/(loansValue+this.getNetWealth());
		}else {
			debtRatio = 0;
		}

		if (this.getRequiredWorkers() > 0) {
			
			// Calculate capital costs
			
			double normalCapitalCosts = (this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*this.getRequiredWorkers()*depositRatio*avInterest)*debtRatio;
			
			// Calculate normal unit costs
			
			normalUnitCosts = (this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*this.getRequiredWorkers()+normalCapitalCosts)/ this.getDesiredOutput();
			
			return normalUnitCosts;

		} else {
			CapitalGood inventoriesLeft = (CapitalGood) this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD);
			if (inventoriesLeft.getQuantity() == 0) {
				return 0;
			} else {
				double residualOutput = inventoriesLeft.getQuantity();
				double requiredWorkers = residualOutput/ this.getLaborProductivity();
				
				// Calculate capital costs
				
				double normalCapitalCosts = (this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*requiredWorkers*depositRatio*avInterest)*debtRatio;
				
				// Calculate normal unit costs
				
				normalUnitCosts = (this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*requiredWorkers+normalCapitalCosts)/ residualOutput;
				
				return normalUnitCosts;
			}

		}
	}

	/* (non-Javadoc)
	 * @see jmab.agents.PriceSetterWithTargets#getReferenceVariableForPrice()
	 * Needed to implement the PriceSetterWithTargets Interface which is necessary to use strategies such as the 
	 * AdpativePriceOnAC.
	 */
	@Override
	public double getReferenceVariableForPrice() {
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
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
	 * Needed to implement the FinanceAgent Interface which is necessary to use the 
	 * strategies such as DynamicTradeOffFiance.
	 */
	@Override
	public double getReferenceVariableForFinance() {
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
		double pastSales=this.getPassedValue(StaticValues.LAG_REALSALES, 1);
		return inventories.getQuantity()/pastSales;
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
	 * [sizeMacroAgentStructure][MacroAgentStructure][targetStock][amountResearch][creditdDemanded][capitalProductivity][capitalLaborRatio]
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][sizeDebtPayments][debtPayments][payableStockId][laborProductvity]
	 * [capitalDuration][capitalAmortization][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
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
	 * [debtBurden][debtInterests][interestReceived][turnoverLabor][sizeDebtPayments][debtPayments][payableStockId][laborProductvity]
	 * [capitalDuration][capitalAmortization][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate(3*debtPayments.length*8+92);
			buf.putDouble(targetStock);//8
			buf.putDouble(amountResearch);//16
			buf.putDouble(creditDemanded);//24
			buf.putDouble(capitalProductivity);//32
			buf.putDouble(capitalLaborRatio);//40
			buf.putDouble(debtBurden);//48
			buf.putDouble(debtInterests);//56
			buf.putDouble(interestReceived);//64
			buf.putDouble(turnoverLabor);//72	
			buf.putInt(debtPayments.length);//76
			for(int i = 0 ; i < debtPayments.length ; i++){
				buf.putDouble(debtPayments[i][0]);
				buf.putDouble(debtPayments[i][1]);
				buf.putDouble(debtPayments[i][2]);
			}//3*debtPayments.length*8
			buf.putInt(payableStockId);//80
			buf.putDouble(laborProductivity);//84
			buf.putInt(capitalDuration);//88
			buf.putInt(capitalAmortization);//92
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
