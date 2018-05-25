package ellipsoidDetector;

import net.imglib2.RealLocalizable;

public class Distance {

	/**
	 * Retruns the squared distance between two double[]'S
	 * 
	 * @param pointA
	 * @param pointB
	 * @return
	 */

	public static double DistanceSq(final double[] pointA, final double[] pointB) {

		double distance = 0;
		int numDim = pointA.length;

		for (int d = 0; d < numDim; ++d) {

			distance += (pointA[d] - pointB[d])
					* (pointA[d] - pointB[d]);

		}
		return distance;
	}
	
	/**
	 * Returns the square root of the distance between two RealLocalizables
	 * 
	 * @param pointA
	 * @param pointB
	 * @return
	 */
	public static double DistanceSqrt(final RealLocalizable pointA, final RealLocalizable pointB) {

		double distance = 0;
		
		double[] pointAA = new double[pointA.numDimensions()];
		double[] pointBB = new double[pointB.numDimensions()];
		
		pointA.localize(pointAA);
		pointB.localize(pointBB);
		
		int numDim = pointAA.length;

		for (int d = 0; d < numDim; ++d) {

			distance += (pointAA[d] - pointBB[d])
					* (pointAA[d] - pointBB[d]);

		}
		return Math.sqrt(distance);
	}
	
	/**
	 * Returns the angle between the vectors
	 * 
	 * @param pointA
	 * @param pointB
	 * @param midpoint
	 * @return
	 */
	
	public static double AngleVectors(final RealLocalizable pointA, final RealLocalizable pointB, final RealLocalizable midpoint) {
		
		
		double[] pointAA = new double[pointA.numDimensions()];
		double[] pointBB = new double[pointB.numDimensions()];
		double[] pointmid = new double[midpoint.numDimensions()];
		pointA.localize(pointAA);
		pointB.localize(pointBB);
		midpoint.localize(pointmid);
		
		final double[] vA = new double[] { pointAA[0] - pointmid[0], pointAA[1] - pointmid[1]  };
		
		final double[] vB = new double[] { pointBB[0] - pointmid[0], pointBB[1] - pointmid[1]  };
        double argument = ( vA[0] * vB[0] +  vA[1] * vB[1] )  / Math.sqrt(( vA[0] * vA[0] +  vA[1] * vA[1])  * ( vB[0] * vB[0] +  vB[1] * vB[1] ) );
		
		double angle = Math.acos(argument);
		double angledeg = Math.toDegrees(angle)%360;
		
		
		return angledeg;
	}
}
