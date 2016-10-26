/*
 * Copyright (C) 2014 Indiana University
 * Authors email winfrees at iupui dot edu
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
/**
 *
 * @author Seth Winfree <Seth Winfree at Indiana University>
 */
package analyzenetworks;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.IJ;
import static ij.measure.Measurements.AREA;
import static ij.measure.Measurements.LIMIT;
import ij.plugin.filter.ParticleAnalyzer;

import static ij.plugin.filter.ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
import static ij.plugin.filter.ParticleAnalyzer.SHOW_PROGRESS;
import static ij.process.ImageProcessor.RED_LUT;

import skeleton_analysis.*;
import java.util.ArrayList;

public class SliceAnalysis {

    private ArrayList Results;
    AnalyzeSkeleton_ skel = new AnalyzeSkeleton_();
    //private ImagePlus image;
    //private ImagePlus imageNetwork;
    //private ImageStack stackNetwork;
    //private ImagePlus imageSkeleton;
    //private ImageProcessor imp;

    final int NETWORKAREA = 2;
    final int NETWORKCOUNT = 1;

    public SliceAnalysis() {
    }

    public SliceAnalysis(ImagePlus impSkeleton, ImagePlus impMask, int slice, int minSize, String ExcludeOnEdge) {

        ArrayList alResult = new ArrayList();

        int isWidth = impSkeleton.getWidth();
        int isHeight = impSkeleton.getHeight();

        ImagePlus impAnalysis = new ImagePlus("", impMask.duplicate().getProcessor());

        IJ.showStatus("Analyzing Skeleton...");
        skel.setup("", impSkeleton);
        SkeletonResult Output = skel.run(AnalyzeSkeleton_.NONE, false, false, null, true, true);

//		Analyze_Skeleton_ generates a SkeletonResult class that contains the
//                values of interest by skeleton.  Whereby array position is each skeleton.
//                Thus, to get totals like:
//
//                # of nodes
//                # of Tubes
//                # of Branches
//
//                they must be summed across all skeletons
                   // IJ.log("                ...Extracting values");
        int[] nodes = Output.getJunctions();
        int[] triples = Output.getTriples();
        int[] quadruples = Output.getQuadruples();
        int[] branches = Output.getBranches();
        double[] branchLengths = Output.getAverageBranchLength();

        int countSkeletons = Output.getJunctions().length; //skeletons
        int countNodes = 0; //junctions
        int countTriples = 0; //3 way junction
        int countQuadruples = 0; //4way junction
        int countBranches = 0; //slabs
        double totalTubeLength = 0; //branches

        int countNetwork = 0;
        int averageSizeNetwork = 0;

                //Output.;
		//NEW AND COOL IDEAS// distribution characteristics?  statistical test of branch lengths
        //NEW AND COOL IDEAS// average network size with time per network, object tracking/growing etc.
        //NEW AND COOL IDEAS// multiparametric analysis a la VTC, machine learning etc.
        IJ.showStatus("Summarizing skeleton and networks...");
        for (int i = 0; i <= countSkeletons - 1; i++) {

            countNodes = countNodes + nodes[i]; //junctions
            countTriples = countTriples + triples[i];
            countQuadruples = countQuadruples + quadruples[i];
            countBranches = countBranches + branches[i];
            totalTubeLength = totalTubeLength + branchLengths[i] * branches[i];
        }

        alResult.add(countSkeletons); //contiguous network (not nesc. closed)
        alResult.add(countNodes); //junctions
        alResult.add(countTriples); //3 way junctions
        alResult.add(countQuadruples); //4 way junctions
        alResult.add(countBranches); //branches
        alResult.add(totalTubeLength); //length summation
        alResult.add((double) totalTubeLength / countSkeletons); //average tube length
        alResult.add((double) countBranches / countNodes); //ratio
        alResult.add((int) calculateClosedNetworkVariables(impAnalysis, this.NETWORKCOUNT, minSize, ExcludeOnEdge)); //sum of closed networks
        alResult.add((double) calculateClosedNetworkVariables(impAnalysis, this.NETWORKAREA, minSize, ExcludeOnEdge)); //average size of closed networks

        impAnalysis.flush();
        impSkeleton.flush();
        impMask.flush();

        this.Results = alResult;
    }

    @Deprecated
    private int calculateClosedNetworkCount(ImagePlus imp_pass) {

        ResultsTable rt = new ResultsTable();

        IJ.run(imp_pass, "Invert", "");
        IJ.setThreshold(imp_pass, 255, 255);

        ParticleAnalyzer pa = new ParticleAnalyzer(EXCLUDE_EDGE_PARTICLES & LIMIT, AREA, rt, 0, Double.POSITIVE_INFINITY, 0, 1);
        pa.analyze(imp_pass);

        //IJ.log("Network count:" + rt.getCounter());
        return rt.getCounter();

    }

    ;


    private double calculateClosedNetworkVariables(ImagePlus impMask, int choice, int minSize, String ExcludeOnEdge) {

        ResultsTable rt = new ResultsTable();
        int countRt = 0;
        int Edge;

        double networkArea = 0;

        if (choice == this.NETWORKCOUNT) {
            impMask.getProcessor().invert();
            impMask.getProcessor().setThreshold(255, 255, RED_LUT);
        }
        if (ExcludeOnEdge.equals("Yes")) {
            Edge = EXCLUDE_EDGE_PARTICLES;
        } else {
            Edge = SHOW_PROGRESS;
        }

        ParticleAnalyzer pa = new ParticleAnalyzer(Edge, AREA, rt, minSize, Double.POSITIVE_INFINITY, 0, 1);
        pa.analyze(impMask);
        countRt = rt.getCounter();

        //rt.show("Slice");
        for (int c = 0; c <= countRt - 1; c++) {

            networkArea = networkArea + rt.getValueAsDouble(0, c);
        }

        if (choice == this.NETWORKAREA) {
            return (double) networkArea / countRt;
        }
        if (choice == this.NETWORKCOUNT) {
            return (double) countRt;
        }

        return 0.0;
    }

    ;


    public ArrayList getResult() {
        return this.Results;
    }

}
