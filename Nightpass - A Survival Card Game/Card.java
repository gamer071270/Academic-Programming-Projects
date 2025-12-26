public class Card {
    String name;
    int A_init;
    int H_init;
    int A_base;
    int H_base;
    int A_cur;
    int H_cur;

    int entry_id; // The order of the card's entry into the deck (to be used in a tie-breaker)
    
    // Type-2 values
    int discard_id; // The order of the card's entry into the discard pile (for heal tie-breaker)
    int revival_progress; // The amount of health restored during partial revival

    // Temporary variable for prioritization during battle
    int lastUsedPriority = 0;

    public Card(String cardName, int attackInit, int healthInit, int entry_id) {
        name = cardName;
        A_init = attackInit;
        H_init = healthInit;
        
        A_base = attackInit;
        H_base = healthInit;
        A_cur = attackInit;
        H_cur = healthInit;

        this.entry_id = entry_id;

        // 0 at the beginning
        revival_progress = 0;
        discard_id = 0;
    }

    /**
     * When the card is damaged, updates its current attack value (A_cur).
     * Formula: A'_cur = max(1, floor( (A_base * H_cur) / H_base ))
     */
    public void updateCurrentAttack() {
        if (H_cur == H_base) {
            // If it is not damaged, attack value does not change
            A_cur = A_base;
        } else {
            int newAttack = (A_base * H_cur) / H_base;
            A_cur = Math.max(1, newAttack);
        }
    }

    /**
     * Applies the revival penalty to the card's base attack value (A_base).
     */
    public void applyRevivalPenalty(boolean isFullRevival) {
        double penaltyRate;
        if (isFullRevival) {
            penaltyRate = 0.90; // Full revival: 10% attack reduction
        } else {
            penaltyRate = 0.95; // Partial revival: 5% attack reduction
        }
        this.A_base = Math.max(1, (int) Math.floor(this.A_base * penaltyRate));
    }

    /**
     * Calculates the missing health value of cards in the discard pile to revive
     * H_missing = H_base - revival_progress
     */
    public int getHMissing() {
        return H_base - revival_progress;
    }
}