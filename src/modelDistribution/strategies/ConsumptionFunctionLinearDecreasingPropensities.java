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
import jmab.strategies.ConsumptionStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ConsumptionFunctionLinearDecreasingPropensities extends AbstractStrategy
		implements ConsumptionStrategy {
	
	double persistency;
	int pastConsumptionId;
	int consPriceExpectationID; //in the config file will be specified as equal to the corresponding static field. 
    double coefficient;
	
	

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
	 * @return the coefficient
	 */
	public double getCoefficient() {
		return coefficient;
	}


	/**
	 * @param coefficient the coefficient to set
	 */
	public void setCoefficient(double coefficient) {
		this.coefficient = coefficient;
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
		double propensityOOI=(1-coefficient*(expRealIncome));
		double propensityOOW=(1-coefficient*(expRealNW));
		double desiredConsumptionOOI=0;
		double desiredConsumptionOOW=0;
		//if expected real income greater or equal than the value for which 
		//(propensityOOI*income) reaches its max (=1/2 coefficient), then desired consumption equal 
		//to the maximum (=1/4 coefficient)
		if (expRealIncome>=(1/(2*coefficient))){
			desiredConsumptionOOI=1/(4*coefficient);
		}
		// else consumption equal to propensityOOI*income
		else {
			desiredConsumptionOOI=propensityOOI*expRealIncome;
		}
		//the same for net worth
		if (expRealNW>=(1/(2*coefficient))){
			desiredConsumptionOOW=1/(4*coefficient);		
		}
		else{
			desiredConsumptionOOW=propensityOOW*expRealNW;
		}
		
		double desiredCons=desiredConsumptionOOI+desiredConsumptionOOW;
		return persistency*pastConsumption+(1-persistency)*(desiredCons);
	}
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [propensityOOW][propensityOOI][persistency][consPriceExpectationID][pastConsumptionId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.putDouble(this.coefficient);
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
		this.coefficient = buf.getDouble();
		this.persistency = buf.getDouble();
		this.consPriceExpectationID = buf.getInt();
		this.pastConsumptionId = buf.getInt();
	}

}
