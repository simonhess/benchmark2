/**
 * 
 */
package benchmark.strategies;

import benchmark.agents.CentralBank;

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
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort & Antoine Godin
 * This strategy lets the central bank set its advances interest rate
 * adaptively using a Taylor rule 
 */
@SuppressWarnings("serial")
public class MonetaryTaylor extends AbstractStrategy implements
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
		// 1. calculate inflation
		double inflation = calculateInflation(null); //TODO what argument to add to incorporate the macro simulation?
		// 2. calculate nominal GDP
		double nominalGDP = calculateNominalGDP(null); // TODO argument? 
		// 3. calculate real GDP 
		double realGDP = nominalGDP / inflation;
		CentralBank agent= (CentralBank) this.getAgent();
		// get from central bank 
		// 4. expectedNaturalRate
		// 5. assumed potential output
		double expectedNaturalRate = agent.getExpectedNaturalRate();
		double expectedGDP = agent.getExpectedPotentialGDP();
		// Compute the interest rate according to the taylor rule, TODO replace magic numbers by parameter 1, inflation target & parameter 2
		double AdvancesRate = inflation + expectedNaturalRate + 0.2*(inflation - 2) + 0.4* (realGDP - expectedGDP);
		
		return AdvancesRate; // return the AdvancesRate
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


}
