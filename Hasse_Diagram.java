/*import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Hasse_Diagram {

    public static void main(String[] args) throws Exception {
        Scanner sc=new Scanner(System.in);
        System.out.print("Enter n: ");
        int n = sc.nextInt();   // change carefully

        Top_Down_Sequential seq = new Top_Down_Sequential();
        Top_Down_Parallel par = new Top_Down_Parallel();

        long start, end;

        // Sequential
        start = System.nanoTime();
        seq.generateHasseDiagram(n);
        end = System.nanoTime();
        long seqTime = (end - start) / 1_000_000;
        System.out.println("\nSequential Time (ms): " + seqTime);

        System.out.println("\n--------------------------------------\n");

        // Parallel
        start = System.nanoTime();
        par.generateHasseDiagram(n);
        end = System.nanoTime();
        long parTime = (end - start) / 1_000_000;
        System.out.println("\nParallel Time (ms): " + parTime);

        double speedup = (double) seqTime / parTime;
        System.out.println("Speedup: " + speedup);
    }
}



class Node {
    private int nodeId;
    private int elements[];

    public Node(int nodeId, int[] elements) {
        this.nodeId = nodeId;
        this.elements = elements.clone();
    }

    public int[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}

class Node_List {
    private Node node;
    private List<Integer> adjList;

    public Node_List() {
        adjList = Collections.synchronizedList(new ArrayList<>());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void add(Integer id) {
        adjList.add(id);
    }
}



class Top_Down_Sequential {

    public void generateHasseDiagram(int n) {

        int estimated = estimatePartitions(n);
        Node_List[] nodeCumAdjList = new Node_List[estimated];

        for (int i = 0; i < estimated; i++)
            nodeCumAdjList[i] = new Node_List();

        Map<String, Integer> partitionMap = new HashMap<>();

        int id = 0;
        int[] first = { n };
        nodeCumAdjList[0].setNode(new Node(0, first));
        partitionMap.put(Arrays.toString(first), 0);

        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        int noEdges = 0;

        while (!queue.isEmpty()) {

            int curId = queue.poll();
            int[] elements = nodeCumAdjList[curId].getNode().getElements();
            int curRank = elements.length;

            if (curRank == n) continue;

            for (int i = 0; i < curRank; i++) {

                if ((i == 0) || (elements[i] != elements[i - 1])) {

                    int[] newPartition = new int[curRank + 1];

                    if (curRank != 1) {
                        System.arraycopy(elements, 0, newPartition, 0, i);
                        System.arraycopy(elements, i + 1, newPartition, i, curRank - (i + 1));
                    }

                    for (int j = 1; j <= elements[i] / 2; j++) {

                        newPartition[newPartition.length - 2] = j;
                        newPartition[newPartition.length - 1] = elements[i] - j;

                        int[] sorted = countingSort(newPartition);
                        String key = Arrays.toString(sorted);

                        noEdges++;

                        if (partitionMap.containsKey(key)) {
                            int existing = partitionMap.get(key);
                            nodeCumAdjList[curId].add(existing);
                            nodeCumAdjList[existing].add(curId);
                        } else {
                            id++;
                            nodeCumAdjList[id].setNode(new Node(id, sorted));
                            partitionMap.put(key, id);
                            nodeCumAdjList[curId].add(id);
                            nodeCumAdjList[id].add(curId);
                            queue.add(id);
                        }
                    }
                }
            }
        }

        System.out.println("Sequential Nodes: " + (id + 1));
        System.out.println("Sequential Edges: " + noEdges);
    }

    private int estimatePartitions(int n) {
        double num = Math.PI * Math.sqrt((double) 2 * n / 3);
        double den = 4 * n * Math.sqrt(3);
        return (int) Math.ceil(Math.exp(num) / den);
    }

    private int[] countingSort(int[] array) {

        int[] copy = array.clone();
        Arrays.sort(copy);
        for (int i = 0; i < copy.length / 2; i++) {
            int temp = copy[i];
            copy[i] = copy[copy.length - i - 1];
            copy[copy.length - i - 1] = temp;
        }
        return copy;
    }
}



class Top_Down_Parallel {

    public void generateHasseDiagram(int n) throws Exception {

        int estimated = estimatePartitions(n);
        Node_List[] nodeCumAdjList = new Node_List[estimated];

        for (int i = 0; i < estimated; i++)
            nodeCumAdjList[i] = new Node_List();

        AtomicInteger idGenerator = new AtomicInteger(0);
        AtomicInteger noEdges = new AtomicInteger(0);

        ConcurrentHashMap<String, Integer> partitionMap = new ConcurrentHashMap<>();

        int[] first = { n };
        nodeCumAdjList[0].setNode(new Node(0, first));
        partitionMap.put(Arrays.toString(first), 0);

        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        while (!currentLevel.isEmpty()) {

            List<Integer> nextLevel = Collections.synchronizedList(new ArrayList<>());
            List<Future<?>> futures = new ArrayList<>();

            for (Integer curId : currentLevel) {

                futures.add(executor.submit(() -> {

                    int[] elements = nodeCumAdjList[curId].getNode().getElements();
                    int curRank = elements.length;

                    if (curRank == n) return;

                    for (int i = 0; i < curRank; i++) {

                        if ((i == 0) || (elements[i] != elements[i - 1])) {

                            int[] newPartition = new int[curRank + 1];

                            if (curRank != 1) {
                                System.arraycopy(elements, 0, newPartition, 0, i);
                                System.arraycopy(elements, i + 1, newPartition, i, curRank - (i + 1));
                            }

                            for (int j = 1; j <= elements[i] / 2; j++) {

                                newPartition[newPartition.length - 2] = j;
                                newPartition[newPartition.length - 1] = elements[i] - j;

                                int[] sorted = countingSort(newPartition);
                                String key = Arrays.toString(sorted);

                                noEdges.incrementAndGet();

                                partitionMap.compute(key, (k, existingId) -> {

                                    if (existingId != null) {
                                        nodeCumAdjList[curId].add(existingId);
                                        nodeCumAdjList[existingId].add(curId);
                                        return existingId;
                                    } else {
                                        int newId = idGenerator.incrementAndGet();
                                        nodeCumAdjList[newId].setNode(new Node(newId, sorted));
                                        nodeCumAdjList[curId].add(newId);
                                        nodeCumAdjList[newId].add(curId);
                                        nextLevel.add(newId);
                                        return newId;
                                    }
                                });
                            }
                        }
                    }
                }));
            }

            for (Future<?> f : futures) f.get();
            currentLevel = nextLevel;
        }

        executor.shutdown();

        System.out.println("Parallel Nodes: " + (idGenerator.get() + 1));
        System.out.println("Parallel Edges: " + noEdges.get());
    }

    private int estimatePartitions(int n) {
        double num = Math.PI * Math.sqrt((double) 2 * n / 3);
        double den = 4 * n * Math.sqrt(3);
        return (int) Math.ceil(Math.exp(num) / den);
    }

    private int[] countingSort(int[] array) {
        int[] copy = array.clone();
        Arrays.sort(copy);
        for (int i = 0; i < copy.length / 2; i++) {
            int temp = copy[i];
            copy[i] = copy[copy.length - i - 1];
            copy[copy.length - i - 1] = temp;
        }
        return copy;
    }
}*/




