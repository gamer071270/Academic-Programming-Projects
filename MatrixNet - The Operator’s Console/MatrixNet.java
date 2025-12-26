import java.util.ArrayList;

public class MatrixNet {

    // Storage for Host and Backdoor objects accessed by String IDs
    private MyHashTable<String, Host> hosts;
    private MyHashTable<String, Backdoor> edges;

    // Optimization: Map String IDs to Integer indices for O(1) array access
    private MyHashTable<String, Integer> hostIndex;
    private ArrayList<Host> indexHost; // Reverse lookup (Index -> Host)
    
    // Adjacency list using integer indices (much faster than HashMaps for traversal)
    private ArrayList<ArrayList<Edge>> graph;

    private int hostCount = 0;

    public MatrixNet() {
        hosts = new MyHashTable<>(600011);
        edges = new MyHashTable<>(600011);

        hostIndex = new MyHashTable<>(600011);
        indexHost = new ArrayList<>();
        graph = new ArrayList<>();
    }

    public String spawnHost(String hostId, int clearanceLevel) {
        if (!isValidHostId(hostId) || hosts.containsKey(hostId)) {
            return "Some error occurred in spawn_host.";
        }

        Host h = new Host(hostId, clearanceLevel);
        hosts.put(hostId, h);
        
        // Assign a unique integer ID to the new host and initialize its adjacency list
        hostIndex.put(hostId, hostCount);
        indexHost.add(h);
        graph.add(new ArrayList<>());
        hostCount++;

        return "Spawned host " + hostId + " with clearance level " + clearanceLevel + ".";
    }

    public String linkBackdoor(String host1Id, String host2Id, int latency, int bandwidth, int firewall) {
        Host host1 = hosts.get(host1Id);
        Host host2 = hosts.get(host2Id);
        String backdoorKey = findBackdoorKey(host1Id, host2Id);

        if (host1 == null || host2 == null || host1Id.equals(host2Id) || edges.containsKey(backdoorKey)) {
            return "Some error occurred in link_backdoor.";
        }

        Backdoor bd = new Backdoor(host1, host2, latency, bandwidth, firewall);
        edges.put(backdoorKey, bd);

        // Retrieve integer indices to update the adjacency list efficiently
        int host1Index = hostIndex.get(host1Id);
        int host2Index = hostIndex.get(host2Id);

        // Add edges to the graph (undirected)
        graph.get(host1Index).add(new Edge(host2Index, bd));
        graph.get(host2Index).add(new Edge(host1Index, bd));

        return "Linked " + host1Id + " <-> " + host2Id + " with latency " + latency + "ms, bandwidth " + bandwidth + "Mbps, firewall " + firewall + ".";
    }

    public String sealBackdoor(String host1Id, String host2Id) {
        String backdoorKey = findBackdoorKey(host1Id, host2Id);
        Backdoor bd = edges.get(backdoorKey);

        if (!hosts.containsKey(host1Id) || !hosts.containsKey(host2Id) || bd == null) {
            return "Some error occurred in seal_backdoor.";
        }

        // Toggle the sealed status (reference updates automatically in Edge objects)
        bd.seal();
        if (bd.isSealed) {
            return "Backdoor " + host1Id + " <-> " + host2Id + " sealed.";
        }
        else {
            return "Backdoor " + host1Id + " <-> " + host2Id + " unsealed.";
        }
    }

