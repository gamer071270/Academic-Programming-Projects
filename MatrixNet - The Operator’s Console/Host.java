/**
 * Represents a secure access point (node) in the Matrix network.
 * Stores identity and security clearance information required for access control.
 */
public class Host {
    String hostId;
    int clearanceLevel;

    public Host(String hostId, int clearanceLevel) {
        this.hostId = hostId;
        this.clearanceLevel = clearanceLevel;
    }
}