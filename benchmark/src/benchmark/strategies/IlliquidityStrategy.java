/**
 * 
 */
package benchmark.strategies;

import jmab2.strategies.SingleStrategy;

/**
 * @author Joeri Schasfoort
 *
 */
public interface IlliquidityStrategy extends SingleStrategy {
	
	public void illiquid();
}
