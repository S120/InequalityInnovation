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
package modelDistribution.strategies;

import java.nio.ByteBuffer;

import jmab.agents.IncomeTaxPayer;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.strategies.SingleStrategy;
import modelDistribution.StaticValues;
import modelDistribution.agents.Government;
import modelDistribution.agents.GovernmentDifferentiatedWorkers;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.Simulation;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ProgressiveTaxRatesComputerStrategyWithDeficitManagement extends AbstractStrategy implements SingleStrategy {

	private double wealthTaxRate;
	private double incomeTaxRate;
	private double workersSharePopulation;
	private double managersResearchersSharePopulation;
	private double topManagersSharePopulation;
	private double progressivenessExponent;
	private double managersNumber;
	private double workersNumber;
	private double topManagersNumber;
	private double researchersNumber;
	private double strategy;
	private boolean fixed=false;
	private int lag;
	/* (non-Javadoc)
	 * @see jmab.strategies.TaxPayerStrategy#computeTaxes()
	 */
	
	

	/**
	 * @return the wealthTaxRate
	 */
	public double getWealthTaxRate() {
		return wealthTaxRate;
	}

	/**
	 * @return the fixed
	 */
	public boolean isFixed() {
		return fixed;
	}

	/**
	 * @param fixed the fixed to set
	 */
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	/**
	 * @return the strategy
	 */
	public double getStrategy() {
		return strategy;
	}

	/**
	 * @param strategy the strategy to set
	 */
	public void setStrategy(double strategy) {
		this.strategy = strategy;
	}

	/**
	 * @return the lag
	 */
	public int getLag() {
		return lag;
	}

	/**
	 * @param lag the lag to set
	 */
	public void setLag(int lag) {
		this.lag = lag;
	}

	/**
	 * @return the managersNumber
	 */
	public double getManagersNumber() {
		return managersNumber;
	}

	/**
	 * @param managersNumber the managersNumber to set
	 */
	public void setManagersNumber(double managersNumber) {
		this.managersNumber = managersNumber;
	}

	/**
	 * @return the workersNumber
	 */
	public double getWorkersNumber() {
		return workersNumber;
	}

	/**
	 * @param workersNumber the workersNumber to set
	 */
	public void setWorkersNumber(double workersNumber) {
		this.workersNumber = workersNumber;
	}

	/**
	 * @return the topManagersNumber
	 */
	public double getTopManagersNumber() {
		return topManagersNumber;
	}

	/**
	 * @param topManagersNumber the topManagersNumber to set
	 */
	public void setTopManagersNumber(double topManagersNumber) {
		this.topManagersNumber = topManagersNumber;
	}

	/**
	 * @return the researchersNumber
	 */
	public double getResearchersNumber() {
		return researchersNumber;
	}

	/**
	 * @param researchersNumber the researchersNumber to set
	 */
	public void setResearchersNumber(double researchersNumber) {
		this.researchersNumber = researchersNumber;
	}

	/**
	 * @param wealthTaxRate the wealthTaxRate to set
	 */
	public void setWealthTaxRate(double wealthTaxRate) {
		this.wealthTaxRate = wealthTaxRate;
	}

	/**
	 * @return the incomeTaxRate
	 */
	public double getIncomeTaxRate() {
		return incomeTaxRate;
	}

	/**
	 * @param incomeTaxRate the incomeTaxRate to set
	 */
	public void setIncomeTaxRate(double incomeTaxRate) {
		this.incomeTaxRate = incomeTaxRate;
	}
	
	

	/**
	 * @return the progressivenessExponent
	 */
	public double getProgressivenessExponent() {
		return progressivenessExponent;
	}

	/**
	 * @param progressivenessExponent the progressivenessExponent to set
	 */
	public void setProgressivenessExponent(double progressivenessExponent) {
		this.progressivenessExponent = progressivenessExponent;
	}



	/**
	 * @return the workersSharePopulation
	 */
	public double getWorkersSharePopulation() {
		return workersSharePopulation;
	}

	/**
	 * @param workersSharePopulation the workersSharePopulation to set
	 */
	public void setWorkersSharePopulation(double workersSharePopulation) {
		this.workersSharePopulation = workersSharePopulation;
	}

	/**
	 * @return the managersResearchersSharePopulation
	 */
	public double getManagersResearchersSharePopulation() {
		return managersResearchersSharePopulation;
	}

	/**
	 * @param managersResearchersSharePopulation the managersResearchersSharePopulation to set
	 */
	public void setManagersResearchersSharePopulation(
			double managersResearchersSharePopulation) {
		this.managersResearchersSharePopulation = managersResearchersSharePopulation;
	}

	/**
	 * @return the topManagersSharePopulation
	 */
	public double getTopManagersSharePopulation() {
		return topManagersSharePopulation;
	}

	/**
	 * @param topManagersSharePopulation the topManagersSharePopulation to set
	 */
	public void setTopManagersSharePopulation(double topManagersSharePopulation) {
		this.topManagersSharePopulation = topManagersSharePopulation;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][incomeTaxRate]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putDouble(wealthTaxRate);
		buf.putDouble(incomeTaxRate);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][incomeTaxRate]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.wealthTaxRate = buf.getDouble();
		this.incomeTaxRate = buf.getDouble();
	}



	
	public void computeTaxRates() {
		GovernmentDifferentiatedWorkers gov= (GovernmentDifferentiatedWorkers) this.getAgent();
		double taxRatesMultiplyingFactor= gov.getTaxRatesMultiplyingFactor();
		MacroSimulation sim = (MacroSimulation)((SimulationController)this.scheduler).getSimulation();
		
		double workerShareIncome=sim.getPassedValue(StaticValues.LAG_WORKERSHAREINCOME, lag);
		double managerResearcherShareIncome=sim.getPassedValue(StaticValues.LAG_RESEARCHERSMANAGERSHAREINCOME, lag);
		double topManagerShareIncome=sim.getPassedValue(StaticValues.LAG_TOPMANAGERSHAREINCOME, lag);
		double workerShareWealth=sim.getPassedValue(StaticValues.LAG_WORKERSHAREWEALTH, lag);
		double managerResearcherShareWealth=sim.getPassedValue(StaticValues.LAG_RESEARCHERSMANAGERSHAREWEALTH, lag);
		double topManagerShareWealth=sim.getPassedValue(StaticValues.LAG_TOPMANAGERSHAREWEALTH, lag);
		
		/*double workerShareIncome=0.3;
		double managerResearcherShareIncome=0.4;
		double topManagerShareIncome=0.3;
		double workerShareWealth=0.3;
		double managerResearcherShareWealth=0.4;
		double topManagerShareWealth=0.3;*/
	
		if (strategy==1){
			double incomePerHeadShare=workerShareIncome/workersNumber*workersSharePopulation+managerResearcherShareIncome/(managersNumber+researchersNumber)*managersResearchersSharePopulation+topManagerShareIncome/topManagersNumber*topManagersSharePopulation;
			double wealthPerHeadShare=workerShareWealth/workersNumber*workersSharePopulation+managerResearcherShareWealth/(managersNumber+researchersNumber)*managersResearchersSharePopulation+topManagerShareWealth/topManagersNumber*topManagersSharePopulation;
			double incomeRedistributionFactorWorkers=(workerShareIncome/workersNumber)/incomePerHeadShare+Math.pow((workerShareIncome/workersNumber)/incomePerHeadShare,progressivenessExponent);
			double incomeRedistributionFactorManagers=(managerResearcherShareIncome/(managersNumber+researchersNumber))/incomePerHeadShare+Math.pow((managerResearcherShareIncome/(managersNumber+researchersNumber))/incomePerHeadShare,progressivenessExponent);
			double incomeRedistributionFactorTopManagers=(topManagerShareIncome/topManagersNumber)/incomePerHeadShare+Math.pow((topManagerShareIncome/topManagersNumber)/incomePerHeadShare, progressivenessExponent);
			double wealthRedistributionFactorWorkers=(workerShareWealth/workersNumber)/wealthPerHeadShare+Math.pow((workerShareWealth/workersNumber)/wealthPerHeadShare,progressivenessExponent);
			double wealthRedistributionFactorManagers=(managerResearcherShareWealth/(managersNumber+researchersNumber))/wealthPerHeadShare+Math.pow((managerResearcherShareWealth/(managersNumber+researchersNumber))/wealthPerHeadShare,progressivenessExponent);
			double wealthRedistributionFactorTopManagers=(topManagerShareWealth/topManagersNumber)/wealthPerHeadShare+Math.pow((topManagerShareWealth/topManagersNumber)/wealthPerHeadShare, progressivenessExponent);
	        double denominatorTaxLoadComputers=incomeRedistributionFactorWorkers*workersNumber+incomeRedistributionFactorManagers*(managersNumber+researchersNumber)+incomeRedistributionFactorTopManagers*topManagersNumber;
			double incomeTaxLoadShareWorkers=incomeRedistributionFactorWorkers/denominatorTaxLoadComputers;
	        double incomeTaxLoadShareManagers=incomeRedistributionFactorManagers/denominatorTaxLoadComputers;
	        double incomeTaxLoadShareTopManagers=incomeRedistributionFactorTopManagers/denominatorTaxLoadComputers;
	        double wealthTaxLoadShareWorkers=wealthRedistributionFactorWorkers/denominatorTaxLoadComputers;
	        double wealthTaxLoadShareManagers=wealthRedistributionFactorManagers/denominatorTaxLoadComputers;
	        double wealthTaxLoadShareTopManagers=wealthRedistributionFactorTopManagers/denominatorTaxLoadComputers;
	        double incomeTaxRateWorkers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareWorkers/(workerShareIncome/workersNumber))*10000)/10000;
	        gov.setWorkersIncomeTaxRate(incomeTaxRateWorkers*taxRatesMultiplyingFactor);
	        double wealthTaxRateWorkers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareWorkers/(workerShareWealth/workersNumber))*10000)/10000;
	        gov.setWorkersWealthTaxRate(wealthTaxRateWorkers*taxRatesMultiplyingFactor);
	  	    double incomeTaxRateManagers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareManagers/(managerResearcherShareIncome/(managersNumber+researchersNumber)))*10000)/10000;
	        gov.setManagersIncomeTaxRate(incomeTaxRateManagers*taxRatesMultiplyingFactor);
	        double wealthTaxRateManagers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareManagers/(managerResearcherShareWealth/(managersNumber+researchersNumber)))*10000)/10000;
	        gov.setManagersWealthTaxRate(wealthTaxRateManagers*taxRatesMultiplyingFactor);
	        double incomeTaxRateTopManagers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareTopManagers/(topManagerShareIncome/topManagersNumber))*10000)/10000;
	        gov.setTopManagersIncomeTaxRate(incomeTaxRateTopManagers*taxRatesMultiplyingFactor);
			double wealthTaxRateTopManagers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareTopManagers/(topManagerShareWealth/topManagersNumber))*10000)/10000;
	        gov.setTopManagersWealthTaxRate(wealthTaxRateTopManagers*taxRatesMultiplyingFactor);
		}
		else if (strategy==2){
			double incomeRedistributionFactorWorkers=workerShareIncome*Math.pow(workerShareIncome/workersSharePopulation,progressivenessExponent);
			double incomeRedistributionFactorManagers=managerResearcherShareIncome*Math.pow(managerResearcherShareIncome/managersResearchersSharePopulation,progressivenessExponent);
			double incomeRedistributionFactorTopManagers=topManagerShareIncome*Math.pow(topManagerShareIncome/topManagersSharePopulation, progressivenessExponent);
			double wealthRedistributionFactorWorkers=workerShareWealth*Math.pow(workerShareWealth/workersSharePopulation,progressivenessExponent);
			double wealthRedistributionFactorManagers=managerResearcherShareWealth*Math.pow(managerResearcherShareWealth/managersResearchersSharePopulation,progressivenessExponent);
			double wealthRedistributionFactorTopManagers=topManagerShareWealth*Math.pow(topManagerShareWealth/topManagersSharePopulation, progressivenessExponent);
	        double incomeTaxLoadShareWorkers=incomeRedistributionFactorWorkers/(incomeRedistributionFactorWorkers+incomeRedistributionFactorManagers+incomeRedistributionFactorTopManagers);
	        double incomeTaxLoadShareManagers=incomeRedistributionFactorManagers/(incomeRedistributionFactorWorkers+incomeRedistributionFactorManagers+incomeRedistributionFactorTopManagers);
	        double incomeTaxLoadShareTopManagers=incomeRedistributionFactorTopManagers/(incomeRedistributionFactorWorkers+incomeRedistributionFactorManagers+incomeRedistributionFactorTopManagers);
	        double wealthTaxLoadShareWorkers=wealthRedistributionFactorWorkers/(wealthRedistributionFactorWorkers+wealthRedistributionFactorManagers+wealthRedistributionFactorTopManagers);
	        double wealthTaxLoadShareManagers=wealthRedistributionFactorManagers/(wealthRedistributionFactorWorkers+wealthRedistributionFactorManagers+wealthRedistributionFactorTopManagers);
	        double wealthTaxLoadShareTopManagers=wealthRedistributionFactorTopManagers/(wealthRedistributionFactorWorkers+wealthRedistributionFactorManagers+wealthRedistributionFactorTopManagers);
	        double incomeTaxRateWorkers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareWorkers/workerShareIncome)*10000)/10000;
	        gov.setWorkersIncomeTaxRate(incomeTaxRateWorkers*taxRatesMultiplyingFactor);
	        double wealthTaxRateWorkers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareWorkers/workerShareWealth)*10000)/10000;
	        gov.setWorkersWealthTaxRate(wealthTaxRateWorkers*taxRatesMultiplyingFactor);
	        double incomeTaxRateManagers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareManagers/managerResearcherShareIncome)*10000)/10000;
	        gov.setManagersIncomeTaxRate(incomeTaxRateManagers*taxRatesMultiplyingFactor);
	        double wealthTaxRateManagers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareManagers/managerResearcherShareWealth)*10000)/10000;
	        gov.setManagersWealthTaxRate(wealthTaxRateManagers*taxRatesMultiplyingFactor);
			double incomeTaxRateTopManagers=(double) Math.round((incomeTaxRate*incomeTaxLoadShareTopManagers/topManagerShareIncome)*10000)/10000;
	        gov.setTopManagersIncomeTaxRate(incomeTaxRateTopManagers*taxRatesMultiplyingFactor);
			double wealthTaxRateTopManagers=(double) Math.round((wealthTaxRate*wealthTaxLoadShareTopManagers/topManagerShareWealth)*10000)/10000;
	        gov.setTopManagersWealthTaxRate(wealthTaxRateTopManagers*taxRatesMultiplyingFactor);
			}
		else if (strategy==3){
			double incomeRedistributionFactorWorkers=Math.pow(workerShareIncome/workersSharePopulation,progressivenessExponent);
			double incomeRedistributionFactorManagers=Math.pow(managerResearcherShareIncome/managersResearchersSharePopulation,progressivenessExponent);
			double incomeRedistributionFactorTopManagers=Math.pow(topManagerShareIncome/topManagersSharePopulation, progressivenessExponent);
			double scalingFactorIncome=(1/incomeTaxRate)*(workerShareIncome*incomeRedistributionFactorWorkers+managerResearcherShareIncome*incomeRedistributionFactorManagers+topManagerShareIncome*incomeRedistributionFactorTopManagers);
			double wealthRedistributionFactorWorkers=Math.pow(workerShareWealth/workersSharePopulation,progressivenessExponent);
			double wealthRedistributionFactorManagers=Math.pow(managerResearcherShareWealth/managersResearchersSharePopulation,progressivenessExponent);
			double wealthRedistributionFactorTopManagers=Math.pow(topManagerShareWealth/topManagersSharePopulation, progressivenessExponent);
	        double scalingFactorWealth=(1/wealthTaxRate)*(workerShareWealth*wealthRedistributionFactorWorkers+managerResearcherShareWealth*wealthRedistributionFactorManagers+topManagerShareWealth*wealthRedistributionFactorTopManagers);
			
	        double incomeTaxRateWorkers=(double) Math.round((incomeRedistributionFactorWorkers/scalingFactorIncome)*10000)/10000;
	        gov.setWorkersIncomeTaxRate(incomeTaxRateWorkers*taxRatesMultiplyingFactor);
	        double wealthTaxRateWorkers=(double) Math.round((wealthRedistributionFactorWorkers/scalingFactorWealth)*10000)/10000;
	        gov.setWorkersWealthTaxRate(wealthTaxRateWorkers*taxRatesMultiplyingFactor);
	        double incomeTaxRateManagers=(double) Math.round((incomeRedistributionFactorManagers/scalingFactorIncome)*10000)/10000;
	        gov.setManagersIncomeTaxRate(incomeTaxRateManagers*taxRatesMultiplyingFactor);
	        double wealthTaxRateManagers=(double) Math.round((wealthRedistributionFactorManagers/scalingFactorWealth)*10000)/10000;
	        gov.setManagersWealthTaxRate(wealthTaxRateManagers*taxRatesMultiplyingFactor);
			double incomeTaxRateTopManagers=(double) Math.round((incomeRedistributionFactorTopManagers/scalingFactorIncome)*10000)/10000;
	        gov.setTopManagersIncomeTaxRate(incomeTaxRateTopManagers*taxRatesMultiplyingFactor);
			double wealthTaxRateTopManagers=(double) Math.round((wealthRedistributionFactorTopManagers/scalingFactorWealth)*10000)/10000;
	        gov.setTopManagersWealthTaxRate(wealthTaxRateTopManagers*taxRatesMultiplyingFactor);
			}
		
		}
	
		
	
	
}
