import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Hasse_Diagram_Parallel {

    // -----------------------------------------------------------------------
    // Trie Node
    // -----------------------------------------------------------------------
    static class TrieNode {
        ConcurrentHashMap<Integer, TrieNode> children = new ConcurrentHashMap<>();
        AtomicInteger id = new AtomicInteger(-1);
    }

    static final TrieNode root = new TrieNode();

    // -----------------------------------------------------------------------
    // Shared state
    // -----------------------------------------------------------------------
    static final AtomicInteger idCounter = new AtomicInteger(0);
    static volatile int[][] allPartitions;
    static volatile ConcurrentSkipListSet<Integer>[] adjSets;
    static CyclicBarrier barrier;
    static List<Integer>[] nodesByRank;

    // -----------------------------------------------------------------------
    // Register Partition using Trie
    // -----------------------------------------------------------------------
    static int registerPartition(int[] sorted) {
        TrieNode node = root;
        for (int val : sorted) {
            node = node.children.computeIfAbsent(val, k -> new TrieNode());
        }
        int existing = node.id.get();
        if (existing != -1) return existing;

        int newId = idCounter.getAndIncrement();
        if (node.id.compareAndSet(-1, newId)) {
            allPartitions[newId] = sorted.clone();
            nodesByRank[sorted.length].add(newId);
            return newId;
        } else {
            return node.id.get();
        }
    }

    // -----------------------------------------------------------------------
    //  getPartitionId — lookup only, never creates new nodes
    // -----------------------------------------------------------------------
    static int getPartitionId(int[] sorted) {
        TrieNode node = root;
        for (int val : sorted) {
            node = node.children.get(val);
            if (node == null) return -1;
        }
        return node.id.get();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    static int[] sortDesc(int[] array) {
        if (array.length == 0) return array;
        int min = array[0], max = array[0];
        for (int v : array) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        int[] count = new int[max - min + 1];
        for (int v : array) count[v - min]++;
        int[] res = new int[array.length];
        int idx = 0;
        for (int v = max; v >= min; v--) {
            while (count[v - min]-- > 0) res[idx++] = v;
        }
        return res;
    }

    static void addEdge(int a, int b) {
        adjSets[a].add(b);
        adjSets[b].add(a);
    }

    // -----------------------------------------------------------------------
    // MAIN
    // -----------------------------------------------------------------------
    public static void main(String[] args) throws InterruptedException {
        int n = 35;

        double pn_num = Math.PI * Math.sqrt(2.0 * n / 3.0);
        double pn_den = 4.0 * n * Math.sqrt(3);
        int pn = (int) Math.ceil(Math.exp(pn_num) / pn_den);

        nodesByRank = new List[n + 1];
        for (int i = 0; i <= n; i++)
            nodesByRank[i] = Collections.synchronizedList(new ArrayList<>());

        System.out.println("Upper bound p(" + n + ") = " + pn);

        allPartitions = new int[pn][];
        adjSets = new ConcurrentSkipListSet[pn];
        for (int i = 0; i < pn; i++) adjSets[i] = new ConcurrentSkipListSet<>();

        barrier = new CyclicBarrier(3);

        int midRank = n / 2;

        Thread t1 = new Thread(new TopDownWorker(n, midRank));
        Thread t2 = new Thread(new BottomUpWorker(n, midRank));

        long start = System.nanoTime();
        t1.start();
        t2.start();

        try { barrier.await(); } catch (Exception e) {}
        int totalNodes = idCounter.get();
        System.out.println("Phase 1 done. Nodes = " + totalNodes);

        try { barrier.await(); } catch (Exception e) {}
        long end = System.nanoTime();
        System.out.println("Time = " + (end - start) / 1_000_000 + " ms");

        int edges = 0;
        for (int i = 0; i < totalNodes; i++) edges += adjSets[i].size();
        System.out.println("Edges = " + edges / 2);

        System.out.println("\nAdjacency Lists:");
        /*for (int i = 0; i < totalNodes; i++)
            System.out.println(" Node " + i + " " + Arrays.toString(allPartitions[i]) + " -> " + adjSets[i]);*/
    }

    // -----------------------------------------------------------------------
    // TOP DOWN — owns ranks [1, midRank] inclusive
    // -----------------------------------------------------------------------
    static class TopDownWorker implements Runnable {
        int n, midRank;

        TopDownWorker(int n, int midRank) {
            this.n = n;
            this.midRank = midRank;
        }

        public void run() {
            // Phase 1: discover partitions top-down down to midRank
            int[] seed = {n};
            int seedId = registerPartition(seed);

            List<Integer> curr = new ArrayList<>();
            curr.add(seedId);

            for (int rank = 1; rank < midRank; rank++) {
                List<Integer> next = new ArrayList<>();
                HashSet<Integer> seen = new HashSet<>();

                for (int id : curr) {
                    int[] parts = allPartitions[id];
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0 && parts[i] == parts[i - 1]) continue;
                        for (int j = 1; j <= parts[i] / 2; j++) {
                            int[] np = split(parts, i, j);
                            int nid = registerPartition(np);
                            if (seen.add(nid)) next.add(nid);
                        }
                    }
                }
                curr = next;
            }

            try { barrier.await(); } catch (Exception e) {}

            // ---------------------------------------------------------------
            // FIX: Phase 2 — edge building
            //   - Use getPartitionId (lookup only) instead of registerPartition
            //   - Skip if child not found (cid == -1)
            //   - TopDown owns ranks where rank <= midRank (i.e. length 1..midRank)
            //     rank == n is the single-part partition [n], skip it (no children above)
            // ---------------------------------------------------------------
            int total = idCounter.get();

            for (int id = 0; id < total; id++) {
                int[] parts = allPartitions[id];
                if (parts == null) continue;

                int rank = parts.length;
                // FIX: TopDown handles ranks 1..midRank (length 1 = [n], up to midRank parts)
                // rank == n means all-ones partition, handled by BottomUp
                if (rank > midRank || rank == n) continue;

                for (int i = 0; i < parts.length; i++) {
                    if (i > 0 && parts[i] == parts[i - 1]) continue;
                    for (int j = 1; j <= parts[i] / 2; j++) {
                        int[] np = split(parts, i, j);
                        // FIX: lookup only — never register new nodes during edge phase
                        int cid = getPartitionId(np);
                        if (cid == -1) continue;   // FIX: skip if child wasn't discovered
                        addEdge(id, cid);
                    }
                }
            }

            try { barrier.await(); } catch (Exception e) {}
        }

        int[] split(int[] p, int i, int j) {
            int[] np = new int[p.length + 1];
            System.arraycopy(p, 0, np, 0, i);
            System.arraycopy(p, i + 1, np, i, p.length - i - 1);
            np[np.length - 2] = j;
            np[np.length - 1] = p[i] - j;
            return sortDesc(np);
        }
    }

    // -----------------------------------------------------------------------
    // BOTTOM UP — owns ranks [midRank+1, n-1] inclusive
    // -----------------------------------------------------------------------
    static class BottomUpWorker implements Runnable {
        int n, midRank;

        BottomUpWorker(int n, int midRank) {
            this.n = n;
            this.midRank = midRank;
        }

        public void run() {
            // Phase 1: discover partitions bottom-up up to midRank+1
            int[] seed = new int[n];
            Arrays.fill(seed, 1);
            int seedId = registerPartition(seed);

            List<Integer> curr = new ArrayList<>();
            curr.add(seedId);

            for (int rank = n; rank > midRank + 1; rank--) {
                List<Integer> next = new ArrayList<>();
                HashSet<Integer> seen = new HashSet<>();

                for (int id : curr) {
                    int[] parts = allPartitions[id];
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0 && parts[i] == parts[i - 1]) continue;
                        for (int j = i + 1; j < parts.length; j++) {
                            if (j > i + 1 && parts[j] == parts[j - 1]) continue;
                            int[] np = merge(parts, i, j);
                            int nid = registerPartition(np);
                            if (seen.add(nid)) next.add(nid);
                        }
                    }
                }
                curr = next;
            }

            try { barrier.await(); } catch (Exception e) {}

            // ---------------------------------------------------------------
            // FIX: Phase 2 — edge building
            //   - Use getPartitionId (lookup only) instead of registerPartition
            //   - Skip if parent not found (pid == -1)
            //   - BottomUp owns ranks midRank+1..n-1
            //     rank == 1 means [n] partition, handled by TopDown
            // ---------------------------------------------------------------
            int total = idCounter.get();

            for (int id = 0; id < total; id++) {
                int[] parts = allPartitions[id];
                if (parts == null) continue;

                int rank = parts.length;
                // FIX: BottomUp handles ranks midRank+1 .. n-1
                // rank == 1 means single-part [n], handled by TopDown
                if (rank <= midRank || rank == 1) continue;

                for (int i = 0; i < parts.length; i++) {
                    if (i > 0 && parts[i] == parts[i - 1]) continue;
                    for (int j = i + 1; j < parts.length; j++) {
                        if (j > i + 1 && parts[j] == parts[j - 1]) continue;
                        int[] np = merge(parts, i, j);
                        // FIX: lookup only — never register new nodes during edge phase
                        int pid = getPartitionId(np);
                        if (pid == -1) continue;   // FIX: skip if parent wasn't discovered
                        addEdge(id, pid);
                    }
                }
            }

            try { barrier.await(); } catch (Exception e) {}
        }

        int[] merge(int[] p, int i, int j) {
            int[] np = new int[p.length - 1];
            int idx = 0;
            for (int k = 0; k < p.length; k++) {
                if (k == i || k == j) continue;
                np[idx++] = p[k];
            }
            np[np.length - 1] = p[i] + p[j];
            return sortDesc(np);
        }
    }
}