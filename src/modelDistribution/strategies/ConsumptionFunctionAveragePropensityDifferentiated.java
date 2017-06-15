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

import jmab.agents.AbstractHousehold;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.strategies.ConsumptionStrategy;
import modelDistribution.StaticValues;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ConsumptionFunctionAveragePropensityDifferentiated extends AbstractStrategy
		implements ConsumptionStrategy {
	
	double persistency;
	int pastConsumptionId;
	int consPriceExpectationID; //in the config file will be specified as equal to the corresponding static field. 
    double propensityOOIWorkers;
    double propensityOOIManagersResearchers;
    double propensityOOITopManagers;
    


	
	

	/**
	 * @return the consPriceExpectationID
	 */
	public int getConsPriceExpectationID() {
		return consPriceExpectationID;
	}


	/**
	 * @param consPriceExpectationID the consPriceExpectationID to set
	 */
	public void setConsPriceExpectationID(int consPriceExpectationID) {
		this.consPriceExpectationID = consPriceExpectationID;
	}
	
	/**
	 * @return the persistency
	 */
	public double getPersistency() {
		return persistency;
	}


	/**
	 * @param persistency the persistency to set
	 */
	public void setPersistency(double persistency) {
		this.persistency = persistency;
	}

	/**
	 * @return the pastConsumptionId
	 */
	public int getPastConsumptionId() {
		return pastConsumptionId;
	}


	/**
	 * @param pastConsumptionId the pastConsumptionId to set
	 */
	public void setPastConsumptionId(int pastConsumptionId) {
		this.pastConsumptionId = pastConsumptionId;
	}


	/**
	 * @return the propensityOOIWorkers
	 */
	public double getPropensityOOIWorkers() {
		return propensityOOIWorkers;
	}


	/**
	 * @param propensityOOIWorkers the propensityOOIWorkers to set
	 */
	public void setPropensityOOIWorkers(double propensityOOIWorkers) {
		this.propensityOOIWorkers = propensityOOIWorkers;
	}


	/**
	 * @return the propensityOOIManagersResearchers
	 */
	public double getPropensityOOIManagersResearchers() {
		return propensityOOIManagersResearchers;
	}


	/**
	 * @param propensityOOIManagersResearchers the propensityOOIManagersResearchers to set
	 */
	public void setPropensityOOIManagersResearchers(
			double propensityOOIManagersResearchers) {
		this.propensityOOIManagersResearchers = propensityOOIManagersResearchers;
	}


	/**
	 * @return the propensityOOITopManagers
	 */
	public double getPropensityOOITopManagers() {
		return propensityOOITopManagers;
	}


	/**
	 * @param propensityOOITopManagers the propensityOOITopManagers to set
	 */
	public void setPropensityOOITopManagers(double propensityOOITopManagers) {
		this.propensityOOITopManagers = propensityOOITopManagers;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.ConsumptionStrategy#computeRealConsumptionDemand()
	 */
	@Override
	public double computeRealConsumptionDemand() {
		AbstractHousehold household= (AbstractHousehold) this.getAgent(); 
		double priceExpectation=household.getExpectation(consPriceExpectationID).getExpectation();
		double pastConsumption=household.getPassedValue(pastConsumptionId, 1);
		double expRealIncome=household.getNetIncome()/priceExpectation;
		double expRealNW=household.getNetWealth()/priceExpectation;
		double desiredConsumption=0;
		if (household.getPopulationId()==StaticValues.WORKERS_ID && household.isEmployed()==true){
			desiredConsumption=propensityOOIWorkers*expRealIncome;
		}
		else if (household.getPopulationId()==StaticValues.MANAGERS_ID && household.isEmployed()==true ){
			desiredConsumption=propensityOOIManagersResearchers*expRealIncome;
		}
		else if (household.getPopulationId()==StaticValues.TOPMANAGERS_ID && household.isEmployed()==true ){
			desiredConsumption=propensityOOITopManagers*expRealIncome;
		}
		else if (household.getPopulationId()==StaticValues.RESEARCHERS_ID && household.isEmployed()==true ){
			desiredConsumption=propensityOOIManagersResearchers*expRealIncome;
		}
		else if (household.isEmployed()==false && !(household.getPopulationId()==StaticValues.TOPMANAGERS_ID)){
			desiredConsumption=expRealIncome;
		}
		else if (household.isEmployed()==false && household.getPopulationId()==StaticValues.TOPMANAGERS_ID){
			desiredConsumption=expRealIncome;
		}
		MacroSimulation macroSim = (MacroSimulation)((SimulationController)this.scheduler).getSimulation();
		if (macroSim.getRound()<2){
			return desiredConsumption;
		}
		return Math.max(persistency*pastConsumption+(1-persistency)*desiredConsumption,desiredConsumption);
		
		
	}
	
	
	
	//TODO UPDATED THE SERIALIZABLE PART HEREUNDER
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [propensityOOW][propensityOOI][persistency][consPriceExpectationID][pastConsumptionId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.putDouble(this.persistency);
		buf.putInt(this.consPriceExpectationID);
		buf.putInt(this.pastConsumptionId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [propensityOOW][propensityOOI][persistency][consPriceExpectationID][pastConsumptionId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.persistency = buf.getDouble();
		this.consPriceExpectationID = buf.getInt();
		this.pastConsumptionId = buf.getInt();
	}

}
