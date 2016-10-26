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

import analyzenetworks.PreProcessor;
import analyzenetworks.SliceAnalysis;
import analyzenetworks.Preferences;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.StackCombiner;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import static ij.plugin.filter.PlugInFilter.DOES_8G;
import static ij.plugin.filter.PlugInFilter.DOES_16;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * This is a template for a plugin that requires one image to be opened, and
 * takes it as parameter.
 */
public class Analyze_Networks implements PlugInFilter {

    private ImagePlus image;
    private ImagePlus imageProcessed;
    private ImageStack isOriginal;

    private Object[] Preferences = new Object[9];
    private Calibration cal = new Calibration();
	//private ImageStack isResults;

    private String[] UnCalibratedHeadings = new String[11];
    private String[] CalibratedHeadings = new String[11];
    
    private String Version = "v0.6.5 r1  06/02/2014";


	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
     * @param imp
	 * @param image is the currently opened image
     * @return
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
    public int setup(String arg, ImagePlus imp) {

        this.image = imp;

        return DOES_8G | DOES_16;

    }

    /**
     * This method is run when the current image was accepted.
     *
     * @param ip is the current slice (typically, plugins use the ImagePlus set
     * above instead).
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {

        if (this.image.getProcessor().getBitDepth() != 8) {
            IJ.run(this.image, "8-bit", "");
        }

        this.isOriginal = this.image.getStack();
        //this.isProcessed = this.image.duplicate().getStack();
        this.cal = this.image.getCalibration();

        String[] uch = {"Slice", "Total Branch Networks", "Nodes", "Triples", "Quadruples", "Tubes", "Total Length", "Avg Length", "Tubes/Nodes", "Closed Networks", "Network size"};
        String[] ch = {"Slice", "Total Branch Networks", "Nodes", "Triples", "Quadruples", "Tubes", "Total Length(" + cal.getUnit() + ")", "Avg Length(" + cal.getUnit() + ")", "Tubes/Nodes", "Closed Networks", "Network size(" + cal.getUnit() + "^2)"};

        this.UnCalibratedHeadings = uch;
        this.CalibratedHeadings = ch;

            //Pre-process the image
        //IJ.log("Pre-processing image...");
        IJ.showStatus("Gathering settings...");

        try {
            Preferences pref = new Preferences();

            if (pref.getStatus()) {
                Preferences = pref.getPreferences();
            } else {
                IJ.showMessage("Analyze Networks cancelled");
                return;
            }

            final Date startTime = new Date();
            IJ.showStatus("PreProcessing image...");
            IJ.log("____________________________________________________");
            IJ.log("Network Analysis "+ Version);
            IJ.log("Starting network analysis on " + this.image.getTitle() + "...");
            IJ.log("Date: " + DateFormat.getDateInstance().format(new Date()));
            IJ.log("Start time: " + DateFormat.getTimeInstance().format(new Date()));

            this.imageProcessed = this.image.duplicate();
            final PreProcessor source = new PreProcessor(this.imageProcessed, Preferences);
            SliceAnalysis[] result = new SliceAnalysis[this.isOriginal.getSize()];

            for (int i = 1; i <= this.isOriginal.getSize(); i++) {
                IJ.showStatus("Processing slices...");
                IJ.showProgress(i, this.isOriginal.getSize());
                result[i - 1] = new SliceAnalysis(new ImagePlus("", source.getResult().getStack().getProcessor(i)), new ImagePlus("", source.getNetwork().getStack().getProcessor(i)), i, (Integer) Preferences[6], Preferences[8].toString());

            }

            ResultsTable rt = new ResultsTable();
            rt = calculateResults(result);
            String results_title = new String("Network Analysis for " + this.image.getTitle());

            if (Preferences[7] == "Yes") {
                results_title = new String("Calibrated Network Analysis for " + this.image.getTitle());
            };
            if (Preferences[7] == "No") {
                results_title = new String("Uncalibrated Network Analysis for " + this.image.getTitle());
            };
            rt.show(results_title);

            StackCombiner sc = new StackCombiner();

            ImagePlus fused = new ImagePlus("fused " + this.image.getTitle(), sc.combineHorizontally(source.getResult().getImageStack(), source.getNetwork().getImageStack()));
            Date finishTime = new Date();

            long totalTime = finishTime.getTime() - startTime.getTime();

            IJ.log("Finish time: " + DateFormat.getTimeInstance().format(finishTime));
            IJ.log("Processing time: " + (totalTime / 1000) + " sec");
            fused.show();
        } catch (NullPointerException npe) {
            IJ.showStatus("Plugin cancelled...");
            IJ.log("Plugin cancelled...");
        } catch (OutOfMemoryError E) {
            IJ.showStatus("ImageJ out of memory...");
            IJ.log("ImageJ out of memory...");
        }
    }

    private ResultsTable calculateResults(SliceAnalysis[] saResult) {

        ResultsTable rtResult = new ResultsTable();
        ArrayList alResult = new ArrayList(10);

        for (int i = 0; i <= this.isOriginal.getSize() - 1; i++) {
            IJ.showStatus("Gathering data...");
            IJ.showProgress(i, this.isOriginal.getSize());
            if (Preferences[7] == "Yes") {
                alResult = saResult[i].getResult();
                rtResult.incrementCounter();
                rtResult.addValue(this.CalibratedHeadings[0], i + 1); //slice
                rtResult.addValue(this.CalibratedHeadings[1], alResult.get(0).toString());
                rtResult.addValue(this.CalibratedHeadings[2], alResult.get(1).toString());
                rtResult.addValue(this.CalibratedHeadings[3], alResult.get(2).toString());
                rtResult.addValue(this.CalibratedHeadings[4], alResult.get(3).toString());
                rtResult.addValue(this.CalibratedHeadings[5], alResult.get(4).toString()); //branches
                rtResult.addValue(this.CalibratedHeadings[6], (cal.pixelWidth * Double.parseDouble(alResult.get(5).toString()))); //junctions
                rtResult.addValue(this.CalibratedHeadings[7], (cal.pixelWidth * Double.parseDouble(alResult.get(6).toString())));
                rtResult.addValue(this.CalibratedHeadings[8], alResult.get(7).toString());
                rtResult.addValue(this.CalibratedHeadings[9], alResult.get(8).toString());
                rtResult.addValue(this.CalibratedHeadings[10], (cal.pixelWidth * cal.pixelWidth * Double.parseDouble(alResult.get(9).toString())));
            }

            if (Preferences[7] == "No") {
                alResult = saResult[i].getResult();
                rtResult.incrementCounter();
                rtResult.addValue("Slice", i + 1); //slice
                rtResult.addValue("Total Branch Networks", alResult.get(0).toString());
                rtResult.addValue("Nodes", alResult.get(1).toString());
                rtResult.addValue("Triples", alResult.get(2).toString());
                rtResult.addValue("Quadruples", alResult.get(3).toString());
                rtResult.addValue("Tubes", alResult.get(4).toString()); //branches
                rtResult.addValue("Total Length", alResult.get(5).toString()); //junctions
                rtResult.addValue("Avg Length", alResult.get(6).toString());
                rtResult.addValue("Tubes/Nodes", alResult.get(7).toString());
                rtResult.addValue("Closed Networks", alResult.get(8).toString());
                rtResult.addValue("Network size", alResult.get(9).toString());
            }
        }

        return rtResult;
    }

    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Analyze_Networks.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }
}
