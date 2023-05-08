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
package benchmark.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.CapitalFirmWagesEnd;
import benchmark.agents.CentralBank;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.ConsumptionFirmWagesEnd;
import benchmark.agents.Government;
import benchmark.agents.GovernmentAntiCyclical;
import benchmark.agents.GovernmentAntiCyclicalWithInvestment;
import benchmark.agents.Households;
import benchmark.expectations.AdaptiveExpectationDoubleExponentialSmoothing;
import benchmark.expectations.AdaptiveExpectationExpectedVsActualValue;
import benchmark.expectations.AdaptiveExpectationTargetInventories;
import benchmark.report.AveragePriceAllProducersComputer;
import benchmark.strategies.AdaptiveMarkupOnAdvancesRate;
import benchmark.strategies.AdaptiveMarkupOnAdvancesRateDF;
import benchmark.strategies.AdaptiveMarkupOnAdvancesRateProfitGrowth;
import benchmark.strategies.FixedShareOfProfitsToPopulationAsShareOfWealthDividends;
import benchmark.strategies.IncomeWealthTaxStrategy;
import benchmark.strategies.InvestmentCapacityOperatingCashFlowExpected;
import benchmark.strategies.ProfitsWealthTaxStrategy;
import benchmark.strategies.RealLumpyCapitalDemandAdaptiveNPV;
import jmab.agents.CreditSupplier;
import jmab.agents.DepositSupplier;
import jmab.agents.GoodSupplier;
import jmab.agents.MacroAgent;
import jmab.agents.SimpleAbstractAgent;
import jmab.expectations.Expectation;
import jmab.expectations.PassedValues;
import jmab.expectations.TreeMapPassedValues;
import jmab.init.AbstractMacroAgentInitialiser;
import jmab.init.MacroAgentInitialiser;
import jmab.population.MacroPopulation;
import jmab.simulations.AbstractMacroSimulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Bond;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.AdaptiveMarkUpOnAC;
import jmab.strategies.BestQualityPriceCapitalSupplierWithSwitching;
import jmab.strategies.CheapestGoodSupplierWithSwitching;
import jmab.strategies.CheapestLenderWithSwitching;
import benchmark.strategies.ConsumptionFixedPropensitiesOOIWWithPersistency;
import jmab.strategies.MostPayingDepositWithSwitching;
import jmab.strategies.TargetExpectedInventoriesOutputStrategy;
import jmab.strategies.TaxPayerStrategy;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.MarkupInterestRateStrategy;
import jmab.strategies.MarkupPricingStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.agent.Agent;
import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;

/**
 * @author Alessandro Caiani and Antoine Godin & Simon Hess
 *
 */
public class SFCSSMacroAgentInitialiser extends AbstractMacroAgentInitialiser implements MacroAgentInitialiser{

	//Exogenous values

	private double hhWage;
	private double gr;
	private int hhsSize;
	private double propensityOOW;
	private double unemploymentBenefit;
	
	private int csEmpl;
    private int ksEmpl;
	
    private double ksShareOfExpIncomeAsDeposit;
    private double csShareOfExpIncomeAsDeposit;
    private double shareOfExpIncomeAsDeposit;
	private double inventoryShare;
	private int csKap;
	private int capitalDuration;
	private double capitalProductivity;
	private double csMarkup;
	private double ksMarkup;
	
	private double bsMarkup;
	private int loanLength;
    private double iDep;
	private double iBonds;
	private double iAdv;
	private double targetedLiquidityRatio;
	private double iReserves;
	private double DISReserveRatio;
	private double targetCapacityUtlization;

	// Steady State values
	
	// kFirms
	private int ksOutput;
	private double laborProductivity;
	private double kUnitCost;
	private double kPrice;
	private int ksInv;
	private double ksProfits;
	private double ksTax;
	private double ksDiv;
	private double ksLoans;
	private double ksDep;
	private double iLoans;
	private double ksEBITDA;
	
	// cFirms
	private int csOutput;
	private double cUnitCost;
	private double cPrice;
	private int csInv;
	private double csProfits;
	private double csDep;
	private double capitalLaborRatio;
	private double cNomKap;
	private double csTax;
	private double csDiv;
	private double csLoans;
	private double csEBITDA;
	
    // hhs, banks and gov
	private int totEmpl;
	private double hhsNI;
	private double hhsDep;
	private double bsDiv;
	private double hhsTax;
	private double hhsNomCons;
	private double hhsRealCons;
	private double hhsNW;
	private double propensityOOI;
	private double bsProfits;
	private double bsBonds;
	private double bsTax;
	private double bsAdv;
	private double bsNW;
	private double bsRes;
	private double bsProfitShareAsDividends;
	private double targetedCapitalAdequacyRatio;
	private double cbBonds;
	private int gEmpl;
	private double gBonds;
	private double cbProfits;
	private double seigniorage;
	private double gRes;
	
	// Additional parameters
	private double csLoans0;
	private double ksLoans0;
	private double csOCF;
	private double targetCashFlow;
	private double ksOCF;
	private double riskAversionK;
	private double riskAversionC;


	//Stocks
	//Households
	private double hhsCash;
	private double hhsRes;
	//Cap Firms

	//Cons Firms

	//Banks

	private double bsCash;

	//Flows
	//Households
	private double dividendsReceived;

	//RandomEngine
	private RandomEngine prng;
	private double uniformDistr;
	
	private AveragePriceAllProducersComputer avpAllProdComputer;

	/* (non-Javadoc)
	 * @see jmab.init.MacroAgentInitialiser#initialise(jmab.population.MacroPopulation)
	 */
	@Override
	public void initialise(MacroPopulation population, MacroSimulation sim) {	
		
		Population households = population.getPopulation(StaticValues.HOUSEHOLDS_ID);
		Population banks = population.getPopulation(StaticValues.BANKS_ID);
		Population kFirms = population.getPopulation(StaticValues.CAPITALFIRMS_ID);
		Population cFirms = population.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);

		GovernmentAntiCyclical govt = (GovernmentAntiCyclical)population.getPopulation(StaticValues.GOVERNMENT_ID).getAgentList().get(0);
		CentralBank cb = (CentralBank)population.getPopulation(StaticValues.CB_ID).getAgentList().get(0);

		int hhSize=households.getSize();
		int bSize=banks.getSize();
		int kSize=kFirms.getSize();
		int cSize=cFirms.getSize();

		int hhPerBank = hhSize/bSize;
		int cFirmPerBank = cSize/bSize;
		int kFirmPerBank = kSize/bSize;
		int cFirmPerkFirm = cSize/kSize;
		int hhPercFirm = hhSize/cSize;

		Uniform distr = new Uniform(-uniformDistr,uniformDistr,prng);
		
		double ksSales = ksOutput*kPrice;
		double csSales = csOutput*cPrice;
		
		//Households
		double hhDep = this.hhsDep/hhSize;
		double hhCash = this.hhsCash/hhSize;
		double hhRes = this.hhsCash/hhSize;
		double hhTax = this.hhsTax/hhSize;
		double hhDiv = (ksDiv+csDiv+bsDiv)/hhSize;
		double hhCons = this.hhsRealCons/hhSize;
		for(int i = 0; i<hhSize; i++){
			Households hh = (Households) households.getAgentList().get(i);
			hh.setDividendsReceived(hhDiv);
			
			//Steady state values
			
			ConsumptionFixedPropensitiesOOIWWithPersistency consumptionStrategy = (ConsumptionFixedPropensitiesOOIWWithPersistency) hh.getStrategy(benchmark.StaticValues.STRATEGY_CONSUMPTION);
			consumptionStrategy.setPropensityOOI(propensityOOI);
			consumptionStrategy.setPropensityOOW(propensityOOW);

			//Cash Holdings
			Cash cash = new Cash(hhCash,(SimpleAbstractAgent)hh,(SimpleAbstractAgent)cb);
			hh.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Deposit Holdings
			int bankId = (int) i/hhPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(hhDep, hh, bank, this.iDep);
			hh.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);
			hh.interestPaid(this.iDep*hhDep/(1+gr));
			
			//Central Bank Deposit Holdings
			Deposit res = new Deposit(hhRes,(SimpleAbstractAgent)hh,(SimpleAbstractAgent)cb,this.iReserves);
			hh.addItemStockMatrix(res, true, StaticValues.SM_RESERVES);
			cb.addItemStockMatrix(res, false, StaticValues.SM_RESERVES);
			hh.reservesInterestPaid(this.iReserves*hhRes/(1+gr));

