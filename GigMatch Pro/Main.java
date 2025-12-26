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

        GigMatchPro gigMatchPro = new GigMatchPro();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer, gigMatchPro);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer, GigMatchPro gigMatchPro)
            throws IOException {

        String[] parts = command.split("\\s+");
        String operation = parts[0];

        try {
            String result = "";

            switch (operation) {
                case "register_customer":
                    // Format: register_customer customerID
                    String customerID = parts[1];
                    result = gigMatchPro.registerCustomer(customerID);
                    break;

                case "register_freelancer":
                    // Format: register_freelancer freelancerID serviceName basePrice T C R E A
                    String freelancerID = parts[1];
                    String serviceName = parts[2];
                    int basePrice = Integer.parseInt(parts[3]);
                    int T = Integer.parseInt(parts[4]);
                    int C = Integer.parseInt(parts[5]);
                    int R = Integer.parseInt(parts[6]);
                    int E = Integer.parseInt(parts[7]);
                    int A = Integer.parseInt(parts[8]);
                    result = gigMatchPro.registerFreelancer(freelancerID, serviceName, basePrice, T, C, R, E, A);
                    break;

                case "request_job":
                    // Format: request_job customerID serviceName topK
                    customerID = parts[1];
                    serviceName = parts[2];
                    int topK = Integer.parseInt(parts[3]);
                    result = gigMatchPro.requestJob(customerID, serviceName, topK);
                    break;

                case "employ_freelancer":
                    // Format: employ_freelancer customerID freelancerID
                    customerID = parts[1];
                    freelancerID = parts[2];
                    result = gigMatchPro.employ(customerID, freelancerID);
                    break;

                case "complete_and_rate":
                    // Format: complete_and_rate freelancerID rating
                    freelancerID = parts[1];
                    int rating = Integer.parseInt(parts[2]);
                    result = gigMatchPro.completeAndRate(freelancerID, rating);
                    break;

                case "cancel_by_freelancer":
                    // Format: cancel_by_freelancer freelancerID
                    freelancerID = parts[1];
                    result = gigMatchPro.cancelByFreelancer(freelancerID);
                    break;

                case "cancel_by_customer":
                    // Format: cancel_by_customer customerID freelancerID
                    customerID = parts[1];
                    freelancerID = parts[2];
                    result = gigMatchPro.cancelByCustomer(customerID, freelancerID);
                    break;

                case "blacklist":
                    // Format: blacklist customerID freelancerID
                    customerID = parts[1];
                    freelancerID = parts[2];
                    result = gigMatchPro.blacklist(customerID, freelancerID);
                    break;

                case "unblacklist":
                    // Format: unblacklist customerID freelancerID
                    customerID = parts[1];
                    freelancerID = parts[2];
                    result = gigMatchPro.unblacklist(customerID, freelancerID);
                    break;

                case "change_service":
                    // Format: change_service freelancerID newService newPrice
                    freelancerID = parts[1];
                    String newService = parts[2];
                    int newPrice = Integer.parseInt(parts[3]);
                    result = gigMatchPro.changeService(freelancerID, newService, newPrice);
                    break;

                case "simulate_month":
                    // Format: simulate_month
                    result = gigMatchPro.simulateMonth();
                    break;

                case "query_freelancer":
                    // Format: query_freelancer freelancerID
                    freelancerID = parts[1];
                    result = gigMatchPro.queryFreelancer(freelancerID);
                    break;

                case "query_customer":
                    // Format: query_customer customerID
                    customerID = parts[1];
                    result = gigMatchPro.queryCustomer(customerID);
                    break;

                case "update_skill":
                    // Format: update_skill freelancerID T C R E A
                    freelancerID = parts[1];
                    T = Integer.parseInt(parts[2]);
                    C = Integer.parseInt(parts[3]);
                    R = Integer.parseInt(parts[4]);
                    E = Integer.parseInt(parts[5]);
                    A = Integer.parseInt(parts[6]);
                    result = gigMatchPro.updateSkill(freelancerID, T, C, R, E, A);
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