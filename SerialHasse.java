import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class SerialHasse {
    public static void main(String[] args) {
        Top_Down td = new Top_Down();
        Bottom_Up bu = new Bottom_Up();
        
		 long start,end;
        int n = 24;
        start = System.nanoTime();
        td.generateHasseDiagram(n);
        end = System.nanoTime();
        long tdParTime = (end - start) / 1_000_000;
        System.out.println("*******************************Top-Down  Time (ms): ********************************" + tdParTime);
       
        start = System.nanoTime();
        bu.generateHasseDiagram(n);
        end = System.nanoTime();
        long buParTime = (end - start) / 1_000_000;
        System.out.println("Bottom-Up  Time (ms): " + buParTime);
    }
}


class Node {
    private int nodeId;
    private int elements[];
    
    public Node() {
        
    }

    public Node(int nodeId, int[] elements) {
        this.nodeId = nodeId;
        this.elements = elements.clone();
    }

    public Node(Node node) {
        this.nodeId = node.nodeId;
        this.elements = node.elements.clone();
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public int[] getElements() {
        return this.elements;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setElements(int[] elements) {
        this.elements = elements.clone();
    } 

    @Override
    public String toString() {
        return "Node{" + "nodeId=" + this.nodeId + ", elements=" + Arrays.toString(this.elements) + '}';
    }
}


class Node_List {
    private Node node;
    private LinkedList<Integer> adjList;
    
    public Node_List() {
        this.node = new Node();
        this.adjList = new LinkedList();
    }

    public Node_List(Node node, LinkedList adjList) {
        this.node = new Node(node);
        this.adjList = new LinkedList();
        Collections.copy(this.adjList, adjList);
    }

    public Node getNode() {
        return this.node;
    }

    public LinkedList getAdjList() {
        return this.adjList;
    }

    public void setNode(Node node) {
        this.node = new Node(node);
    }

    public boolean add(Integer id) {
        return adjList.add(id);
    }

    public void clear() {
        adjList.clear();
    }

    public void setAdjList(LinkedList adjList) {
        this.adjList = new LinkedList();
        Collections.copy(this.adjList, adjList);
    }

    @Override
    public String toString() {
        return "Node_List{" + "node=" + this.node + ", adjList=" + this.adjList + '}';
    }
}

class SearchInfo {
    private boolean isPresent;
    private int id;

    public SearchInfo() {
        
    }

    public SearchInfo(boolean isPresent, int id) {
        this.isPresent = isPresent;
        this.id = id;
    }

    public boolean isIsPresent() {
        return isPresent;
    }

    public int getId() {
        return id;
    }

    public void setIsPresent(boolean isPresent) {
        this.isPresent = isPresent;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SearchInfo{" + "isPresent=" + isPresent + ", id=" + id + '}';
    }
}


class Trie {
    // Root of Trie 
    TrieNode root; 
  
    // Constructor 
    Trie() {  
        this.root = null; 
    } 
    
    /* Insert partition P whose id is 'id' into the trie */
    void insert(int[] P, int n, int id) {
        int k = P.length; 
        int required_sum = n;
        int level = 0; 
        int index, max, min, noChildren;
        
        /* If the trie is empty, return a new node */
        if (this.root == null) { 
            int key = -1;
            max = required_sum - (k-level) + 1;
            min = (int)Math.ceil(required_sum/(k-level));
            noChildren = max - min + 1;
            this.root = new TrieNode(key, noChildren); // -1 to denote it is root
        } 
        TrieNode trieNode = this.root; 

        for (level = 0; level < k-1; level++)  {
            int key = P[level];
            min = (int)Math.ceil(required_sum/(k-level));
            index = key - min;
            
            required_sum = required_sum - key;
            if (trieNode.children[index] == null) {
                max = required_sum - (k-level-1) + 1;
                min = (int)Math.ceil(required_sum/(k-level-1));
                noChildren = max - min + 1;
                trieNode.children[index] = new TrieNode(key,noChildren);
            }
            trieNode = trieNode.children[index];   
        }
        int key = P[level];
        index = 0;
        trieNode.children[index] = new TrieNode(key,0, true, id);
    } 

    
    /* Search a partition P into the m-ary tree */
    SearchInfo search(int[] P, int n) {
        int level; 
        int k = P.length; 
        int required_sum = n;
        int index; 
        
        TrieNode trieNode = root; 
       
        for (level = 0; level < k; level++) {
            //index = P[level]; 
            int key = P[level];
            int min = (int)Math.ceil(required_sum/(k-level));
            index = key - min;
            
            required_sum = required_sum - key;
            if (trieNode.children[index] == null) {
                SearchInfo si = new SearchInfo(false, -1);
                return si; 
            }
            trieNode = trieNode.children[index]; 
        } 
       
        if (trieNode == null) {
            SearchInfo si = new SearchInfo(false, -1);
            return si; 
        } else {
            SearchInfo si = new SearchInfo(true, trieNode.Id);
            return si; 
        }
    }  
}

class TrieNode {
    int key; 
    TrieNode[] children; 
    boolean isLeaf;
    int Id;
  
    public TrieNode() { 
        this.isLeaf = false;
        this.Id = -1;
    } 

    public TrieNode(int key) { 
        this.key = key; 
        this.isLeaf = false;
        this.Id = -1;
    }   
    
    public TrieNode(int key, int size) { 
        this.key = key; 
        this.children = new TrieNode[size];
        this.isLeaf = false;
        this.Id = -1;
    } 

    public TrieNode(int key, int size, boolean isleaf, int id) { 
        this.key = key; 
        this.children = new TrieNode[size];
        this.isLeaf = true;
        this.Id = id;
    } 
    
    public TrieNode(TrieNode trieNode) { 
        this.key = trieNode.key; 
        this.children = trieNode.children.clone();
        this.isLeaf = trieNode.isLeaf;
        this.Id = trieNode.Id;
    } 
    
    @Override
    public String toString() {
        return "TrieNode{" + "key=" + key + ", children=" + children + ", isLeaf=" + isLeaf + ", Id=" + Id + '}';
    }
}


class Top_Down {
    public void generateHasseDiagram (int n) {
        // Calculate the number of partitions using Hardy-Ramanujan Asymptotic Partition Formula
        double pn_numerator = Math.PI * Math.sqrt((double)2*n/(double)3);
        double pn_denominator = 4*n* Math.sqrt(3);
        int pn = (int) Math.ceil(Math.exp(pn_numerator) / pn_denominator);
        
        System.out.println("Number of partions by Hardy-Ramanujan Asymptotic Partition Formula = " + pn);
        
        // Initialize the number of nodes and edges in the Hasse Diagram
        int noNodes = 0;
        int noEdges = 0;
        
        int count = 0;
        
        /* Adjancy list representation of the Hasse Diagram
         * We have considered: Node as well as the adjancy list 
         * Advantage: Partition can be accessed in constant time given the id for that corresponding partition
         * (Considered the number of partitions given by 
         * Hardy-Ramanujan Asymptotic Partition Formula)
         * This will waste some space */ 
        Node_List[] nodeCumAdjList = new Node_List[pn];
        for(int i = 0; i < pn; i++) {
            nodeCumAdjList[i] = new Node_List();
        }

        /* First node of the Hasse Diagram */
        int id = 0;
        int[] elements = {n};
        Node node = new Node(id, elements);
        nodeCumAdjList[id].setNode(node);
        
        Queue<Integer> Q = new LinkedList();
        // Insert node id into the queue
        noNodes++;
        Q.add(id);

        // Initialize the Trie
        Trie trie = new Trie();
        
        int prevRank = 0;
        
        while(!Q.isEmpty()) {
            Node curNode = new Node();
            int curPartitionId = Q.poll();
            curNode = nodeCumAdjList[curPartitionId].getNode();
            int curRank = curNode.getElements().length;
            
            /* All the partitions of rank 'prevRank (=curRank-1)' have been processed
             * So reinitialize the Trie */
            if(prevRank != curRank) { 
                trie = new Trie();
            }
            if(curRank == n) { // All the partition have been generated
                break;
            }
            
            /* Generate all the partitions of rank curRank+1 from a partition curNode.getElements() of 
             * rank curRank */
            for(int i = 0; i < curRank; i++) {
                if ((i == 0) || (curNode.getElements()[i] != curNode.getElements()[i-1]) ) {
                    int[] newPartition = new int[curRank + 1];
                    if(curRank != 1) {
                        System.arraycopy(curNode.getElements(), 0, newPartition, 0, i);
                        System.arraycopy(curNode.getElements(), i+1, newPartition, i, curRank-(i+1));
                    }
                    for(int j = 1; j <= curNode.getElements()[i]/2; j++) {
                        newPartition[newPartition.length-2] = j;
                        newPartition[newPartition.length-1] = curNode.getElements()[i]-j;
                        int[] sortedNewPartition = new int[newPartition.length];
                        sortedNewPartition = countingSort(newPartition); // Sort in non-increasing order
                        
                        /* Chcek whether the generated partition has been alreday generated or not
                         using Trie data structure */
                        SearchInfo si = new SearchInfo();
                        if(trie.root == null) {
                            si = new SearchInfo(false, -1);
                        } else {
                            si = trie.search(sortedNewPartition, n);
                        }
                        noEdges++;
                        if(si.isIsPresent()) {
                           int existingNodeid = si.getId();
                           nodeCumAdjList[curPartitionId].add(existingNodeid);   
                           nodeCumAdjList[existingNodeid].add(curPartitionId); 
                        } else {
                            id++;   
                            node = new Node(id, sortedNewPartition);
                            nodeCumAdjList[id].setNode(node);   
                            nodeCumAdjList[curPartitionId].add(id);   
                            nodeCumAdjList[id].add(curPartitionId);   
                            trie.insert(sortedNewPartition, n, id);
                            noNodes++;
                            Q.add(id);
                        }
                    }
                }
            }  
            prevRank = curRank;
        }
        System.out.println("Number of nodes = " + noNodes);
        System.out.println("Number of edges = " + noEdges);
        for(int i = 0; i < noNodes; i++) {
            System.out.println("Adjacency List: " + nodeCumAdjList[i]);
        }
    }

   
    
    
    public int[] countingSort(int[] array) { 
        int[] aux = new int[array.length];

        // Find the smallest and the largest value in the array
        int min = array[0];
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            } else if (array[i] > max) {
                max = array[i];
            }
        }

        // Initialize array of frequencies
        int[] counts = new int[max - min + 1];

        // Initialize the frequencies
        for (int i = 0;  i < array.length; i++) {
          counts[array[i] - min]++;
        }

        // Recalculate the array - create the array of occurences
        counts[0]--;
        for (int i = 1; i < counts.length; i++) {
          counts[i] = counts[i] + counts[i-1];
        }

        for (int i = array.length - 1; i >= 0; i--) {
            aux[counts[array[i] - min]--] = array[i];
        }
        
        int[] auxDescending = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            auxDescending[i] = aux[array.length - i - 1];
        }
        return auxDescending;
    } 
}



