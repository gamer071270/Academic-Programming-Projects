// An Edge class. Even though we have a Backdoor class, this Edge class is used to store edges with the opposite hosts.
public class Edge {
    int oppositeHostIndex; // The pre-calculated integer index of the neighbor host (avoids checking "which host is the other one?")
    Backdoor backdoor;     // Reference to the shared Backdoor object to access live data (latency, sealed status)

    Edge(int opp, Backdoor b) {
        oppositeHostIndex = opp;
        backdoor = b;
    }
}