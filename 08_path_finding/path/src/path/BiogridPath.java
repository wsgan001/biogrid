/*

 */
package path;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import clusters.ROps;
import java.io.PrintWriter;
import org.neo4j.graphdb.Node;


public class BiogridPath {

    // note that the neo4j server must be stopped ($NEO4J_HOME/bin/neo4j stop), otherwise we cannot start another  neo4j process
    public  static void main(String[] args) {
        String species = "homo_sapiens";
        String storeDir = "/Users/ivana/databases/neo4j/data/"+species+"_graph.db";
        String msg = dbDirCheck(storeDir);
        if (msg.length() > 0) {
            System.out.println (msg);
            System.exit(1);
        }
        Neo4jOps neoInterface = new Neo4jOps(storeDir);
        neoInterface.dbStatistics();
        
        String source = "ESR1";
        String target = "HAND2";
        int max_path_length = 4; // 5: java.lang.OutOfMemoryError: GC overhead limit exceeded
        // find all paths from source to target
        ArrayList <Node> vertexNodes =  new ArrayList <Node> ();
        ArrayList <ArrayList <String>>  pathsAsStringList =  new ArrayList <ArrayList <String>>();
        neoInterface.findPaths (source, target, max_path_length, pathsAsStringList, vertexNodes);
        // find all connections, including inter-path
        ArrayList <ArrayList<String>> retVal = neoInterface.findEdges(vertexNodes);        
        ArrayList <String> verticesAsString  = retVal.get(0);
        ArrayList <String> edgesAsString     = retVal.get(1);
        
        ArrayList <String> penultimate = new ArrayList <String>();
        
        for (ArrayList<String> pathNodes: pathsAsStringList) {
            int last = pathNodes.size() -1;
            String pen = pathNodes.get(last-1);
            if ( !penultimate.contains(pen)) penultimate.add(pen);
        }
       
        try {
            PrintWriter writer = new PrintWriter("/Users/ivana/scratch/" + species + "_clusters.txt");
            for (String pen: penultimate) {
                writer.println("cluster " +pen);
                 for (ArrayList<String> pathNodes: pathsAsStringList) {
                     int last = pathNodes.size() -1;
                     if (pen == pathNodes.get(last-1)) {
                         for (String name: pathNodes) {
                            writer.print(name+ " ");
                         }
                         writer.println();
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            System.out.printf("error opening cluster file for output: " + e.getMessage());
            System.exit(1);
        }
        
        // call R to handle clustering - I just can't get this to give me anything meaningful in this case
        //ROps rInterface = new ROps();
        //HashMap  <String,Integer> clusterMembership = rInterface.findClusters(edges, vertices);
        //rInterface.shutDown();
        
        try {
            PrintWriter writer =  new PrintWriter("/Users/ivana/scratch/" + species + "_pubmed_ids.txt");
            for (String pair: edgesAsString) {
               String[] fields = pair.split(" ");
               writer.printf("%s   %s   %s   %s\n", fields[0], fields[1], fields[2], fields[3]);
            }
            writer.close();
        } catch (Exception e) {
            System.out.printf("error opening pubmed id file for output: " + e.getMessage());
            System.exit(1);
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