import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Hasse_Diagram {

    public static void main(String[] args) throws Exception {

        int n=30;

        Top_Down_Sequential tdSeq = new Top_Down_Sequential();
        Top_Down_Parallel tdPar = new Top_Down_Parallel();
        Bottom_Up_Sequential buSeq = new Bottom_Up_Sequential();
        Bottom_Up_Parallel buPar = new Bottom_Up_Parallel();

        long start, end;

        /* ================= TOP DOWN SEQUENTIAL ================= */
        start = System.nanoTime();
        tdSeq.generateHasseDiagram(n);
        end = System.nanoTime();
        long tdSeqTime = (end - start) / 1_000_000;
        System.out.println("Top-Down Sequential Time (ms): " + tdSeqTime);

        System.out.println("------------------------------------------------");

        /* ================= TOP DOWN PARALLEL ================= */
        start = System.nanoTime();
        tdPar.generateHasseDiagram(n);
        end = System.nanoTime();
        long tdParTime = (end - start) / 1_000_000;
        System.out.println("Top-Down Parallel Time (ms): " + tdParTime);
        System.out.println("Top-Down Speedup: " + (double) tdSeqTime / tdParTime);

        System.out.println("------------------------------------------------");

        /* ================= BOTTOM UP SEQUENTIAL ================= */
        start = System.nanoTime();
        buSeq.generateHasseDiagram(n);
        end = System.nanoTime();
        long buSeqTime = (end - start) / 1_000_000;
        System.out.println("Bottom-Up Sequential Time (ms): " + buSeqTime);

        System.out.println("------------------------------------------------");

        /* ================= BOTTOM UP PARALLEL ================= */
        start = System.nanoTime();
        buPar.generateHasseDiagram(n);
        end = System.nanoTime();
        long buParTime = (end - start) / 1_000_000;
        System.out.println("Bottom-Up Parallel Time (ms): " + buParTime);
        System.out.println("Bottom-Up Speedup: " + (double) buSeqTime / buParTime);
    }
}

