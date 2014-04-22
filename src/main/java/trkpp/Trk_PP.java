/*
 * Copyright (C) 2014 Indiana University
 * Author email winfrees at iupui dot edu
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
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
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
 * 
 * 
 */
package trkpp;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;



public class Trk_PP implements PlugInFilter {

 //for running as a minimal imageJ, https://github.com/imagej/minimal-ij1-plugin
 //uncomment the following main method.
    
//     public static void main(String[] args) {
//		// set the plugins.dir property to make the plugin appear in the Plugins menu
//		Class<?> clazz = Trk_PP.class;
//		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
//		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
//		System.setProperty("plugins.dir", pluginsDir);
//
//		// start ImageJ
//		new ImageJ();
//
//		// run the plugin
//		//IJ.runPlugIn(clazz.getName(), "");
//	}
    
    
    ImagePlus imp; 
    Object[] start = new Object[10];
    boolean logging = true;
    
    
    @Override
    public int setup(final String string, final ImagePlus ip) {  
    this.imp = ip;
    
    if(showDialog()){return PlugInFilter.DONE;}
    logging = start[9] != "No";
    
    if(logging){
          IJ.log("Starting tracking pre-processing...");
          IJ.log("v 0.6.0");
          IJ.log("author:  Seth Winfree, winfrees at iu dot edu, Indiana University");
          IJ.log("___________________________________________");}
    
    return DOES_8G | DOES_16 | DOES_32;
                //return DOES_8G;
    }

