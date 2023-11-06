package benchmark2.strategies;

import java.util.List;

import benchmark2.agents.Bank;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Item;
import jmab2.strategies.SupplyCreditStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

@SuppressWarnings("serial")
public class InterbankSupplyCreditBaselIII extends AbstractStrategy implements
SupplyCreditStrategy {


	private int[] capitalIds;//in the simplest case cash, reserves to which we must add the net wealth
	private int[] assetsIds; 
	private double[] capitalsWeights;
	private double[] assetsWeights;
	private double interbankLoanWeight;

	/* 
	 * Compute credit supply based on total loans outstanding and the preferred amount
	 * of loans outstanding
	 */
	@Override
	public double computeCreditSupply() {
		Bank supplier=(Bank) this.getAgent();
		double targetedCapitalAdequacyRatio = supplier.getTargetedCapitalAdequacyRatio(); 
		double capitalsValue=supplier.getNetWealth();
		// calculate the capital value to calculate the capital adequacy ratio later
		for(int i=0;i<capitalIds.length;i++){
			double capitalValue=0;
			List<Item> caps = supplier.getItemsStockMatrix(true, capitalIds[i]);
			for(Item cap:caps){
				capitalValue+=cap.getValue();
			}
			capitalsValue+=capitalValue*capitalsWeights[i];
		} 
		// calculate risk weighted assets (specified in XML, at the moment just loans)
		double assetsValue=0;
		for(int i=0;i<assetsIds.length;i++){
			double assetValue=0;
			List<Item> assets = supplier.getItemsStockMatrix(true, assetsIds[i]);
			for(Item asset:assets){
				assetValue+=asset.getValue();
			}
			assetsValue+=assetValue*assetsWeights[i]; // not including interbank loans
		}

		double desiredInterbankLoansStock=(capitalsValue - assetsValue*targetedCapitalAdequacyRatio)/(1+(interbankLoanWeight*targetedCapitalAdequacyRatio));
		// use this code in case of more than one period interbankLoans:
		/*		List<Item> currentLoans=supplier.getItemsStockMatrix(true, loansId);
		double currentLoansStock=0;
		double expiringLoans=0;
		for (Item i:currentLoans){
			Loan loan=(Loan)i;
			currentLoansStock+=i.getValue();
			if (loan.getLength()-loan.getAge()<=1){ //the loan is going to be fully repaid
				expiringLoans+=loan.getValue();
			}
		}
		 */
		double newLoansSupply=desiredInterbankLoansStock;
		return newLoansSupply;
	}
	//TODO: Shouldn't we remove from the variation in reserve, the quantity of advances already taken that should disapear?
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [capitalAdequacyRatio][depositsExpectationsId][depositsId][loansId][nbAssets][assetsIds][assetsWeights]
	 * [nbCapital][capitalIds][capitalWeights]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		return null;
		//TODO 
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [capitalAdequacyRatio][depositsExpectationsId][depositsId][loansId][nbAssets][assetsIds][assetsWeights]
	 * [nbCapital][capitalIds][capitalWeights]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		//TODO
	}
	public int[] getCapitalIds() {
		return capitalIds;
	}
	public void setCapitalIds(int[] capitalIds) {
		this.capitalIds = capitalIds;
	}
	public int[] getAssetsIds() {
		return assetsIds;
	}
	public void setAssetsIds(int[] assetsIds) {
		this.assetsIds = assetsIds;
	}
	public double[] getCapitalsWeights() {
		return capitalsWeights;
	}
	public void setCapitalsWeights(double[] capitalsWeights) {
		this.capitalsWeights = capitalsWeights;
	}
	public double[] getAssetsWeights() {
		return assetsWeights;
	}
	public void setAssetsWeights(double[] assetsWeights) {
		this.assetsWeights = assetsWeights;
	}
	public double getInterbankLoanWeight() {
		return interbankLoanWeight;
	}
	public void setInterbankLoanWeight(double interbankLoanWeight) {
		this.interbankLoanWeight = interbankLoanWeight;
	}

}
