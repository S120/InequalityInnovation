package modelDistribution.strategies;

import java.nio.ByteBuffer;

import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.strategies.SingleStrategy;
import modelDistribution.StaticValues;
import modelDistribution.agents.Government;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.strategy.AbstractStrategy;

public class DeficitManagementStrategy extends AbstractStrategy  implements SingleStrategy{
	/**
	 * 
	 */
	private double adjustmentParameter;
	private double deficitLowerThreshold;
	private double deficitUpperThreshold;
	private double multiplyingFactor;

	
	public double computeTaxRatesMultiplyingFactor () {
		Government government = (Government) this.agent;
		MacroSimulation macroSim = (MacroSimulation)((SimulationController)this.scheduler).getSimulation();
		if (macroSim.getRound()==1){
			return 1;
		}
		else if (macroSim.getRound()==2){
			double pastPeriodGDP=government.getAggregateValue(StaticValues.LAG_NOMINALGDP, 1);
			double pastPeriodGovBalance=-(government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 1)-government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 2));
			double pastPeriodDeficit= pastPeriodGovBalance/pastPeriodGDP;
			if (pastPeriodDeficit<-this.deficitUpperThreshold){
				multiplyingFactor+=adjustmentParameter;
			}
			if (pastPeriodDeficit>-this.deficitLowerThreshold){
				multiplyingFactor-=adjustmentParameter;
			}
			return multiplyingFactor;
		}
		else {
			double pastPeriodGDP=government.getAggregateValue(StaticValues.LAG_NOMINALGDP, 1);
			double pastPeriodGovBalance=-(government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 1)-government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 2));
			double pastPeriodDeficit= pastPeriodGovBalance/pastPeriodGDP;
			double past2PeriodDebtGdp=government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 2)/government.getAggregateValue(StaticValues.LAG_NOMINALGDP, 2);
			double pastPeriodDebtGdp=government.getPassedValue(StaticValues.LAG_PUBLICDEBT, 1)/government.getAggregateValue(StaticValues.LAG_NOMINALGDP, 1);
			if (pastPeriodDeficit<-this.deficitUpperThreshold||(pastPeriodDebtGdp-past2PeriodDebtGdp)>0){
				multiplyingFactor+=adjustmentParameter;
			}
			if (pastPeriodDeficit>-this.deficitLowerThreshold&&(pastPeriodDebtGdp-past2PeriodDebtGdp)<0){
				multiplyingFactor-=adjustmentParameter;
			}
			return Math.max(0.0,Math.min(3.0,multiplyingFactor));
		}

	}
	
	
	
	
	/**
	 * @return the adjustmentParameter
	 */
	public double getAdjustmentParameter() {
		return adjustmentParameter;
	}




	/**
	 * @param adjustmentParameter the adjustmentParameter to set
	 */
	public void setAdjustmentParameter(double adjustmentParameter) {
		this.adjustmentParameter = adjustmentParameter;
	}




	/**
	 * @return the deficitLowerThreshold
	 */
	public double getDeficitLowerThreshold() {
		return deficitLowerThreshold;
	}




	/**
	 * @param deficitLowerThreshold the deficitLowerThreshold to set
	 */
	public void setDeficitLowerThreshold(double deficitLowerThreshold) {
		this.deficitLowerThreshold = deficitLowerThreshold;
	}




	/**
	 * @return the deficitUpperThreshold
	 */
	public double getDeficitUpperThreshold() {
		return deficitUpperThreshold;
	}




	/**
	 * @param deficitUpperThreshold the deficitUpperThreshold to set
	 */
	public void setDeficitUpperThreshold(double deficitUpperThreshold) {
		this.deficitUpperThreshold = deficitUpperThreshold;
	}




	/**
	 * @return the multiplyingFactor
	 */
	public double getMultiplyingFactor() {
		return multiplyingFactor;
	}




	/**
	 * @param multiplyingFactor the multiplyingFactor to set
	 */
	public void setMultiplyingFactor(double multiplyingFactor) {
		this.multiplyingFactor = multiplyingFactor;
	}




	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [haircut]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.putDouble(this.adjustmentParameter);
		buf.putDouble(this.deficitLowerThreshold);
		buf.putDouble(this.deficitUpperThreshold);
		buf.putDouble(this.multiplyingFactor);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [haircut]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.adjustmentParameter = buf.getDouble();
		this.deficitLowerThreshold = buf.getDouble();
		this.deficitUpperThreshold= buf.getDouble();
		this.multiplyingFactor = buf.getDouble();
	}
	

}
