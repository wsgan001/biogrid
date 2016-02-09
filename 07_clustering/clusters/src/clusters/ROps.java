/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.util.ArrayList;
import java.util.HashMap;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author ivana
 */
public class ROps {

    private Rengine rEngine;


    public ROps() {
        // see rtest for all the hassle to get the thing to run
        // could not get it o un until I put the libjri.so int /usr/lib 
        // /usr/lib was on java.library.path
        // can be read using String property = System.getProperty("java.library.path");
        // Also, before starting netbeans, netbeans.conf must be edited to contain pointer to R home
        // export R_HOME="/usr/lib/R"
        // also, from R itself, igraph package needs to be installed: install.packages("igraph")
        // (this takes forever)
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
    
    public HashMap <String,Integer> findClusters (ArrayList <String[]> listOfEdges, ArrayList <String[]> listOfVertices) {
  
        array2dataFrame(listOfEdges, "edges");
        System.out.println("created edges data.frame");
        array2dataFrame(listOfVertices, "vertices");
        System.out.println("created vertices data.frame");
        //
        REXP x;
        x = rEngine.eval("library(igraph)"); // load the necessary library
        // create graph
        x = rEngine.eval("ig = graph.data.frame(edges, directed=FALSE, vertices)");
        x = rEngine.eval("print(ig, e=TRUE, v=TRUE)");
        // Run Girvan-Newman clustering algorithm.
        x = rEngine.eval("communities = edge.betweenness.community(ig)");
        
        x = rEngine.eval("membership(communities)");
        // by some trial end error we discover that this works: (usin X.asList or x.asVector does not)
        // trial and error consists on inspecting the R output and noticing it looks like this:
        // [REAL* (1.0, 2.0, 3.0, 4.0, 5.0, ..., 17.0, 18.0, 4.0)]
        double[] ret3 =  x.asDoubleArray();
        HashMap  <String,Integer> clusterMembership = new HashMap<> ();
        for (String[] vertex: listOfVertices) {
            String name = vertex[0];
            int idx = listOfVertices.indexOf(vertex);
            clusterMembership.put(name, (Integer)(int)ret3[idx] );
        }
        return clusterMembership;
         
    }

    private void array2dataFrame(ArrayList<String[]> inputArray, String dataFrameName) {
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
        rEngine.rniAssign(dataFrameName, listOfArraysId, 0);
                
   }
    
    public void shutDown() {
        rEngine.end();
        //rEngine.startMainLoop();
    }

}