/* ======================= NODE STRUCTURES ======================= */

class Node {
    private int nodeId;
    private int elements[];

    public Node(int nodeId, int[] elements) {
        this.nodeId = nodeId;
        this.elements = elements.clone();
    }

    public int[] getElements() {
        return elements;
    }
}

class Node_List {
    private Node node;
    private List<Integer> adjList;

    public Node_List() {
        adjList = Collections.synchronizedList(new ArrayList<>());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void add(Integer id) {
        adjList.add(id);
    }
}

/* ======================= TOP DOWN SEQUENTIAL ======================= */

class Top_Down_Sequential {

    public void generateHasseDiagram(int n) throws Exception {

        int estimated = estimatePartitions(n);
        Node_List[] nodes = new Node_List[estimated];
        for (int i = 0; i < estimated; i++)
            nodes[i] = new Node_List();

        Map<String, Integer> map = new HashMap<>();

        int id = 0;
        int[] first = { n };
        nodes[0].setNode(new Node(0, first));
        map.put(Arrays.toString(first), 0);

        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        int edges = 0;

        while (!queue.isEmpty()) {

            int curId = queue.poll();
            int[] elements = nodes[curId].getNode().getElements();

            if (elements.length == n) continue;

            for (int i = 0; i < elements.length; i++) {

                if (i == 0 || elements[i] != elements[i - 1]) {

                    int[] newPart = new int[elements.length + 1];

                    if (elements.length != 1) {
                        System.arraycopy(elements, 0, newPart, 0, i);
                        System.arraycopy(elements, i + 1, newPart, i,
                                elements.length - (i + 1));
                    }

                    for (int j = 1; j <= elements[i] / 2; j++) {

                        newPart[newPart.length - 2] = j;
                        newPart[newPart.length - 1] = elements[i] - j;

                        int[] sorted = sortDescending(newPart);
                        String key = Arrays.toString(sorted);

                        edges++;

                        if (map.containsKey(key)) {

                            int existing = map.get(key);
                            nodes[curId].add(existing);
                            nodes[existing].add(curId);

                        } else {

                            id++;
                            nodes[id].setNode(new Node(id, sorted));
                            map.put(key, id);
                            nodes[curId].add(id);
                            nodes[id].add(curId);
                            queue.add(id);
                        }
                    }
                }
            }
        }

        System.out.println("Top-Down Sequential Nodes: " + (id + 1));
        System.out.println("Top-Down Sequential Edges: " + edges);
    }

    protected int estimatePartitions(int n) {
        double num = Math.PI * Math.sqrt((double) 2 * n / 3);
        double den = 4 * n * Math.sqrt(3);
        return (int) Math.ceil(Math.exp(num) / den);
    }

    protected int[] sortDescending(int[] array) {
        int[] copy = array.clone();
        Arrays.sort(copy);
        for (int i = 0; i < copy.length / 2; i++) {
            int temp = copy[i];
            copy[i] = copy[copy.length - 1 - i];
            copy[copy.length - 1 - i] = temp;
        }
        return copy;
    }
}

/* ======================= TOP DOWN PARALLEL ======================= */

class Top_Down_Parallel extends Top_Down_Sequential {

