/**
 * 
 */
package benchmark.strategies;

import benchmark.StaticValues;
import benchmark.agents.CentralBank;
import benchmark.agents.Government;

import java.util.LinkedHashMap;
import java.util.List;

import jmab.agents.AbstractFirm;
import jmab.agents.LaborDemander;
import jmab.agents.LaborSupplier;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.AbstractGood;
import jmab.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort & Antoine Godin
 * This strategy lets the central bank set its advances interest rate
 * adaptively using a Taylor rule 
 */
@SuppressWarnings("serial")
public class MonetaryTaylorWithSmoothingAndUnempGap extends AbstractStrategy implements
		MonetaryPolicyStrategy {

	private int gdpAVID;
	private int inflationAVID;
	//private int inflationCoefficientId;
	//private int outputCoefficientId;
	private double taylorInterestRate;
	private int priceIndexProducerId;//This is the population id of agents that produce the goods entering in the CPI
	private int realSaleId;//This is the id of the lagged value of real sales
	private int priceGoodId;//This is the stock matrix if of the good entering in the CPI
	private int[] gdpPopulationIds;//These are all the populations ids of agents that have either bought or produced goods entering in GDP
	private int[] gdpGoodsIds;//These are all the stock matrix ids of goods that enter in GDP
	private int[] gdpGoodsAges;//These are all age limit of goods that enter in GDP
	private LinkedHashMap<Integer,Integer> goodPassedValueMap;
	private int governmentPopulationId; // the id of the government
	private double targetInflation;
	private double naturalRateOfInterest;
	private double inflationCoefficient;
	private double unemploymentGapCoefficient;
	private double targetUnemployment;
	private double smoothingParameter;
	
	public double getTaylorInterestRate() {
		return this.taylorInterestRate;
	}

	public void setTaylorInterestRate(double taylorInterestRate) {
		this.taylorInterestRate = taylorInterestRate;
	}
	
	/**
	 * compute the AdvancesRate based on the Taylor rule 
	 * advancesInterestRate = 
	 * inflation + expectedNaturalRate  
	 * + inflationCoefficient * (inflation - desiredInflation) 
	 * + outputCoefficient * (Math.log(Output) � Math.log(PotentialOutput))
	 */
	@Override
	public double computeAdvancesRate() {
		Population govpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) govpop.getAgentList().get(0);
		// 1. Calculate Consumer Price Inflation
		double lastCPrice = gov.getAggregateValue(StaticValues.LAG_CPRICE, 2);
		double currentCPrice = gov.getAggregateValue(StaticValues.LAG_CPRICE, 1);
		double inflation = (currentCPrice-lastCPrice)/lastCPrice;
		// 2. Get nominal GDP
		double nominalGDP = gov.getAggregateValue(StaticValues.LAG_NOMINALGDP, 1);
		// 3. Get real GDP 
		double realGDP = gov.getAggregateValue(StaticValues.LAG_REALGDP, 1);
		// 4. Get Potential GDP
		double potentialGDP = gov.getAggregateValue(StaticValues.LAG_POTENTIALGDP, 1);
		CentralBank agent= (CentralBank) this.getAgent();
		// get from central bank 
		// 5. expectedNaturalRate
		double expectedNaturalRate = agent.getExpectedNaturalRate();
		
		double previosAdvancesRate = agent.getAdvancesInterestRate();
		
		// Get unemployment rate
		double unemploymentRate = gov.getAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 1);
	
		// Compute the interest rate according to the taylor rule
		double AdvancesRate = previosAdvancesRate*smoothingParameter+(1-smoothingParameter)*(inflation + naturalRateOfInterest + inflationCoefficient*(inflation - targetInflation) + unemploymentGapCoefficient* (targetUnemployment-unemploymentRate));

		AdvancesRate = (double)Math.round(AdvancesRate * 10000d) / 10000d;
		
