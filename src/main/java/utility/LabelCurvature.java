package utility;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JProgressBar;

import curvatureUtils.DisplaySelected;
import curvatureUtils.PointExtractor;
import ellipsoidDetector.Distance;
import ellipsoidDetector.Intersectionobject;
import ellipsoidDetector.Tangentobject;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import kalmanForSegments.Segmentobject;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.ransac.RansacModels.*;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import pluginTools.InteractiveSimpleEllipseFit;
import ransacPoly.RegressionFunction;

public class LabelCurvature implements Runnable {

	final InteractiveSimpleEllipseFit parent;
	final RandomAccessibleInterval<FloatType> ActualRoiimg;
	List<RealLocalizable> truths;
	final int t;
	final int z;
	final int celllabel;
	int percent;
	final ArrayList<Line> resultlineroi;
	final ArrayList<OvalRoi> resultcurvelineroi;
	final ArrayList<OvalRoi> resultallcurvelineroi;
	final ArrayList<EllipseRoi> ellipselineroi;
	final ArrayList<OvalRoi> segmentrect;
	final JProgressBar jpb;
	ArrayList<Intersectionobject> AllCurveintersection;

	public LabelCurvature(final InteractiveSimpleEllipseFit parent,
			final RandomAccessibleInterval<FloatType> ActualRoiimg, List<RealLocalizable> truths,
			ArrayList<Line> resultlineroi, ArrayList<OvalRoi> resultcurvelineroi,
			ArrayList<OvalRoi> resultallcurvelineroi,ArrayList<EllipseRoi> ellipselineroi, ArrayList<OvalRoi> segmentrect,  ArrayList<Intersectionobject> AllCurveintersection, final int t,
			final int z, final int celllabel) {

		this.parent = parent;
		this.ActualRoiimg = ActualRoiimg;
		this.truths = truths;
		this.t = t;
		this.z = z;
		this.AllCurveintersection = AllCurveintersection;
		this.jpb = null;
		this.percent = 0;
		this.resultlineroi = resultlineroi;
		this.resultcurvelineroi = resultcurvelineroi;
		this.resultallcurvelineroi = resultallcurvelineroi;
		this.ellipselineroi = ellipselineroi;
		this.celllabel = celllabel;
		this.segmentrect = segmentrect;
	}

	public LabelCurvature(final InteractiveSimpleEllipseFit parent,
			final RandomAccessibleInterval<FloatType> ActualRoiimg, List<RealLocalizable> truths,
			ArrayList<Line> resultlineroi, ArrayList<OvalRoi> resultcurvelineroi,
			ArrayList<OvalRoi> resultallcurvelineroi,ArrayList<EllipseRoi> ellipselineroi,ArrayList<OvalRoi> segmentrect, ArrayList<Intersectionobject> AllCurveintersection, final int t,
			final int z, final JProgressBar jpb, final int percent, final int celllabel) {

		this.parent = parent;
		this.ActualRoiimg = ActualRoiimg;
		this.resultlineroi = resultlineroi;
		this.resultcurvelineroi = resultcurvelineroi;
		this.resultallcurvelineroi = resultallcurvelineroi;
		this.ellipselineroi = ellipselineroi;
		this.truths = truths;
		this.t = t;
		this.z = z;
		this.jpb = jpb;
		this.percent = percent;
		this.AllCurveintersection = AllCurveintersection;
		this.celllabel = celllabel;
		this.segmentrect = segmentrect;
	}

	
	private Pair<Integer, HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>>> SliderLoop(
			List<RealLocalizable> Ordered, RealLocalizable centerpoint) {

		String uniqueID = Integer.toString(z) + Integer.toString(t);
		// Get the sparse list of points
		HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>> Bestdelta = new HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>>();
		int count = 0;
		if (parent.minNumInliers > truths.size())
			parent.minNumInliers = truths.size();

		
		int increment = parent.increment;
	
		for (int i = 1; i < Ordered.size() - 1; i += increment) {

			
			if (i >= Ordered.size() - 1) {
			
				break;
				
			}

			// Get the sparse list of points

			List<RealLocalizable> allorderedtruths = Listordereing.getList(Ordered, i);

			if (parent.fourthDimensionSize > 1)
				parent.timeslider.setValue(utility.Slicer.computeScrollbarPositionFromValue(parent.fourthDimension,
						parent.fourthDimensionsliderInit, parent.fourthDimensionSize, parent.scrollbarSize));
			parent.zslider.setValue(utility.Slicer.computeScrollbarPositionFromValue(parent.thirdDimension,
					parent.thirdDimensionsliderInit, parent.thirdDimensionSize, parent.scrollbarSize));
			final int ndims = ActualRoiimg.numDimensions();
			// Make a tree of a certain depth

			int treedepth = parent.depth - 1;

			if (treedepth <= 0)
				treedepth = 0;

			CurvatureFunction computecurve = new CurvatureFunction(parent);
			
			Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>> resultpair = computecurve.getCurvature(
					allorderedtruths,centerpoint, parent.insideCutoff, parent.minNumInliers, ndims, celllabel,
					Math.abs(Math.max(parent.degree, parent.secdegree)),
					Math.abs(Math.min(parent.degree, parent.secdegree)), t, z);

			// Here counter the segments where the number of inliers was too low

			Bestdelta.put(count, resultpair);
			count++;

			parent.localCurvature = resultpair.getB();
			parent.functions = resultpair.getA();
			// Make intersection object here

		
			
			Intersectionobject currentobject = PointExtractor.CurvaturetoIntersection(parent.localCurvature,
					parent.functions, centerpoint, parent.smoothing);

			
		
			
			if (parent.maxperimeter >=currentobject.perimeter)
				parent.maxperimeter = (int)Math.round(currentobject.perimeter);
			resultlineroi.addAll(currentobject.linerois);
			if(currentobject.curvelinerois!= null)
			resultcurvelineroi.addAll(currentobject.curvelinerois);
			resultallcurvelineroi.addAll(currentobject.curvealllinerois);
			if(parent.displayIntermediate)
			ellipselineroi.addAll(currentobject.ellipselinerois);
			if(parent.displayIntermediateBox)
			segmentrect.addAll(currentobject.segmentrect);

			Roiobject currentroiobject = new Roiobject(ellipselineroi, resultallcurvelineroi, resultlineroi, resultcurvelineroi, segmentrect,
					z, t, celllabel, true);
			parent.ZTRois.put(uniqueID, currentroiobject);
			
			
			DisplayAuto.Display(parent);
			if(increment == 0)
				break;

		}
		
		return new ValuePair<Integer, HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>>>(
				count, Bestdelta);
	}

