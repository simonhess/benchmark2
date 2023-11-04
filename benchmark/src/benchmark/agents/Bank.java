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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jmab2.agents.AbstractBank;
import jmab2.agents.BaselIIIAgent;
import jmab2.agents.BondDemander;
import jmab2.agents.BondSupplier;
import jmab2.agents.CreditDemander;
import jmab2.agents.CreditSupplier;
import jmab2.agents.DepositDemander;
import jmab2.agents.DepositSupplier;
import jmab2.agents.InterestRateSetterWithTargets;
import jmab2.agents.MacroAgent;
import jmab2.agents.ProfitsTaxPayer;
import jmab2.events.MacroTicEvent;
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.simulations.TwoStepMarketSimulation;
import jmab2.stockmatrix.Bond;
import jmab2.stockmatrix.Cash;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.InterestBearingItem;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.BankruptcyStrategy;
import jmab2.strategies.BondDemandStrategy;
import jmab2.strategies.DividendsStrategy;
import jmab2.strategies.FinanceStrategy;
import jmab2.strategies.InterestRateStrategy;
import jmab2.strategies.SelectLenderStrategy;
import jmab2.strategies.SpecificCreditSupplyStrategy;
import jmab2.strategies.SupplyCreditStrategy;
import jmab2.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;
import benchmark.StaticValues;
import benchmark.strategies.PriciestBorrower;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class Bank extends AbstractBank implements CreditSupplier, CreditDemander,
		DepositSupplier, ProfitsTaxPayer, BondDemander, InterestRateSetterWithTargets, BaselIIIAgent, DepositDemander {
	public double transferSum = 0;
	private double reserveInterestRate;
	private double advancesInterestRate;
	private double bankInterestRate;
	private double depositInterestRate;
	private double totalLoanSupply;
	private double advancesDemand;
	private double fundingRate;
	private int advancesLength;
	private int advancesAmortizationType;
	private double bondPrice;
	private long bondDemand;
	private BondSupplier selectedBondSupplier;
	private double bondInterestRate;
	private double riskAversionC;
	private double riskAversionK;
	private double capitalRatio;
	private double CapitalAdequacyRatio;
	private double liquidityRatio;
	private double netLiquidityRatio;
	protected double advancesInterests;
	protected double reservesInterests;
	protected double bondInterestReceived;
	protected double totInterestsLoans;
	protected double totInterestsDeposits;
	private double dividends;
	private double bailoutCost;
	//private double preTaxProfits;
	//private double profitsAfterTax;
	protected double interestsReceived;
	protected double totInterestsInterbank;
	private double interbankAsk;
	private double interbankDemand;
	private double interbankSupply;
	protected double debtBurden;
	private double targetedLiquidityRatio;
	private double targetedCapitalAdequacyRatio;
	private double riskAversionMarkUp;
	private double interBankRiskPremium;
	private double DISReserveRatio;
	
	private double loansRiskWeight;
	private double interbankLoansRiskWeight;

	/* (non-Javadoc)
	 * @see jmab2.agents.MacroAgent#onRoundFinished(net.sourceforge.jabm.event.RoundFinishedEvent)
	 */
	@Override
	public void onRoundFinished(RoundFinishedEvent event) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see jmab2.agents.MacroAgent#initialiseCounterpart(net.sourceforge.jabm.agent.Agent, int)
	 */
	@Override
	public void initialiseCounterpart(Agent counterpart, int marketID) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see jmab2.agents.TaxPayer#payTaxes(jmab.goods.Item)
	 */
	@Override
	public void payTaxes(Item account) {
		double nW=this.getNetWealth();
		double profitsPreTax = nW-this.getPassedValue(StaticValues.LAG_NETWEALTH, 1);
		this.addValue(StaticValues.LAG_PROFITPRETAX,profitsPreTax);
		TaxPayerStrategy strategy = (TaxPayerStrategy) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double taxes=0;
		if (!this.defaulted){
		taxes=strategy.computeTaxes(); 
		}
		else{
			taxes=0;
		}
		this.addValue(StaticValues.LAG_TAXES, taxes);
		Item res = this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		res.setValue(res.getValue()-taxes);
		account.setValue(account.getValue()+taxes);
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditSupplier#getInterestRate(jmab2.agents.MacroAgent, double, int)
	 */
	@Override
	public double getInterestRate(int idLoanSM, MacroAgent creditDemander, double amount,
			int length) {
		//InterestRateStrategy strategy = (InterestRateStrategy)this.getStrategy(StaticValues.STRATEGY_LOANAGENTINTERESTRATE);
		//double agentIR=strategy.computeInterestRate(creditDemander,amount,length);
		//return Math.max(this.bankInterestRate+agentIR, this.bondInterestRate);
		switch(idLoanSM){
		case StaticValues.SM_LOAN:
			return this.bankInterestRate;
		case StaticValues.SM_INTERBANK:
			return this.interbankAsk;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditSupplier#getLoanSupply(jmab2.agents.MacroAgent, double)
	 */
	@Override
	public double getLoanSupply(int loansId, MacroAgent creditDemander, double required) {
		switch(loansId){
		case StaticValues.SM_LOAN:
			SpecificCreditSupplyStrategy strategy=(SpecificCreditSupplyStrategy) this.getStrategy(StaticValues.STRATEGY_SPECIFICCREDITSUPPLY);
			double specificLoanSupply=strategy.computeSpecificSupply(creditDemander, required);
			return specificLoanSupply;
		case StaticValues.SM_INTERBANK:
			return this.interbankSupply;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditSupplier#getDepositInterestRate(jmab2.agents.MacroAgent, double)
	 */
	@Override
	public double getDepositInterestRate(MacroAgent creditDemander,
			double amount) {
		return this.depositInterestRate;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.SimpleAbstractAgent#onTicArrived(jmab2.events.AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			setBailoutCost(0);
			setDebtBurden(0);
			// TODO set the interest rates here?
			setCurrentNonPerformingLoans(StaticValues.SM_LOAN,0); // we delete non performing loans from previous period
			this.totInterestsLoans=0;
			this.defaulted=false;
			computeExpectations();
			double depositsValue=0;
			for(Item i:this.getItemsStockMatrix(false, StaticValues.SM_DEP)){
				depositsValue+=i.getValue();
				}
			double reservesValue=0;
			for(Item i:this.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
				reservesValue+=i.getValue();
				}
			if (depositsValue==0){
				this.liquidityRatio=0;
				this.netLiquidityRatio=0;
			}
			else{
			this.liquidityRatio=reservesValue/depositsValue;
			this.netLiquidityRatio=this.getNetLiquidity()/depositsValue;
			}
			double outstandingLoans=0;
			for (Item i:this.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				outstandingLoans+=i.getValue();
			}
			double outstandingInterbankLoans=0;
			for (Item i:this.getItemsStockMatrix(true, StaticValues.SM_INTERBANK)){
				outstandingInterbankLoans+=i.getValue();
			}
			//ALE HAI AGGIUNTO QUESTO IL 24/1/2015
			if (Math.floor(outstandingLoans)==0){
				this.capitalRatio=0;
				this.CapitalAdequacyRatio=0;
			}
			else {
				this.capitalRatio=this.getPassedValue(StaticValues.LAG_NETWEALTH, 1)/outstandingLoans;
				this.CapitalAdequacyRatio=this.getPassedValue(StaticValues.LAG_NETWEALTH, 1)/(outstandingLoans*this.getLoansRiskWeight()+outstandingInterbankLoans*this.getInterbankLoansRiskWeight());
				BigDecimal bd = BigDecimal.valueOf(this.CapitalAdequacyRatio);
			    bd = bd.setScale(4, RoundingMode.HALF_UP);
			    this.CapitalAdequacyRatio = bd.doubleValue();
			}
			break;
		case StaticValues.TIC_BANKSUPDATECBINTERESTRATES:
			this.updateCentralBankInterestRates();
			break;
		case StaticValues.TIC_BANKSUPDATEIBINTERESTRATE:
			determineInterbankAsk();
			break;
		case StaticValues.TIC_DEPINTERESTS:
			payDepositInterests();
			break;
		case StaticValues.TIC_CREDITSUPPLY:
			determineBankGenericInterestRate();
			determineCreditSupply();
			break;
		case StaticValues.TIC_DEPOSITSUPPLY:
			determineDepositInterestRate();
			break;
		case StaticValues.TIC_ADVINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_BANKRUPTCY:
			determineBankruptcy();
			break;
		case StaticValues.TIC_BANKRUPTCY2:
			conductBankruptcy();
			break;
		case StaticValues.TIC_BONDDEMAND:
			determineBondDemand();
			break;
		case StaticValues.TIC_DIVIDENDS:
			payDividends();
			break;
		case StaticValues.TIC_RESERVEDEMANDBOND:
			determineAdvancesDemandBond();
			break;
		case StaticValues.TIC_RESERVEDEMANDBASEL:
			determineAdvancesDemandBasel();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS: 
			updateExpectations();
			break;
			// added interbank operations:
		case StaticValues.TIC_INTERBANKINTERESTS:
			payInterbankInterests();
			break;
		case StaticValues.TIC_INTERBANKDEMANDSUPPLY:
			determineInterbankSupplyOrDemand();
			break;
		}

	}

	/**
	 * Method used to update the advances and reserves interest rates
	 */
	private void updateCentralBankInterestRates() {
		// get CB from controller?
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population pop = macroPop.getPopulation(StaticValues.CB_ID);
		CentralBank CB= (CentralBank)pop.getAgentList().get(0);
		this.setReserveInterestRate(CB.getReserveInterestRate());
		this.setAdvancesInterestRate(CB.getAdvancesInterestRate());
	}

	// (5) New interbank interest + principal interbank payment function
	private void payInterbankInterests() {
		// the list loans loads from the stock matrix
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_INTERBANK);
		
		if(loans.size()>0) {
			
		// reset total interest to 0
		this.totInterestsInterbank = 0;
		// define variable amountToPay here because it is used in the loop and after
		double amountToPay = 0;
		double[][] amounts = new double[2][loans.size()];
		// loop over the loans in the loan list
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			if(loan.getAge()>0){
				double iRate=loan.getInterestRate();
				double amount=loan.getInitialAmount();
				int length = loan.getLength();
				double interests=iRate*loan.getValue();
				amounts[0][i]=interests;
				double principal=0.0;
				switch(loan.getAmortization()){
				// determine the loan repayment amount 
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
				amounts[1][i]=principal;
				this.totInterestsInterbank+=interests;
				// use previous to calculate total amount to pay
				amountToPay=interests;
				debtBurden=amountToPay;
			}
		}	
		// end of loop --> total payment amount is now calculated
		// Check for solvency using this information
		boolean iCanPay = true;
		double netWealthAfterBill = this.getNetWealth()- amountToPay;
		if(netWealthAfterBill<0)
			iCanPay=false;
		//If the bank remains solvent, the payments are made.
		if(iCanPay){
			for(int i=0;i<loans.size();i++){
				Loan loan=(Loan)loans.get(i);
				amountToPay=amounts[0][i]+amounts[1][i];
				// look up my reserves
				Item myRes = this.getItemStockMatrix(true,StaticValues.SM_RESERVES);
				myRes.setValue(myRes.getValue()-amountToPay);
				Item oBankRes = loan.getAssetHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
				oBankRes.setValue(oBankRes.getValue()+amountToPay);
				loan.setValue(loan.getValue()-amounts[1][i]);
			}
		}
		//Else, the firm defaults
		else{
			System.out.println("Insolvency " + this.getAgentId() +" due to interbankdebt service");
			BankruptcyStrategy strategy = (BankruptcyStrategy)this.getStrategy(StaticValues.STRATEGY_BANKRUPTCY);
			this.defaulted=true;
		}
		}
	}
	
	/**
	 * Function to determine how many reserves the bank either demands or supplies
	 * on the interbank market based on the supply or demand strategy
	 */
	private void determineInterbankSupplyOrDemand() {
		// compute creditsupplyOrDemand
		SupplyCreditStrategy strategy=(SupplyCreditStrategy)this.getStrategy(StaticValues.STRATEGY_INTERBANKSUPPLY);
		double interbankSupplyDemand = strategy.computeCreditSupply();
		// if number is positive set this as interbank supply
		if (interbankSupplyDemand>0) {
			setInterbankSupply(interbankSupplyDemand);
			setInterbankDemand(0);
			this.addValue(StaticValues.LAG_TOTINTERBANKSUPPLY, interbankSupplyDemand);
			this.addValue(StaticValues.LAG_TOTINTERBANKDEMAND, 0);
			this.setActive(true, StaticValues.MKT_INTERBANK);
			this.addToMarketPopulation(StaticValues.MKT_INTERBANK, false);
		}
		// if number is negative set this as interbank demand
		else if (interbankSupplyDemand<0) {
			setInterbankSupply(0);
			setInterbankDemand(-interbankSupplyDemand);
			this.addValue(StaticValues.LAG_TOTINTERBANKSUPPLY, 0);
			this.addValue(StaticValues.LAG_TOTINTERBANKDEMAND, -interbankSupplyDemand);
			this.setActive(true, StaticValues.MKT_INTERBANK);
			this.addToMarketPopulation(StaticValues.MKT_INTERBANK, true);
		}
		else if (interbankSupplyDemand==0) {
			setInterbankSupply(0);
			setInterbankDemand(0);
			this.addValue(StaticValues.LAG_TOTINTERBANKSUPPLY, 0);
			this.addValue(StaticValues.LAG_TOTINTERBANKDEMAND, 0);
		}
	}
	
	// getter and setter for determineInterbankSupply
	public double getInterbankSupply() {
		return this.interbankSupply;
	}
	public void setInterbankSupply(double d) {
		this.interbankSupply=d;
	}
	
	// New interbank ask rate strategy methods 
		/**
		 * Determines the bank interbank ask interest rate, i.e. 
		 * using the ask rate strategy. 
		 * Borrowed from generic interest rate, right approach? 
		 */
		private void determineInterbankAsk() {
			InterestRateStrategy strategy = (InterestRateStrategy)this.getStrategy(StaticValues.STRATEGY_INTERBANKRATE);
			this.interbankAsk =strategy.computeInterestRate(null,0,1);
		}
	
	protected void payDividends(){
		double nW=this.getNetWealth();
		double profitsAfterTax = nW-this.getPassedValue(StaticValues.LAG_NETWEALTH, 1);
		this.addValue(StaticValues.LAG_PROFITAFTERTAX,profitsAfterTax);
		double[] afterTaxProfit = new double[1];
		afterTaxProfit[0] = profitsAfterTax;
		this.getExpectation(StaticValues.EXPECTATIONS_PROFITAFTERTAX).addObservation(afterTaxProfit);
		DividendsStrategy strategy=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		strategy.payDividends();
	}

	/**
	 * 
	 */
	private void determineBankruptcy() {
		double nW=this.getNetWealth();
		if(nW<0){
			this.defaulted=true;
			//this.dead=true;
			//this.unsubscribeFromEvents();
		}
	}
	
	/**
	 * 
	 */
	private void conductBankruptcy() {
		if(this.defaulted==true){
			BankruptcyStrategy strategy = (BankruptcyStrategy)this.getStrategy(StaticValues.STRATEGY_BANKRUPTCY);
			strategy.bankrupt();
			//this.dead=true;
			//this.unsubscribeFromEvents();
		}
	}

	/**
	 * Updates the various expectations the bank is making.
	 */
	private void updateExpectations() { 
		double nW=this.getNetWealth();
		this.addValue(StaticValues.LAG_NETWEALTH,nW);
		//this.addValue(StaticValues.LAG_PROFIT,profits);
		this.addValue(StaticValues.LAG_REMAININGCREDIT, this.totalLoanSupply);
		this.addValue(StaticValues.LAG_NONPERFORMINGLOANS, this.currentNonPerformingLoans);
		this.addValue(StaticValues.LAG_DEPOSITINTEREST, depositInterestRate);
		this.addValue(StaticValues.LAG_LOANINTEREST, bankInterestRate);
		// interbank expectations
		this.addValue(StaticValues.LAG_INTERBANKINTEREST,interbankAsk);
		// end interbank expectations
		double[] deposit = new double[1];
		deposit[0]=this.getNumericBalanceSheet()[1][StaticValues.SM_DEP];
		this.addValue(StaticValues.LAG_DEPOSITS,this.getNumericBalanceSheet()[1][StaticValues.SM_DEP]);
		this.getExpectation(StaticValues.EXPECTATIONS_DEPOSITS).addObservation(deposit);
		
		
		double newLoansThisPeriod=0;
		for (Item item:this.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
			Loan loan = (Loan) item;
			if(loan.getAge()==0) {
				newLoansThisPeriod+=loan.getInitialAmount();
			}
		}
		double[] realNewLoans = new double[1];
		Population govpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) govpop.getAgentList().get(0);
		double cPrice = gov.getAggregateValue(StaticValues.LAG_CPRICE, 0);
		realNewLoans[0]=newLoansThisPeriod/cPrice;
		this.getExpectation(StaticValues.EXPECTATIONS_REALNEWLOANS).addObservation(realNewLoans);
		
		this.cleanSM();
	}

	/**
	 * Determines the bank generic interest rate, i.e. the fixed mark-up on the basis interest rate. 
	 */
	private void determineBankGenericInterestRate() {
		InterestRateStrategy strategy = (InterestRateStrategy)this.getStrategy(StaticValues.STRATEGY_LOANBANKINTERESTRATE);
		this.bankInterestRate=strategy.computeInterestRate(null,0,0);
		
	}

	/**
	 * Determines the advances to be requested to the central bank in order to meet BaselII requirements
	 */
	private void determineAdvancesDemandBasel() {
		FinanceStrategy strategy = (FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_ADVANCES);
		this.advancesDemand=strategy.computeCreditDemand(0);//see BaselIIReserveRequirements strategy
		if(this.advancesDemand>0)
			this.setActive(true, StaticValues.MKT_ADVANCES);
	}

	/**
	 * Determines the advances to be requested to the central bank in order to be able to buy bonds
	 */
	private void determineAdvancesDemandBond() {
		FinanceStrategy strategy = (FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		this.advancesDemand=Math.ceil(strategy.computeCreditDemand(bondDemand*bondPrice));
		if(this.advancesDemand>0)
			this.setActive(true, StaticValues.MKT_ADVANCES);
	}

	private void payInterests() {
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_ADVANCES);
		Deposit deposit = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		this.advancesInterests = 0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			if(loan.getAge()>0){
				double iRate=loan.getInterestRate();
				double amount=loan.getInitialAmount();
				int length = loan.getLength();
				double interests=iRate*loan.getValue();
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
				this.advancesInterests+=interests;
				double amountToPay=interests+principal;
				deposit.setValue(deposit.getValue()-amountToPay);
				loan.setValue(loan.getValue()-principal);
			}
		}
	}
	
	/**
	 * What to do when the bank is awaken by the arrival of an event (market-based events)
	 * @param event the event that awoke the agent
	 */
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_BONDS:
			TwoStepMarketSimulation sim = (TwoStepMarketSimulation)macroSim.getActiveMarket();
			if(sim.isFirstStep()){
				this.selectedBondSupplier=(BondSupplier)event.getObjects().get(0);
				this.bondPrice=this.selectedBondSupplier.getBondPrice();
				this.bondInterestRate=this.selectedBondSupplier.getBondInterestRate();
			}else if(sim.isSecondStep())
				macroSim.getActiveMarket().commit(this, this.selectedBondSupplier,marketID);
			break;
		case StaticValues.MKT_ADVANCES:
			macroSim.getActiveMarket().commit(this, (MacroAgent)event.getObjects().get(0),marketID);
			break;
			// on arrival in the interbank market
		case StaticValues.MKT_INTERBANK:
			if(interbankDemand>0) { // Determine if this bank is lender or borrower at the interbank market 
				SelectLenderStrategy borrowingStrategy = (SelectLenderStrategy) this.getStrategy(StaticValues.STRATEGY_BORROWING);
				MacroAgent lender= (MacroAgent)borrowingStrategy.selectLender(event.getObjects(), this.getLoanRequirement(StaticValues.SM_INTERBANK),1);//1 because its the length of the interbank loans
				macroSim.getActiveMarket().commit(this, lender,marketID);
			}else {
				PriciestBorrower lendingStrategy = (PriciestBorrower) this.getStrategy(StaticValues.STRATEGY_LENDING);
				MacroAgent borrower= (MacroAgent)lendingStrategy.selectBorrower(event.getObjects(), this.interbankSupply,1);//1 because its the length of the interbank loans
				macroSim.getActiveMarket().commit(borrower, this,marketID);
			}
			break;
		}
	}
	
	/**
	 * Determines the demand for bonds
	 */
	private void determineBondDemand() {
		BondDemandStrategy strategy = (BondDemandStrategy)this.getStrategy(StaticValues.STRATEGY_BONDDEMAND);
		this.bondDemand=strategy.bondDemand(this.selectedBondSupplier);
		if (this.bondDemand>0){
			this.setActive(true, StaticValues.MKT_BONDS);
		}
	}

	/**
	 * Determines the interest rate that is offered on deposits
	 */
	private void determineDepositInterestRate() {
		InterestRateStrategy strategy = (InterestRateStrategy)this.getStrategy(StaticValues.STRATEGY_DEPOSITINTERESTRATE);
		this.depositInterestRate=strategy.computeInterestRate(null, 0, 0);
		this.setActive(true, StaticValues.MKT_DEPOSIT);
	}

	/**
	 * Determines the total credit supplied
	 */
	private void determineCreditSupply() {
		SupplyCreditStrategy strategy=(SupplyCreditStrategy)this.getStrategy(StaticValues.STRATEGY_CREDITSUPPLY);
		double loanSupply=strategy.computeCreditSupply();
		setTotalLoansSupply(StaticValues.SM_LOAN, loanSupply);
		this.addValue(StaticValues.LAG_BANKTOTLOANSUPPLY, loanSupply);
		if (this.getTotalLoansSupply(StaticValues.SM_LOAN)>0){
			this.setActive(true, StaticValues.MKT_CREDIT);
		}
		
	}

	/**
	 * Pays interest rates to all deposit holders. Note that there is no counterpart flow.
	 */
	private void payDepositInterests() {
		List<Item> deposits = this.getItemsStockMatrix(false, StaticValues.SM_DEP);
		double totInterests=0;
		for(Item d:deposits){
			Deposit dep = (Deposit)d;
			DepositDemander depositor = (DepositDemander)dep.getAssetHolder();
			depositor.interestPaid(dep.getInterestRate()*dep.getValue());
			totInterests+=dep.getInterestRate()*dep.getValue();
			dep.setValue(dep.getValue()*(1+dep.getInterestRate()));	
		}
		totInterestsDeposits=totInterests;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditDemander#getLoanRequirement()
	 */
	@Override
	public double getLoanRequirement(int idLoanSM) {
		switch(idLoanSM){
		case StaticValues.SM_ADVANCES:
			return this.advancesDemand;
		case StaticValues.SM_INTERBANK:
			return this.interbankDemand;
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditDemander#decideLoanLength()
	 */
	@Override
	public int decideLoanLength(int idLoanSM) {
		switch(idLoanSM){
		case StaticValues.SM_ADVANCES:
			return this.advancesLength;
		case StaticValues.SM_INTERBANK:
			return this.advancesLength;
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditDemander#decideLoanAmortization()
	 */
	@Override
	public int decideLoanAmortizationType(int idLoanSM) {
		switch(idLoanSM){
		case StaticValues.SM_ADVANCES:
			return this.advancesAmortizationType;
		case StaticValues.SM_INTERBANK:
			return this.advancesAmortizationType;
		}
		return -1;
	}

	/**
	 * @return the reserveLength
	 */
	public int getAdvancesLength() {
		return advancesLength;
	}

	/**
	 * @param reserveLength the reserveLength to set
	 */
	public void setAdvancesLength(int advancesLength) {
		this.advancesLength = advancesLength;
	}

	/**
	 * @return the reserveAmortization
	 */
	public int getAdvancesAmortizationType() {
		return advancesAmortizationType;
	}

	/**
	 * @param reserveAmortization the reserveAmortization to set
	 */
	public void setAdvancesAmortizationType(int advancesAmortizationType) {
		this.advancesAmortizationType = advancesAmortizationType;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.BondDemander#getBondsDemand(double)
	 */
	@Override
	public long getBondsDemand(double price, BondSupplier issuer) {
		return bondDemand;
	}
	public long getBondsDemand(){
		return bondDemand;
	}

	/**
	 * Implements the bond demand paying stocks. Since we assume banks to buy all bonds, need to make sure
	 * there is enough money on the reserve account. Thus allow cash to go negative if needed. Anyway the reserve market
	 * afterwards will make sure there is no negative values for cash and reserves.
	 * TODO: CHECK THIS
	 * @see jmab2.agents.BondDemander#getPayingStocks(int, jmab2.stockmatrix.Item)
	 */
	@Override
	public List<Item> getPayingStocks(int idBondSM, Item payableStock) {
		List<Item> result = new ArrayList<Item>();
		result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_RESERVES));
		return result;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditDemander#setLoanRequirement(double)
	 */
	@Override
	public void setLoanRequirement(int idLoanSM, double d) {
		switch(idLoanSM){
		case StaticValues.SM_ADVANCES:
			this.advancesDemand=d;
			break;
		case StaticValues.SM_INTERBANK:
			this.interbankDemand=d;
			break;
		}
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditSupplier#getTotalLoansSupply()
	 */
	@Override
	public double getTotalLoansSupply(int loansId) {
		switch(loansId){
		case StaticValues.SM_LOAN:
			return this.totalLoanSupply;
		case StaticValues.SM_INTERBANK:
			return this.interbankSupply;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.CreditSupplier#setTotalLoansSupply(double)
	 */
	@Override
	public void setTotalLoansSupply(int loansId, double d) {
		switch(loansId){
		case StaticValues.SM_LOAN:
			this.totalLoanSupply=d;
			break;
		case StaticValues.SM_INTERBANK:
			this.interbankSupply=d;
			break;
		}
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.InterestRateSetterWithTargets#getInterestRate()
	 */
	@Override
	public double getInterestRate(int mktId) {
		switch(mktId){
		case StaticValues.MKT_CREDIT:
			return this.bankInterestRate;
		case StaticValues.MKT_DEPOSIT:
			return this.depositInterestRate;	
		case StaticValues.MKT_INTERBANK:
			return this.interbankAsk;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.InterestRateSetterWithTargets#getInterestRateLowerBound()
	 */
	@Override
	public double getInterestRateLowerBound(int mktId) {
		switch(mktId){
		case StaticValues.MKT_CREDIT:
			return (0);
			//return (0-this.advancesInterestRate);
		case StaticValues.MKT_DEPOSIT:
			return (0);
		case StaticValues.MKT_INTERBANK:
			return this.getReserveInterestRate(); // or also make difference between supply/demand?
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.InterestRateSetterWithTargets#getReferenceVariableForInterestRate()
	 */
	@Override
	public double getReferenceVariableForInterestRate(int mktId) {
		switch(mktId){
		case StaticValues.MKT_CREDIT:
			double outstandingLoans=0;
			for (Item i:this.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				outstandingLoans+=i.getValue();
			}
			if (outstandingLoans==0){
//TODO				System.out.println("CR (0.1799): NO LOANS");
				return Double.POSITIVE_INFINITY;
			}
			else{
//TODO				System.out.println("CR (0.1799): " + this.getPassedValue(StaticValues.LAG_NETWEALTH, 1)/outstandingLoans );
				return this.getPassedValue(StaticValues.LAG_NETWEALTH, 1)/outstandingLoans;
			}
			
			
		case StaticValues.MKT_DEPOSIT:
			double depositsValue=0;
			for(Item i:this.getItemsStockMatrix(false, StaticValues.SM_DEP)){
				depositsValue+=i.getValue();
				}
			double reservesValue=0;
			for(Item i:this.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
				reservesValue+=i.getValue();
				}
			if (depositsValue==0){
//TODO				System.out.println("LR(0.25802): DEPOSITS 0");
				return 0;
			}
			else{
//TODO			System.out.println("LR(0.25802): " + reservesValue/depositsValue );
			return reservesValue/depositsValue;
			}
			
			}
		return Double.NaN;
	}
		/* OLD VERSION
		switch(mktId){
		case StaticValues.MKT_CREDIT:
			return this.getPassedValue(StaticValues.LAG_REMAININGCREDIT, 1)/this.getPassedValue(StaticValues.LAG_BANKTOTLOANSUPPLY, 1);
		case StaticValues.MKT_DEPOSIT:
			return this.getPassedValue(StaticValues.LAG_REMAININGCREDIT, 1)/this.getPassedValue(StaticValues.LAG_BANKTOTLOANSUPPLY, 1);
		}
		return Double.NaN;	
	}
*/
	/* (non-Javadoc)
	 * @see jmab2.agents.InterestRateSetterWithTargets#getInterestRateUpperBound(int)
	 */
	@Override
	public double getInterestRateUpperBound(int mktId) {
		switch(mktId){
		case StaticValues.MKT_CREDIT:
			return Double.POSITIVE_INFINITY;
		case StaticValues.MKT_DEPOSIT:
			
			// Calculate external funding costs
			double interestPay=0;
			double totValue=0;
			List<Item> liabilities = this.getItemsStockMatrix(false, StaticValues.SM_ADVANCES);
				for(Item item:liabilities){
					InterestBearingItem liability = (InterestBearingItem) item;
					interestPay += liability.getInterestRate()*liability.getValue();
					totValue +=liability.getValue();
				}
			liabilities = this.getItemsStockMatrix(false, StaticValues.SM_INTERBANK);
				for(Item item:liabilities){
					InterestBearingItem liability = (InterestBearingItem) item;
					interestPay += liability.getInterestRate()*liability.getValue();
					totValue +=liability.getValue();
				}
				
				
				SimulationController controller = (SimulationController)this.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
				double totalReserves = 0;
				double totalDeposits=0;
				
				for (Agent b:banks.getAgents()){
					Bank bank = (Bank) b;
					
					for(Item i:bank.getItemsStockMatrix(false, StaticValues.SM_DEP)){
						totalDeposits+=i.getValue();
						}
					//totalReserves+=reservesValue;
					totalReserves+=bank.getNetLiquidity();
	
					}
				
				double avgNetLiquidityRatio =0;
				if(totalDeposits==0) {
					avgNetLiquidityRatio=0;
				}else {
					avgNetLiquidityRatio = totalReserves/totalDeposits;
				}
			
				return this.interbankAsk-Math.max(0,Math.round(this.targetedLiquidityRatio * 1000.0) / 1000.0-Math.round(Math.max(0, avgNetLiquidityRatio) * 1000.0) / 1000.0)*(this.advancesInterestRate+this.reserveInterestRate)-(Math.max(0, avgNetLiquidityRatio) * 1000.0/1000.0)*(this.interbankAsk-this.reserveInterestRate);
				
		case StaticValues.MKT_INTERBANK:
			return this.advancesInterestRate;
		}
		return 0;
	}

	/**
	 * @return the reserveInterestRate
	 */
	public double getReserveInterestRate() {
		return reserveInterestRate;
	}

	/**
	 * @param reserveInterestRate the reserveInterestRate to set
	 */
	public void setReserveInterestRate(double reserveInterestRate) {
		this.reserveInterestRate = reserveInterestRate;
	}

	public double getRiskAversionMarkUp() {
		return riskAversionMarkUp;
	}

	public void setRiskAversionMarkUp(double riskAversionMarkUp) {
		this.riskAversionMarkUp = riskAversionMarkUp;
	}

	public double getInterBankRiskPremium() {
		return interBankRiskPremium;
	}

	public void setInterBankRiskPremium(double interBankRiskPremium) {
		this.interBankRiskPremium = interBankRiskPremium;
	}

	/**
	 * @return the bankInterestRate
	 */
	public double getBankInterestRate() {
		return bankInterestRate;
	}

	/**
	 * @param bankInterestRate the bankInterestRate to set
	 */
	public void setBankInterestRate(double bankInterestRate) {
		this.bankInterestRate = bankInterestRate;
	}

	/**
	 * @return the depositInterestRate
	 */
	public double getDepositInterestRate() {
		return depositInterestRate;
	}

	/**
	 * @param depositInterestRate the depositInterestRate to set
	 */
	public void setDepositInterestRate(double depositInterestRate) {
		this.depositInterestRate = depositInterestRate;
	}

	/**
	 * @return the reserveDemand
	 */
	public double getReserveDemand() {
		return advancesDemand;
	}

	/**
	 * @param reserveDemand the reserveDemand to set
	 */
	public void setReserveDemand(double reserveDemand) {
		this.advancesDemand = reserveDemand;
	}

	/**
	 * @return the bondPrice
	 */
	public double getBondPrice() {
		return bondPrice;
	}

	/**
	 * @param bondPrice the bondPrice to set
	 */
	public void setBondPrice(double bondPrice) {
		this.bondPrice = bondPrice;
	}

	/**
	 * @return the bondDemand
	 */
	public long getBondDemand() {
		return bondDemand;
	}

	/**
	 * @param bondDemand the bondDemand to set
	 */
	public void setBondDemand(int bondDemand) {
		this.bondDemand = bondDemand;
	}

	/**
	 * @return the bondInterestRate
	 */
	public double getBondInterestRate() {
		return bondInterestRate;
	}

	/**
	 * @param bondInterestRate the bondInterestRate to set
	 */
	public void setBondInterestRate(double bondInterestRate) {
		this.bondInterestRate = bondInterestRate;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.ProfitsTaxPayer#getProfits()
	 */
	@Override
	public double getPreTaxProfits() {
		if(!dead)
			return this.getPassedValue(StaticValues.LAG_PROFITPRETAX, 0);
		else
			return Double.NaN;
	}

	/**
	 * @return the advancesInterestRate
	 */
	public double getAdvancesInterestRate() {
		return advancesInterestRate;
	}

	/**
	 * @param advancesInterestRate the advancesInterestRate to set
	 */
	public void setAdvancesInterestRate(double advancesInterestRate) {
		this.advancesInterestRate = advancesInterestRate;
	}

	/**
	 * @return the riskAversion
	 */
	public double getRiskAversion(MacroAgent creditDemander) {
		if (creditDemander instanceof ConsumptionFirm){
			return riskAversionC;
		}
		else{
			return riskAversionK;
		}
	}

	/**
	 * @param riskAversion the riskAversion to set
	 */
	public void setRiskAversionC(double riskAversion) {
		this.riskAversionC = riskAversion;
	}
	public void setRiskAversionK(double riskAversion) {
		this.riskAversionK = riskAversion;
	}

	/**
	 * @return the capitalRatio
	 */
	public double getCapitalRatio() {
		return capitalRatio;
	}

	/**
	 * @param capitalRatio the capitalRatio to set
	 */
	public void setCapitalRatio(double capitalRatio) {
		this.capitalRatio = capitalRatio;
	}

	/**
	 * @return the liquidityRatio
	 */
	public double getLiquidityRatio() {
		return liquidityRatio;
	}

	/**
	 * @param liquidityRatio the liquidityRatio to set
	 */
	public void setLiquidityRatio(double liquidityRatio) {
		this.liquidityRatio = liquidityRatio;
	}

	/**
	 * @return the advancesInterests
	 */
	public double getAdvancesInterests() {
		return advancesInterests;
	}

	/**
	 * @param advancesInterests the advancesInterests to set
	 */
	public void setAdvancesInterests(double advancesInterests) {
		this.advancesInterests = advancesInterests;
	}

	/**
	 * @return the bondInterestReceived
	 */
	public double getBondInterestReceived() {
		return bondInterestReceived;
	}

	/**
	 * @param bondInterestReceived the bondInterestReceived to set
	 */
	public void setBondInterestReceived(double bondInterestReceived) {
		this.bondInterestReceived = bondInterestReceived;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.BondDemander#setBondInterestsReceived(double)
	 */
	@Override
	public void setBondInterestsReceived(double interests) {
		this.bondInterestReceived = interests;
		
	}


	/**
	 * @return the totInterestsLoans
	 */
	public double getTotInterestsLoans() {
		return totInterestsLoans;
	}

	/**
	 * @return the dividends
	 */
	public double getDividends() {
		return dividends;
	}

	/**
	 * @param dividends the dividends to set
	 */
	public void setDividends(double dividends) {
		this.dividends = dividends;
	}

	/**
	 * @param totInterestsLoans the totInterestsLoans to set
	 */
	public void setTotInterestsLoans(double totInterestsLoans) {
		this.totInterestsLoans = totInterestsLoans;
	}

	/**
	 * @return the totInterestsDeposits
	 */
	public double getTotInterestsDeposits() {
		return totInterestsDeposits;
	}

	/**
	 * @param totInterestsDeposits the totInterestsDeposits to set
	 */
	public void setTotInterestsDeposits(double totInterestsDeposits) {
		this.totInterestsDeposits = totInterestsDeposits;
	}

	/**
	 * @return the bailoutCost
	 */
	public double getBailoutCost() {
		return bailoutCost;
	}

	/**
	 * @param bailoutCost the bailoutCost to set
	 */
	public void setBailoutCost(double bailoutCost) {
		this.bailoutCost = bailoutCost;
	}
	
	// additional getters and setters interbank

	//General question�. Why sometimes return this.variable and other times variable??
	public double getTotInterestsInterbank() {
		return totInterestsInterbank;
	}
	
	public double getInterbankDemand() {
		return this.interbankDemand;
	}

	// end interbank extra getters and setters
	
	@Override
	public void transfer(Item paying, Item receiving, double amount){
		MacroAgent otherBank = receiving.getLiabilityHolder();
//		Item BankRes = this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
//		Item BankCash = this.getItemStockMatrix(true, StaticValues.SM_CASH);

//		 Check if there is enough liquidity to perform the transfer
//		if((BankRes.getValue()+BankCash.getValue())<amount){
//			
//			this.advancesDemand =+ amount-BankRes.getValue();
//			SimulationController controller = (SimulationController)this.getScheduler();
//			MacroSimulation ms = (MacroSimulation) controller.getSimulation();
//			MacroPopulation populations = (MacroPopulation)controller.getPopulation();
//			MacroAgent cb = (MacroAgent) populations.getPopulation(StaticValues.CB_ID).getAgentList().get(0);
//			ms.getMarket(benchmark.StaticValues.MKT_ADVANCES).commit((MacroAgent) this, cb, benchmark.StaticValues.MKT_ADVANCES);
//			this.setActive(true, StaticValues.MKT_ADVANCES);
//		}
		Item balancingItem = this.getItemStockMatrix(true, this.depositCounterpartId);
		//If the payer and the receiver is a bank
		if(otherBank.getPopulationId()==StaticValues.BANKS_ID){
		Item otherBalancingItem = otherBank.getItemStockMatrix(true, this.depositCounterpartId);
		paying.setValue(paying.getValue()-amount);
		balancingItem.setValue(balancingItem.getValue()-amount);
		receiving.setValue(receiving.getValue()+amount);
		otherBalancingItem.setValue(otherBalancingItem.getValue()+amount);
		//If the central bank is the receiver (in cash or deposit) there is no otherBalancingItem
		}else if(otherBank.getPopulationId()==StaticValues.CB_ID) {
			// Check if there is enough cash/ reserves to perform the transfer otherwise reallocate assets
//			Item counterpartItem = otherBank.getItemStockMatrix(false, StaticValues.SM_RESERVES);
//			Item otherCounterpartItem = otherBank.getItemStockMatrix(false, StaticValues.SM_CASH);
//			if(receiving instanceof Cash && BankCash.getValue()<amount){
//				BankRes.setValue(BankRes.getValue()-(amount-BankCash.getValue()));
//				counterpartItem.setValue(counterpartItem.getValue()-(amount-BankCash.getValue()));
//				otherCounterpartItem.setValue(counterpartItem.getValue()+(amount-BankCash.getValue()));
//				BankCash.setValue(BankCash.getValue()+(amount-BankCash.getValue()));
//			} else if (receiving instanceof Deposit && BankRes.getValue()<amount) {
//				BankCash.setValue(BankCash.getValue()-(amount-BankRes.getValue()));
//				counterpartItem.setValue(counterpartItem.getValue()+(amount-BankRes.getValue()));
//				otherCounterpartItem.setValue(counterpartItem.getValue()-(amount-BankRes.getValue()));
//				BankRes.setValue(BankRes.getValue()+(amount-BankRes.getValue()));
//			}
			paying.setValue(paying.getValue()-amount);
			receiving.setValue(receiving.getValue()+amount);
			balancingItem.setValue(balancingItem.getValue()-amount);

			transferSum+=amount;
		}
	}

	/**
	 * Populates the agent characteristics using the byte array content. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][reserveIR][advancesIR][bankIR][depositIR][bondsIR][loanSupply][advancesDemand]
	 * [bondPrice][riskAversionC][riskAversionK][capitalRatio][liquidityRatio][advancesInterests][bondInterestsReceived]
	 * [advancesLenght][advancesAmortizationType][bondDemand][bondSupplierPopulationId][bondSupplierId][matrixSize][stockMatrixStructure]
	 * [expSize][ExpectationStructure][passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public void populateAgent(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		byte[] macroBytes = new byte[buf.getInt()];
		buf.get(macroBytes);
		super.populateCharacteristics(macroBytes, pop);
		reserveInterestRate = buf.getDouble();
		advancesInterestRate = buf.getDouble();
		bankInterestRate = buf.getDouble();
		depositInterestRate = buf.getDouble();
		bondInterestRate = buf.getDouble();
		totalLoanSupply = buf.getDouble();
		advancesDemand = buf.getDouble();
		bondPrice = buf.getDouble();
		riskAversionC = buf.getDouble();
		riskAversionK = buf.getDouble();
		capitalRatio = buf.getDouble();
		liquidityRatio = buf.getDouble();
		advancesInterests = buf.getDouble();
		bondInterestReceived = buf.getDouble();
		advancesLength = buf.getInt();
		advancesAmortizationType = buf.getInt();
		bondDemand = buf.getInt();
		Collection<Agent> aHolders = pop.getPopulation(buf.getInt()).getAgents();
		long selSupplierId = buf.getLong(); 
		for(Agent a:aHolders){
			MacroAgent pot = (MacroAgent) a;
			if(pot.getAgentId()==selSupplierId){
				this.selectedBondSupplier=(BondSupplier)pot;
				break;
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
	 * Generates the byte array containing all relevant informations regarding the bank agent. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][reserveIR][advancesIR][bankIR][depositIR][bondsIR][loanSupply][advancesDemand]
	 * [bondPrice][riskAversionC][riskAversionK][capitalRatio][liquidityRatio][advancesInterests][bondInterestsReceived]
	 * [advancesLenght][advancesAmortizationType][bondDemand][bondSupplierPopId][bondSupplierId][matrixSize][stockMatrixStructure]
	 * [expSize][ExpectationStructure][passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate(136);
			buf.putDouble(reserveInterestRate);
			buf.putDouble(advancesInterestRate);
			buf.putDouble(bankInterestRate);
			buf.putDouble(depositInterestRate);
			buf.putDouble(bondInterestRate);
			buf.putDouble(totalLoanSupply);
			buf.putDouble(advancesDemand);
			buf.putDouble(bondPrice);
			buf.putDouble(riskAversionC);
			buf.putDouble(riskAversionK);
			buf.putDouble(capitalRatio);
			buf.putDouble(liquidityRatio);
			buf.putDouble(advancesInterests);
			buf.putDouble(bondInterestReceived);
			buf.putInt(advancesLength);
			buf.putInt(advancesAmortizationType);
			buf.putLong(bondDemand);
			buf.putInt(selectedBondSupplier.getPopulationId());
			buf.putLong(selectedBondSupplier.getAgentId());
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
				case StaticValues.SM_ADVANCES:
					it = new Loan(itemData, pop, this);
					break;
				case StaticValues.SM_BONDS:
					it = new Bond(itemData, pop, this);
					break;
				case StaticValues.SM_CASH:
					it = new Cash(itemData, pop, this);
					break;
//				case StaticValues.SM_DEP:
//					it = new Deposit(itemData, pop, this);
//					break;
				case StaticValues.SM_LOAN:
					it = new Loan(itemData, pop, this);
					break;
				default:
					it = new Deposit(itemData, pop, this);
					break;
				}
				this.addItemStockMatrix(it, true, stockId);
				MacroAgent liabHolder = it.getLiabilityHolder();
				liabHolder.addItemStockMatrix(it, false, stockId);
			}
		}	
	}

	/**
	 * @return the riskAversionC
	 */
	public double getRiskAversionC() {
		return riskAversionC;
	}

	/**
	 * @return the riskAversionK
	 */
	public double getRiskAversionK() {
		return riskAversionK;
	}

	@Override
	public double getTargetedLiquidityRatio() {
		return this.targetedLiquidityRatio;
	}

	public void setTargetedLiquidityRatio(double targetedLiquidityRatio) {
		this.targetedLiquidityRatio = targetedLiquidityRatio;
	}
	
	@Override
	public double getTargetedCapitalAdequacyRatio() {
		return this.targetedCapitalAdequacyRatio;
	}

	public void setTargetedCapitalAdequacyRatio(double targetedCapitalAdequacyRatio) {
		this.targetedCapitalAdequacyRatio = targetedCapitalAdequacyRatio;
	}

	@Override
	public double getDepositAmount() {
		double amount=0;
		for(Item item:this.getItemsStockMatrix(false, StaticValues.SM_DEP)){
			amount+=item.getValue();
		}
		return amount;
	}

	@Override
	public double getCashAmount() {
		double amount=0;
		for(Item item:this.getItemsStockMatrix(false, StaticValues.SM_CASH)){
			amount+=item.getValue();
		}
		return amount;
	}

	@Override
	public double getReservesAmount() {
		double reservesValue=0;
		for(Item i:this.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
			reservesValue+=i.getValue();
			}
		return reservesValue;
	}

	@Override
	public void interestPaid(double interests) {
		this.interestsReceived=interests;
	}

	public void setInterbankAsk(double interbankAsk) {
		this.interbankAsk = interbankAsk;
	}
	
	public double getInterbankAsk() {
		return this.interbankAsk;
	}
		
	public void setInterestsReceived(double interestsReceived) {
		this.interestsReceived = interestsReceived;
	}
	
	public void setTotInterestsInterbank(double totInterestsInterbank) {
		this.totInterestsInterbank = totInterestsInterbank;
	}
	
	public double getFundingRate() {
		return fundingRate;
	}

	public void setFundingRate(double fundingRate) {
		this.fundingRate = fundingRate;
	}

	public void setInterbankDemand(double interbankDemand) {
		this.interbankDemand = interbankDemand;
	}

	
	public double getDebtBurden() {
		return debtBurden;
	}
	
	public void setDebtBurden(double debtBurden) {
		this.debtBurden = debtBurden;
	}

	@Override
	public void reservesInterestPaid(double interests) {
		// TODO Auto-generated method stub
		this.reservesInterests=interests;
	}
	
	public double getReservesInterestReceived(){
		return this.reservesInterests;
	}

	public double getCapitalAdequacyRatio() {
		return CapitalAdequacyRatio;
	}

	public void setCapitalAdequacyRatio(double capitalAdequacyRatio) {
		CapitalAdequacyRatio = capitalAdequacyRatio;
	}

	public double getDISReserveRatio() {
		return DISReserveRatio;
	}

	public void setDISReserveRatio(double designatedReserveRatio) {
		this.DISReserveRatio = designatedReserveRatio;
	}
	
	public double getExcessLiquidity() {
		double advValue = 0;
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_ADVANCES);
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			advValue+=loan.getValue();
			}
		double interbankLoansReceived = 0;
		List<Item> loansReceived=this.getItemsStockMatrix(false, StaticValues.SM_INTERBANK);
		for(int i=0;i<loansReceived.size();i++){
			Loan loan=(Loan)loansReceived.get(i);
			interbankLoansReceived+=loan.getValue();
			}
		double interbankLoansGiven = 0;
		List<Item> loansGiven=this.getItemsStockMatrix(true, StaticValues.SM_INTERBANK);
		for(int i=0;i<loansGiven.size();i++){
			Loan loan=(Loan)loansGiven.get(i);
			interbankLoansGiven+=loan.getValue();
			}
		double reservesValue=0;
		for(Item i:this.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
			reservesValue+=i.getValue();
			}
		double depositsValue=0;
		for(Item i:this.getItemsStockMatrix(false, StaticValues.SM_DEP)){
			depositsValue+=i.getValue();
			}
		double excessLiquidity = reservesValue+interbankLoansGiven-interbankLoansReceived-advValue-this.getTargetedLiquidityRatio()*depositsValue;
		return excessLiquidity;
	}
	
	public double getNetLiquidity() {
		double advValue = 0;
		List<Item> loans=this.getItemsStockMatrix(false, StaticValues.SM_ADVANCES);
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			advValue+=loan.getValue();
			}
		double interbankLoansReceived = 0;
		List<Item> loansReceived=this.getItemsStockMatrix(false, StaticValues.SM_INTERBANK);
		for(int i=0;i<loansReceived.size();i++){
			Loan loan=(Loan)loansReceived.get(i);
			interbankLoansReceived+=loan.getValue();
			}
		double interbankLoansGiven = 0;
		List<Item> loansGiven=this.getItemsStockMatrix(true, StaticValues.SM_INTERBANK);
		for(int i=0;i<loansGiven.size();i++){
			Loan loan=(Loan)loansGiven.get(i);
			interbankLoansGiven+=loan.getValue();
			}
		double reservesValue=0;
		for(Item i:this.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
			reservesValue+=i.getValue();
			}
		double liquidity = reservesValue+interbankLoansGiven-interbankLoansReceived-advValue;
		return liquidity;
	}

	public double getNetLiquidityRatio() {
		return netLiquidityRatio;
	}

	public void setNetLiquidityRatio(double netLiquidityRatio) {
		this.netLiquidityRatio = netLiquidityRatio;
	}

	public double getLoansRiskWeight() {
		return loansRiskWeight;
	}

	public void setLoansRiskWeight(double loansRiskWeight) {
		this.loansRiskWeight = loansRiskWeight;
	}

	public double getInterbankLoansRiskWeight() {
		return interbankLoansRiskWeight;
	}

	public void setInterbankLoansRiskWeight(double interbankLoansRiskWeight) {
		this.interbankLoansRiskWeight = interbankLoansRiskWeight;
	}
}
