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

/**
 * Trk_PP is a preprocessing plugin for modifying datasets to allow the plugin
 * Trackmate to efficiently follow cells stained either with poor cell body
 * delineation, as stained by a fluid phase marker, or extended cells with
 * multiple processes as with dendritic cells.  The plugin allows for 
 * significant preprocessing of specific channels from multichannel datasets
 * with ImageJ core functionality.
 * 
 * General algorithm 1) clean-up cross channel signal by simple image math.
 *                   2) Dilate cleaned up image.
 *                   3) Threshold, find object centroids with particle Analyzer.
 *                   4) Plot centroids per size requirements.
 */
package TrkPP;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JButton;


public class Trk_PP implements PlugInFilter {

    
    ImagePlus imp; 
    Object[] start = new Object[9];
    
    @Override
    public int setup(String string, ImagePlus ip) {  
    this.imp = ip;
    
    showDialog();
    
    return DOES_8G | DOES_16 | DOES_32;
                //return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {     
      ImageStack[] stacks;
      ImagePlus impResult1, impResult2, impResult4;
      stacks = getInterleavedStacks(this.imp);
      ResultsTable rt1 = new ResultsTable(), rt2 = new ResultsTable(); 
      ImageCalculator ic = new ImageCalculator();  
      impResult1 = ic.run("Subtract create stack", new ImagePlus("C3" ,stacks[(Integer)start[0]-1]), new ImagePlus("C1" ,stacks[(Integer)start[1]-1]));
      if((start[2].toString().equals(""))){impResult1 = ic.run("Subtract create stack", impResult1, new ImagePlus("C1" ,stacks[(Integer)start[2]-1]));}
      if(start[3].toString().equals("Yes")){IJ.run(impResult1, "Gaussian Blur...", "sigma=2 stack");}
      ImageStack isResult1;
      ImageStack isResult2 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      ImageStack isResult3 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      impResult1.setTitle("Processed "+((Integer)start[0]));
      impResult1.show();     
      isResult1 = impResult1.getImageStack();    
      IJ.run("Set Measurements...", "area redirect=None decimal=3");   
      ParticleAnalyzer maskParticles = new ParticleAnalyzer(ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, 0 ,rt1, (Integer)start[7], (imp.getWidth()*imp.getHeight()));
      maskParticles.setHideOutputImage(true);   
      for(int n = 1; n <= stacks[2].getSize(); n++){
		ImageProcessor ipStack = isResult1.getProcessor(n);
		ipStack.setThreshold((Integer)start[5], (Integer)start[6], ImageProcessor.RED_LUT);
		maskParticles.analyze(impResult1, ipStack);
		ImagePlus maskImage1 = maskParticles.getOutputImage();
		ImageProcessor maskProcessor = maskImage1.getProcessor();	
		isResult2.addSlice(maskProcessor);
      }    
      impResult2 = new ImagePlus("Pre Center of Mass Measure", isResult2);
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Maximum...", "radius=5 stack");
      for(Integer c = 1; c <= (Integer)start[4]; c++){IJ.run(impResult2, "Dilate", "stack");}
      IJ.run(impResult2, "Fill Holes", "stack");
      impResult2.show("Mask of regions "+ ((Integer)start[0]));    
      ParticleAnalyzer maskCells = new ParticleAnalyzer(ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, Measurements.CENTER_OF_MASS | Measurements.SLICE ,rt2, 0, (imp.getWidth()*imp.getHeight()));
      maskCells.setHideOutputImage(true); 
      int count = 1;
      int end;
       for(int n = 1; n <= stacks[2].getSize(); n++){   
		ImageProcessor ipStack = isResult2.getProcessor(n); 
		ipStack.setThreshold(150, 255, ImageProcessor.RED_LUT);
		maskCells.analyze(impResult2, ipStack);
                end = rt2.getCounter();
                for(int m = count; m <= end-1; m++ ){rt2.setValue("Slice", m, n);}
                count = end;
		ImagePlus maskImage2 = maskParticles.getOutputImage();
		ImageProcessor maskProcessor = maskImage2.getProcessor();	
		isResult3.addSlice(maskProcessor);
      }   
      ImageStack ResultStack = new ImageStack();     
      ResultStack = this.getCenterOfMassImage(rt2, stacks[0], ResultStack); 
      impResult4 = new ImagePlus("Center-of-mass image in channel " + ((Integer)start[0]), ResultStack);
      IJ.run(impResult4, "Maximum...", "radius="+start[8]+" stack"); 
      IJ.resetMinAndMax(impResult4);
      impResult4.show();  
    }
    
