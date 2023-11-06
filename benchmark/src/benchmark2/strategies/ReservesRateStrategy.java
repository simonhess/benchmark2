package benchmark2.strategies;

import jmab2.strategies.SingleStrategy;

/**
 * @author Joeri Schasfoort interface is the basis for reserve deposit rates
 */
public interface ReservesRateStrategy extends SingleStrategy {

	public double computeReservesRate();
}
