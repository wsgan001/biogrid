/*

 */
package path;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import clusters.ROps;


public class BiogridPath {

    // note that the neo4j server must be stopped ($NEO4J_HOME/bin/neo4j stop), otherwise we cannot start another  neo4j process
    public  static void main(String[] args) {
        String storeDir = "/Users/ivana/databases/neo4j/data/mouse_graph.db";
        String msg = dbDirCheck(storeDir);
        if (msg.length() > 0) {
            System.out.println (msg);
            System.exit(1);
        }
        Neo4jOps neoInterface = new Neo4jOps(storeDir);
        neoInterface.dbStatistics();
        
        String source = "ESR1";
        String target = "HAND2";
        int max_path_length = 3;
        ArrayList <ArrayList<String>> retPair = neoInterface.findPaths (source, target, max_path_length);
        ArrayList <String> verticesAsString = retPair.get(0);
        ArrayList <String> edgesAsString = retPair.get(1);
        
        ROps rInterface = new ROps();
        ArrayList <String[]> vertices = new ArrayList <String[]> ();
        ArrayList <String[]> edges    = new ArrayList <String[]> ();
        
        for (String vertStr: verticesAsString) {
            String[] vertex = new String[1];
            vertex[0] = vertStr;
            vertices.add(vertex);
        }
        
        for (String pair: edgesAsString) {
            String[] edge =  pair.split("_");
            edges.add(edge);
        }
        
        HashMap  <String,Integer> clusterMembership = rInterface.findClusters(edges, vertices);
        
        for (String symbol: verticesAsString) {
            System.out.printf("%d   %s   %d \n",  verticesAsString.indexOf(symbol), symbol, clusterMembership.get(symbol));
        }      
        
        for (String[] pair: edges) {
             System.out.printf("e %d  %d \n", verticesAsString.indexOf(pair[0]), verticesAsString.indexOf(pair[1]));
        }

    }
    
    private static String dbDirCheck(String storeDir) {
        String msg = "";
        File f = new File(storeDir);
        if (!f.exists()) {
            msg = storeDir + " not found";
        } else if (!f.isDirectory()) {
            msg = storeDir + " is not a directory";
        } else if (f.list().length == 0) {
            msg = storeDir + " seems to be empty ...";
       }

        return msg;
    }
}
