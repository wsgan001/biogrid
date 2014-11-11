/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clusters;

import java.io.BufferedReader;
import java.io.FileReader;
import static java.lang.System.in;
import java.util.ArrayList;



/**
 *
 * @author ivana
 */
public class Clusters {
       
    public static void main(String[] args) {
        
        ArrayList <String> names = readNames("/Users/ivana/scratch/test.list", "\\t", 1);   
        //ArrayList <String> names = readNames("/Users/ivana/scratch/yujia.names.resolved", "\\t", 1);   
           
        Neo4jOps neoInterface = new Neo4jOps();
        //Neo4jOps.firstNbrs   (names);
        ArrayList <String[]> interactingPairs = Neo4jOps.interaction (names);       
        neoInterface.shutdownDb();
        
        ROps rInterface = new ROps();
        rInterface.findClusters(interactingPairs);
        rInterface.shutDown();
    }

    
    private static  ArrayList <String> readNames(String filename, String separator, int column) {
        String [] fields;
        ArrayList <String> names = new ArrayList ();
        try {

            BufferedReader br = new BufferedReader(new FileReader(filename));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // chop the input line into fields
                if (strLine.trim().length() ==0) continue;
                fields = strLine.trim().split(separator) ;
                names.add(fields[column-1]);
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } 
        
        return names;
    }

    
       
    
    
}
