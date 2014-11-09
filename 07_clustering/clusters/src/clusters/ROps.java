/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author ivana
 */
public class ROps {

    
    private Rengine rEngine;
  
    public ROps() {
        // see rtest for all the hassle to get the thing to run
        System.loadLibrary("jri");
        if (!Rengine.versionCheck()) {
            System.err.println("** Version mismatch - Java files don't match library version.");
            System.exit(1);
        }
        System.out.println("Creating Rengine (with arguments)");
        // 1) we pass the arguments from the command line
        // 2) we won't use the main loop at first, we'll start it later
        //    (that's the "false" as second argument)
        // 3) the callbacks are implemented by the TextConsole class above
        String args2[] = {"--no-save"};
        // otherwise I get Fatal error: si deve specificare '--save', '--no-save' oppure '--vanilla'
        rEngine = new Rengine(args2, false, new RTextConsole());
        System.out.println("Rengine created, waiting for R");
        // the engine creates R is a new thread, so we should wait until it's ready
        if (!rEngine.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }
   }

}