    public void generateHasseDiagram(int n) throws Exception {

        int estimated = estimatePartitions(n);
        Node_List[] nodes = new Node_List[estimated];
        for (int i = 0; i < estimated; i++)
            nodes[i] = new Node_List();

        AtomicInteger idGen = new AtomicInteger(0);
        AtomicInteger edges = new AtomicInteger(0);

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        int[] first = { n };
        nodes[0].setNode(new Node(0, first));
        map.put(Arrays.toString(first), 0);

        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(0);

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors());

        while (!currentLevel.isEmpty()) {

            List<Integer> nextLevel =
                    Collections.synchronizedList(new ArrayList<>());

            List<Future<?>> futures = new ArrayList<>();

            for (Integer curId : currentLevel) {

                futures.add(executor.submit(() -> {

                    int[] elements =
                            nodes[curId].getNode().getElements();

                    if (elements.length == n) return;

                    for (int i = 0; i < elements.length; i++) {

                        if (i == 0 || elements[i] != elements[i - 1]) {

                            int[] newPart =
                                    new int[elements.length + 1];

                            if (elements.length != 1) {
                                System.arraycopy(elements, 0,
                                        newPart, 0, i);
                                System.arraycopy(elements, i + 1,
                                        newPart, i,
                                        elements.length - (i + 1));
                            }

                            for (int j = 1;
                                 j <= elements[i] / 2;
                                 j++) {

                                newPart[newPart.length - 2] = j;
                                newPart[newPart.length - 1] =
                                        elements[i] - j;

                                int[] sorted =
                                        sortDescending(newPart);

                                String key =
                                        Arrays.toString(sorted);

                                edges.incrementAndGet();

                                map.compute(key,
                                        (k, existing) -> {

                                            if (existing != null) {
                                                nodes[curId]
                                                        .add(existing);
                                                nodes[existing]
                                                        .add(curId);
                                                return existing;
                                            } else {
                                                int newId =
                                                        idGen.incrementAndGet();
                                                nodes[newId]
                                                        .setNode(
                                                                new Node(
                                                                        newId,
                                                                        sorted));
                                                nodes[curId]
                                                        .add(newId);
                                                nodes[newId]
                                                        .add(curId);
                                                nextLevel.add(newId);
                                                return newId;
                                            }
                                        });
                            }
                        }
                    }
                }));
            }

            for (Future<?> f : futures) f.get();
            currentLevel = nextLevel;
        }

        executor.shutdown();

        System.out.println("Top-Down Parallel Nodes: "
                + (idGen.get() + 1));
        System.out.println("Top-Down Parallel Edges: "
                + edges.get());
    }
}

/* ======================= BOTTOM UP SEQUENTIAL ======================= */

class Bottom_Up_Sequential extends Top_Down_Sequential {

