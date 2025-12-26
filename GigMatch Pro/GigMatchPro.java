import java.util.ArrayList;
import java.util.LinkedList;

public class GigMatchPro {

    // Maps IDs to registered freelancers/customers
    private MyHashTable<String, Freelancer> freelancerMap; 
    private MyHashTable<String, Customer> customerMap;

    // For each service: an AVL tree ranking freelancers by composite score
    private MyHashTable<String, MyAVLTree<Freelancer>> serviceRankings;

    // Tracks which freelancer is currently employed and by which customer
    private MyHashTable<String, String> activeFreelancerJobs;

    // Platform-wide banned freelancers
    private MyHashTable<String, Boolean> platformBlacklist;

    // Default skill profiles per service
    private MyHashTable<String, int[]> serviceProfiles;

    // Pending service change requests
    private LinkedList<ServiceChangeRequest> queuedServiceChanges;

    private FreelancerComparator freelancerComparator;

    public GigMatchPro() {
        // Large hash tables for lookups in O(1), capacities are chosen primes
        this.freelancerMap = new MyHashTable<>(600011); 
        this.customerMap = new MyHashTable<>(600011);
        this.activeFreelancerJobs = new MyHashTable<>(600011);

        this.platformBlacklist = new MyHashTable<>(100003);
        this.queuedServiceChanges = new LinkedList<>();
        this.freelancerComparator = new FreelancerComparator();

        // Small table: one entry per service
        this.serviceRankings = new MyHashTable<>(17); 
        this.serviceProfiles = new MyHashTable<>(17);
        
        initializeServices();
    }

    // Pre-loads supported services and their weight profiles
    private void initializeServices() {
        addService("paint", new int[]{70, 60, 50, 85, 90});
        addService("web_dev", new int[]{95, 75, 85, 80, 90});
        addService("graphic_design", new int[]{75, 85, 95, 70, 85});
        addService("data_entry", new int[]{50, 50, 30, 95, 95});
        addService("tutoring", new int[]{80, 95, 70, 90, 75});
        addService("cleaning", new int[]{40, 60, 40, 90, 85});
        addService("writing", new int[]{70, 85, 90, 80, 95});
        addService("photography", new int[]{85, 80, 90, 75, 90});
        addService("plumbing", new int[]{85, 65, 60, 90, 85});
        addService("electrical", new int[]{90, 65, 70, 95, 95});
    }

    // Registers a service with its own freelancer ranking tree
    private void addService(String serviceName, int[] skills) {
        this.serviceRankings.put(serviceName, new MyAVLTree<>(freelancerComparator));
        this.serviceProfiles.put(serviceName, skills);
    }
    
    // Registers a new customer
    public String registerCustomer(String customerID) {
        if (customerMap.containsKey(customerID) || freelancerMap.containsKey(customerID)) {
            return "Some error occurred in register_customer.";
        }
        
        customerMap.put(customerID, new Customer(customerID));
        return "registered customer " + customerID;
    }

    // Registers a freelancer, computes composite score, inserts into ranking tree
    public String registerFreelancer(String freelancerID, String serviceType, int price, int t, int c, int r, int e, int a) {

        if (customerMap.containsKey(freelancerID) || freelancerMap.containsKey(freelancerID)) {
            return "Some error occurred in register_freelancer.";
        }
        if (price <= 0 || !serviceProfiles.containsKey(serviceType) ||
            !(t>=0&&t<=100 && c>=0&&c<=100 && r>=0&&r<=100 && e>=0&&e<=100 && a>=0&&a<=100)) {
            return "Some error occurred in register_freelancer.";
        }

        Freelancer newFreelancer = new Freelancer(freelancerID, serviceType, price, new int[]{t, c, r, e, a});

        recalculateCompositeScore(newFreelancer);
        freelancerMap.put(freelancerID, newFreelancer);

        // Add to service ranking tree
        serviceRankings.get(serviceType).insert(newFreelancer);
        
        return "registered freelancer " + freelancerID;
    }