    public String traceRoute(String source, String dest, int minBandwidth, int lambda) {
        Integer sourceIndex = hostIndex.get(source);
        Integer destIndex = hostIndex.get(dest);

        if (sourceIndex == null || destIndex == null) {
            return "Some error occurred in trace_route.";
        }

        if (sourceIndex.equals(destIndex)) {
            return "Optimal route " + source + " -> " + source + ": " + source + " (Latency = 0ms)";
        }

        // Optimization: Keep track of the minimum steps to reach each host.
        // If we reach a host with more steps than recorded, prune that path (due to lambda penalty).
        int[] bestSteps = new int[hostCount];
        for (int i = 0; i < hostCount; i++) {
            bestSteps[i] = Integer.MAX_VALUE;
        }

        // Dijkstra's Algorithm using a custom MinHeap
        MyMinHeap heap = new MyMinHeap(1000);
        State start = new State(sourceIndex, 0, 0, null, indexHost);
        heap.add(start);
        bestSteps[sourceIndex] = 0;

        State result = null;

        while (!heap.isEmpty()) {
            State current = heap.poll();

            if (current.hostIndex == destIndex) {
                result = current;
                break;
            }

            // Pruning: If we found a path to this node with fewer steps previously, skip this one
            if (current.steps > bestSteps[current.hostIndex]) {
                continue;
            }

            bestSteps[current.hostIndex] = current.steps;

            ArrayList<Edge> neighbors = graph.get(current.hostIndex);
            Host curHost = indexHost.get(current.hostIndex);

            for (Edge edge : neighbors) {
                Backdoor bd = edge.backdoor;

                // Check constraints: Sealed status, Bandwidth, and Security Clearance
                if (bd.isSealed || bd.bandwidth < minBandwidth || curHost.clearanceLevel < bd.firewallLevel) {
                    continue;
                }

                // Calculate dynamic latency based on step count (lambda)
                int effLatency = bd.baseLatency + lambda * current.steps;
                int newLatency = current.totalLatency + effLatency;
                int newSteps = current.steps + 1;

                if (newSteps >= bestSteps[edge.oppositeHostIndex]) {
                    continue;
                }

                heap.add(new State(edge.oppositeHostIndex, newLatency, newSteps, current, indexHost));
            }
        }

        if (result == null) {
            return "No route found from " + source + " to " + dest;
        }

        // Reconstruct path by backtracking through parent states
        ArrayList<String> path = new ArrayList<>();
        State resultState = result;
        while (resultState!= null) {
            path.add(indexHost.get(resultState.hostIndex).hostId);
            resultState = resultState.parent;
        }

        String resultString = "Optimal route " + source + " -> " + dest + ": ";
        for (int i = path.size() - 1; i > 0; i--) {
            resultString = resultString + path.get(i) + " -> ";
        }
        resultString = resultString + dest + " (Latency = " + result.totalLatency + "ms)";
        
        return resultString;
    }

    public String scanConnectivity() {
        // Count connected components using BFS
        int c = countComponents(-1, null);
        if (c <= 1) {
            return "Network is fully connected.";
        }
        else {
            return "Network has " + c + " disconnected components.";
        }
    }

    public String simulateHostBreach(String hostId) {
        Integer idx = hostIndex.get(hostId);
        if (idx == null) {
            return "Some error occurred in simulate_breach.";
        }

        // Articulation Point check: Compare components before and after "removing" the host
        int before = countComponents(-1, null);
        int after = countComponents(idx, null); // Pass idx to ignore this host during traversal

        if (hostCount == 1) {
            return "Host " + hostId + " is NOT an articulation point. Network remains the same.";
        }

        if (after > before) {
            return "Host " + hostId + " IS an articulation point.\n" +
                    "Failure results in " + after + " disconnected components.";
        }

        return "Host " + hostId + " is NOT an articulation point. Network remains the same.";
    }

    public String simulateBackdoorBreach(String host1Id, String host2Id) {
        Integer host1Index = hostIndex.get(host1Id);
        Integer host2Index = hostIndex.get(host2Id);
        Backdoor bd = edges.get(findBackdoorKey(host1Id, host2Id));

        if (host1Index == null || host2Index == null || bd == null || bd.isSealed) {
            return "Some error occurred in simulate_breach.";
        }

        // Bridge check: Compare components before and after "sealing" the backdoor temporarily
        int before = countComponents(-1, null);
        int after = countComponents(-1, bd); // Pass bd reference to ignore this edge

        if (after > before) {
            return "Backdoor " + host1Id + " <-> " + host2Id + " IS a bridge.\n" + 
                    "Failure results in " + after + " disconnected components.";
        }

        return "Backdoor " + host1Id + " <-> " + host2Id + " is NOT a bridge. Network remains the same.";
    }