class Bottom_Up {
    public void generateHasseDiagram (int n) {
        // Calculate the number of partitions using Hardy-Ramanujan Asymptotic Partition Formula
        double pn_numerator = Math.PI * Math.sqrt((double)2*n/(double)3);
        double pn_denominator = 4*n* Math.sqrt(3);
        int pn = (int) Math.ceil(Math.exp(pn_numerator) / pn_denominator); 
        
        // Initialize the number of nodes and edges in the Hasse Diagram
        int noNodes = 0;
        int noEdges = 0;
        
        int count = 0;
        
        /* Adjancy list representation of the Hasse Diagram
         * We have considered: Node as well as the adjancy list 
         * Advantage: Partition can be accessed in constant time given the id for that corresponding partition
         * (Considered the number of partitions given by 
         * Hardy-Ramanujan Asymptotic Partition Formula)
         * This will waste some space */ 
        Node_List[] nodeCumAdjList = new Node_List[pn];
        for(int i = 0; i < pn; i++) {
            nodeCumAdjList[i] = new Node_List();
        }

        /* First node of the Hasse Diagram */
        int id = 0;
        int[] elements = new int[n];
        for(int i = 0; i < n ; i++) {
            elements[i] = 1;
        }
        
        Node node = new Node(id, elements);
        nodeCumAdjList[id].setNode(node);
        
        Queue<Integer> Q = new LinkedList();
        // Insert node id into the queue
        noNodes++;
        Q.add(id);

        // Initialize the Trie
        Trie trie = new Trie();
        
        int prevRank = n+1;
        
        while(!Q.isEmpty()) {
            Node curNode = new Node();
            int curPartitionId = Q.poll();
            curNode = nodeCumAdjList[curPartitionId].getNode();
            int curRank = curNode.getElements().length;
            
            /* All the partitions of rank 'prevRank (=curRank+1)' have been processed
             * So reinitialize the Trie */
            if(prevRank != curRank) { 
                trie = new Trie();
            }
            if(curRank == 1) { // All the partition have been generated
                break;
            }
            /* Generate all the partitions of rank curRank+1 from a partition curNode.getElements() of 
             * rank curRank */
            for(int i = 0; i < curRank; i++) {
                if((i == 0) || (curNode.getElements()[i] != curNode.getElements()[i-1])) {
                    for(int j = i+1; j < curRank; j++) {
                        if((j == i+1) || (curNode.getElements()[j] != curNode.getElements()[j-1])) {
                            int[] newPartition = new int[curRank - 1];
                            if(curRank != 2) {
                                System.arraycopy(curNode.getElements(), 0, newPartition, 0, i);
                                System.arraycopy(curNode.getElements(), i+1, newPartition, i, j-(i+1));
                                System.arraycopy(curNode.getElements(), j+1, newPartition, j-1, curRank-(j+1));
                            }
                            
                            newPartition[newPartition.length-1] = curNode.getElements()[i] + curNode.getElements()[j];
                            int[] sortedNewPartition = new int[newPartition.length];
                            sortedNewPartition = countingSort(newPartition); // Sort in non-increasing order
                            
                            /* Chcek whether the generated partition has been alreday generated or not
                             * using Trie data structure */
                            SearchInfo si = new SearchInfo();
                            if(trie.root == null) {
                                si = new SearchInfo(false, -1);
                            } else {
                                si = trie.search(sortedNewPartition, n);
                            }
                            noEdges++;
                            if(si.isIsPresent()) {
                               int existingNodeid = si.getId();
                               nodeCumAdjList[curPartitionId].add(existingNodeid);   
                               nodeCumAdjList[existingNodeid].add(curPartitionId); 
                            } else {
                                id++;   
                                node = new Node(id, sortedNewPartition);
                                nodeCumAdjList[id].setNode(node);   
                                nodeCumAdjList[curPartitionId].add(id);   
                                nodeCumAdjList[id].add(curPartitionId);   
                                trie.insert(sortedNewPartition, n, id);
                                noNodes++;
                                Q.add(id);
                            }   
                        }
                    }
                }
            }  
            prevRank = curRank;
        }
        System.out.println("\nNumber of  nodes = " + noNodes);
        System.out.println("Number of  edges = " + noEdges);
        for(int i = 0; i < noNodes; i++) {
            System.out.println("Adjacency List: " + nodeCumAdjList[i]);
        }
    }

   
    
    
    public int[] countingSort(int[] array) { 
        int[] aux = new int[array.length];

        // Find the smallest and the largest value in the array
        int min = array[0];
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            } else if (array[i] > max) {
                max = array[i];
            }
        }

        // Initialize array of frequencies
        int[] counts = new int[max - min + 1];

        // Initialize the frequencies
        for (int i = 0;  i < array.length; i++) {
          counts[array[i] - min]++;
        }

        // Recalculate the array - create the array of occurences
        counts[0]--;
        for (int i = 1; i < counts.length; i++) {
          counts[i] = counts[i] + counts[i-1];
        }

        for (int i = array.length - 1; i >= 0; i--) {
            aux[counts[array[i] - min]--] = array[i];
        }
        
        int[] auxDescending = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            auxDescending[i] = aux[array.length - i - 1];
        }
        return auxDescending;
    } 
}

