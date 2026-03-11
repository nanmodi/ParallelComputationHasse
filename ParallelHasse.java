import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class ParallelHasse {
    public static void main(String[] args) {
        Top_Down td = new Top_Down();
        Bottom_Up bu = new Bottom_Up();
        long start,end;
        int n = 24;
        start = System.nanoTime();
        td.generateHasseDiagram(n);
        end = System.nanoTime();
        long tdParTime = (end - start) / 1_000_000;
        System.out.println("********************************Top-Down Parallel Time (ms):******************************* " + tdParTime);
       
        start = System.nanoTime();
        bu.generateHasseDiagram(n);
        end = System.nanoTime();
        long buParTime = (end - start) / 1_000_000;
        System.out.println("Bootom up Parallel Time (ms): " + buParTime);
      
    }
}


class Node {
    private int nodeId;
    private int elements[];

    public Node() {}

    public Node(int nodeId, int[] elements) {
        this.nodeId = nodeId;
        this.elements = elements.clone();
    }

    public Node(Node node) {
        this.nodeId = node.nodeId;
        this.elements = node.elements.clone();
    }

    public int getNodeId() { return this.nodeId; }
    public int[] getElements() { return this.elements; }
    public void setNodeId(int nodeId) { this.nodeId = nodeId; }
    public void setElements(int[] elements) { this.elements = elements.clone(); }

    @Override
    public String toString() {
        return "Node{" + "nodeId=" + this.nodeId + ", elements=" + Arrays.toString(this.elements) + '}';
    }
}


// Thread-safe adjacency list node using ConcurrentLinkedQueue
class Node_List {
    private Node node;
    private ConcurrentLinkedQueue<Integer> adjList;

    public Node_List() {
        this.node = new Node();
        this.adjList = new ConcurrentLinkedQueue<>();
    }

    public Node getNode() { return this.node; }
    public ConcurrentLinkedQueue<Integer> getAdjList() { return this.adjList; }

    public void setNode(Node node) { this.node = new Node(node); }

    public boolean add(Integer id) { return adjList.add(id); }

    public void clear() { adjList.clear(); }

    @Override
    public String toString() {
        return "Node_List{" + "node=" + this.node + ", adjList=" + this.adjList + '}';
    }
}

class SearchInfo {
    private boolean isPresent;
    private int id;

    public SearchInfo() {}

    public SearchInfo(boolean isPresent, int id) {
        this.isPresent = isPresent;
        this.id = id;
    }

    public boolean isIsPresent() { return isPresent; }
    public int getId() { return id; }
    public void setIsPresent(boolean isPresent) { this.isPresent = isPresent; }
    public void setId(int id) { this.id = id; }

    @Override
    public String toString() {
        return "SearchInfo{" + "isPresent=" + isPresent + ", id=" + id + '}';
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
        return "TrieNode{" + "key=" + key + ", isLeaf=" + isLeaf + ", Id=" + Id + '}';
    }
}


// Trie wrapped with a ReentrantLock for thread-safe access across parallel tasks
class SynchronizedTrie {
    private final ReentrantLock lock = new ReentrantLock();
    private Trie trie = new Trie();

