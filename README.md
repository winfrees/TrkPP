TrkPP

=======================

This is a minimal Maven project implementing an ImageJ 1.x plugin Trk_PP

track-preprocessing is a preprocessing plugin for modifying datasets to allow the ImageJplugin Trackmate 
to efficiently follow cells stained either with poor cell bodydelineation, as stained by a fluid phase marker, 
or extended cells with multiple processes as with dendritic cells.  The plugin allows for significant preprocessing 
of specific channels from multichannel datasets with ImageJ core functionality.

 General algorithm 1) clean-up cross channel signal by simple image math.
                   2) Dilate cleaned up image.
                   3) Threshold, find object centroids with particle Analyzer.
                   4) Plot centroids per size requirements.
                   
Please direct any questions or comments to https://github.com/icbm-iupui/track-processing/ or winfrees@iu.edu.