	@Override
	public void run() {
		String uniqueID = Integer.toString(z) + Integer.toString(t);
		parent.Allnodes.clear();
		parent.Nodemap.clear();
		parent.Listmap.clear();

		if (parent.fourthDimensionSize != 0 && parent.Accountedframes.size() != 0 && parent.Accountedframes != null)
			utility.ProgressBar.SetProgressBar(jpb, 100 * percent / (parent.pixellist.size()), "Computing Curvature = "
					+ t + "/" + parent.fourthDimensionSize + " Z = " + z + "/" + parent.thirdDimensionSize);
		else if (parent.thirdDimensionSize != 0 && parent.AccountedZ.size() != 0 && parent.AccountedZ != null)
			utility.ProgressBar.SetProgressBar(jpb, 100 * percent / (parent.pixellist.size()),
					"Computing Curvature T/Z = " + z + "/" + parent.thirdDimensionSize);
		else {

			utility.ProgressBar.SetProgressBar(jpb, 100 * (percent ) / (parent.pixellist.size()), "Computing Curvature ");
		}
		
		// Get the candidate points for fitting
		truths = ConnectedComponentCoordinates.GetCoordinatesBit(ActualRoiimg);
		// A Hash map for the slider loop from reference point, 0 to incremental positions
		HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>> Bestdelta = new HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>>();
		
		// Get mean co-ordinate from the candidate points
		RealLocalizable centerpoint = Listordereing.getMeanCord(truths);

		// Get the sparse list of points
		Pair<RealLocalizable, List<RealLocalizable>> Ordered = Listordereing.getOrderedList(truths);

		// Start sliding
		Pair<Integer, HashMap<Integer, Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>>> slider = SliderLoop(
				Ordered.getB(), centerpoint);

		parent.Refcord = Ordered.getA();
		Bestdelta = slider.getB();
		int count = slider.getA();
		
		// Default result
		Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>> resultpair = Bestdelta.get(0);
		
		ArrayList<Curvatureobject> RefinedCurvature = new ArrayList<Curvatureobject>();
		ArrayList<Curvatureobject> localCurvature = resultpair.getB();

		double[] X = new double[localCurvature.size()];
		double[] Y = new double[localCurvature.size()];
		double[] Z = new double[localCurvature.size()];
		double[] I = new double[localCurvature.size()];
		double[] ISec = new double[localCurvature.size()];

		ArrayList<Double> CurvePeri = new ArrayList<Double>();
		CurvePeri.add(localCurvature.get(0).perimeter);
		for (int index = 0; index < localCurvature.size(); ++index) {

			ArrayList<Double> CurveXY = new ArrayList<Double>();
			ArrayList<Double> CurveI = new ArrayList<Double>();
			ArrayList<Double> CurveISec = new ArrayList<Double>();
			
			X[index] = localCurvature.get(index).cord[0];
			Y[index] = localCurvature.get(index).cord[1];
			Z[index] = localCurvature.get(index).radiusCurvature;
			I[index] = localCurvature.get(index).Intensity;
			ISec[index] = localCurvature.get(index).SecIntensity;
			
			CurveXY.add(Z[index]);
			CurveI.add(I[index]);
			CurveISec.add(ISec[index]);
			for (int i = 1; i < count; ++i) {

				Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>> testpair = Bestdelta.get(i);

				ArrayList<Curvatureobject> testlocalCurvature = testpair.getB();

				double[] Xtest = new double[testlocalCurvature.size()];
				double[] Ytest = new double[testlocalCurvature.size()];
				double[] Ztest = new double[testlocalCurvature.size()];
				double[] Itest = new double[testlocalCurvature.size()];
				double[] ISectest = new double[testlocalCurvature.size()];
				
				CurvePeri.add(testlocalCurvature.get(0).perimeter);
				for (int testindex = 0; testindex < testlocalCurvature.size(); ++testindex) {

					Xtest[testindex] = testlocalCurvature.get(testindex).cord[0];
					Ytest[testindex] = testlocalCurvature.get(testindex).cord[1];
					Ztest[testindex] = testlocalCurvature.get(testindex).radiusCurvature;
					Itest[index] = testlocalCurvature.get(testindex).Intensity;
					ISectest[index] = testlocalCurvature.get(testindex).SecIntensity;
					if (X[index] == Xtest[testindex] && Y[index] == Ytest[testindex]) {

						CurveXY.add(Ztest[testindex]);
						CurveI.add(Itest[index]);
						CurveISec.add(ISectest[index]);
						
					}

				}

			}
		
		
			double frequdeltaperi = localCurvature.get(0).perimeter ;
			double frequdelta = Z[index];
			double intensitydelta = I[index];
			double intensitySecdelta = ISec[index];
				
				
				
				Iterator<Double> setiter = CurveXY.iterator();
				while (setiter.hasNext()) {

					Double s = setiter.next();

				frequdelta+=s;

				}

				frequdelta/=CurveXY.size();
				Iterator<Double> perisetiter = CurvePeri.iterator();
				while (perisetiter.hasNext()) {

					Double s = perisetiter.next();

				
					frequdeltaperi+=s;

				}
				
				frequdeltaperi/=CurvePeri.size();
				
				

				
				
				
				Iterator<Double> Iiter = CurveI.iterator();
				while (Iiter.hasNext()) {

					Double s = Iiter.next();

					intensitydelta+=s;

				}

				intensitydelta/=CurveI.size();
				
				

				
				Iterator<Double> ISeciter = CurveISec.iterator();
				while (ISeciter.hasNext()) {

					Double s = ISeciter.next();

					intensitySecdelta+=s;

				}

				intensitySecdelta/=CurveISec.size();
				
				
				
			
			Curvatureobject newobject = new Curvatureobject((float) frequdelta, frequdeltaperi, intensitydelta, intensitySecdelta, 
					localCurvature.get(index).Label, localCurvature.get(index).cord, localCurvature.get(index).t,
					localCurvature.get(index).z);
		
			RefinedCurvature.add(newobject);
		}

		Pair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>> Refinedresultpair = new ValuePair<ArrayList<RegressionFunction>, ArrayList<Curvatureobject>>(
				resultpair.getA(), RefinedCurvature);

		parent.localCurvature = Refinedresultpair.getB();
		parent.functions.addAll(Refinedresultpair.getA());
		// Make intersection object here

		Intersectionobject currentobject = PointExtractor.CurvaturetoIntersection(parent.localCurvature,
				parent.functions, centerpoint, parent.smoothing);

		 resultlineroi.addAll(currentobject.linerois);
		 resultcurvelineroi.addAll(currentobject.curvelinerois);
		 resultallcurvelineroi.addAll(currentobject.curvealllinerois);
		 segmentrect.addAll(currentobject.segmentrect);

		 Roiobject currentroiobject = new Roiobject(ellipselineroi, resultallcurvelineroi, resultlineroi, resultcurvelineroi, segmentrect,
					z, t, celllabel, true);
		 
		parent.ZTRois.put(uniqueID, currentroiobject);
		
		System.out.println(uniqueID + " " + segmentrect.size());
		DisplayAuto.Display(parent);
		AllCurveintersection.add(currentobject);

		parent.AlllocalCurvature.add(parent.localCurvature);

	}

}
