package utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ellipsoidDetector.Intersectionobject;
import ellipsoidDetector.Tangentobject;
import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.ransac.RansacModels.Angleobject;
import net.imglib2.algorithm.ransac.RansacModels.Ellipsoid;
import net.imglib2.algorithm.ransac.RansacModels.Intersections;
import net.imglib2.algorithm.ransac.RansacModels.Tangent2D;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import pluginTools.InteractiveSimpleEllipseFit;

public class SuperIntersection {

	final InteractiveSimpleEllipseFit parent;

	public SuperIntersection(final InteractiveSimpleEllipseFit parent) {

		this.parent = parent;

	}

	public void Getsuperintersection(ArrayList<EllipseRoi> resultroi, ArrayList<OvalRoi> resultovalroi,
			ArrayList<Line> resultlineroi, final ArrayList<Tangentobject> AllPointsofIntersect,
			final ArrayList<Intersectionobject> Allintersection, int t, int z) {

		System.out.println("Super fitting post loop");

		final ArrayList<Pair<Ellipsoid, Ellipsoid>> fitmapspecial = new ArrayList<Pair<Ellipsoid, Ellipsoid>>();

		for (int index = 0; index < parent.superReducedSamples.size(); ++index) {

			for (int indexx = 0; indexx < parent.superReducedSamples.size(); ++indexx) {

				if (index != indexx) {

					fitmapspecial.add(new ValuePair<Ellipsoid, Ellipsoid>(parent.superReducedSamples.get(index).getA(),
							parent.superReducedSamples.get(indexx).getA()));

				}

			}

		}

		final ArrayList<Pair<Ellipsoid, Ellipsoid>> fitmapspecialred = new ArrayList<Pair<Ellipsoid, Ellipsoid>>();
		fitmapspecialred.addAll(fitmapspecial);
		for (int i = 0; i < fitmapspecialred.size(); ++i) {

			Pair<Ellipsoid, Ellipsoid> ellipsepairA = fitmapspecialred.get(i);

			for (int j = 0; j < fitmapspecialred.size(); ++j) {

				if (i != j) {
					Pair<Ellipsoid, Ellipsoid> ellipsepairB = fitmapspecialred.get(j);

					if (ellipsepairA.getA().hashCode() == (ellipsepairB.getB().hashCode())
							&& ellipsepairA.getB().hashCode() == (ellipsepairB.getA().hashCode())) {
						fitmapspecialred.remove(ellipsepairB);
						break;
					}

				}

			}

		}

		for (int i = 0; i < fitmapspecialred.size(); ++i) {

			Pair<Ellipsoid, Ellipsoid> ellipsepair = fitmapspecialred.get(i);

			ArrayList<double[]> pos = Intersections.PointsofIntersection(ellipsepair);

			Tangentobject PointsIntersect = new Tangentobject(pos, ellipsepair, t, z);

			for (int j = 0; j < pos.size(); ++j) {

				OvalRoi intersectionsRoi = new OvalRoi(pos.get(j)[0] - parent.radiusdetection,
						pos.get(j)[1] - parent.radiusdetection, 2 * parent.radiusdetection, 2 * parent.radiusdetection);
				intersectionsRoi.setStrokeColor(parent.colorDet);
				resultovalroi.add(intersectionsRoi);

				double[] lineparamA = Tangent2D.GetTangent(ellipsepair.getA(), pos.get(j));

				double[] lineparamB = Tangent2D.GetTangent(ellipsepair.getB(), pos.get(j));

				Angleobject angleobject = Tangent2D.GetTriAngle(lineparamA, lineparamB, pos.get(j), ellipsepair);
				resultlineroi.add(angleobject.lineA);
				resultlineroi.add(angleobject.lineB);

				Intersectionobject currentintersection = new Intersectionobject(pos.get(j), angleobject.angle,
						ellipsepair, resultlineroi, t, z);

				Allintersection.add(currentintersection);

				System.out.println("Angle: " + angleobject.angle + " " + pos.get(j)[0]);

			}

			AllPointsofIntersect.add(PointsIntersect);

		}
		String uniqueID = Integer.toString(z) + Integer.toString(t);
		
		if (!parent.redoing) {
			

			parent.ALLIntersections.put(uniqueID, Allintersection);

			// Add new result rois to ZTRois
			for (Map.Entry<String, Roiobject> entry : parent.ZTRois.entrySet()) {

				Roiobject currentobject = entry.getValue();

				if (currentobject.fourthDimension == t && currentobject.thirdDimension == z) {

					currentobject.resultroi = resultroi;
					currentobject.resultovalroi = resultovalroi;
					currentobject.resultlineroi = resultlineroi;

				}

			}

			Roiobject currentobject = new Roiobject(resultroi, resultovalroi, resultlineroi, z, t, true);
			parent.ZTRois.put(uniqueID, currentobject);

			DisplayAuto.Display(parent);
		}

		else {
			ArrayList<Intersectionobject> target = parent.ALLIntersections.get(uniqueID);
			
			// Change the entry for a give ZT
			for(Intersectionobject current: Allintersection) {
				
				
				
				double[] changepoint = current.Intersectionpoint;
				
				
				Intersectionobject changeobject = utility.NearestRoi.getNearestIntersection(target, changepoint, parent);
				target.remove(changeobject);
				target.add(current);
				
			}
			
			// Change the Rois
		
			for (Map.Entry<String, Roiobject> entry : parent.ZTRois.entrySet()) {

				Roiobject currentobject = entry.getValue();

				if (currentobject.fourthDimension == t && currentobject.thirdDimension == z) {

					
					// Find the closest resultroi
					/*
					System.out.println("Size" + resultroi.size());
					for(EllipseRoi current: resultroi) {
						
						double[] center = current.getContourCentroid();
						System.out.println(center[0]);
						EllipseRoi changeroi = utility.NearestRoi.getNearestRois(currentobject, center, parent); 
						System.out.println(changeroi.getContourCentroid()[0]);
						if(changeroi!=null) {
						currentobject.resultroi.remove(changeroi);
						currentobject.resultroi.add(current);
						}
						
						
					}
					
					/*
					for (Line current: resultlineroi) {
						
						double[] center = current.getContourCentroid();
						Line changeline = utility.NearestRoi.getNearestLineRois(currentobject, center, parent);
						
						currentobject.resultlineroi.remove(changeline);
						currentobject.resultlineroi.add(current);
					}
					
					for (OvalRoi current: resultovalroi) {
						
						double[] center = current.getContourCentroid();
						
						OvalRoi changeroi = utility.NearestRoi.getNearestIntersectionRois(currentobject, center, parent);
						
						currentobject.resultovalroi.remove(changeroi);
						currentobject.resultovalroi.add(current);
						
						
					}
					*/
					
					
					
				}
				
			//	resultroi = currentobject.resultroi;
			//	resultovalroi = currentobject.resultovalroi;
			//	resultlineroi = currentobject.resultlineroi;
			}
			
			System.out.println("I did it");
			Roiobject currentobject = new Roiobject(resultroi, resultovalroi, resultlineroi, z, t, true);
			//parent.ZTRois.put(uniqueID, currentobject);
			//parent.ALLIntersections.put(uniqueID, target);
			DisplayAuto.Display(parent);
		}

	}

}