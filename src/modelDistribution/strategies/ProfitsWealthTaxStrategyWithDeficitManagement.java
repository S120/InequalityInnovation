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

import jmab.agents.AbstractBank;
import jmab.agents.MacroAgent;
import jmab.agents.ProfitsTaxPayer;
import jmab.population.MacroPopulation;
import modelDistribution.agents.Government;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class ProfitsWealthTaxStrategyWithDeficitManagement extends AbstractStrategy implements TaxPayerStrategyWithDeficitManagement {

	private double wealthTaxRate;
	private double profitTaxRate;
	private int depositId;
	
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
	 * @param wealthTaxRate the wealthTaxRate to set
	 */
	public void setWealthTaxRate(double wealthTaxRate) {
		this.wealthTaxRate = wealthTaxRate;
	}

	/**
	 * @return the profitTaxRate
	 */
	public double getProfitTaxRate() {
		return profitTaxRate;
	}

	/**
	 * @param profitTaxRate the profitTaxRate to set
	 */
	public void setProfitTaxRate(double profitTaxRate) {
		this.profitTaxRate = profitTaxRate;
	}

	/**
	 * @return the depositId
	 */
	public int getDepositId() {
		return depositId;
	}

	/**
	 * @param depositId the depositId to set
	 */
	public void setDepositId(int depositId) {
		this.depositId = depositId;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][profitTaxRate][depositId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(20);
		buf.putDouble(wealthTaxRate);
		buf.putDouble(profitTaxRate);
		buf.putInt(depositId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][profitTaxRate][depositId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.wealthTaxRate = buf.getDouble();
		this.profitTaxRate = buf.getDouble();
		this.depositId = buf.getInt();
	}

	@Override
	public double computeTaxes(MacroAgent government) {
		ProfitsTaxPayer taxPayer = (ProfitsTaxPayer)this.getAgent();
		Government gov= (Government) government;
		double taxRatesMultiplyingFactor= gov.getTaxRatesMultiplyingFactor();
		double profits = taxPayer.getPreTaxProfits();
		double wealth=taxPayer.getNetWealth();
		if (taxPayer instanceof AbstractBank){ 
			return Math.max(wealthTaxRate*taxRatesMultiplyingFactor*wealth+profitTaxRate*taxRatesMultiplyingFactor*profits, 0);
		}
		else{
			if (wealthTaxRate*taxRatesMultiplyingFactor*wealth+profitTaxRate*taxRatesMultiplyingFactor*profits>taxPayer.getItemStockMatrix(true, depositId).getValue()){
				return 0;
			}
			else{
				return Math.max(wealthTaxRate*taxRatesMultiplyingFactor*wealth+profitTaxRate*taxRatesMultiplyingFactor*profits, 0);
				}
		}
	}
	
	
}
