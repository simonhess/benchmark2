package benchmark.strategies;

import jmab.strategies.SingleStrategy;

/**
 * @author Joeri Schasfoort interface is the basis for reserve deposit rates
 */
public interface ReservesRateStrategy extends SingleStrategy {

	public double computeReservesRate();
}