    // Customer employs a freelancer (removes them from the available ranking)
    public String employ(String customerID, String freelancerID) {
        Customer customer = customerMap.get(customerID);
        Freelancer freelancer = freelancerMap.get(freelancerID);

        if (customer == null || freelancer == null) return "Some error occurred in employ.";
        if (activeFreelancerJobs.containsKey(freelancerID)) return "Some error occurred in employ.";
        if (platformBlacklist.containsKey(freelancerID) || customer.isBlacklisted(freelancerID))
            return "Some error occurred in employ.";

        activeFreelancerJobs.put(freelancerID, customerID);
        serviceRankings.get(freelancer.serviceType).delete(freelancer);

        customer.totalEmploymentCount++;
        return customerID + " employed " + freelancerID + " for " + freelancer.serviceType;
    }

    // Returns top candidates for a service and auto-employs best option
    public String requestJob(String customerID, String serviceType, int numCandidates) {
        Customer customer = customerMap.get(customerID);
        if (customer == null || !serviceProfiles.containsKey(serviceType))
            return "Some error occurred in request_job.";

        MyAVLTree<Freelancer> availableTree = serviceRankings.get(serviceType);
        if (availableTree == null || availableTree.isEmpty())
            return "no freelancers available";

        // Fetch extra to filter out blacklisted ones
        int buffer = numCandidates + customer.getBlacklistCount();
        ArrayList<Freelancer> allAvailable = new ArrayList<>();
        availableTree.getTopK(buffer, allAvailable);

        // Filter out blacklisted
        ArrayList<Freelancer> eligible = new ArrayList<>();
        for (Freelancer f : allAvailable) {
            if (!platformBlacklist.containsKey(f.id) && !customer.isBlacklisted(f.id)) {
                eligible.add(f);
            }
        }

        if (eligible.isEmpty()) return "no freelancers available";
        int topK = Math.min(numCandidates, eligible.size());

        // Build output of top matches
        StringBuilder output = new StringBuilder();
        output.append("available freelancers for ").append(serviceType)
              .append(" (top ").append(topK).append("):\n");

        int count = 0;
        for (Freelancer f : eligible) {
            if (count >= numCandidates) break;
            output.append(f.id)
                  .append(" - composite: ").append(f.compositeScore)
                  .append(", price: ").append(f.price)
                  .append(", rating: ").append(String.format("%.1f", f.averageRating))
                  .append("\n");
            count++;
        }

        // Auto-employ highest ranked eligible freelancer
        Freelancer best = eligible.get(0);
        activeFreelancerJobs.put(best.id, customerID);
        availableTree.delete(best);
        customer.totalEmploymentCount++;

        output.append("auto-employed best freelancer: ").append(best.id)
              .append(" for customer ").append(customerID);

        return output.toString();
    }

    // Completes job, applies rating effects, reinserts freelancer if still available
    public String completeAndRate(String freelancerID, int rating) {
        String customerID = activeFreelancerJobs.get(freelancerID);
        if (customerID == null || rating < 0 || rating > 5)
            return "Some error occurred in complete_and_rate.";

        Freelancer freelancer = freelancerMap.get(freelancerID);
        Customer customer = customerMap.get(customerID);

        activeFreelancerJobs.remove(freelancerID);

        // Apply customer payment/subsidy logic
        int price = freelancer.price;
        int payment = (int)Math.floor(price * (1.0 - customer.getSubsidyRate()));
        customer.totalSpent += payment;
        customer.updateLoyaltyPoints();

        // Update freelancer performance
        freelancer.updateRating(rating);
        freelancer.completedJobs++;
        freelancer.monthlyCompletedJobs++;

        if (rating >= 4) applySkillGains(freelancer);

        updateFreelancerRanking(freelancer, true);

        return freelancerID + " completed job for " + customerID + " with rating " + rating;
    }