    public void generateHasseDiagram(int n) throws Exception {

        int estimated = estimatePartitions(n);
        Node_List[] nodes = new Node_List[estimated];
        for (int i = 0; i < estimated; i++)
            nodes[i] = new Node_List();

        Map<String, Integer> map = new HashMap<>();

        int id = 0;
        int[] first = new int[n];
        Arrays.fill(first, 1);

        nodes[0].setNode(new Node(0, first));
        map.put(Arrays.toString(first), 0);

        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        int edges = 0;

        while (!queue.isEmpty()) {

            int curId = queue.poll();
            int[] elements = nodes[curId].getNode().getElements();

            if (elements.length == 1) continue;

            for (int i = 0; i < elements.length; i++) {

                if (i == 0 || elements[i] != elements[i - 1]) {

                    for (int j = i + 1;
                         j < elements.length;
                         j++) {

                        if (j == i + 1
                                || elements[j]
                                != elements[j - 1]) {

                            int[] newPart =
                                    new int[elements.length - 1];

                            if (elements.length != 2) {
                                System.arraycopy(elements, 0,
                                        newPart, 0, i);
                                System.arraycopy(elements,
                                        i + 1,
                                        newPart, i,
                                        j - (i + 1));
                                System.arraycopy(elements,
                                        j + 1,
                                        newPart,
                                        j - 1,
                                        elements.length
                                                - (j + 1));
                            }

                            newPart[newPart.length - 1] =
                                    elements[i] + elements[j];

                            int[] sorted =
                                    sortDescending(newPart);

                            String key =
                                    Arrays.toString(sorted);

                            edges++;

                            if (map.containsKey(key)) {

                                int existing =
                                        map.get(key);
                                nodes[curId]
                                        .add(existing);
                                nodes[existing]
                                        .add(curId);

                            } else {

                                id++;
                                nodes[id]
                                        .setNode(
                                                new Node(
                                                        id,
                                                        sorted));
                                map.put(key, id);
                                nodes[curId]
                                        .add(id);
                                nodes[id]
                                        .add(curId);
                                queue.add(id);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Bottom-Up Sequential Nodes: "
                + (id + 1));
        System.out.println("Bottom-Up Sequential Edges: "
                + edges);
    }
}

/* ======================= BOTTOM UP PARALLEL ======================= */

class Bottom_Up_Parallel extends Bottom_Up_Sequential {

    public void generateHasseDiagram(int n) throws Exception {

        int estimated = estimatePartitions(n);
        Node_List[] nodes = new Node_List[estimated];
        for (int i = 0; i < estimated; i++)
            nodes[i] = new Node_List();

        AtomicInteger idGen = new AtomicInteger(0);
        AtomicInteger edges = new AtomicInteger(0);

        ConcurrentHashMap<String, Integer> map =
                new ConcurrentHashMap<>();

        int[] first = new int[n];
        Arrays.fill(first, 1);

        nodes[0].setNode(new Node(0, first));
        map.put(Arrays.toString(first), 0);

        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(0);

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime()
                                .availableProcessors());

        while (!currentLevel.isEmpty()) {

            List<Integer> nextLevel =
                    Collections.synchronizedList(
                            new ArrayList<>());

            List<Future<?>> futures =
                    new ArrayList<>();

            for (Integer curId : currentLevel) {

                futures.add(executor.submit(() -> {

                    int[] elements =
                            nodes[curId]
                                    .getNode()
                                    .getElements();

                    if (elements.length == 1)
                        return;

                    for (int i = 0;
                         i < elements.length;
                         i++) {

                        if (i == 0
                                || elements[i]
                                != elements[i - 1]) {

                            for (int j = i + 1;
                                 j < elements.length;
                                 j++) {

                                if (j == i + 1
                                        || elements[j]
                                        != elements[j - 1]) {

                                    int[] newPart =
                                            new int[
                                                    elements.length
                                                            - 1];

                                    if (elements.length
                                            != 2) {

                                        System.arraycopy(
                                                elements,
                                                0,
                                                newPart,
                                                0,
                                                i);

                                        System.arraycopy(
                                                elements,
                                                i + 1,
                                                newPart,
                                                i,
                                                j - (i + 1));

                                        System.arraycopy(
                                                elements,
                                                j + 1,
                                                newPart,
                                                j - 1,
                                                elements.length
                                                        - (j + 1));
                                    }

                                    newPart[
                                            newPart.length
                                                    - 1] =
                                            elements[i]
                                                    + elements[j];

                                    int[] sorted =
                                            sortDescending(
                                                    newPart);

                                    String key =
                                            Arrays.toString(
                                                    sorted);

                                    edges.incrementAndGet();

                                    map.compute(key,
                                            (k, existing) -> {

                                                if (existing
                                                        != null) {

                                                    nodes[curId]
                                                            .add(
                                                                    existing);
                                                    nodes[existing]
                                                            .add(
                                                                    curId);
                                                    return existing;
                                                } else {

                                                    int newId =
                                                            idGen.incrementAndGet();

                                                    nodes[newId]
                                                            .setNode(
                                                                    new Node(
                                                                            newId,
                                                                            sorted));

                                                    nodes[curId]
                                                            .add(
                                                                    newId);
                                                    nodes[newId]
                                                            .add(
                                                                    curId);

                                                    nextLevel.add(
                                                            newId);

                                                    return newId;
                                                }
                                            });
                                }
                            }
                        }
                    }
                }));
            }

            for (Future<?> f : futures)
                f.get();

            currentLevel = nextLevel;
        }

        executor.shutdown();

        System.out.println("Bottom-Up Parallel Nodes: "
                + (idGen.get() + 1));
        System.out.println("Bottom-Up Parallel Edges: "
                + edges.get());
    }
}