    @Override
    public void run(final ImageProcessor ip) {  
      try{  
      ImageStack[] stacks;
      ImagePlus impResult1, impResult2, impResult3;
      stacks = getInterleavedStacks(this.imp);
      final ResultsTable rt1 = new ResultsTable(), rt2 = new ResultsTable(); 
      //simple cross channel subtraction and blur
      //impResult1 = new ImagePlus();
      ImageCalculator ic = new ImageCalculator();  

      if((Integer)start[1] == 0 && (Integer)start[2] == 0){
          impResult1 = new ImagePlus("target" ,stacks[(Integer)start[0]-1]);
          if(logging){IJ.log("Basic crosstalk subtraction: NONE");}}
     
      else if((Integer)start[1] == 0 && (Integer)start[2] >= 1){
          impResult1 = ic.run("Subtract create stack", new ImagePlus("target" ,stacks[(Integer)start[0]-1]), new ImagePlus("C2" ,stacks[(Integer)start[2]-1]));
          if(logging){IJ.log("Basic crosstalk subtraction: Ch. " + (Integer)start[2]);}}
      
      else if((Integer)start[1] >= 1 && (Integer)start[2] == 0){
           impResult1 = ic.run("Subtract create stack", new ImagePlus("target" ,stacks[(Integer)start[0]-1]), new ImagePlus("C1" ,stacks[(Integer)start[1]-1]));
           if(logging){IJ.log("Basic crosstalk subtraction: Ch. " + (Integer)start[1]);}}
      //case4
      else {
       impResult1 = ic.run("Subtract create stack", new ImagePlus("target" ,stacks[(Integer)start[0]-1]), new ImagePlus("C1" ,stacks[(Integer)start[1]-1]));
       impResult1 = ic.run("Subtract create stack", impResult1, new ImagePlus("C2" ,stacks[(Integer)start[2]-1]));
       if(logging){IJ.log("Basic crosstalk subtraction: Ch. " + (Integer)start[1] + " then,");
       IJ.log("Basic crosstalk subtraction: Ch. " + (Integer)start[2]);}}
      
     

      if(start[3].toString().equals("Yes")){IJ.run(impResult1, "Gaussian Blur...", "sigma=2 stack");if(logging){IJ.log("Processing with gaussian blur.");}}
      //
      ImageStack isResult1;
      final ImageStack isResult2 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      final ImageStack isResult3 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      impResult1.setTitle("Processed Ch. "+((Integer)start[0]));
      impResult1.show();     
      isResult1 = impResult1.getImageStack();    
      IJ.run("Set Measurements...", "area redirect=None decimal=3");   
      final ParticleAnalyzer maskParticles = new ParticleAnalyzer(ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, 0 ,rt1, (Integer)start[7], (imp.getWidth()*imp.getHeight()));
      maskParticles.setHideOutputImage(true);  
      if(logging){IJ.log("Finding objects with threshold of: " + (Integer)start[5] + " to " + (Integer)start[6] + " with ParticleAnalyzer.");}
      if(logging){IJ.log("Limiting object to a pixel size of: " + (Integer)start[7] + " with ParticleAnalyzer.");}
      for(int n = 1; n <= stacks[2].getSize(); n++){
		final ImageProcessor ipStack = isResult1.getProcessor(n);
		ipStack.setThreshold((Integer)start[5], (Integer)start[6], ImageProcessor.RED_LUT);
		maskParticles.analyze(impResult1, ipStack);
		final ImagePlus maskImage1 = maskParticles.getOutputImage();
		final ImageProcessor maskProcessor = maskImage1.getProcessor();	
		isResult2.addSlice(maskProcessor);
      }    
      impResult2 = new ImagePlus("Mask of Regions Ch. " + ((Integer)start[0]), isResult2);
      if(logging){IJ.log("Initial dilation and maximum filter with radius of 5. ");}
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Maximum...", "radius=5 stack");
      if(logging){IJ.log("Repeated dilation for " + (Integer)start[4] + " cycles.");}
      for(Integer c = 1; c <= (Integer)start[4]; c++){IJ.run(impResult2, "Dilate", "stack");}
      if(logging){IJ.log("Filling holes in regions.");}
      IJ.run(impResult2, "Fill Holes", "stack");
      impResult2.show("Mask of regions "+ ((Integer)start[0]));
      if(logging){IJ.log("Identifying individual cells with ParticleAnalyzer.");}
      final ParticleAnalyzer maskCells = new ParticleAnalyzer(ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, Measurements.CENTER_OF_MASS | Measurements.SLICE ,rt2, 0, (imp.getWidth()*imp.getHeight()));
      maskCells.setHideOutputImage(true); 
      int count = 1;
      int end;
       for(int n = 1; n <= stacks[2].getSize(); n++){   
		final ImageProcessor ipStack = isResult2.getProcessor(n); 
		ipStack.setThreshold(150, 255, ImageProcessor.RED_LUT);
		maskCells.analyze(impResult2, ipStack);
                end = rt2.getCounter();
                for(int m = count; m <= end-1; m++ ){rt2.setValue("Slice", m, n);}
                count = end;
		final ImagePlus maskImage2 = maskParticles.getOutputImage();
		final ImageProcessor maskProcessor = maskImage2.getProcessor();	
		isResult3.addSlice(maskProcessor);
      }   
      ImageStack ResultStack = new ImageStack();     
      ResultStack = this.getCenterOfMassImage(rt2, stacks[0], ResultStack);
      if(logging){IJ.log("Generating center-of-mass image with a radius of " + (Integer)start[8] + ".");}
      impResult3 = new ImagePlus("Center-of-mass image Ch. " + ((Integer)start[0]), ResultStack);
      IJ.run(impResult3, "Maximum...", "radius="+start[8]+" stack"); 
      IJ.resetMinAndMax(impResult3);
      if(logging){IJ.log("___________________________________________");}
      impResult3.show(); }
      
      catch(NullPointerException E){IJ.showMessage("Plugin error, process cancelled."); if(logging){IJ.log("...out of memory error.");IJ.log("___________________________________________");}}
      
      
    }
    
    private ImageStack getCenterOfMassImage(ResultsTable rt, ImageStack stackOriginal, ImageStack StackResult){    
      ImagePlus impResult = NewImage.createImage("Centroids", stackOriginal.getWidth(), stackOriginal.getHeight(), stackOriginal.getSize(), 16, NewImage.FILL_BLACK);
      StackResult = impResult.getImageStack();  
     final int height = stackOriginal.getSize();
     final int rtsize = rt.getCounter();
     int XM;
     int YM;
     int slice;   
      short[] workingpixels;  
      for(int j = 0; j <= rtsize-1; j++){
          XM = (int)rt.getValueAsDouble(rt.getColumnIndex("XM"),j);
          YM = (int)rt.getValueAsDouble(rt.getColumnIndex("YM"),j);
          slice = (int)rt.getValueAsDouble(rt.getColumnIndex("Slice"),j);  
          workingpixels = (short[])StackResult.getPixels(slice);
          workingpixels[pushingpixels.PixelPositionLinearArray.translate(XM, YM, stackOriginal.getWidth(), stackOriginal.getHeight())] = 255;     
          StackResult.setPixels(workingpixels,slice);
      }
      return StackResult;
    }
    
