package se.kth.jabeja;

import org.apache.log4j.Logger;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Jabeja {
    final static Logger logger = Logger.getLogger(Jabeja.class);
    private final Config config;
    private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
    private final List<Integer> nodeIds;
    private int numberOfSwaps;
    private int round;
    private float T;
    private boolean resultFileCreated = false;
    Random random = new Random();
    private float momentum = 1.0f;
    private float prevBenefit = 0;
    private float dT = 0;
    private float T_prev;

    //-------------------------------------------------------------------
    public Jabeja(HashMap<Integer, Node> graph, Config config) {
        this.entireGraph = graph;
        this.nodeIds = new ArrayList(entireGraph.keySet());
        this.round = 0;
        this.numberOfSwaps = 0;
        this.config = config;
        this.T = config.getTemperature();
    }


    //-------------------------------------------------------------------
    public void startJabeja() throws IOException {
        T = 1;

        for (round = 0; round < config.getRounds()+2000; round++) {
            for (int id : entireGraph.keySet()) {
                sampleAndSwap(id);
                if (round % 400 == 0) T=1; //restart
            }

            //one cycle for all nodes have completed.
            //reduce the temperature



            saCoolDown();
            report();
        }
    }

    /**
     * Simulated analealing cooling function
     */
    private void saCoolDown(){
        // TODO for second tasl
//        task 1
//        if (T > 1)
//            T *= 0.95f;
//        if (T < 1)
//            T = 1;

////        task 2 momentums
//        T_prev = T;
//        T = (float) (T - config.getDelta() - T*config.getDelta() - (round * dT/Math.pow(Math.E, round)));
//        dT = T - T_prev;
//
//      if (T <= 0.0000001f) T = 0.0000001f;
        T = (float) (T * Math.pow(config.getDelta(), T/round));
        //bonus
//        double T_min = 0.0001;
//        if (T > T_min) {
//          T = T * 0.9f;
//        } else {
//          T = (float) T_min;
//        }
    }

    /**
     * Sample and swap algorith at node p
     * @param nodeId
     */
    private void sampleAndSwap(int nodeId) {
        Node partner = null;
        Node nodep = entireGraph.get(nodeId);



        if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
                || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
            // swap with random neighbors
            //row 3 pseudocode
            partner = findPartner(nodeId, getNeighbors(nodep));

        }

        if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
                || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM) {
            // if local policy fails then randomly sample the entire graph
            // row 5 pseudocode
            if (partner == null) {
                partner = findPartner(nodeId, getSample(nodeId));
            }
        }

        // swap the colors
        if (partner != null) {
            int colorp = nodep.getColor();
            nodep.setColor(partner.getColor());
            partner.setColor(colorp);
            numberOfSwaps++;
        }
    }

    public Node findPartner(int nodeId, Integer[] nodes){
        Node nodep = entireGraph.get(nodeId);

        Node bestPartner = null;
        double highestBenefit = 0;

        double alpha = config.getAlpha();

        for (int qid: nodes) {
            Node nodeq = entireGraph.get(qid);

            if (nodeq.getColor() == nodep.getColor()) continue; //skip if same color

            int dpp = getDegree(nodep, nodep.getColor());
            int dqq = getDegree(nodeq, nodeq.getColor());
            double oldE = Math.pow(dpp, alpha) + Math.pow(dqq, alpha);

            int dpq = getDegree(nodep, nodeq.getColor());
            int dqp = getDegree(nodeq, nodep.getColor());

            double newE = Math.pow(dpq, alpha) + Math.pow(dqp, alpha);

            //Task 1 done:
//            if ((newE * this.T > oldE) && (newE > highestBenefit)) {
//                bestPartner = nodeq;
//                highestBenefit = newE;
//            }


            //Task 2, simulated anealing
//            double acceptProbability = Math.pow(Math.E, (newE - oldE) / T); //original annealing
            double acceptProbability = 1 / (1 + Math.pow(Math.E, (oldE - newE) / T));
            double r = random.nextDouble();
//            System.out.println("r=" + r);
//            System.out.println("ap = " + acceptProbability);
            if ((acceptProbability > r) && (newE > highestBenefit)) {
                bestPartner = nodeq;
                highestBenefit = newE;
            }


            //Bonus
//            momentum = Math.max(0, momentum * (float)(newE - prevBenefit));
//
//            double acceptProbability = Math.pow(Math.E, (newE + momentum - oldE)/T);
//
//            if ((acceptProbability > random.nextDouble()) && (newE > highestBenefit)) {
//                bestPartner = nodeq;
//                highestBenefit = newE;
//            }

            }

//            prevBenefit = (float) highestBenefit;
            return bestPartner;
        }

        /**
         * The the degreee on the node based on color
         * @param node
         * @param colorId
         * @return how many neighbors of the node have color == colorId
         */
        private int getDegree(Node node, int colorId){
            int degree = 0;
            for(int neighborId : node.getNeighbours()){
                Node neighbor = entireGraph.get(neighborId);
                if(neighbor.getColor() == colorId){
                    degree++;
                }
            }
            return degree;
        }

        /**
         * Returns a uniformly random sample of the graph
         * @param currentNodeId
         * @return Returns a uniformly random sample of the graph
         */
        private Integer[] getSample(int currentNodeId) {
            int count = config.getUniformRandomSampleSize();
            int rndId;
            int size = entireGraph.size();
            ArrayList<Integer> rndIds = new ArrayList<Integer>();

            while (true) {
                rndId = nodeIds.get(RandNoGenerator.nextInt(size));
                if (rndId != currentNodeId && !rndIds.contains(rndId)) {
                    rndIds.add(rndId);
                    count--;
                }

                if (count == 0)
                    break;
            }

            Integer[] ids = new Integer[rndIds.size()];
            return rndIds.toArray(ids);
        }

        /**
         * Get random neighbors. The number of random neighbors is controlled using
         * -closeByNeighbors command line argument which can be obtained from the config
         * using {@link Config#getRandomNeighborSampleSize()}
         * @param node
         * @return
         */
        private Integer[] getNeighbors(Node node) {
            ArrayList<Integer> list = node.getNeighbours();
            int count = config.getRandomNeighborSampleSize();
            int rndId;
            int index;
            int size = list.size();
            ArrayList<Integer> rndIds = new ArrayList<Integer>();

            if (size <= count)
                rndIds.addAll(list);
            else {
                while (true) {
                    index = RandNoGenerator.nextInt(size);
                    rndId = list.get(index);
                    if (!rndIds.contains(rndId)) {
                        rndIds.add(rndId);
                        count--;
                    }

                    if (count == 0)
                        break;
                }
            }

            Integer[] arr = new Integer[rndIds.size()];
            return rndIds.toArray(arr);
        }


        /**
         * Generate a report which is stored in a file in the output dir.
         *
         * @throws IOException
         */
        private void report() throws IOException {
            int grayLinks = 0;
            int migrations = 0; // number of nodes that have changed the initial color
            int size = entireGraph.size();

            for (int i : entireGraph.keySet()) {
                Node node = entireGraph.get(i);
                int nodeColor = node.getColor();
                ArrayList<Integer> nodeNeighbours = node.getNeighbours();

                if (nodeColor != node.getInitColor()) {
                    migrations++;
                }

                if (nodeNeighbours != null) {
                    for (int n : nodeNeighbours) {
                        Node p = entireGraph.get(n);
                        int pColor = p.getColor();

                        if (nodeColor != pColor)
                            grayLinks++;
                    }
                }
            }

            int edgeCut = grayLinks / 2;

            logger.info("round: " + round +
                    ", edge cut:" + edgeCut +
                    ", swaps: " + numberOfSwaps +
                    ", migrations: " + migrations + ", T: " + T);

            saveToFile(edgeCut, migrations);
        }

        private void saveToFile(int edgeCuts, int migrations) throws IOException {
            String delimiter = "\t\t";
            String outputFilePath;

            //output file name
            File inputFile = new File(config.getGraphFilePath());
            outputFilePath = config.getOutputDir() +
                    File.separator +
                    inputFile.getName() + "_" +
                    "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
                    "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
                    "T" + "_" + config.getTemperature() + "_" +
                    "D" + "_" + config.getDelta() + "_" +
                    "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
                    "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
                    "A" + "_" + config.getAlpha() + "_" +
                    "R" + "_" + config.getRounds() + ".txt";

            if (!resultFileCreated) {
                File outputDir = new File(config.getOutputDir());
                if (!outputDir.exists()) {
                    if (!outputDir.mkdir()) {
                        throw new IOException("Unable to create the output directory");
                    }
                }
                // create folder and result file with header
                String header = "# Migration is number of nodes that have changed color.";
                header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
                FileIO.write(header, outputFilePath);
                resultFileCreated = true;
            }

            FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
        }
    }
