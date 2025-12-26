import java.util.ArrayList;

/**
 * Represents a state in the search space (Min-Heap).
 * Stores the current host, total latency, and path history.
 */
public class State {
    int hostIndex;        // Current host index
    int totalLatency;     // Accumulated dynamic latency from source to this host
    int steps;            // Number of steps taken (used for calculating future lambda penalties)
    State parent;         // Pointer to the previous state (used to reconstruct the path backwards)
    ArrayList<Host> indexToHost; // Reference to the global list for ID lookup during tie-breaking

    public State(int hostIndex, int lat, int steps, State parent, ArrayList<Host> indexToHost) {
        this.hostIndex = hostIndex;
        this.totalLatency = lat;
        this.steps = steps;
        this.parent = parent;
        this.indexToHost = indexToHost;
    }

    // Defines the priority order for the Min-Heap (Optimization Criteria)
    public int compareTo(State other) {
        // 1. Primary Objective: Minimize Total Dynamic Latency
        if (this.totalLatency != other.totalLatency) {
            if (this.totalLatency < other.totalLatency) {
                return -1;
            }
            else {
                return 1;
            }
        }
        
        // 2. Secondary Objective: Minimize Number of Steps
        if (this.steps != other.steps) {
            if (this.steps < other.steps) {
                return -1;
            }
            else {
                return 1;
            }
        }
        
        // 3. Tie-Breaker: Lexicographical order of Host IDs
        String thisId = indexToHost.get(this.hostIndex).hostId;
        String otherId = indexToHost.get(other.hostIndex).hostId;
        return thisId.compareTo(otherId); 
    }
}