    public void reset() {
        lock.lock();
        try {
            trie = new Trie();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically search-then-insert: returns existing ID if found,
     * or inserts with newId and returns -1 to indicate a fresh insertion.
     */
    public int searchOrInsert(int[] partition, int n, int newId) {
        lock.lock();
        try {
            if (trie.root != null) {
                SearchInfo si = trie.search(partition, n);
                if (si.isIsPresent()) {
                    return si.getId(); // duplicate found
                }
            }
            trie.insert(partition, n, newId);
            return -1; // newly inserted
        } finally {
            lock.unlock();
        }
    }
}


class Trie {
    TrieNode root;

    Trie() { this.root = null; }

    void insert(int[] P, int n, int id) {
        int k = P.length;
        int required_sum = n;
        int level = 0;
        int index, max, min, noChildren;

        if (this.root == null) {
            int key = -1;
            max = required_sum - (k - level) + 1;
            min = (int) Math.ceil(required_sum / (k - level));
            noChildren = max - min + 1;
            this.root = new TrieNode(key, noChildren);
        }
        TrieNode trieNode = this.root;

        for (level = 0; level < k - 1; level++) {
            int key = P[level];
            min = (int) Math.ceil(required_sum / (k - level));
            index = key - min;

            required_sum = required_sum - key;
            if (trieNode.children[index] == null) {
                max = required_sum - (k - level - 1) + 1;
                min = (int) Math.ceil(required_sum / (k - level - 1));
                noChildren = max - min + 1;
                trieNode.children[index] = new TrieNode(key, noChildren);
            }
            trieNode = trieNode.children[index];
        }
        int key = P[level];
        index = 0;
        trieNode.children[index] = new TrieNode(key, 0, true, id);
    }

    SearchInfo search(int[] P, int n) {
        int level;
        int k = P.length;
        int required_sum = n;
        int index;

        TrieNode trieNode = root;

        for (level = 0; level < k; level++) {
            int key = P[level];
            int min = (int) Math.ceil(required_sum / (k - level));
            index = key - min;

            required_sum = required_sum - key;
            if (trieNode.children[index] == null) {
                return new SearchInfo(false, -1);
            }
            trieNode = trieNode.children[index];
        }

        if (trieNode == null) {
            return new SearchInfo(false, -1);
        } else {
            return new SearchInfo(true, trieNode.Id);
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// TOP-DOWN (parallel)
// ─────────────────────────────────────────────────────────────────────────────
class Top_Down {

    public void generateHasseDiagram(int n) {
        double pn_numerator = Math.PI * Math.sqrt((double) 2 * n / (double) 3);
        double pn_denominator = 4 * n * Math.sqrt(3);
        int pn = (int) Math.ceil(Math.exp(pn_numerator) / pn_denominator);

        System.out.println("Number of partitions by Hardy-Ramanujan Asymptotic Partition Formula = " + pn);

        AtomicInteger noNodes = new AtomicInteger(0);
        AtomicInteger noEdges = new AtomicInteger(0);

        Node_List[] nodeCumAdjList = new Node_List[pn];
        for (int i = 0; i < pn; i++) {
            nodeCumAdjList[i] = new Node_List();
        }

        // Atomic counter for assigning unique IDs to new partitions
        AtomicInteger idCounter = new AtomicInteger(0);

        // Seed: partition {n}
        int seedId = idCounter.getAndIncrement();
        int[] elements = {n};
        nodeCumAdjList[seedId].setNode(new Node(seedId, elements));
        noNodes.incrementAndGet();

        // Level-by-level BFS: currentLevel holds the node IDs to process this round
        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(seedId);

        SynchronizedTrie syncTrie = new SynchronizedTrie();
        int prevRank = 0;

        // Use all available CPU cores
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        while (!currentLevel.isEmpty()) {
            int curRank = nodeCumAdjList[currentLevel.get(0)].getNode().getElements().length;

            if (curRank == n) break; // reached rank-n (all 1s)

            // New rank → reset the trie for this level
            if (prevRank != curRank) {
                syncTrie.reset();
            }

            // Next level collector (thread-safe)
            ConcurrentLinkedQueue<Integer> nextLevelQueue = new ConcurrentLinkedQueue<>();

            List<Integer> levelSnapshot = currentLevel; // effectively final for lambda

            try {
                pool.submit(() ->
                    levelSnapshot.parallelStream().forEach(curPartitionId -> {
                        Node curNode = nodeCumAdjList[curPartitionId].getNode();
                        int[] curElems = curNode.getElements();
                        int rank = curElems.length;

                        for (int i = 0; i < rank; i++) {
                            if (i != 0 && curElems[i] == curElems[i - 1]) continue;

                            for (int j = 1; j <= curElems[i] / 2; j++) {
                                int[] newPartition = new int[rank + 1];
                                if (rank != 1) {
                                    System.arraycopy(curElems, 0, newPartition, 0, i);
                                    System.arraycopy(curElems, i + 1, newPartition, i, rank - (i + 1));
                                }
                                newPartition[newPartition.length - 2] = j;
                                newPartition[newPartition.length - 1] = curElems[i] - j;

                                int[] sorted = countingSort(newPartition);

                                noEdges.incrementAndGet();

                                // Atomic search-or-insert into the trie (locked internally)
                                int newId = idCounter.get(); // tentative ID
                                int existingId = syncTrie.searchOrInsert(sorted, n, newId);

                                if (existingId >= 0) {
                                    // Duplicate: just add edges
                                    nodeCumAdjList[curPartitionId].add(existingId);
                                    nodeCumAdjList[existingId].add(curPartitionId);
                                } else {
                                    // Fresh partition: claim the ID and register it
                                    int assignedId = idCounter.getAndIncrement();
                                    nodeCumAdjList[assignedId].setNode(new Node(assignedId, sorted));
                                    nodeCumAdjList[curPartitionId].add(assignedId);
                                    nodeCumAdjList[assignedId].add(curPartitionId);
                                    noNodes.incrementAndGet();
                                    nextLevelQueue.add(assignedId);
                                }
                            }
                        }
                    })
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            prevRank = curRank;
            currentLevel = new ArrayList<>(nextLevelQueue);
        }

        pool.shutdown();

        System.out.println("Number of nodes = " + noNodes.get());
        System.out.println("Number of edges = " + noEdges.get());
        for (int i = 0; i < noNodes.get(); i++) {
            System.out.println("Adjacency List: " + nodeCumAdjList[i]);
        }
    }

    public int[] countingSort(int[] array) {
        int[] aux = new int[array.length];
        int min = array[0], max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
            else if (array[i] > max) max = array[i];
        }
        int[] counts = new int[max - min + 1];
        for (int v : array) counts[v - min]++;
        counts[0]--;
        for (int i = 1; i < counts.length; i++) counts[i] += counts[i - 1];
        for (int i = array.length - 1; i >= 0; i--) aux[counts[array[i] - min]--] = array[i];
        int[] desc = new int[array.length];
        for (int i = 0; i < array.length; i++) desc[i] = aux[array.length - i - 1];
        return desc;
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM-UP (parallel)
// ─────────────────────────────────────────────────────────────────────────────
class Bottom_Up {

    public void generateHasseDiagram(int n) {
        double pn_numerator = Math.PI * Math.sqrt((double) 2 * n / (double) 3);
        double pn_denominator = 4 * n * Math.sqrt(3);
        int pn = (int) Math.ceil(Math.exp(pn_numerator) / pn_denominator);

        AtomicInteger noNodes = new AtomicInteger(0);
        AtomicInteger noEdges = new AtomicInteger(0);

        Node_List[] nodeCumAdjList = new Node_List[pn];
        for (int i = 0; i < pn; i++) {
            nodeCumAdjList[i] = new Node_List();
        }

        AtomicInteger idCounter = new AtomicInteger(0);

        // Seed: partition {1,1,...,1} (n ones)
        int seedId = idCounter.getAndIncrement();
        int[] elements = new int[n];
        Arrays.fill(elements, 1);
        nodeCumAdjList[seedId].setNode(new Node(seedId, elements));
        noNodes.incrementAndGet();

        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(seedId);

        SynchronizedTrie syncTrie = new SynchronizedTrie();
        int prevRank = n + 1;

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        while (!currentLevel.isEmpty()) {
            int curRank = nodeCumAdjList[currentLevel.get(0)].getNode().getElements().length;

            if (curRank == 1) break; // reached rank-1 (partition {n})

            if (prevRank != curRank) {
                syncTrie.reset();
            }

            ConcurrentLinkedQueue<Integer> nextLevelQueue = new ConcurrentLinkedQueue<>();
            List<Integer> levelSnapshot = currentLevel;

            try {
                pool.submit(() ->
                    levelSnapshot.parallelStream().forEach(curPartitionId -> {
                        Node curNode = nodeCumAdjList[curPartitionId].getNode();
                        int[] curElems = curNode.getElements();
                        int rank = curElems.length;

                        for (int i = 0; i < rank; i++) {
                            if (i != 0 && curElems[i] == curElems[i - 1]) continue;

                            for (int j = i + 1; j < rank; j++) {
                                if (j != i + 1 && curElems[j] == curElems[j - 1]) continue;

                                int[] newPartition = new int[rank - 1];
                                if (rank != 2) {
                                    System.arraycopy(curElems, 0, newPartition, 0, i);
                                    System.arraycopy(curElems, i + 1, newPartition, i, j - (i + 1));
                                    System.arraycopy(curElems, j + 1, newPartition, j - 1, rank - (j + 1));
                                }
                                newPartition[newPartition.length - 1] = curElems[i] + curElems[j];

                                int[] sorted = countingSort(newPartition);

                                noEdges.incrementAndGet();

                                int newId = idCounter.get();
                                int existingId = syncTrie.searchOrInsert(sorted, n, newId);

                                if (existingId >= 0) {
                                    nodeCumAdjList[curPartitionId].add(existingId);
                                    nodeCumAdjList[existingId].add(curPartitionId);
                                } else {
                                    int assignedId = idCounter.getAndIncrement();
                                    nodeCumAdjList[assignedId].setNode(new Node(assignedId, sorted));
                                    nodeCumAdjList[curPartitionId].add(assignedId);
                                    nodeCumAdjList[assignedId].add(curPartitionId);
                                    noNodes.incrementAndGet();
                                    nextLevelQueue.add(assignedId);
                                }
                            }
                        }
                    })
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            prevRank = curRank;
            currentLevel = new ArrayList<>(nextLevelQueue);
        }

        pool.shutdown();

        System.out.println("\nNumber of nodes = " + noNodes.get());
        System.out.println("Number of edges = " + noEdges.get());
        for (int i = 0; i < noNodes.get(); i++) {
            System.out.println("Adjacency List: " + nodeCumAdjList[i]);
        }
    }

    public int[] countingSort(int[] array) {
        int[] aux = new int[array.length];
        int min = array[0], max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
            else if (array[i] > max) max = array[i];
        }
        int[] counts = new int[max - min + 1];
        for (int v : array) counts[v - min]++;
        counts[0]--;
        for (int i = 1; i < counts.length; i++) counts[i] += counts[i - 1];
        for (int i = array.length - 1; i >= 0; i--) aux[counts[array[i] - min]--] = array[i];
        int[] desc = new int[array.length];
        for (int i = 0; i < array.length; i++) desc[i] = aux[array.length - i - 1];
        return desc;
    }
}