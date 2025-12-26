/**
 * Represents a bidirectional hidden tunnel (edge) between two hosts.
 * Stores static link properties and dynamic state (sealed/unsealed) used for routing constraints.
 */
public class Backdoor {
    Host host1;
    Host host2;
    int baseLatency;
    int bandwidth;
    int firewallLevel;
    boolean isSealed;

    public Backdoor(Host host1, Host host2, int baseLatency, int bandwidth, int firewallLevel) {
        this.host1 = host1;
        this.host2 = host2;
        this.baseLatency = baseLatency;
        this.bandwidth = bandwidth;
        this.firewallLevel = firewallLevel;
        this.isSealed = false;
    }

    public void seal() {
        this.isSealed = !isSealed;
    }
}