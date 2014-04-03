/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TrkPP;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.StackProcessor;
import java.awt.Image;
import java.awt.image.ColorModel;

/**
 *
 * @author winfrees
 */
public class TrkPP_ implements PlugInFilter {

     public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = TrkPP_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
    
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(UI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(UI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(UI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(UI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new UI().setVisible(true);
//            }
//        });
//    }
    
    ImagePlus imp; 
    
    @Override
    public int setup(String string, ImagePlus ip) {  
    this.imp = ip;
    	return DOES_8G | DOES_16 | DOES_32;
                //return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
      
      ImageStack[] stacks;
      ImagePlus impResult1, impResult2, impResult4;
      stacks = getInterleavedStacks(this.imp);
      ResultsTable rt1 = new ResultsTable();
      ResultsTable rt2 = new ResultsTable();
      
      ImageCalculator ic = new ImageCalculator();
      
      impResult1 = ic.run("Subtract create stack", new ImagePlus("C3" ,stacks[2]), new ImagePlus("C1" ,stacks[0]));
      ImageStack isResult1 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      ImageStack isResult2 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      ImageStack isResult3 = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      impResult1.setTitle("Bleed Through Subtracted");
      impResult1.show();
      
      isResult1 = impResult1.getImageStack();
      
      IJ.setThreshold(impResult1, 2090, 4038);
      IJ.run("Set Measurements...", "area redirect=None decimal=3");
      
      ParticleAnalyzer maskParticles = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, 0 ,rt1, 0, (imp.getWidth()*imp.getHeight()));
      maskParticles.setHideOutputImage(true);
      
      for(int n = 1; n <= stacks[2].getSize(); n++){
		ImageProcessor ipStack = isResult1.getProcessor(n);
		ipStack.setThreshold(2090, 4038, ImageProcessor.RED_LUT);
		maskParticles.analyze(impResult1, ipStack);
		ImagePlus maskImage1 = maskParticles.getOutputImage();
		ImageProcessor maskProcessor = maskImage1.getProcessor();	
		isResult2.addSlice(maskProcessor);
      }
      
      impResult2 = new ImagePlus("Pre Center of Mass Measure", isResult2);
      
      
      
     //impResult2.show(); 
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Maximum...", "radius=5 stack");
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Dilate", "stack");
      IJ.run(impResult2, "Fill Holes", "stack");
      
      impResult2.show("Regions grown");
      
      ParticleAnalyzer maskCells = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.SHOW_PROGRESS | ParticleAnalyzer.SHOW_MASKS, Measurements.CENTER_OF_MASS | Measurements.SLICE ,rt2, 0, (imp.getWidth()*imp.getHeight()));
      maskCells.setHideOutputImage(true);
      //impResult3 = maskParticles.getOutputImage();
      
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
      
      //impResult2 = new ImagePlus("Regions");
       
      ImageStack ResultStack = new ImageStack(); 
      
      ResultStack = this.getCenterOfMassImage(rt2, stacks[0], ResultStack);
     
      impResult4 = new ImagePlus("Centroids", ResultStack);
      //IJ.run(impResult4, "8-bit", "");
      IJ.run(impResult4, "Maximum...", "radius=5 stack");
      
      impResult4.show();
      
      //impResult4.setTitle("Centroid Result");
      

      
       
    }
    
    private ImageStack getCenterOfMassImage(ResultsTable rt, ImageStack stackOriginal, ImageStack StackResult){
    
       
      IJ.log(rt.getCounter() + "objects found.");
       
      ImagePlus impResult = NewImage.createImage("Centroids", stackOriginal.getWidth(), stackOriginal.getHeight(), stackOriginal.getSize(), 16, NewImage.FILL_BLACK);
      StackResult = impResult.getImageStack();
     
      
     int height = stackOriginal.getSize();
     int rtsize = rt.getCounter();
     int XM = 0;
     int YM = 0;
     int slice = 0;
     
     
     
      //short[] pixels = new short[stackOriginal.getWidth()*stackOriginal.getHeight()];
      
      //for(int i = 0; i <= (stackOriginal.getWidth()*stackOriginal.getHeight())-1; i++){pixels[i] = 0;}
      //for(int h = 1; h <= height; h++){StackResult.addSlice("1", pixels);}
      
      short[] workingpixels = new short[stackOriginal.getWidth()*stackOriginal.getHeight()];


      
      //add specific points
      
      for(int j = 0; j <= rtsize-1; j++){
       
          
          IJ.log("center mass: " + (int)rt.getValueAsDouble(rt.getColumnIndex("XM"), j)    + ", " + (int)rt.getValueAsDouble(rt.getColumnIndex("YM"), j)  + ", " + (int)rt.getValueAsDouble(rt.getColumnIndex("Slice"), j));
          XM = (int)rt.getValueAsDouble(rt.getColumnIndex("XM"),j);
          YM = (int)rt.getValueAsDouble(rt.getColumnIndex("YM"),j);
          slice = (int)rt.getValueAsDouble(rt.getColumnIndex("Slice"),j);  
         
          //lets do pixel array math, to hell with imageprocessors
          
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
    
}