    public String oracleReport() {
        // Aggregate statistics for the report
        int totalHosts = hostCount;
        int openBackdoorCount = 0;
        double bandwidthSum = 0;

        for (Backdoor b : edges.values()) {
            if (!b.isSealed) {
                openBackdoorCount++;
                bandwidthSum += b.bandwidth;
            }
        }

        double clearanceSum = 0;
        for (Host h : indexHost) {
            clearanceSum += h.clearanceLevel;
        }

        int comps = countComponents(-1, null);
        boolean cycles = checkCycles();

        // Calculate averages using BigDecimal for precise rounding
        java.math.BigDecimal avgBandwidth;
        if (openBackdoorCount == 0) {
            avgBandwidth = java.math.BigDecimal.ZERO;
        }
        else {
            avgBandwidth = new java.math.BigDecimal(bandwidthSum / openBackdoorCount).setScale(1, java.math.RoundingMode.HALF_UP);
        }
        
        java.math.BigDecimal avgClearance;
        if (totalHosts == 0) {
            avgClearance = java.math.BigDecimal.ZERO;
        }
        else {
            avgClearance = new java.math.BigDecimal(clearanceSum / totalHosts).setScale(1, java.math.RoundingMode.HALF_UP);
        }

        // Build the formatted output string
        String result = "--- Resistance Network Report ---\n";
        result += "Total Hosts: " + totalHosts + "\n";
        result += "Total Unsealed Backdoors: " + openBackdoorCount + "\n" + "Network Connectivity: ";
        if (comps <= 1) {
            result += "Connected\n";
        }
        else {
            result += "Disconnected\n";
        }
        result += "Connected Components: " + comps + "\nContains Cycles: ";
        if (cycles) {
            result += "Yes\n";
        }
        else {
            result += "No\n";
        }
        result += "Average Bandwidth: " + avgBandwidth + "Mbps\nAverage Clearance Level: " + avgClearance;

        return result;
    }

    private static String findBackdoorKey(String a, String b) {
        // Create a consistent key regardless of order (A|B same as B|A)
        if (a.compareTo(b) < 0) {
            return a + "|" + b;
        }
        else {
            return b + "|" + a;
        }
    }

    private static boolean isValidHostId(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_') {
                return false;
            }
        }
        return true;
    }

    // BFS implementation to count connected components
    // Supports ignoring a specific host or backdoor for breach simulation
    private int countComponents(int ignoreHost, Backdoor ignoreBd) {
        if (hostCount == 0 || (hostCount == 1 && ignoreHost != -1)) {
            return 0;
        }

        boolean[] visited = new boolean[hostCount];
        if (ignoreHost != -1) {
            visited[ignoreHost] = true; // Mark ignored host as visited so BFS skips it
        }

        int count = 0;

        for (int i = 0; i < hostCount; i++) {
            if (!visited[i]) {
                count++;
                // Start BFS
                ArrayList<Integer> queue = new ArrayList<>();
                queue.add(i);
                visited[i] = true;

                int queueIndex = 0;
                while (queueIndex < queue.size()) {
                    int currentHost = queue.get(queueIndex++);
                    for (Edge edge : graph.get(currentHost)) {
                        // Skip if sealed, or if it matches the ignored simulation parameters
                        if (edge.backdoor.isSealed || edge.backdoor == ignoreBd || edge.oppositeHostIndex == ignoreHost) {
                            continue;
                        }
                        if (!visited[edge.oppositeHostIndex]) {
                            visited[edge.oppositeHostIndex] = true;
                            queue.add(edge.oppositeHostIndex);
                        }
                    }
                }
            }
        }
        return count;
    }

    private boolean checkCycles() {
        boolean[] visited = new boolean[hostCount];
        for (int i = 0; i < hostCount; i++) {
            if (!visited[i] && dfsCycle(i, -1, visited)) {
                return true;
            }
        }
        return false;
    }

    // DFS to detect cycles in an undirected graph
    private boolean dfsCycle(int u, int parent, boolean[] visited) {
        visited[u] = true;
        for (Edge edge : graph.get(u)) {
            if (edge.backdoor.isSealed) {
                continue;
            }
            int v = edge.oppositeHostIndex;
            if (v == parent) {
                continue; // Don't go back to the node we just came from
            }
            if (visited[v] || dfsCycle(v, u, visited)) {
                return true; // Cycle detected
            }
        }
        return false;
    }
}