    // Job cancelled by customer
    public String cancelByCustomer(String customerID, String freelancerID) {
        String activeCustomerID = activeFreelancerJobs.get(freelancerID);

        if (activeCustomerID == null || !activeCustomerID.equals(customerID))
            return "Some error occurred in cancel_by_customer.";

        Customer customer = customerMap.get(customerID);
        Freelancer freelancer = freelancerMap.get(freelancerID);

        activeFreelancerJobs.remove(freelancerID);

        customer.cancelledJobs++;
        customer.updateLoyaltyPoints();

        updateFreelancerRanking(freelancer, true);

        return "cancelled by customer: " + customerID + " cancelled " + freelancerID;
    }

    // Job cancelled by freelancer (may trigger platform ban)
    public String cancelByFreelancer(String freelancerID) {
        String customerID = activeFreelancerJobs.get(freelancerID);
        if (customerID == null) return "Some error occurred in cancel_by_freelancer.";

        Freelancer freelancer = freelancerMap.get(freelancerID);

        activeFreelancerJobs.remove(freelancerID);

        freelancer.updateRating(0);
        freelancer.cancelledJobs++;
        freelancer.monthlyCancelledJobs++;

        applySkillDegradation(freelancer);

        if (freelancer.monthlyCancelledJobs >= 5) {
            // Ban and remove from rankings permanently
            platformBlacklist.put(freelancerID, true);
            updateFreelancerRanking(freelancer, false);
            return "cancelled by freelancer: " + freelancerID + " cancelled " + customerID + "\n"
                 + "platform banned freelancer: " + freelancerID;
        } else {
            updateFreelancerRanking(freelancer, true);
            return "cancelled by freelancer: " + freelancerID + " cancelled " + customerID;
        }
    }

    // Queues a service change to be processed later
    public String changeService(String freelancerID, String newServiceType, int newPrice) {
        Freelancer freelancer = freelancerMap.get(freelancerID);
        
        if (freelancer == null || !serviceProfiles.containsKey(newServiceType) || newPrice <= 0 || freelancer.serviceType.equals(newServiceType))
            return "Some error occurred in change_service.";

        queuedServiceChanges.add(new ServiceChangeRequest(freelancerID, newServiceType, newPrice));

        return "service change for " + freelancerID + " queued from " 
                + freelancer.serviceType + " to " + newServiceType;
    }

    // Direct skill update (recalculates ranking entry)
    public String updateSkill(String freelancerID, int t, int c, int r, int e, int a) {
        Freelancer freelancer = freelancerMap.get(freelancerID);
        if (freelancer == null ||
            !(t>=0&&t<=100 && c>=0&&c<=100 && r>=0&&r<=100 && e>=0&&e<=100 && a>=0&&a<=100)) {
            return "Some error occurred in update_skill.";
        }

        freelancer.skills = new int[]{t, c, r, e, a};

        // Reinsert if available
        boolean available = !activeFreelancerJobs.containsKey(freelancerID);
        updateFreelancerRanking(freelancer, available);

        return "updated skills of " + freelancerID + " for " + freelancer.serviceType;
    }

    public String simulateMonth() {
        // Apply queued service/price changes
        for (ServiceChangeRequest request : queuedServiceChanges) {
            Freelancer f = freelancerMap.get(request.freelancerID);
            if (f == null) continue;

            boolean isAvailable = !activeFreelancerJobs.containsKey(f.id);

            // Remove from old ranking if currently listed
            if (isAvailable) {
                serviceRankings.get(f.serviceType).delete(f);
            }

            // Update fields
            f.serviceType = request.newService;
            f.price = request.newPrice;

            // Reinsert with updated values
            updateFreelancerRanking(f, isAvailable);
        }
        queuedServiceChanges.clear();

        // Update burnout, reset counters, refresh rankings
        for (Freelancer f : freelancerMap.values()) {

            // Burnout logic based on monthly activity
            if (f.isBurnedOut) {
                if (f.monthlyCompletedJobs <= 2) f.isBurnedOut = false;
            } else {
                if (f.monthlyCompletedJobs >= 5) f.isBurnedOut = true;
            }

            boolean isAvailable = !activeFreelancerJobs.containsKey(f.id);
            updateFreelancerRanking(f, isAvailable);

            // Reset monthly stats
            f.monthlyCompletedJobs = 0;
            f.monthlyCancelledJobs = 0;
        }

        // Update all customer loyalty tiers
        for (Customer c : customerMap.values()) {
            c.updateTierStatus();
        }

        return "month complete";
    }

