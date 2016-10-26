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
import ij.IJ;
import ij.*;

/**
 * This is a template for a plugin that requires one image to
 * be opened, and takes it as parameter.
 */
public class PreProcessor {
	private ImagePlus imageOriginal;  
        private ImagePlus imageResult;
        private ImagePlus imageNetwork;
        
	
public PreProcessor(ImagePlus imp, Object[] Preferences) {
                        
                        this.imageOriginal = imp.duplicate();
                        this.imageOriginal.setTitle("Original");
                        this.imageResult = imp.duplicate();
                        
                        
                        //this.imageResult = new ImagePlus("PreProcessing Result", imp.getStack());
                        
                        IJ.log("Starting PreProcessing...");
                        
                        if((String)Preferences[10] == "Yes"){
                            
                            IJ.log("Removing gris with 3x3 convolution..." + Preferences[11] + " iterations.");
                            
                            ImagePlus imageGrid = imageResult.duplicate();
                            imageGrid.setTitle("GridRemoval");
                            for(int i = 1; 1 <= (Integer)Preferences[10]; i++)
                            {
                            IJ.run("Convolve...", "text1=[0 0 0 0 0 0\n 0 0 0 0 0\n 1 1 1 1 1\n 0 0 0 0 0\n 0 0 0 0 0\n] normalize stack");
                            }
                        }

                        if((String)Preferences[0] == "Yes"){IJ.run(imageResult, "Subtract Background...", "rolling="+Preferences[1]+" stack"); IJ.log("Subtract Background... rolling="+Preferences[1]+" stack");}
			if((String)Preferences[2] == "Yes"){IJ.run(imageResult, "Enhance Contrast...", "saturated=0.4 normalize equalize process_all");IJ.log("Enhance Contrast...  saturated=0.4 normalize equalize process_all");}
			if((String)Preferences[3] == "Yes"){IJ.run(imageResult, "Maximum...", "radius="+Preferences[4]+" stack");IJ.log("Maximum...  radius="+Preferences[4]+" stack");}
			if((String)Preferences[5] == "Yes"){IJ.run(imageResult, "Gaussian Blur...", "sigma=3 stack");IJ.log("Gaussian Blur...  sigma=3 stack");}		
			IJ.run(imageResult, "Convert to Mask", "method=" + Preferences[9].toString() +  " background=Dark calculate stack");IJ.log("Calculating Mask... method: " + Preferences[9].toString());
                        this.imageNetwork = imageResult.duplicate();
			IJ.run(imageResult, "Skeletonize", "stack");
                           
                        imp.close();
                        
                        
                        //imageOriginal.show();
			//imageResult.show();
                        //imageNetwork.setTitle("Mask Result");
                        //imageNetwork.show();
			//getResults(imp);
	}
public ImagePlus getNetwork(){return this.imageNetwork;}
public ImagePlus getResult(){return this.imageResult;}
public ImagePlus getSlice(int i){
    ImageStack is = imageResult.getStack();
    
    return new ImagePlus("Slice: " + i,is.getProcessor(i));
}

    }