//		System.out.println((Math.log(realGDP) - Math.log(potentialGDP)));
//		System.out.println("nom GDP: "+nominalGDP);
//		System.out.println("realGDP: "+realGDP);
//		System.out.println("potentialGDP: "+potentialGDP);
//		System.out.println("cAVGPrices: "+currentCPrice);
//      System.out.println(AdvancesRate);
		
		return Math.max(AdvancesRate,0); // return the AdvancesRate
		/*/
		return 0;
		//*/
	}
	
	/*
	 * Helper function used to calculate inflation
	 */
	public double calculateInflation (MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(priceIndexProducerId);
		double totalSales=0;
		double averagePrice=0;
		for (Agent a:pop.getAgents()){
			AbstractFirm firm= (AbstractFirm) a;
			totalSales+=firm.getPassedValue(realSaleId, 0);
			AbstractGood good = (AbstractGood)firm.getItemStockMatrix(true, priceGoodId);
			averagePrice+=good.getPrice()*firm.getPassedValue(realSaleId,0);
		}
		double inflation = averagePrice/totalSales;
		return inflation;
	}
	
	/*
	 * Helper function defined to calculate nominal GDP 
	 */
	public double calculateNominalGDP(MacroSimulation sim) {
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(priceIndexProducerId);
		double gdpGoodsComponent=0;
			double pastInventories=0;
			double publicServantsWages=0;
			double nominalGDP=0;
			for(int popId:gdpPopulationIds){
				pop = macroPop.getPopulation(popId);
				//Population pop = macroPop.getPopulation(i); GET RID OF THIS?
				for(Agent j:pop.getAgents()){
					MacroAgent agent=(MacroAgent) j;
					for(int k=0; k<gdpGoodsIds.length;k++){
						List<Item> items= agent.getItemsStockMatrix(true, gdpGoodsIds[k]);
						for(Item item:items){
							if(item.getAge()<gdpGoodsAges[k]){
								gdpGoodsComponent+=item.getValue();
							}
							AbstractGood good = (AbstractGood)item;
							if(good.getProducer().getAgentId()==agent.getAgentId()){
								int passedValueId = goodPassedValueMap.get(good.getSMId());
								pastInventories+=agent.getPassedValue(passedValueId, 1);
							}
						}
					}					
				}
				gdpGoodsComponent-=pastInventories;
				if(governmentPopulationId!=-1){
					LaborDemander govt = (LaborDemander)macroPop.getPopulation(governmentPopulationId).getAgentList().get(0);
					for(MacroAgent agent:govt.getEmployees()){
						LaborSupplier publicServant = (LaborSupplier)agent;
						publicServantsWages+=publicServant.getWage();
					}
					nominalGDP = gdpGoodsComponent+publicServantsWages;
				}else
					nominalGDP = gdpGoodsComponent;
			}
			return nominalGDP;
	}
	
			
	
	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.SingleStrategy#populateFromBytes(byte[], jmab.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub

	}

	public int getGdpAVID() {
		return gdpAVID;
	}

	public void setGdpAVID(int gdpAVID) {
		this.gdpAVID = gdpAVID;
	}

	public int getInflationAVID() {
		return inflationAVID;
	}

	public void setInflationAVID(int inflationAVID) {
		this.inflationAVID = inflationAVID;
	}

	public double getTargetInflation() {
		return targetInflation;
	}

	public void setTargetInflation(double targetInflation) {
		this.targetInflation = targetInflation;
	}

	public double getInflationCoefficient() {
		return inflationCoefficient;
	}

	public void setInflationCoefficient(double inflationCoefficient) {
		this.inflationCoefficient = inflationCoefficient;
	}


	public double getNaturalRateOfInterest() {
		return naturalRateOfInterest;
	}

	public void setNaturalRateOfInterest(double naturalRateOfInterest) {
		this.naturalRateOfInterest = naturalRateOfInterest;
	}

	public double getSmoothingParameter() {
		return smoothingParameter;
	}

	public void setSmoothingParameter(double smoothingParameter) {
		this.smoothingParameter = smoothingParameter;
	}

	public double getUnemploymentGapCoefficient() {
		return unemploymentGapCoefficient;
	}

	public void setUnemploymentGapCoefficient(double unemploymentGapCoefficient) {
		this.unemploymentGapCoefficient = unemploymentGapCoefficient;
	}

	public double getTargetUnemployment() {
		return targetUnemployment;
	}

	public void setTargetUnemployment(double targetUnemployment) {
		this.targetUnemployment = targetUnemployment;
	}

}
