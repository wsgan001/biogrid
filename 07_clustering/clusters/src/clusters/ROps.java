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
import java.util.ArrayList;
import org.rosuda.JRI.REXP;
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
        System.out.println("Rengine up, moving on");
    }
    
    public void findClusters (ArrayList <String[]> listOfEdges) {
  
        array2dataFrame(listOfEdges, "edges");
        System.out.println("created edges data.frame");
        //
        REXP x;
        x = rEngine.eval("edges", false);
        System.out.println("edges:");
        System.out.println(x);
        x = rEngine.eval("library(igraph)"); // load the necessary library
        // create graph
        x = rEngine.eval("ig = graph.data.frame(edges, directed=FALSE, vertices=NULL)");
        x = rEngine.eval("print(ig, e=TRUE, v=TRUE)");
        // Run Girvan-Newman clustering algorithm.
        x = rEngine.eval("communities = edge.betweenness.community(ig)");
        System.out.println(x);
        x = rEngine.eval("print(communities)");
        System.out.println(x);

        
    }

    private void array2dataFrame(ArrayList<String[]> inputArray, String dataFrameName) {
       // data frame is a list of lists. for example
        //> test2 <- list( c('a','b','c'), c(a='d',b='e',c='f'))
        //> as.data.frame(test2)
        //    a b c
        //  1 a b c
        //  2 d e f
//        
//        long[] idArray = new long[2];
//        // I should not really be needing this, but
//        // the thinkg wont print without it
//        // <0 rows> (or 0-length row.names)
//        String[] rowNames = new String[3];
//        String da[] = {"ab", "cv", "mn"};
//        String db[] = {"kn", "jh", "gh"};
//        idArray[0] = rEngine.rniPutStringArray(da);
//        idArray[1] = rEngine.rniPutStringArray(db);
//        rowNames[0] = "1";
//        rowNames[1] = "2";
//        rowNames[2] = "3";
//
//        // now build a list (generic vector is how that's called in R)
//        long listOfArraysId = rEngine.rniPutVector(idArray);
//
//        // ok, we have a proper list now
//        // we could use assign and then eval "b<-data.frame(b)", but for now let's build it by hand:       
//        long rowNamesID = rEngine.rniPutStringArray(rowNames);
//        rEngine.rniSetAttr(listOfArraysId, "row.names", rowNamesID);
//
//        long keywordId = rEngine.rniPutString("data.frame");
//        rEngine.rniSetAttr(listOfArraysId, "class", keywordId);
//
//        // assign the whole thing to the "b" variable
//        rEngine.rniAssign("edges", listOfArraysId, 0);
//
//        REXP x;
//        x = rEngine.eval("print(edges)", false);
//        System.exit(1);
//        
//        
          // we are storinf the data column-wise
        // since this is quick-and-dirty thing, I'll take that the 
        // input is fair, and all entries in the input list
        // are String arrrays of the same length
        int numberOfColumns = inputArray.get(0).length;
        int numberOfRows    = inputArray.size();
        String[][] column = new String[numberOfColumns][numberOfRows];
        // we need to reformat the input
        for (int i=0; i<numberOfRows; i++) {
           String [] row = inputArray.get(i);
           for (int j=0; j<numberOfColumns; j++) {
               column[j][i] = row[j];
           }    
        }
        // now assign the columns to their counterparts in R
        long[] idArray    = new long[numberOfColumns];
        
        for (int j = 0; j< numberOfColumns; j++) {
            idArray[j] = rEngine.rniPutStringArray(column[j]);
        }
        // now build a list (generic vector is how that's called in R)
        long listOfArraysId = rEngine.rniPutVector(idArray);

       // I should not really be needing the row names, but
        // the thing wont print without it
        // <0 rows> (or 0-length row.names)
        // so I'm not sure if the things are going ok
        String[] rowNames = new String[inputArray.size()];
        for (int i=0; i<numberOfRows; i++) {
            rowNames[i] = Integer.toString(i);
        }
        
        

        // ok, we have a proper list now
        // we could use assign and then eval "b<-data.frame(b)", but for now let's build it by hand:       
        long rowNamesID = rEngine.rniPutStringArray(rowNames);
        rEngine.rniSetAttr(listOfArraysId, "row.names", rowNamesID);

        long keywordId = rEngine.rniPutString("data.frame");
        rEngine.rniSetAttr(listOfArraysId, "class", keywordId);

        // assign the whole thing to the "b" variable
        rEngine.rniAssign("edges", listOfArraysId, 0);

        REXP x;
        x = rEngine.eval("print(edges)", false);
        System.exit(1);
        
                
   }
    
    public void shutDown() {
        rEngine.end();
        //rEngine.startMainLoop();
    }

}