    public String queryFreelancer(String freelancerID) {
        Freelancer f = freelancerMap.get(freelancerID);
        if (f == null) return "Some error occurred in query_freelancer.";

        return String.format(
            "%s: %s, price: %d, rating: %.1f, completed: %d, cancelled: %d, " +
            "skills: (%d,%d,%d,%d,%d), available: %s, burnout: %s",
            f.id, f.serviceType, f.price, f.averageRating, f.completedJobs,
            f.cancelledJobs, f.skills[0], f.skills[1], f.skills[2], f.skills[3], f.skills[4],
            activeFreelancerJobs.containsKey(f.id) ? "no" : "yes",
            f.isBurnedOut ? "yes" : "no"
        );
    }

    public String queryCustomer(String customerID) {
        Customer c = customerMap.get(customerID);
        if (c == null) return "Some error occurred in query_customer.";

        return String.format(
            "%s: total spent: $%d, loyalty tier: %s, blacklisted freelancer count: %d, total employment count: %d",
            c.id, c.totalSpent, c.currentTier, c.getBlacklistCount(), c.totalEmploymentCount
        );
    }

    public String blacklist(String customerID, String freelancerID) {
        Customer c = customerMap.get(customerID);
        Freelancer f = freelancerMap.get(freelancerID);
        if (c == null || f == null) return "Some error occurred in blacklist.";

        if (c.isBlacklisted(freelancerID)) return "Some error occurred in blacklist.";

        // Add to customer's blacklist
        c.blacklistFreelancer(freelancerID);
        return customerID + " blacklisted " + freelancerID;
    }

    public String unblacklist(String customerID, String freelancerID) {
        Customer c = customerMap.get(customerID);
        Freelancer f = freelancerMap.get(freelancerID);
        if (c == null || f == null) return "Some error occurred in unblacklist.";

        if (!c.isBlacklisted(freelancerID)) return "Some error occurred in unblacklist.";

        // Remove from blacklist
        c.unblacklistFreelancer(freelancerID);
        return customerID + " unblacklisted " + freelancerID;
    }

    private void updateFreelancerRanking(Freelancer freelancer, boolean isAvailable) {
        MyAVLTree<Freelancer> tree = serviceRankings.get(freelancer.serviceType);
        if (tree == null) return;

        // Remove stale entry before recalculation
        tree.delete(freelancer);

        // Update composite score for correct ordering
        recalculateCompositeScore(freelancer);

        // Reinsert if allowed and free for hiring
        if (isAvailable && !platformBlacklist.containsKey(freelancer.id)) {
            tree.insert(freelancer);
        }
    }