    private ImageStack getCenterOfMassImage(ResultsTable rt, ImageStack stackOriginal, ImageStack StackResult){    
      ImagePlus impResult = NewImage.createImage("Centroids", stackOriginal.getWidth(), stackOriginal.getHeight(), stackOriginal.getSize(), 16, NewImage.FILL_BLACK);
      StackResult = impResult.getImageStack();  
     int height = stackOriginal.getSize();
     int rtsize = rt.getCounter();
     int XM;
     int YM;
     int slice;   
      short[] workingpixels;  
      for(int j = 0; j <= rtsize-1; j++){
          XM = (int)rt.getValueAsDouble(rt.getColumnIndex("XM"),j);
          YM = (int)rt.getValueAsDouble(rt.getColumnIndex("YM"),j);
          slice = (int)rt.getValueAsDouble(rt.getColumnIndex("Slice"),j);  
          workingpixels = (short[])StackResult.getPixels(slice);
          workingpixels[PushingPixels.PixelPositionLinearArray.translate(XM, YM, stackOriginal.getWidth(), stackOriginal.getHeight())] = 255;     
          StackResult.setPixels(workingpixels,slice);
      }
      return StackResult;
    }
    
    public ImageStack[] getInterleavedStacks(ImagePlus imp){
	ImageStack[] stacks = new ImageStack[imp.getNChannels()];
	ImageStack stack = imp.getImageStack();
		for(int m = 0; m <= imp.getNChannels()-1; m++){
			stacks[m] = new ImageStack(imp.getWidth(), imp.getHeight());
			for(int n = m; n <= imp.getStackSize()-1; n += imp.getNChannels()){stacks[m].addSlice(stack.getProcessor(n+1));}
		}	
	return stacks;
}   
    public void showDialog() throws NullPointerException{
                       String[] YesNo = {"Yes", "No"};
                       String FileInfo = "File: " + this.imp.getTitle() + ", " + this.imp.getNChannels() + " channels, " + this.imp.getNFrames()+ " frames.";           
                        String[] Channels = new String[imp.getNChannels()+1];  
                        for(int i = 1; i <= imp.getNChannels(); i++) {Channels[0] = "";Channels[i] = "Channel " + i;}
                        

                        int SizeDilation = 5;               
                        int minThreshold = 2000;
                        double maxThreshold = Math.pow(2,imp.getBitDepth());  
                        int FinalObjectDiameter = 5;
                   
                        GenericDialog gd = new GenericDialog("Tracking PreProcessing v0.1");
                        gd.addMessage("Preprocessing Options:");
                        gd.addMessage(FileInfo); 
                        gd.add(new JButton());
                        gd.addMessage("___________________________________________");
                        gd.addChoice("Target Channel:", Channels, "Channel 2");
                        gd.addChoice("Simple Bleed Through Correction:", Channels, "Channel 1");
                        gd.addChoice("Second Bleed Through Correction:", Channels, "Do not process");
                        gd.addRadioButtonGroup("Smooth target:", YesNo, 1, 1, YesNo[0]); 
                        gd.addNumericField("Size dilation", SizeDilation, 0);
                        gd.addMessage("Intensity Thresholds:");
                        gd.addNumericField("Min", minThreshold, 0);
                        gd.addNumericField("Max", maxThreshold, 0);
                        gd.addNumericField("Minimum cell size", 15, 2);
                        gd.addNumericField("Final object diameter", FinalObjectDiameter, 0);
                        gd.addMessage("Interface for preprocessing of tracking analysis");
                        gd.addMessage("Author: Seth Winfree Indiana University   04/4/2014");
                        gd.showDialog();
                        if (gd.wasCanceled()) {return;}
                        
                        this.start[0] = gd.getNextChoiceIndex();
                        this.start[1] = gd.getNextChoiceIndex();
                        this.start[2] = gd.getNextChoiceIndex();
                        this.start[3] = gd.getNextRadioButton();
                        this.start[4] = (int)gd.getNextNumber();
                        this.start[5] = (int)gd.getNextNumber();
                        this.start[6] = (int)gd.getNextNumber();
                        this.start[7] = (int)gd.getNextNumber();
                        this.start[8] = (int)gd.getNextNumber();
                } 
    
}
