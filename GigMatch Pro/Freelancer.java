public class Freelancer {
    String id;
    String serviceType;
    int price;
    int[] skills;

    double averageRating;
    int totalReviews; // n_completed + n_cancelled
    int completedJobs;
    int cancelledJobs;
    int monthlyCompletedJobs; // For tracking burnout and recovery
    int monthlyCancelledJobs; // For tracking platform-blacklisted
    boolean isBurnedOut;
    int compositeScore;

    public Freelancer(String id, String serviceType, int price, int[] skills) {
        this.id = id;
        this.serviceType = serviceType;
        this.price = price;
        this.skills = skills;

        this.averageRating = 5.0;
        this.totalReviews = 0;
        this.completedJobs = 0;
        this.cancelledJobs = 0;
        this.monthlyCompletedJobs = 0;
        this.monthlyCancelledJobs = 0;
        this.isBurnedOut = false;
        this.compositeScore = 0;
    }

    // Updates the rating after completing a job
    public void updateRating(int rating) {
        this.totalReviews++;
        this.averageRating = ((this.averageRating * this.totalReviews) + rating) / (double)(this.totalReviews + 1);
    }

    // We need to override equals and hashCode to make them based on freelancer ID
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Freelancer that = (Freelancer) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}