    private void recalculateCompositeScore(Freelancer f) {
        int[] serviceSkills = serviceProfiles.get(f.serviceType);
        if (serviceSkills == null) {
            f.compositeScore = 0;
            return;
        }

        // Find skill score with the dot product
        int dotProduct = 0;
        int sumServiceSkills = 0;
        for (int i = 0; i < 5; i++) {
            dotProduct += (int) f.skills[i] * serviceSkills[i];
            sumServiceSkills += serviceSkills[i];
        }
        double skillScore = (double) dotProduct / (100.0 * sumServiceSkills);

        // Rating score
        double ratingScore = f.averageRating / 5.0;

        // Calculate reliability score
        double reliabilityScore;
        int totalJobs = f.completedJobs + f.cancelledJobs;
        if (totalJobs == 0) reliabilityScore = 1.0;
        else reliabilityScore = 1.0 - ((double) f.cancelledJobs / totalJobs);

        double burnoutPenalty = f.isBurnedOut ? 0.45 : 0.0;

        // Weighted final score
        double weightedSum = (0.55 * skillScore) + (0.25 * ratingScore) + (0.20 * reliabilityScore);
        double finalScore = 10000.0 * (weightedSum - burnoutPenalty);

        f.compositeScore = (int) Math.floor(finalScore);
    }

    private void applySkillGains(Freelancer f) {
        // Increase relevant attributes depending on service type
        switch (f.serviceType) {
            case "paint":
                f.skills[4] = Math.min(100, f.skills[4] + 2); // A (primary)
                f.skills[3] = Math.min(100, f.skills[3] + 1); // E (secondary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                break;
            case "web_dev":
                f.skills[0] = Math.min(100, f.skills[0] + 2); // T (primary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                f.skills[2] = Math.min(100, f.skills[2] + 1); // R (secondary)
                break;
            case "graphic_design":
                f.skills[2] = Math.min(100, f.skills[2] + 2); // R (primary)
                f.skills[1] = Math.min(100, f.skills[1] + 1); // C (secondary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                break;
            case "data_entry":
                f.skills[3] = Math.min(100, f.skills[3] + 2); // E (primary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                break;
            case "tutoring":
                f.skills[1] = Math.min(100, f.skills[1] + 2); // C (primary)
                f.skills[3] = Math.min(100, f.skills[3] + 1); // E (secondary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                break;
            case "cleaning":
                f.skills[3] = Math.min(100, f.skills[3] + 2); // E (primary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                f.skills[1] = Math.min(100, f.skills[1] + 1); // C (secondary)
                break;
            case "writing":
                f.skills[4] = Math.min(100, f.skills[4] + 2); // A (primary)
                f.skills[2] = Math.min(100, f.skills[2] + 1); // R (secondary)
                f.skills[1] = Math.min(100, f.skills[1] + 1); // C (secondary)
                break;
            case "photography":
                f.skills[2] = Math.min(100, f.skills[2] + 2); // R (primary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                break;
            case "plumbing":
                f.skills[3] = Math.min(100, f.skills[3] + 2); // E (primary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                break;
            case "electrical":
                f.skills[3] = Math.min(100, f.skills[3] + 2); // E (primary)
                f.skills[4] = Math.min(100, f.skills[4] + 1); // A (secondary)
                f.skills[0] = Math.min(100, f.skills[0] + 1); // T (secondary)
                break;
        }
    }

    private void applySkillDegradation(Freelancer f) {
        // Decrease each skill by 3
        for (int i = 0; i < 5; i++) {
            f.skills[i] = Math.max(0, f.skills[i] - 3);
        }
    }

    // Creates service change request objects to be stored and applied after each month
    private static class ServiceChangeRequest {
        String freelancerID;
        String newService;
        int newPrice;

        public ServiceChangeRequest(String freelancerID, String newService, int newPrice) {
            this.freelancerID = freelancerID;
            this.newService = newService;
            this.newPrice = newPrice;
        }
    }


    // Comparator for Freelancer objects based on composite score
    private static class FreelancerComparator implements MyComparator<Freelancer> {

        @Override
        public int compare(Freelancer f1, Freelancer f2) {
            if (f1.compositeScore < f2.compositeScore) {
                return -1; // f1 has lower score → f2 comes first
            }
            if (f1.compositeScore > f2.compositeScore) {
                return 1; // f1 has higher score → f1 comes first
            }

            // Tie-breaker: compare IDs lexicographically
            return f2.id.compareTo(f1.id);
        }
    }
}