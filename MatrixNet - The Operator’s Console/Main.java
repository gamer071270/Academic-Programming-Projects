import java.io.*;
import java.util.Locale;

/**
 * Main entry point for GigMatch Pro platform.
 */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        MatrixNet matrixNet = new MatrixNet();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer, matrixNet);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer, MatrixNet matrixNet)
            throws IOException {

        String[] parts = command.split("\\s+");
        String operation = parts[0];

        try {
            String result = "";

            switch (operation) {
                case "spawn_host":
                    // spawn_host <hostId> <clearanceLevel>
                    String hostId = parts[1];
                    int clearanceLevel = Integer.parseInt(parts[2]);
                    // matrixNet.spawnHost(hostId, clearanceLevel);
                    result = matrixNet.spawnHost(hostId, clearanceLevel);
                    break;
                case "link_backdoor":
                    // link_backdoor <hostId1> <hostId2> <latency> <bandwidth> <firewall_level>
                    String hostId1 = parts[1];
                    String hostId2 = parts[2];
                    int latency = Integer.parseInt(parts[3]);
                    int bandwidth = Integer.parseInt(parts[4]);
                    int firewallLevel = Integer.parseInt(parts[5]);
                    result = matrixNet.linkBackdoor(hostId1, hostId2, latency, bandwidth, firewallLevel);
                    break;
                case "seal_backdoor":
                    // seal_backdoor <hostId1> <hostId2>
                    hostId1 = parts[1];
                    hostId2 = parts[2];
                    result = matrixNet.sealBackdoor(hostId1, hostId2);
                    break;
                case "trace_route":
                    // trace_route <sourceId> <destId> <min bandwidth> <lambda>
                    String sourceId = parts[1];
                    String destId = parts[2];
                    int minBandwidth = Integer.parseInt(parts[3]);
                    int lambda = Integer.parseInt(parts[4]);
                    result = matrixNet.traceRoute(sourceId, destId, minBandwidth, lambda);
                    break;
                case "scan_connectivity":
                    result = matrixNet.scanConnectivity();
                    break;
                case "simulate_breach":
                    if (parts.length == 2) {
                        // simulate breach <hostId>
                        hostId = parts[1];
                        result = matrixNet.simulateHostBreach(hostId);
                    }
                    else {
                        // simulate breach <hostId1> <hostId2>
                        hostId1 = parts[1];
                        hostId2 = parts[2];
                        result = matrixNet.simulateBackdoorBreach(hostId1, hostId2);
                    }
                    break;
                case "oracle_report":
                    result = matrixNet.oracleReport();
                    break;
                default:
                    result = "Unknown command: " + operation;
            }

            writer.write(result);
            writer.newLine();

        } catch (Exception e) {
            writer.write("Error processing command: " + command);
            writer.newLine();
        }
    }
}