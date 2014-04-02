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
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

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
            //IJ.log("Analyzing slice: " + n);
		ImageProcessor ipStack = isResult2.getProcessor(n);
		ipStack.setThreshold(150, 255, ImageProcessor.RED_LUT);
		maskCells.analyze(impResult2, ipStack);
                
                
                //end = rt2.getCounter();
                //IJ.log("Rt Counter at " + rt2.getCounter());
                //rt2.show("rt2");
                end = rt2.getCounter();
                
                //IJ.log("Starting at " +count+ " and going to" + end);
                
                for(int m = count; m <= end-1; m++ ){
                    //IJ.log("Analyzing value: " + m);
                    //IJ.log("Counter at: " + rt2.getCounter());
                rt2.setValue("Slice", m, n);         
                }
                count = end;
		ImagePlus maskImage2 = maskParticles.getOutputImage();
		ImageProcessor maskProcessor = maskImage2.getProcessor();	
		isResult3.addSlice(maskProcessor);
      }
      
      impResult2 = new ImagePlus("Regions");
      
      impResult4 = new ImagePlus("Centroid Image", getCenterOfMassImage(rt2, stacks[2].getSize()));
      
      //impResult4.setTitle("Centroid Result");
      

      
       
    }
    
    public ImageStack getCenterOfMassImage(ResultsTable rt, int height){
    
       //rt.show("results");
       IJ.log(rt.getCounter() + "objects found.");
       //IJ.log("with "+ rt.getColumnHeadings() + " headings");
     
      ImageStack is = new ImageStack(this.imp.getWidth(), this.imp.getHeight());
      
      int[] pixels = new int[this.imp.getWidth()*this.imp.getHeight()];
      
      IJ.log("Setting up pixel array to black. " + this.imp.getWidth()*this.imp.getHeight());
      
      for (int i=0; i<=this.imp.getWidth()*this.imp.getHeight()-1; i++){
          
          pixels[i] = 0;}
      IJ.log("Adding slices: " + (height));
      for (int n = 0; n <= height-1; n++){
          is.addSlice(null, pixels);
          
          
          //getProcessor((int)rt.getValueAsDouble(rt.getColumnIndex("Slice"), i)).setPixels(pixels);
      }
      
      ImagePlus imp = new ImagePlus("result", is);
       imp.show();
        
//      float[] Xm = rt.getColumn(1);
//      float[] Ym = rt.getColumn(2);
//      float[] Z = rt.getColumn(3);
//      
//      IJ.log("X: " + Xm);
//      IJ.log("Y: " + Ym);
//      IJ.log("Z: " + Z);
      


      for(int i = 0; i <= rt.getCounter()-1; i++){
          IJ.log("Making center of mass image at object: " + i);   
            is.getProcessor((int)rt.getValueAsDouble(rt.getColumnIndex("Slice"), i)).putPixelValue(rt.getColumnIndex("XM"), rt.getColumnIndex("YM"), (double)255);
      }
    
       //ImagePlus imp = new ImagePlus("result", is);
      // imp.show();
    
      return is;
    }
    
        public ImageStack[] getInterleavedStacks(ImagePlus imp){
	ImageStack[] stacks = new ImageStack[imp.getNChannels()];
	ImageStack stack = imp.getImageStack();
	
		for(int m = 0; m <= imp.getNChannels()-1; m++){
			stacks[m] = new ImageStack(imp.getWidth(), imp.getHeight());
			for(int n = m; n <= imp.getStackSize()-1; n += imp.getNChannels()){stacks[m].addSlice(stack.getProcessor(n+1));}
		}
//		IJ.log("microSetup::getInterleavedStacks           Generated stack array.");
//		IJ.log("        ImagePlus height:  " + imp.getStackSize());
//		IJ.log("        Interleaved height:  " + interleavedHeight);
		//IJ.log("        Channel count:  " + channelCount);
		//IJ.log("        Stack height:  " + stacks[0].getSize());
		
	return stacks;
} 
    
}