			//Make sure there are no employer
			hh.setEmployer(null);

			//Set previous seller
			int sellerId= (int) i/hhPercFirm;
			GoodSupplier previousSeller= (GoodSupplier) cFirms.getAgentList().get(sellerId);
			CheapestGoodSupplierWithSwitching buyingStrategy= (CheapestGoodSupplierWithSwitching) hh.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSeller(previousSeller);

			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) hh.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);

			//Expectations and Lagged values
			hh.setWage(hhWage);
			hh.addValue(StaticValues.LAG_NETWEALTH, hh.getNetWealth());
			Expectation cPriceExp = hh.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE);
			int nbObs = cPriceExp.getNumberPeriod();
			double[][] passedcPrices = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedcPrices[j][0]=this.cPrice;
				passedcPrices[j][1]=this.cPrice;
			}
			cPriceExp.setPassedValues(passedcPrices);
			hh.addValue(StaticValues.LAG_EMPLOYED,0);
			hh.addValue(StaticValues.LAG_CONSUMPTION,hhCons*(1+distr.nextDouble()));
			hh.addValue(StaticValues.LAG_TAXES,hhTax*(1+distr.nextDouble()));
			hh.computeExpectations();

		}

		households.getAgentList().shuffle(prng);

		//Capital Firms
		int kInv = ksInv/kSize;
		double kDep = ksDep/kSize;
		double kLoan=ksLoans0/kSize;
		int hWorkerCounter = 0;
		int bankLoanIterator=0;
		int kEmpl = ksEmpl/kSize;
		double kProfit=this.ksProfits/kSize;
		double kSales=ksSales/kSize;
		double kOutput=ksOutput/kSize;
		double kOCF=ksOCF/kSize;
		double kEBITDA=ksEBITDA/kSize;
		double lMat=0;
		double kTax = ksTax/kSize;
		double kTotalLoan = ksLoans/kSize;
		
		for(int i = 0 ; i < kSize ; i++){
			CapitalFirmWagesEnd k = (CapitalFirmWagesEnd) kFirms.getAgentList().get(i);

			// Steady state values
			k.setCapitalDuration(capitalDuration);
			k.setCapitalProductivity(capitalProductivity);
			k.setLaborProductivity(laborProductivity);
			k.setCapitalLaborRatio(capitalLaborRatio);
			k.setLoanLength(loanLength);
			k.setShareOfExpIncomeAsDeposit(ksShareOfExpIncomeAsDeposit);
			
			TargetExpectedInventoriesOutputStrategy productionStrategy = (TargetExpectedInventoriesOutputStrategy) k.getStrategy(benchmark.StaticValues.STRATEGY_PRODUCTION);
			productionStrategy.setInventoryShare(inventoryShare);
			
			MarkupPricingStrategy pricingStrategy = (MarkupPricingStrategy) k.getStrategy(benchmark.StaticValues.STRATEGY_PRICING);
			pricingStrategy.setMarkUp(ksMarkup*(1+distr.nextDouble()));
			
			//Inventories
			CapitalGood kGood = new CapitalGood(kInv*this.kPrice, kInv, k, k, this.kPrice, k.getCapitalProductivity(), 
					k.getCapitalDuration(), k.getCapitalAmortization(), k.getCapitalLaborRatio());
			kGood.setUnitCost(kUnitCost);
			kGood.setAge(0);
			k.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);

			//Workers
			for(int j=0;j<kEmpl;j++){
				Households hh = (Households) households.getAgentList().get(hWorkerCounter);
				hh.setEmployer(k);
				hh.addValue(StaticValues.LAG_EMPLOYED,1);
				k.addEmployee(hh);
				hWorkerCounter++;
			}

			//Deposit Holdings
			int bankId = (int) i/kFirmPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(kDep, k, bank, this.iDep);
			k.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);
			k.interestPaid(this.iDep*kDep/(1+gr));
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) k.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);

			//Central Bank Deposit Holdings
			Deposit kRes = new Deposit(0,(SimpleAbstractAgent)k,(SimpleAbstractAgent)cb,this.iReserves);
			k.addItemStockMatrix(kRes, true, StaticValues.SM_RESERVES);
			cb.addItemStockMatrix(kRes, false, StaticValues.SM_RESERVES);
			k.reservesInterestPaid(0/(1+this.iReserves));

			//Cash
			Cash cash = new Cash(0,(SimpleAbstractAgent)k,(SimpleAbstractAgent)cb);
			k.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);


			//Loans
			bankLoanIterator=bankId;
			lMat = k.getLoanLength();
			for(int j = 0 ; j <= lMat -1 ; j++){
				Bank loanBank = (Bank) banks.getAgentList().get(bankLoanIterator);
				bankLoanIterator++;
				if(bankLoanIterator>=bSize)bankLoanIterator=0;
				Loan loan = new Loan(kLoan*(1/(Math.pow((1+gr),j)))*((lMat-j)/lMat), loanBank, k, this.iLoans, j+1, k.getLoanAmortizationType(), (int)lMat);
				loan.setInitialAmount(kLoan/Math.pow((1+gr),j));
				k.addItemStockMatrix(loan, false, StaticValues.SM_LOAN);
				loanBank.addItemStockMatrix(loan, true, StaticValues.SM_LOAN);
				//Set last period lender as previous lender in the firm's borrowing strategy
				if (j==1){
					CheapestLenderWithSwitching borrowingStrategy = (CheapestLenderWithSwitching) k.getStrategy(StaticValues.STRATEGY_BORROWING);
					CreditSupplier previousCreditor= (CreditSupplier) bank;
					borrowingStrategy.setPreviousLender(previousCreditor);
				}
			}

			//Expectations and Lagged Values
			k.addValue(StaticValues.LAG_PROFITPRETAX, kProfit*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_PROFITAFTERTAX, (kProfit-kTax)*(1+distr.nextDouble()));
			//double lagKInv=kInv*(1+distr.nextDouble());
			double lagKInv=kInv;
			k.addValue(StaticValues.LAG_INVENTORIES, lagKInv);
			k.addValue(StaticValues.LAG_PRODUCTION, kOutput*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_REALSALES, kOutput*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_PRICE, kPrice);
			k.addValue(StaticValues.LAG_NOMINALSALES, kSales*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_NETWEALTH, k.getNetWealth()*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_NOMINALINVENTORIES, lagKInv*kUnitCost);
			k.addValue(StaticValues.LAG_OPERATINGCASHFLOW,kOCF);
			k.addValue(StaticValues.LAG_DEFAULTED,0);
			Expectation kWageExp = k.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			int nbObs = kWageExp.getNumberPeriod();
			double[][] passedWage = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedWage[j][0]=this.hhWage*(1+distr.nextDouble());
				passedWage[j][1]=this.hhWage*(1+distr.nextDouble());
			}
			kWageExp.setPassedValues(passedWage);
			Expectation kSalesExp = k.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
			nbObs = kSalesExp .getNumberPeriod();
			double[][] passedSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedSales[j][0]=kSales*(1+distr.nextDouble());
				passedSales[j][1]=kSales*(1+distr.nextDouble());
			}
			passedSales[0][0]=k.getPassedValue(StaticValues.LAG_NOMINALSALES, 0);
			kSalesExp.setPassedValues(passedSales);
			Expectation kRSalesExp = k.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
			nbObs = kRSalesExp .getNumberPeriod();
			double[][] passedRSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedRSales[j][0]=kOutput*(1+distr.nextDouble());
				passedRSales[j][1]=kOutput*(1+distr.nextDouble());
			}
			passedRSales[0][0]=k.getPassedValue(StaticValues.LAG_REALSALES, 0);
			kRSalesExp.setPassedValues(passedRSales);
			if(kRSalesExp instanceof AdaptiveExpectationTargetInventories) {
				((AdaptiveExpectationTargetInventories) kRSalesExp).setAgent(k);
			}
			Expectation kEBITDAExp = k.getExpectation(StaticValues.EXPECTATIONS_EBITDA);
			nbObs = kEBITDAExp .getNumberPeriod();
			double[][] passedEBITDA = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedEBITDA[j][0]=(kEBITDA)*(1+distr.nextDouble());
				passedEBITDA[j][1]=(kEBITDA)*(1+distr.nextDouble());
			}
			kEBITDAExp.setPassedValues(passedEBITDA);
			if(kEBITDAExp instanceof AdaptiveExpectationDoubleExponentialSmoothing) {
				((AdaptiveExpectationDoubleExponentialSmoothing) kEBITDAExp).setLevel(passedEBITDA[0][0]);
			}
			
			
			k.addValue(StaticValues.LAG_TAXES,kTax);
			k.computeExpectations();
		}
		
		

		

		//Consumption Firms
		int cInv = csInv/cSize;
		double cDep = csDep/cSize;
		double cLoan = csLoans0/cSize;
		int cEmpl = csEmpl/cSize;
		double cProfit=this.csProfits/cSize;
		double cSales=csSales/cSize;
		double cOutput=csOutput/cSize;
		double cOCF=csOCF/cSize;
		double cEBITDA=csEBITDA/cSize;
		double cTax = csTax/cSize;
		double cTotalLoan = csLoans/cSize;
		
		double csCapFinancialValue=0;
		
		HashMap<Integer,ArrayList<CapitalGood>> capList = new HashMap<Integer,ArrayList<CapitalGood>>();
		
		for(int k = 0;k<20;k++) {
			ArrayList<CapitalGood> cL = new ArrayList<CapitalGood>();
			capList.put(k, cL);
		}
		
		for(int i = 0 ; i < cSize ; i++){
			ConsumptionFirmWagesEnd c = (ConsumptionFirmWagesEnd) cFirms.getAgentList().get(i);

			// Steady state values
			InvestmentCapacityOperatingCashFlowExpected investmentStrategy = (InvestmentCapacityOperatingCashFlowExpected) c.getStrategy(benchmark.StaticValues.STRATEGY_INVESTMENT);
			
			investmentStrategy.setTargetCashFlow(targetCashFlow);
			
			TargetExpectedInventoriesOutputStrategy productionStrategy = (TargetExpectedInventoriesOutputStrategy) c.getStrategy(benchmark.StaticValues.STRATEGY_PRODUCTION);
			productionStrategy.setInventoryShare(inventoryShare);
			c.setLoanLength(loanLength);
			c.setShareOfExpIncomeAsDeposit(csShareOfExpIncomeAsDeposit);
			
			MarkupPricingStrategy pricingStrategy = (MarkupPricingStrategy) c.getStrategy(benchmark.StaticValues.STRATEGY_PRICING);
			pricingStrategy.setMarkUp(csMarkup*(1+distr.nextDouble()));

			//Inventories
			ConsumptionGood cGood = new ConsumptionGood(cInv*this.cPrice, cInv, c, c, this.cPrice, 0);
			cGood.setUnitCost(cUnitCost);
			cGood.setAge(-1);
			c.addItemStockMatrix(cGood, true, StaticValues.SM_CONSGOOD);

			//Capital Stock
			int kFirmId = (int) i/cFirmPerkFirm;
			CapitalFirm kFirm = (CapitalFirm) kFirms.getAgentList().get(kFirmId);
			int kMat = kFirm.getCapitalDuration();
			double kAm = kFirm.getCapitalAmortization();
			double cCap = this.csKap/cSize;
			double cCapPerPeriod=cCap/kMat;
			double capitalValue=0;//Changed this, because we assume the capital stock to work fine until it becomes obsolete
			if(c.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND) instanceof RealLumpyCapitalDemandAdaptiveNPV) {
				for(int j = 0 ; j < kMat ; j++){
					CapitalGood kGood = new CapitalGood(this.kPrice*cCapPerPeriod*(1-j/kAm)/Math.pow((1+gr),j), cCapPerPeriod, c, kFirm, 
							this.kPrice/Math.pow((1+gr),j),kFirm.getCapitalProductivity(),kMat,(int)kAm,kFirm.getCapitalLaborRatio());
					kGood.setAge(j);
					kGood.setUnitCost(kUnitCost);
					capitalValue+=kGood.getValue();
					//c.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);
					ArrayList<CapitalGood> cL = capList.get(j);
					cL.add(kGood);
					csCapFinancialValue+=kGood.getValue();
				}
				
			}else {
			for(int j = 0 ; j < kMat ; j++){
				CapitalGood kGood = new CapitalGood(this.kPrice*cCapPerPeriod*(1-j/kAm)/Math.pow((1+gr),j), cCapPerPeriod, c, kFirm, 
						this.kPrice/Math.pow((1+gr),j),kFirm.getCapitalProductivity(),kMat,(int)kAm,kFirm.getCapitalLaborRatio());
				kGood.setAge(j);
				kGood.setUnitCost(kUnitCost);
				capitalValue+=kGood.getValue();
				c.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);
			}
			}

			//Previous cpaital supplier
			BestQualityPriceCapitalSupplierWithSwitching buyingStrategy= (BestQualityPriceCapitalSupplierWithSwitching) c.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSupplier(kFirm);


			//Workers
			for(int j=0;j<cEmpl;j++){
				Households hh = (Households) households.getAgentList().get(hWorkerCounter);
				hh.setEmployer(c);
				hh.addValue(StaticValues.LAG_EMPLOYED,1);
	
				c.addEmployee(hh);
				hWorkerCounter++;
			}

			//Deposit Holdings
			int bankId = (int)i/cFirmPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(cDep, c, bank, this.iDep);
			c.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);
			c.interestPaid(this.iDep*cDep/(1+gr));
			//Set Previous DepositSupplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) c.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousDepositSupplier= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousDepositSupplier);

			//Cash
			Cash cash = new Cash(0,(SimpleAbstractAgent)c,(SimpleAbstractAgent)cb);
			c.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);
			
			//Central Bank Deposit Holdings
			Deposit cRes = new Deposit(0,(SimpleAbstractAgent)c,(SimpleAbstractAgent)cb,this.iReserves);
			c.addItemStockMatrix(cRes, true, StaticValues.SM_RESERVES);
			cb.addItemStockMatrix(cRes, false, StaticValues.SM_RESERVES);
			c.reservesInterestPaid(0/(1+this.iReserves));

			//Loans
			bankLoanIterator=bankId;
			lMat = c.getLoanLength();
			
			if(c.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND) instanceof RealLumpyCapitalDemandAdaptiveNPV) {
							

			}else {

			for(int j = 0 ; j <= lMat-1 ; j++){
				Bank loanBank = (Bank) banks.getAgentList().get(bankLoanIterator);
				bankLoanIterator++;
				if(bankLoanIterator>=bSize)bankLoanIterator=0;
				Loan loan = new Loan(cLoan*(1/(Math.pow((1+gr),j)))*((lMat-j)/lMat), loanBank, c, this.iLoans, j+1, c.getLoanAmortizationType(), (int)lMat);
				loan.setInitialAmount(cLoan/Math.pow((1+gr),j));
				c.addItemStockMatrix(loan, false, StaticValues.SM_LOAN);
				loanBank.addItemStockMatrix(loan, true, StaticValues.SM_LOAN);
				//Set last period lender as previous lender in the firm's borrowing strategy
				if (j==1){
					CheapestLenderWithSwitching borrowingStrategy = (CheapestLenderWithSwitching) c.getStrategy(StaticValues.STRATEGY_BORROWING);
					CreditSupplier previousCreditor= (CreditSupplier) bank;
					borrowingStrategy.setPreviousLender(previousCreditor);
				}
			}

			}

			//Expectations and Lagged Values
			c.addValue(StaticValues.LAG_PROFITPRETAX, cProfit*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_PROFITAFTERTAX, (cProfit-cTax)*(1+distr.nextDouble()));
			//double lagCInv=cInv*(1+distr.nextDouble());
			double lagCInv=cInv;
			c.addValue(StaticValues.LAG_INVENTORIES, lagCInv);
			c.addValue(StaticValues.LAG_PRODUCTION, cOutput*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_REALSALES, cOutput*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_PRICE, cPrice);
			c.addValue(StaticValues.LAG_CAPACITY,cCap*kFirm.getCapitalProductivity()*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_CAPITALFINANCIALVALUE,capitalValue);
			c.addValue(StaticValues.LAG_NOMINALSALES, cOutput*cPrice*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_NETWEALTH, c.getNetWealth()*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_NOMINALINVENTORIES, lagCInv*cUnitCost);
			c.addValue(StaticValues.LAG_OPERATINGCASHFLOW, cOCF);
			c.addValue(StaticValues.LAG_DEFAULTED,0);
			Expectation cWageExp = c.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			int nbObs = cWageExp.getNumberPeriod();
			double[][] passedWage = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedWage[j][0]=this.hhWage*(1+distr.nextDouble());
				passedWage[j][1]=this.hhWage*(1+distr.nextDouble());
			}
			cWageExp.setPassedValues(passedWage);
			Expectation cSalesExp = c.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
			nbObs = cSalesExp .getNumberPeriod();
			double[][] passedSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedSales[j][0]=cSales*(1+distr.nextDouble());
				passedSales[j][1]=cSales*(1+distr.nextDouble());
			}
			passedSales[0][0]=c.getPassedValue(StaticValues.LAG_NOMINALSALES, 0);
			cSalesExp.setPassedValues(passedSales);
			Expectation cRSalesExp = c.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
			nbObs = cRSalesExp .getNumberPeriod();
			double[][] passedRSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedRSales[j][0]=cOutput*(1+distr.nextDouble());
				passedRSales[j][1]=cOutput*(1+distr.nextDouble());
			}
			passedRSales[0][0]=c.getPassedValue(StaticValues.LAG_REALSALES, 0);
			cRSalesExp.setPassedValues(passedRSales);
			
			if(cRSalesExp instanceof AdaptiveExpectationTargetInventories) {
				((AdaptiveExpectationTargetInventories) cRSalesExp).setAgent(c);
			}
			Expectation cEBITDAExp = c.getExpectation(StaticValues.EXPECTATIONS_EBITDA);
			nbObs = cEBITDAExp .getNumberPeriod();
			double[][] passedEBITDA = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedEBITDA[j][0]=(cEBITDA)*(1+distr.nextDouble());
				passedEBITDA[j][1]=(cEBITDA)*(1+distr.nextDouble());
			}
			cEBITDAExp.setPassedValues(passedEBITDA);
			if(cEBITDAExp instanceof AdaptiveExpectationDoubleExponentialSmoothing) {
				((AdaptiveExpectationDoubleExponentialSmoothing) cEBITDAExp).setLevel(passedEBITDA[0][0]);
			}
			
			Expectation cUFCFPerCapExp = c.getExpectation(StaticValues.EXPECTATIONS_UNLEVEREDFREECASHFLOWPERCAPACITY);
			nbObs = cUFCFPerCapExp .getNumberPeriod();
			double[][] pastUFCFPerCap = new double[nbObs][2];
			double UFCFPerCap = (cOCF+cTotalLoan*iLoans/(1+gr))/cCap;
			for(int j = 0; j<nbObs; j++){
				pastUFCFPerCap[j][0]=UFCFPerCap*(1+distr.nextDouble());
				pastUFCFPerCap[j][1]=UFCFPerCap*(1+distr.nextDouble());
			}
			cUFCFPerCapExp.setPassedValues(pastUFCFPerCap);
			if(cUFCFPerCapExp instanceof AdaptiveExpectationDoubleExponentialSmoothing) {
				((AdaptiveExpectationDoubleExponentialSmoothing) cUFCFPerCapExp).setLevel(pastUFCFPerCap[0][0]);
			}
			
			// Set past debt Interest values 
			
			Map<Integer, PassedValues> firmPassedValues = c.getPassedValues();
			
			TreeMapPassedValues pastDebtInterest = (TreeMapPassedValues)firmPassedValues.get(StaticValues.LAG_DEBTINTEREST);

			for(int k =0; k<pastDebtInterest.getNbLags();k++) {
				pastDebtInterest.addObservation(cTotalLoan*iLoans/Math.pow((1+gr),k),  sim.getRound()-k);
			}
			
			c.addValue(StaticValues.LAG_TAXES,cTax);

			c.computeExpectations();
		}
		
		MacroAgent refAgent = (MacroAgent)cFirms.getAgentList().get(0);
		
		// Distribute loans to firms proportionally to the financial value of their capacity
		if(refAgent.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND) instanceof RealLumpyCapitalDemandAdaptiveNPV) {
			
			RealLumpyCapitalDemandAdaptiveNPV capitalDemandStrategy = (RealLumpyCapitalDemandAdaptiveNPV)refAgent.getStrategy(StaticValues.STRATEGY_CAPITALDEMAND);
			
		for(int i = 0 ; i < cSize ; i++){
			ConsumptionFirmWagesEnd c = (ConsumptionFirmWagesEnd) cFirms.getAgentList().get(i);
			CapitalFirm kFirm = (CapitalFirm) kFirms.getAgentList().get(0);
			int kMat = kFirm.getCapitalDuration();
			
			int investmentFrequency = capitalDemandStrategy.getInvestmentFrequency();
			
			double investingFirmsEveryRound=cSize/investmentFrequency;
			double loansPerFirm=cSize/investingFirmsEveryRound;
			double investmentsPerKDuration= kMat/investmentFrequency;
			
			double kQuantity = 0;
			
			for(int j = 0; j<investmentsPerKDuration;j++) {
				
				double capValue = 0;
				
				int capAge =(i%investmentFrequency)+j*investmentFrequency;
				
				ArrayList<CapitalGood> cL = capList.get(capAge);
				
				for(int k = 0;k<loansPerFirm;k++) {	
					CapitalGood kGood = cL.remove(0);
					kGood.setAssetHolder(c);
					c.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);
					capValue+=kGood.getValue();
					kQuantity+=kGood.getQuantity();
				}
				
				// Distribute loans
				double capRatio = capValue/csCapFinancialValue;
				
				double loanValue = csLoans*capRatio;
				
				double loanPerBank = loanValue/banks.getSize();
				
				for(int k = 0;k<banks.getSize();k++) {
				
					Bank loanBank = (Bank) banks.getAgentList().get(bankLoanIterator);
					bankLoanIterator++;
					if(bankLoanIterator>=bSize)bankLoanIterator=0;
					Loan loan = new Loan(loanPerBank, loanBank, null, this.iLoans, capAge+1, c.getLoanAmortizationType(), (int)lMat);
					loan.setInitialAmount(loanPerBank*lMat/(lMat-capAge));
					loanBank.addItemStockMatrix(loan, true, StaticValues.SM_LOAN);
					loan.setLiabilityHolder(c);
					c.addItemStockMatrix(loan, false, StaticValues.SM_LOAN);
					
					//Set last period lender as previous lender in the firm's borrowing strategy
					if (k==0&&j==0){
						CheapestLenderWithSwitching borrowingStrategy = (CheapestLenderWithSwitching) c.getStrategy(StaticValues.STRATEGY_BORROWING);
						CreditSupplier previousCreditor= (CreditSupplier) loanBank;
						borrowingStrategy.setPreviousLender(previousCreditor);
					}
						
				}
				
				
			}
			
		}
		
		}
		
		
		//Banks
		double bCash = this.bsCash/bSize;
		double bRes = this.bsRes/bSize;
		int bondMat = govt.getBondMaturity();
		double bBond=this.bsBonds/bSize;
		int bondPrice = (int)govt.getBondPrice();
		int nbBondsPerPeriod = (int) bBond/(bondMat*bondPrice);
		double bProfit = this.bsProfits/bSize;
		double bAdv = this.bsAdv/bSize;
		double bTax = this.bsTax/bSize;
		double bDep = (hhsDep+ksDep+csDep)/bSize;
		for(int i = 0; i<bSize; i++){
			Bank b = (Bank) banks.getAgentList().get(i);
			//b.setRiskAversion(3);
			//b.setRiskAversionC(distr1.nextDouble());
			//b.setRiskAversionK(distr1.nextDouble());
			
			//Steady state and exogenous values
			b.setDepositInterestRate(iDep);
			b.setBondInterestRate(iBonds);
			b.setAdvancesInterestRate(iAdv);
			b.setReserveInterestRate(iReserves);
			b.setTargetedLiquidityRatio(targetedLiquidityRatio);
			b.setTargetedCapitalAdequacyRatio(targetedCapitalAdequacyRatio);
			b.setDISReserveRatio(DISReserveRatio);
			b.setRiskAversionC(riskAversionC);
			b.setRiskAversionK(riskAversionK);
			
			DividendsStrategy dividendsStrategy = (DividendsStrategy) b.getStrategy(benchmark.StaticValues.STRATEGY_DIVIDENDS);
			
			dividendsStrategy.setProfitShare(bsProfitShareAsDividends);
			
			if(b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof AdaptiveMarkupOnAdvancesRate){
				AdaptiveMarkupOnAdvancesRate banksBankSpecificLoanInterestStrategy = (AdaptiveMarkupOnAdvancesRate) b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
				banksBankSpecificLoanInterestStrategy.setMarkup(bsMarkup);
			}else if(b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof AdaptiveMarkupOnAdvancesRateDF){
				AdaptiveMarkupOnAdvancesRateDF banksBankSpecificLoanInterestStrategy = (AdaptiveMarkupOnAdvancesRateDF) b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
				banksBankSpecificLoanInterestStrategy.setMarkup(bsMarkup);
			}else if(b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof AdaptiveMarkupOnAdvancesRateProfitGrowth){
				AdaptiveMarkupOnAdvancesRateProfitGrowth banksBankSpecificLoanInterestStrategy = (AdaptiveMarkupOnAdvancesRateProfitGrowth) b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
				banksBankSpecificLoanInterestStrategy.setMarkup(bsMarkup);
			}else if(b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE) instanceof MarkupInterestRateStrategy){
				MarkupInterestRateStrategy banksBankSpecificLoanInterestStrategy = (MarkupInterestRateStrategy) b.getStrategy(benchmark.StaticValues.STRATEGY_LOANBANKINTERESTRATE);
				banksBankSpecificLoanInterestStrategy.setMarkup(bsMarkup);
			}
			
			//Cash Holdings
			Cash cash = new Cash(bCash,(SimpleAbstractAgent)b,(SimpleAbstractAgent)cb);
			b.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Reserve Holdings
			Deposit res = new Deposit(bRes,(SimpleAbstractAgent)b,(SimpleAbstractAgent)cb,this.iReserves);
			b.addItemStockMatrix(res, true, StaticValues.SM_RESERVES);
			cb.addItemStockMatrix(res, false, StaticValues.SM_RESERVES);

			//Bonds Holdings
			for(int j = 1 ; j<=bondMat; j++){
				Bond bond = new Bond(nbBondsPerPeriod*bondPrice, nbBondsPerPeriod, b, govt, govt.getBondMaturity(), this.iBonds, bondPrice);
				bond.setAge(j);
				b.addItemStockMatrix(bond, true, StaticValues.SM_BONDS);
				govt.addItemStockMatrix(bond, false, StaticValues.SM_BONDS);
			}

			//Advances			
			int aMat = b.getAdvancesLength();
			double aValue = bAdv/aMat;
			for(int j = 1 ; j <= aMat ; j++){
				Loan loan = new Loan(aValue, cb, b, this.iAdv, j, b.getAdvancesAmortizationType(), aMat);
				loan.setInitialAmount(aValue);
				b.addItemStockMatrix(loan, false, StaticValues.SM_ADVANCES);
				cb.addItemStockMatrix(loan, true, StaticValues.SM_ADVANCES);
			}

			//Expectations and Lagged Values

			b.addValue(StaticValues.LAG_PROFITPRETAX, bProfit*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_PROFITAFTERTAX, (bProfit-bTax)*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_NONPERFORMINGLOANS, 0*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_REMAININGCREDIT, 0*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_NETWEALTH, b.getNetWealth()*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_BANKTOTLOANSUPPLY, (((csLoans+ksLoans)/bSize))*2/(lMat+1)*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_DEPOSITINTEREST,iDep);
			b.addValue(StaticValues.LAG_LOANINTEREST,iLoans);
			b.addValue(StaticValues.LAG_INTERBANKINTEREST,this.iAdv);
			b.addValue(StaticValues.LAG_TOTINTERBANKSUPPLY,0);
			b.addValue(StaticValues.LAG_TOTINTERBANKDEMAND,0);
			b.addValue(StaticValues.LAG_DEPOSITS,(csDep+ksDep+hhsDep)/bSize);
			Map<Integer, PassedValues> passedValues = b.getPassedValues();
			passedValues.get(StaticValues.LAG_PROFITAFTERTAX).addObservation(b.getPassedValue(StaticValues.LAG_PROFITAFTERTAX, 0)/(1+gr), sim.getRound()-1);
			passedValues.get(StaticValues.LAG_PROFITAFTERTAX).addObservation(passedValues.get(StaticValues.LAG_PROFITAFTERTAX).getObservation(-1)/(1+gr), sim.getRound()-2);
			double[][] bs = b.getNumericBalanceSheet();
			Expectation bDepExp = b.getExpectation(StaticValues.EXPECTATIONS_DEPOSITS);
			int nbObs = bDepExp.getNumberPeriod();
			double[][] passedbDep = new double[nbObs][2];
			double passedDebValue = bs[1][StaticValues.SM_DEP]*(1+iDep*distr.nextDouble());
			for(int j = 0; j<nbObs; j++){
				passedbDep[j][0]=passedDebValue;
				passedbDep[j][1]=passedDebValue;
			}
			bDepExp.setPassedValues(passedbDep);
			
			Expectation bProfitExp = b.getExpectation(StaticValues.EXPECTATIONS_PROFITAFTERTAX);
			nbObs = bProfitExp .getNumberPeriod();
			double[][] passedProfit = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedProfit[j][0]=(bProfit-bTax)*(1+distr.nextDouble());
				passedProfit[j][1]=(bProfit-bTax)*(1+distr.nextDouble());
			}
			if(bProfitExp instanceof AdaptiveExpectationDoubleExponentialSmoothing) {
				((AdaptiveExpectationDoubleExponentialSmoothing) bProfitExp).setLevel(passedProfit[0][0]);
			}
			bProfitExp.setPassedValues(passedProfit);
			
			// Set past values of real new loans expectation
			Expectation bRealNewLoansExp = b.getExpectation(StaticValues.EXPECTATIONS_REALNEWLOANS);
			nbObs = bRealNewLoansExp .getNumberPeriod();
			double[][] passedRealNewLoans = new double[nbObs][2];
			double newLoansLastPeriod=0;
			for (Item item:b.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				Loan loan = (Loan) item;
				if(loan.getAge()==1) {
					newLoansLastPeriod+=loan.getInitialAmount();
				}
			}
			double pastRealLoans = newLoansLastPeriod/cPrice;
			for(int j = 0; j<nbObs; j++){
				passedRealNewLoans[j][0]=(pastRealLoans)*(1+distr.nextDouble());
				passedRealNewLoans[j][1]=(pastRealLoans)*(1+distr.nextDouble());
			}
			if(bRealNewLoansExp instanceof AdaptiveExpectationExpectedVsActualValue) {
				((AdaptiveExpectationExpectedVsActualValue) bRealNewLoansExp).setAdaptiveParam(pastRealLoans);;
			}
			bRealNewLoansExp.setPassedValues(passedRealNewLoans);
			
			b.addValue(StaticValues.LAG_TAXES,bTax);
			
			double liquidityRatio;
			if(bDep==0){
				liquidityRatio = 0;
			}else {
				liquidityRatio = bRes/bDep;
			}
			
			b.setLiquidityRatio(liquidityRatio);

			b.computeExpectations();
		}

		//Government
		//Employment
		
		govt.setUnemploymentBenefit(this.unemploymentBenefit);
		
		for(int i = 0 ; i < gEmpl ; i++){
			Households hh = (Households) households.getAgentList().get(hWorkerCounter);
			hh.setEmployer(govt);
			hh.addValue(StaticValues.LAG_EMPLOYED,1);
			govt.addEmployee(hh);
			hWorkerCounter++;
		}
		
		//Steady State values
		govt.setBondInterestRate(iBonds);

		//Central Bank Deposit
		Deposit govtRes = new Deposit(0,(SimpleAbstractAgent)govt,(SimpleAbstractAgent)cb,this.iReserves);
		govt.addItemStockMatrix(govtRes, true, StaticValues.SM_RESERVES);
		cb.addItemStockMatrix(govtRes, false, StaticValues.SM_RESERVES);
		
		if(govt instanceof GovernmentAntiCyclicalWithInvestment) {
		
		// Add central bank deposit only for seigniorage profits if government makes investments with seigniorage profits
		Deposit govtResSeign = new Deposit(gRes,(SimpleAbstractAgent)govt,(SimpleAbstractAgent)cb,this.iReserves);
		govt.addItemStockMatrix(govtResSeign, true, StaticValues.SM_RESERVES);
		cb.addItemStockMatrix(govtResSeign, false, StaticValues.SM_RESERVES);
		}

		//Central Bank
		int nbBondsPerPeriod1 = (int) this.cbBonds/(bondMat*bondPrice);
		for(int j = 1 ; j<=bondMat; j++){
			Bond bond = new Bond(nbBondsPerPeriod1*bondPrice, nbBondsPerPeriod1, cb, govt, govt.getBondMaturity(), this.iBonds, bondPrice);
			bond.setAge(j);
			cb.addItemStockMatrix(bond, true, StaticValues.SM_BONDS);
			govt.addItemStockMatrix(bond, false, StaticValues.SM_BONDS);
		}
		
		//Steady state values
		cb.setAdvancesInterestRate(iAdv);
		cb.setReserveInterestRate(iReserves);
		
		cb.addValue(StaticValues.LAG_RESERVESINTEREST,iReserves);
		
		Expectation cbUnemplExp = cb.getExpectation(StaticValues.EXPECTATIONS_UNEMPLOYMENT);
		int nbObs = cbUnemplExp.getNumberPeriod();
		double[][] passedUnmpl = new double[nbObs][2];
		double passedValueUnempl = (hhSize-csEmpl-ksEmpl-gEmpl)/Double.valueOf(hhSize);
		for(int j = 0; j<nbObs; j++){
			passedUnmpl[j][0]=passedValueUnempl;
			passedUnmpl[j][1]=passedValueUnempl;
		}
		cbUnemplExp.setPassedValues(passedUnmpl);
		
		//TODO: Add Aggregate values, we could use the macrosimulation
		
		double publicServantsWages = 0;
		
		for(MacroAgent ma: govt.getEmployees()) {
			Households hh = (Households) ma;
			publicServantsWages += hh.getWage();
		}
		// GDP calculation
		double nomGDP = publicServantsWages + csSales+ ksSales;
		
		govt.setAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, (hhSize-csEmpl-ksEmpl-gEmpl)/Double.valueOf(hhSize)*(1+distr.nextDouble()));//TODO
		govt.setAggregateValue(StaticValues.LAG_INFLATION, 0);//TODO
		govt.setAggregateValue(StaticValues.LAG_AGGCREDIT, csLoans+ksLoans);//TODO
		govt.setAggregateValue(StaticValues.LAG_NOMINALGDP, nomGDP);//TODO
		govt.setAggregateValue(StaticValues.LAG_ALLPRICE, 
				avpAllProdComputer.computeVariable(sim));
		govt.setAggregateValue(StaticValues.LAG_CPRICE, cPrice);
		govt.setAggregateValue(StaticValues.LAG_KPRICE, kPrice);
		govt.setAggregateValue(StaticValues.LAG_REALGDP, nomGDP/govt.getAggregateValue(StaticValues.LAG_ALLPRICE, 0));
		govt.setAggregateValue(StaticValues.LAG_POTENTIALGDP, govt.getAggregateValue(StaticValues.LAG_REALGDP, 0));
		govt.setAggregateValue(StaticValues.LAG_GOVTAX, (ksTax+csTax+hhsTax+bsTax));
		govt.setAggregateValue(StaticValues.LAG_AVCPRICE, cPrice);
		govt.setAggregateValue(StaticValues.LAG_AVKPRICE, kPrice);
		
		// Calculate nonBankMoneySupply
		
		double nonBankMoneySupply = govt.getNumericBalanceSheet()[0][StaticValues.SM_DEP]
				+govt.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES]
				+govt.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		
		for (Agent i:cFirms.getAgents()){
			ConsumptionFirm firm= (ConsumptionFirm) i;
			nonBankMoneySupply +=firm.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		}
		
		for (Agent i:kFirms.getAgents()){
			CapitalFirm firm= (CapitalFirm) i;
			nonBankMoneySupply +=firm.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
			
		}
		
		for (Agent i:households.getAgents()){
			Households hh= (Households) i;
			nonBankMoneySupply +=hh.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += hh.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += hh.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		}
		
		govt.setAggregateValue(StaticValues.LAG_NONBANKMONEYSUPPLY, nonBankMoneySupply);
		
		double cFirmsTotalEquity = 0;
		for(int i = 0 ; i < cSize ; i++){
			ConsumptionFirmWagesEnd c = (ConsumptionFirmWagesEnd) cFirms.getAgentList().get(i);
			cFirmsTotalEquity+=c.getPassedValue(StaticValues.LAG_NETWEALTH, 0);
		}	
		govt.setAggregateValue(StaticValues.LAG_AVCFIRMCOSTOFEQUITY, (cProfit-cTax)/cFirmsTotalEquity);
		govt.setAggregateValue(StaticValues.LAG_AVCFIRMEQUITYRATIO, cFirmsTotalEquity/(csLoans+cFirmsTotalEquity));
		
		// Set lagged values for the second last period
		
		AbstractMacroSimulation macroSim = (AbstractMacroSimulation) sim;
		
		Map<Integer, PassedValues> passedValues = macroSim.getPassedValues();
		
		passedValues.get(StaticValues.LAG_ALLPRICE).addObservation(govt.getAggregateValue(StaticValues.LAG_ALLPRICE, 0)/(1+gr), sim.getRound()-1);
		passedValues.get(StaticValues.LAG_CPRICE).addObservation(cPrice/(1+gr), sim.getRound()-1);
		passedValues.get(StaticValues.LAG_KPRICE).addObservation(kPrice/(1+gr), sim.getRound()-1);
		
		
		// Set past k prices so that c firms can use them to calculate amortisation costs as part of their normal units costs calculation 
		TreeMapPassedValues pastKprice = (TreeMapPassedValues)passedValues.get(StaticValues.LAG_KPRICE);

		for(int i =0; i<pastKprice.getNbLags();i++) {
			pastKprice.addObservation(kPrice/Math.pow((1+gr),i),  sim.getRound()-i);
		}
		
		macroSim.setPassedValues(passedValues);
	}

	/**
	 * @return the hhsDep
	 */
	public double getHhsDep() {
		return hhsDep;
	}

	/**
	 * @param hhsDep the hhsDep to set
	 */
	public void setHhsDep(double hhsDep) {
		this.hhsDep = hhsDep;
	}

	/**
	 * @return the hhsCash
	 */
	public double getHhsCash() {
		return hhsCash;
	}

	/**
	 * @param hhsCash the hhsCash to set
	 */
	public void setHhsCash(double hhsCash) {
		this.hhsCash = hhsCash;
	}

	public double getHhsRes() {
		return hhsRes;
	}

	public void setHhsRes(double hhsRes) {
		this.hhsRes = hhsRes;
	}
	
	/**
	 * @return the ksDep
	 */
	public double getKsDep() {
		return ksDep;
	}

	/**
	 * @param ksDep the ksDep to set
	 */
	public void setKsDep(double ksDep) {
		this.ksDep = ksDep;
	}

	/**
	 * @return the ksInv
	 */
	public int getKsInv() {
		return ksInv;
	}

	/**
	 * @param ksInv the ksInv to set
	 */
	public void setKsInv(int ksInv) {
		this.ksInv = ksInv;
	}

	/**
	 * @return the ksLoans
	 */
	public double getKsLoans() {
		return ksLoans;
	}

	/**
	 * @param ksLoans the ksLoans to set
	 */
	public void setKsLoans(double ksLoans) {
		this.ksLoans = ksLoans;
	}

	/**
	 * @return the csDep
	 */
	public double getCsDep() {
		return csDep;
	}

	/**
	 * @param csDep the csDep to set
	 */
	public void setCsDep(double csDep) {
		this.csDep = csDep;
	}

	/**
	 * @return the csInv
	 */
	public int getCsInv() {
		return csInv;
	}

	/**
	 * @param csInv the csInv to set
	 */
	public void setCsInv(int csInv) {
		this.csInv = csInv;
	}

	/**
	 * @return the csLoans
	 */
	public double getCsLoans() {
		return csLoans;
	}

	/**
	 * @param csLoans the csLoans to set
	 */
	public void setCsLoans(double csLoans) {
		this.csLoans = csLoans;
	}

	/**
	 * @return the csKap
	 */
	public int getCsKap() {
		return csKap;
	}

	/**
	 * @param csKap the csKap to set
	 */
	public void setCsKap(int csKap) {
		this.csKap = csKap;
	}

	/**
	 * @return the bsBonds
	 */
	public double getBsBonds() {
		return bsBonds;
	}

	/**
	 * @param bsBonds the bsBonds to set
	 */
	public void setBsBonds(double bsBonds) {
		this.bsBonds = bsBonds;
	}

	/**
	 * @return the bsRes
	 */
	public double getBsRes() {
		return bsRes;
	}

	/**
	 * @param bsRes the bsRes to set
	 */
	public void setBsRes(double bsRes) {
		this.bsRes = bsRes;
	}

	/**
	 * @return the bsAdv
	 */
	public double getBsAdv() {
		return bsAdv;
	}

	/**
	 * @param bsAdv the bsAdv to set
	 */
	public void setBsAdv(double bsAdv) {
		this.bsAdv = bsAdv;
	}

	/**
	 * @return the bsCash
	 */
	public double getBsCash() {
		return bsCash;
	}

	/**
	 * @param bsCash the bsCash to set
	 */
	public void setBsCash(double bsCash) {
		this.bsCash = bsCash;
	}

	/**
	 * @return the hhWage
	 */
	public double getHhWage() {
		return hhWage;
	}

	/**
	 * @param hhWage the hhWage to set
	 */
	public void setHhWage(double hhWage) {
		this.hhWage = hhWage;
	}

	/**
	 * @return the ksEmpl
	 */
	public int getKsEmpl() {
		return ksEmpl;
	}

	/**
	 * @param ksEmpl the ksEmpl to set
	 */
	public void setKsEmpl(int ksEmpl) {
		this.ksEmpl = ksEmpl;
	}


	/**
	 * @return the ksProfits
	 */
	public double getKsProfits() {
		return ksProfits;
	}

	/**
	 * @param ksProfits the ksProfits to set
	 */
	public void setKsProfits(double ksProfits) {
		this.ksProfits = ksProfits;
	}

	/**
	 * @return the kPrice
	 */
	public double getkPrice() {
		return kPrice;
	}

	/**
	 * @param kPrice the kPrice to set
	 */
	public void setkPrice(double kPrice) {
		this.kPrice = kPrice;
	}

	/**
	 * @return the csEmpl
	 */
	public int getCsEmpl() {
		return csEmpl;
	}

	/**
	 * @param csEmpl the csEmpl to set
	 */
	public void setCsEmpl(int csEmpl) {
		this.csEmpl = csEmpl;
	}

	/**
	 * @return the csProfits
	 */
	public double getCsProfits() {
		return csProfits;
	}

	/**
	 * @param csProfits the csProfits to set
	 */
	public void setCsProfits(double csProfits) {
		this.csProfits = csProfits;
	}

	/**
	 * @return the cPrice
	 */
	public double getcPrice() {
		return cPrice;
	}

	/**
	 * @param cPrice the cPrice to set
	 */
	public void setcPrice(double cPrice) {
		this.cPrice = cPrice;
	}

	/**
	 * @return the iLoans
	 */
	public double getiLoans() {
		return iLoans;
	}

	/**
	 * @param iLoans the iLoans to set
	 */
	public void setiLoans(double iLoans) {
		this.iLoans = iLoans;
	}

	/**
	 * @return the iDep
	 */
	public double getiDep() {
		return iDep;
	}

	/**
	 * @param iDep the iDep to set
	 */
	public void setiDep(double iDep) {
		this.iDep = iDep;
	}

	/**
	 * @return the bsProfits
	 */
	public double getBsProfits() {
		return bsProfits;
	}

	/**
	 * @param bsProfits the bsProfits to set
	 */
	public void setBsProfits(double bsProfits) {
		this.bsProfits = bsProfits;
	}

	/**
	 * @return the iBonds
	 */
	public double getiBonds() {
		return iBonds;
	}

	/**
	 * @param iBonds the iBonds to set
	 */
	public void setiBonds(double iBonds) {
		this.iBonds = iBonds;
	}

	/**
	 * @return the iAdv
	 */
	public double getiAdv() {
		return iAdv;
	}

	/**
	 * @param iAdv the iAdv to set
	 */
	public void setiAdv(double iAdv) {
		this.iAdv = iAdv;
	}

	/**
	 * @return the iAdv
	 */
	public double getiReserves() {
		return iReserves;
	}

	/**
	 * @param iAdv the iAdv to set
	 */
	public void setiReserves(double iReserves) {
		this.iReserves = iReserves;
	}
	
	/**
	 * @return the gEmpl
	 */
	public int getgEmpl() {
		return gEmpl;
	}

	/**
	 * @param gEmpl the gEmpl to set
	 */
	public void setgEmpl(int gEmpl) {
		this.gEmpl = gEmpl;
	}

	/**
	 * @return the kUnitCost
	 */
	public double getkUnitCost() {
		return kUnitCost;
	}

	/**
	 * @param kUnitCost the kUnitCost to set
	 */
	public void setkUnitCost(double kUnitCost) {
		this.kUnitCost = kUnitCost;
	}

	/**
	 * @return the cUnitCost
	 */
	public double getcUnitCost() {
		return cUnitCost;
	}

	/**
	 * @param cUnitCost the cUnitCost to set
	 */
	public void setcUnitCost(double cUnitCost) {
		this.cUnitCost = cUnitCost;
	}


	/**
	 * @return the dividendsReceived
	 */
	public double getDividendsReceived() {
		return dividendsReceived;
	}

	/**
	 * @param dividendsReceived the dividendsReceived to set
	 */
	public void setDividendsReceived(double dividendsReceived) {
		this.dividendsReceived = dividendsReceived;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.jabm.init.AgentInitialiser#initialise(net.sourceforge.jabm.Population)
	 */
	@Override
	public void initialise(Population population) {
		MacroPopulation macroPop = (MacroPopulation) population;
		this.initialise(macroPop);
	}

	/**
	 * @return the prng
	 */
	public RandomEngine getPrng() {
		return prng;
	}

	/**
	 * @param prng the prng to set
	 */
	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}

	/**
	 * @return the uniformDistr
	 */
	public double getUniformDistr() {
		return uniformDistr;
	}

	/**
	 * @param uniformDistr the uniformDistr to set
	 */
	public void setUniformDistr(double uniformDistr) {
		this.uniformDistr = uniformDistr;
	}
	/**
	 * @return the kOCF
	 */
	public double getKsOCF() {
		return ksOCF;
	}

	/**
	 * @param ksOCF the kOCF to set
	 */
	public void setKsOCF(double ksOCF) {
		this.ksOCF = ksOCF;
	}

	/**
	 * @return the cOCF
	 */
	public double getCsOCF() {
		return csOCF;
	}

	/**
	 * @param csOCF the cOCF to set
	 */
	public void setCsOCF(double csOCF) {
		this.csOCF = csOCF;
	}

	/**
	 * @return the gr
	 */
	public double getGr() {
		return gr;
	}

	/**
	 * @param gr the gr to set
	 */
	public void setGr(double gr) {
		this.gr = gr;
	}

	/**
	 * @return the cbBonds
	 */
	public double getCbBonds() {
		return cbBonds;
	}

	/**
	 * @param cbBonds the cbBonds to set
	 */
	public void setCbBonds(double cbBonds) {
		this.cbBonds = cbBonds;
	}

	/**
	 * @return the ksLoans0
	 */
	public double getKsLoans0() {
		return ksLoans0;
	}

	/**
	 * @param ksLoans0 the ksLoans0 to set
	 */
	public void setKsLoans0(double ksLoans0) {
		this.ksLoans0 = ksLoans0;
	}

	/**
	 * @return the csLoans0
	 */
	public double getCsLoans0() {
		return csLoans0;
	}

	/**
	 * @param csLoans0 the csLoans0 to set
	 */
	public void setCsLoans0(double csLoans0) {
		this.csLoans0 = csLoans0;
	}

	public AveragePriceAllProducersComputer getAvpAllProdComputer() {
		return avpAllProdComputer;
	}

	public void setAvpAllProdComputer(AveragePriceAllProducersComputer avpAllProdComputer) {
		this.avpAllProdComputer = avpAllProdComputer;
	}
	
	public double getInventoryShare() {
		return inventoryShare;
	}

	public void setInventoryShare(double inventoryShare) {
		this.inventoryShare = inventoryShare;
	}

	public int getCapitalDuration() {
		return capitalDuration;
	}

	public void setCapitalDuration(int capitalDuration) {
		this.capitalDuration = capitalDuration;
	}

	public double getCapitalProductivity() {
		return capitalProductivity;
	}

	public void setCapitalProductivity(double capitalProductivity) {
		this.capitalProductivity = capitalProductivity;
	}

	public int getLoanLength() {
		return loanLength;
	}

	public void setLoanLength(int loanLength) {
		this.loanLength = loanLength;
	}

	public double getUnemploymentBenefit() {
		return unemploymentBenefit;
	}

	public void setUnemploymentBenefit(double unemploymentBenefit) {
		this.unemploymentBenefit = unemploymentBenefit;
	}

	public int getHhsSize() {
		return hhsSize;
	}

	public void setHhsSize(int hhsSize) {
		this.hhsSize = hhsSize;
	}

	public double getTargetedLiquidityRatio() {
		return targetedLiquidityRatio;
	}

	public void setTargetedLiquidityRatio(double targetedLiquidityRatio) {
		this.targetedLiquidityRatio = targetedLiquidityRatio;
	}

	public double getDISReserveRatio() {
		return DISReserveRatio;
	}

	public void setDISReserveRatio(double dISReserveRatio) {
		DISReserveRatio = dISReserveRatio;
	}

	public double getTargetCapacityUtlization() {
		return targetCapacityUtlization;
	}

	public void setTargetCapacityUtlization(double targetCapacityUtlization) {
		this.targetCapacityUtlization = targetCapacityUtlization;
	}

	public int getKsOutput() {
		return ksOutput;
	}

	public void setKsOutput(int ksOutput) {
		this.ksOutput = ksOutput;
	}

	public double getKsTax() {
		return ksTax;
	}

	public void setKsTax(double ksTax) {
		this.ksTax = ksTax;
	}

	public double getKsDiv() {
		return ksDiv;
	}

	public void setKsDiv(double ksDiv) {
		this.ksDiv = ksDiv;
	}

	public int getCsOutput() {
		return csOutput;
	}

	public void setCsOutput(int csOutput) {
		this.csOutput = csOutput;
	}

	public double getCapitalLaborRatio() {
		return capitalLaborRatio;
	}

	public void setCapitalLaborRatio(double capitalLaborRatio) {
		this.capitalLaborRatio = capitalLaborRatio;
	}

	public double getcNomKap() {
		return cNomKap;
	}

	public void setcNomKap(double cNomKap) {
		this.cNomKap = cNomKap;
	}

	public double getCsTax() {
		return csTax;
	}

	public void setCsTax(double csTax) {
		this.csTax = csTax;
	}

	public double getCsDiv() {
		return csDiv;
	}

	public void setCsDiv(double csDiv) {
		this.csDiv = csDiv;
	}

	public int getTotEmpl() {
		return totEmpl;
	}

	public void setTotEmpl(int totEmpl) {
		this.totEmpl = totEmpl;
	}

	public double getHhsNI() {
		return hhsNI;
	}

	public void setHhsNI(double hhsNI) {
		this.hhsNI = hhsNI;
	}

	public double getBsDiv() {
		return bsDiv;
	}

	public void setBsDiv(double bsDiv) {
		this.bsDiv = bsDiv;
	}

	public double getHhsTax() {
		return hhsTax;
	}

	public void setHhsTax(double hhsTax) {
		this.hhsTax = hhsTax;
	}

	public double getHhsNomCons() {
		return hhsNomCons;
	}

	public void setHhsNomCons(double hhsNomCons) {
		this.hhsNomCons = hhsNomCons;
	}

	public double getHhsRealCons() {
		return hhsRealCons;
	}

	public void setHhsRealCons(double hhsRealCons) {
		this.hhsRealCons = hhsRealCons;
	}

	public double getHhsNW() {
		return hhsNW;
	}

	public void setHhsNW(double hhsNW) {
		this.hhsNW = hhsNW;
	}

	public double getBsTax() {
		return bsTax;
	}

	public void setBsTax(double bsTax) {
		this.bsTax = bsTax;
	}

	public double getBsNW() {
		return bsNW;
	}

	public void setBsNW(double bsNW) {
		this.bsNW = bsNW;
	}

	public double getCbProfits() {
		return cbProfits;
	}

	public void setCbProfits(double cbProfits) {
		this.cbProfits = cbProfits;
	}

	public double getSeigniorage() {
		return seigniorage;
	}

	public void setSeigniorage(double seigniorage) {
		this.seigniorage = seigniorage;
	}

	public double getgRes() {
		return gRes;
	}

	public void setgRes(double gRes) {
		this.gRes = gRes;
	}

	public double getTargetCashFlow() {
		return targetCashFlow;
	}

	public void setTargetCashFlow(double targetCashFlow) {
		this.targetCashFlow = targetCashFlow;
	}

	public double getRiskAversionK() {
		return riskAversionK;
	}

	public void setRiskAversionK(double riskAversionK) {
		this.riskAversionK = riskAversionK;
	}

	public double getRiskAversionC() {
		return riskAversionC;
	}

	public void setRiskAversionC(double riskAversionC) {
		this.riskAversionC = riskAversionC;
	}

	public double getgBonds() {
		return gBonds;
	}

	public void setgBonds(double gBonds) {
		this.gBonds = gBonds;
	}

	public double getLaborProductivity() {
		return laborProductivity;
	}

	public void setLaborProductivity(double laborProductivity) {
		this.laborProductivity = laborProductivity;
	}

	public double getPropensityOOI() {
		return propensityOOI;
	}

	public void setPropensityOOI(double propensityOOI) {
		this.propensityOOI = propensityOOI;
	}

	public double getTargetedCapitalAdequacyRatio() {
		return targetedCapitalAdequacyRatio;
	}

	public void setTargetedCapitalAdequacyRatio(double targetedCapitalAdequacyRatio) {
		this.targetedCapitalAdequacyRatio = targetedCapitalAdequacyRatio;
	}
	
	public double getBsProfitShareAsDividends() {
		return bsProfitShareAsDividends;
	}

	public void setBsProfitShareAsDividends(double bsProfitShareAsDividends) {
		this.bsProfitShareAsDividends = bsProfitShareAsDividends;
	}

	public double getCsMarkup() {
		return csMarkup;
	}

	public void setCsMarkup(double csMarkup) {
		this.csMarkup = csMarkup;
	}

	public double getKsMarkup() {
		return ksMarkup;
	}

	public void setKsMarkup(double ksMarkup) {
		this.ksMarkup = ksMarkup;
	}

	public double getBsMarkup() {
		return bsMarkup;
	}

	public void setBsMarkup(double bsMarkup) {
		this.bsMarkup = bsMarkup;
	}

	public double getPropensityOOW() {
		return propensityOOW;
	}

	public void setPropensityOOW(double propensityOOW) {
		this.propensityOOW = propensityOOW;
	}

	public double getShareOfExpIncomeAsDeposit() {
		return shareOfExpIncomeAsDeposit;
	}

	public void setShareOfExpIncomeAsDeposit(double shareOfExpIncomeAsDeposit) {
		this.shareOfExpIncomeAsDeposit = shareOfExpIncomeAsDeposit;
	}

	public double getKsShareOfExpIncomeAsDeposit() {
		return ksShareOfExpIncomeAsDeposit;
	}

	public void setKsShareOfExpIncomeAsDeposit(double ksShareOfExpIncomeAsDeposit) {
		this.ksShareOfExpIncomeAsDeposit = ksShareOfExpIncomeAsDeposit;
	}

	public double getCsShareOfExpIncomeAsDeposit() {
		return csShareOfExpIncomeAsDeposit;
	}

	public void setCsShareOfExpIncomeAsDeposit(double csShareOfExpIncomeAsDeposit) {
		this.csShareOfExpIncomeAsDeposit = csShareOfExpIncomeAsDeposit;
	}

	public double getKsEBITDA() {
		return ksEBITDA;
	}

	public void setKsEBITDA(double ksEBITDA) {
		this.ksEBITDA = ksEBITDA;
	}

	public double getCsEBITDA() {
		return csEBITDA;
	}

	public void setCsEBITDA(double csEBITDA) {
		this.csEBITDA = csEBITDA;
	}

}