    public ImageStack[] getInterleavedStacks(final ImagePlus imp){
	ImageStack[] stacks = new ImageStack[imp.getNChannels()];
	ImageStack stack = imp.getImageStack();
		for(int m = 0; m <= imp.getNChannels()-1; m++){
			stacks[m] = new ImageStack(imp.getWidth(), imp.getHeight());
			for(int n = m; n <= imp.getStackSize()-1; n += imp.getNChannels()){stacks[m].addSlice(stack.getProcessor(n+1));}
		}	
	return stacks;
}   
    public boolean showDialog() throws NullPointerException{
                       final String[] YesNo = {"Yes", "No"};
                       final String FileInfo = "File: " + this.imp.getTitle();   
                       final String FileDetail = "for, " + this.imp.getNChannels() + " channels, " + this.imp.getNFrames()+ " frames.";
                        String[] Channels = new String[imp.getNChannels()+1];
                        Channels[0] = "";
                        for(int i = 1; i <= imp.getNChannels(); i++) {Channels[i] = "Channel " + i;}
                        
                        
                        
                        final int SizeDilation = Prefs.getInt("trkpppref.TrkPP_4", 5);               
                        final int minThreshold = Prefs.getInt("trkpppref.TrkPP_5", 1000);
                        final double maxThreshold = Prefs.get("trkpppref.TrkPP_6", Math.pow(2,imp.getBitDepth())-1); 
                        final int minCellSize = Prefs.getInt("trkpppref.TrkPP_7", 5);
                         
                        final int FinalObjectDiameter = Prefs.getInt("trkpppref.TrkPP_8", 5);
                   
                        final GenericDialog gd = new GenericDialog("Tracking PreProcessing v0.6");
                        gd.addMessage("Preprocessing Options:");
                        gd.addMessage(FileInfo); 
                        gd.addMessage(FileDetail);
                        gd.addMessage("___________________________________________");
                        gd.addChoice("Target Channel:", Channels, "Channel 3");
                        gd.addChoice("Simple Bleed Through Correction:", Channels, "Channel 1");
                        gd.addChoice("Second Bleed Through Correction:", Channels, "");
                        gd.addRadioButtonGroup("Smooth target:", YesNo, 1, 1, Prefs.get("trkpppref.TrkPP_3", "No")); 
                        gd.addNumericField("Size dilation", SizeDilation, 0);
                        gd.addMessage("Intensity Thresholds:");
                        gd.addNumericField("Min", minThreshold, 0);
                        gd.addNumericField("Max", maxThreshold, 0);
                        gd.addNumericField("Minimum cell size", minCellSize, 0);
                        gd.addNumericField("Final object diameter", FinalObjectDiameter, 0);
                        gd.addMessage("___________________________________________");
                        gd.addRadioButtonGroup("Logging:", YesNo, 1, 1, Prefs.get("trkpppref.TrkPP_9", "Yes")); 
                        gd.addMessage("Interface for preprocessing of tracking analysis");
                        gd.addMessage("Author: Seth Winfree Indiana University   04/04/2014");
                        gd.showDialog();
                        if (gd.wasCanceled()) {return true;}
                        this.start[0] = gd.getNextChoiceIndex();
                        this.start[1] = gd.getNextChoiceIndex();
                        this.start[2] = gd.getNextChoiceIndex();
                        this.start[3] = gd.getNextRadioButton();
                        this.start[4] = (int)gd.getNextNumber();
                        this.start[5] = (int)gd.getNextNumber();
                        this.start[6] = (int)gd.getNextNumber();
                        this.start[7] = (int)gd.getNextNumber();
                        this.start[8] = (int)gd.getNextNumber();
                        this.start[9] =  gd.getNextRadioButton();                  
                        Prefs.set("trkpppref.TrkPP_0", (Integer)start[0]);
                        Prefs.set("trkpppref.TrkPP_1", (Integer)start[1]);
                        Prefs.set("trkpppref.TrkPP_2", (Integer)start[2]);
                        Prefs.set("trkpppref.TrkPP_3", start[3].toString());
                        Prefs.set("trkpppref.TrkPP_4", (Integer)start[4]);
                        Prefs.set("trkpppref.TrkPP_5", (Integer)start[5]);
                        Prefs.set("trkpppref.TrkPP_6", (Integer)start[6]);
                        Prefs.set("trkpppref.TrkPP_7", (Integer)start[7]);
                        Prefs.set("trkpppref.TrkPP_8", (Integer)start[8]);
                        Prefs.set("trkpppref.TrkPP_9", start[9].toString());
       
                        if (this.start[0] == null) {IJ.showMessage("Target channel required."); showDialog();}        
                        return false;
                }   
}
