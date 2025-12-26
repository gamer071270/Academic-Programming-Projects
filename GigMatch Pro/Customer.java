public class Customer {
    String id;

    int totalSpent;
    int loyaltyPoint;
    int totalEmploymentCount;
    int cancelledJobs;
    String currentTier;
    MyHashTable<String, Boolean> personalBlacklist;

    public Customer(String id) {
        this.id = id;

        this.totalSpent = 0;
        this.loyaltyPoint = 0;
        this.totalEmploymentCount = 0;
        this.cancelledJobs = 0;
        this.currentTier = "BRONZE";
        this.personalBlacklist = new MyHashTable<>(17);
    }

    // Recalculates loyalty points after applying penalties for cancelled jobs
    public void updateLoyaltyPoints() {
        this.loyaltyPoint = Math.max(0, this.totalSpent - 250 * this.cancelledJobs);
    }

    // Updates the customer’s loyalty tier based on their loyalty points
    public void updateTierStatus() {
        if (this.loyaltyPoint >= 5000) {
            this.currentTier = "PLATINUM";
        } else if (this.loyaltyPoint >= 2000) {
            this.currentTier = "GOLD";
        } else if (this.loyaltyPoint >= 500) {
            this.currentTier = "SILVER";
        } else {
            this.currentTier = "BRONZE";
        }
    }

    // Returns the subsidy discount rate given to the customer
    public double getSubsidyRate() {
        switch (this.currentTier) {
            case "PLATINUM": return 0.15;
            case "GOLD":     return 0.10;
            case "SILVER":   return 0.05;
            default:         return 0.0;
        }
    }

    // Adds a freelancer to this customer's personal blacklist
    public void blacklistFreelancer(String freelancerID) {
        personalBlacklist.put(freelancerID, true);
    }

    // Removes a freelancer from the personal blacklist
    public void unblacklistFreelancer(String freelancerID) {
        personalBlacklist.remove(freelancerID);
    }

    // Checks if a freelancer is on this customer's blacklist
    public boolean isBlacklisted(String freelancerID) {
        return personalBlacklist.containsKey(freelancerID);
    }
    
    // Returns the number of blacklisted freelancers
    public int getBlacklistCount() {
        return personalBlacklist.size();
    }
}
