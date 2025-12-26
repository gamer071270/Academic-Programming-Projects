public class Game {

    private int entryCounter = 0;
    private int discardCounter = 0;

    private int survivorScore = 0;
    private int strangerScore = 0;

    private int healPool;

    // There are three AVL trees to manage the deck and discard pile.

    // Tree 1: For P1, P3, and steal_card. Sorts by Min A_cur, then Min H_cur.
    private final AVLTree deckTreeMinAttack;

    // Tree 2: For P2 and P4. Sorts by Max A_cur, then Min H_cur.
    private final AVLTree deckTreeMaxAttack;

    // Tree 3: For managing the discard pile. Sorts by Min H_missing.
    private final AVLTree discardPileTree;

    public Game() {
        deckTreeMinAttack = new AVLTree(new DeckMinAttackComparator());
        deckTreeMaxAttack = new AVLTree(new DeckMaxAttackComparator());
        discardPileTree = new AVLTree(new DiscardComparator());
    }

    // draw_card <card_name> <attack_init> <health_init>
    // Draw a new card and add it to the deck
    public String draw_card(String name, int attackInit, int healthInit) {
        Card card = new Card(name, attackInit, healthInit, ++entryCounter);
        
        deckTreeMinAttack.insert(card);
        deckTreeMaxAttack.insert(card);
        
        return "Added " + name + " to the deck";
    }

    // battle <stranger_attack> <stranger_health> <heal_pool>
    // Manages the battle and healing phases (for Type-2)
    public String battle(int strangerAttack, int strangerHealth, int heal_pool_amount) {
        Card playedCard = findOptimalCard(strangerAttack, strangerHealth);

        int priorityNumber = 0;
        healPool = heal_pool_amount;
        String out;

        if (playedCard == null) { // No card to play
            strangerScore += 2;
            out = "No card to play";
        
        } else {
            priorityNumber = playedCard.lastUsedPriority;
            
            // Delete the played card from both trees temporarily
            deckTreeMinAttack.delete(playedCard);
            deckTreeMaxAttack.delete(playedCard);

            // New health values for both survivor and stranger
            int survivorOldHealth = playedCard.H_cur;
            int strangerOldHealth = strangerHealth;

            int survivorNewHealth = playedCard.H_cur - strangerAttack;
            int strangerNewHealth = strangerHealth - playedCard.A_cur;

            // Scoring
            if (survivorNewHealth <= 0) {
                strangerScore += 2;
            }
            if (strangerNewHealth <= 0) {
                survivorScore += 2;
            }
            if (survivorNewHealth > 0 && survivorNewHealth < survivorOldHealth) {
                strangerScore += 1;
            }
            if (strangerNewHealth > 0 && strangerNewHealth < strangerOldHealth) {
                survivorScore += 1;
            }

            // Update the played card's health and decide its fate
            if (survivorNewHealth <= 0) {
                // The card is discarded
                playedCard.H_cur = 0;
                playedCard.revival_progress = 0;
                playedCard.discard_id = ++discardCounter;
                discardPileTree.insert(playedCard);
                out = "the played card is discarded";
            } else {
                // The card survived and it is returned to the deck
                playedCard.H_cur = survivorNewHealth;
                playedCard.updateCurrentAttack();
                
                // Since the card is re-entering the deck, update its entry_id
                playedCard.entry_id = ++entryCounter;

                // Add it back to both trees
                deckTreeMinAttack.insert(playedCard);
                deckTreeMaxAttack.insert(playedCard);
                out = "the played card returned to deck";
            }
        }

        // Healing phase for Type-2
        int revivedCount = 0;
        if (healPool > 0 && discardPileTree.getSize() > 0) {
            
            boolean canRevive = true;
            while (canRevive) {
                // Find the card with the largest H_missing smaller than or equal to heal pool points
                Card toRevive = discardPileTree.findFloorCard(healPool);
                
                if (toRevive != null) {  // Fully revival
                    
                    // Decrease the heal pool by the amount of hp the card needs to revive
                    healPool -= toRevive.getHMissing(); 
                    
                    discardPileTree.delete(toRevive); // Remove it from the discard pile

                    // Revive the card
                    toRevive.H_cur = toRevive.H_base;
                    toRevive.revival_progress = 0; 
                    toRevive.applyRevivalPenalty(true); // Apply %10 penalty
                    toRevive.updateCurrentAttack();
                    toRevive.entry_id = ++entryCounter; 

                    // Add it back to the deck
                    deckTreeMinAttack.insert(toRevive);
                    deckTreeMaxAttack.insert(toRevive);
                    revivedCount++;
                    
                } else { // Not enough heal points in the heal pool to fully revive a card
                    canRevive = false; 
                }
            }

            // Partial revival
            if (healPool > 0 && discardPileTree.getSize() > 0) {
                Card toPartiallyRevive = discardPileTree.getMin(); // Find the card with the minimum H_missing

                if (toPartiallyRevive != null) {
                        discardPileTree.delete(toPartiallyRevive); // Remove from the tree temporarily
                        toPartiallyRevive.revival_progress += healPool; // Apply the heal points to the revival progress of the card
                        healPool = 0; 
                        toPartiallyRevive.applyRevivalPenalty(false); // Apply %5 penalty
                        toPartiallyRevive.discard_id = ++discardCounter; 
                        discardPileTree.insert(toPartiallyRevive); // Add it to the discard pile again with its new H_missing
                }
            }
        } // Healing phase completed
        

        // Format the output
        if (playedCard == null) {
            return out + ", " + revivedCount + " cards revived";
        } else {
            return "Found with priority " + priorityNumber + ", Survivor plays " + playedCard.name + ", " + 
                   out + ", " + revivedCount + " cards revived";
        }
    }

    // Find the best card to play by trying the different priorities one by one
    private Card findOptimalCard(int strangerAttack, int strangerHealth) {
        Card selectedCard;

        selectedCard = deckTreeMinAttack.firstPriorityCard(strangerAttack, strangerHealth);
        if (selectedCard != null) {
            selectedCard.lastUsedPriority = 1;
            return selectedCard;
        }

        selectedCard = deckTreeMaxAttack.secondPriorityCard(strangerAttack, strangerHealth);
        if (selectedCard != null) {
            selectedCard.lastUsedPriority = 2;
            return selectedCard;
        }

        selectedCard = deckTreeMinAttack.thirdPriorityCard(strangerAttack, strangerHealth);
        if (selectedCard != null) {
            selectedCard.lastUsedPriority = 3;
            return selectedCard;
        }

        selectedCard = deckTreeMaxAttack.fourthPriorityCard();
        if (selectedCard != null) {
            selectedCard.lastUsedPriority = 4;
            return selectedCard;
        }

        return null; // No cards to play
    }

    // steal_card <attack_limit> <health_limit>
    // Find which card stranger steals
    public String steal_card(int attackLimit, int healthLimit) {
        Card cardToSteal = deckTreeMinAttack.findStealCard(attackLimit, healthLimit);

        if (cardToSteal == null) {
            return "No card to steal";
        } else { // The strangers steals the card, so remove it from the deck
            deckTreeMinAttack.delete(cardToSteal);
            deckTreeMaxAttack.delete(cardToSteal);
            
            return "The Stranger stole the card: " + cardToSteal.name;
        }
    }

    // Number of cards in the deck
    public String deckCount() {
        return "Number of cards in the deck: " + deckTreeMinAttack.getSize();
    }

    // Number of cards in the discard pile
    public String discardPileCount() {
        return "Number of cards in the discard pile: " + discardPileTree.getSize();
    }

    // Returns the current winner according to the scores
    public String findWinning() {
        if (survivorScore >= strangerScore) {
            return "The Survivor, Score: " + survivorScore;
        } else {
            return "The Stranger, Score: " + strangerScore;
        }
    }

    // Comparator classes decide how the cards in the AVL trees are going to be ordered

    // Order according to increasing A_cur, then increasing H_cur, then increasing entry_id
    private static class DeckMinAttackComparator implements MyComparator<Card> {
        @Override
        public int compare(Card c1, Card c2) {
            if (c1.A_cur != c2.A_cur) {
                return c1.A_cur - c2.A_cur;
            }
            if (c1.H_cur != c2.H_cur) {
                return c1.H_cur - c2.H_cur;
            }
            return c1.entry_id - c2.entry_id;
        }
    }

    // Order according to decreasing A_cur, then increasing H_cur, then increasing entry_id
    private static class DeckMaxAttackComparator implements MyComparator<Card> {
        @Override
        public int compare(Card c1, Card c2) {
            // Azalan A_cur için c2'den c1'i çıkar
            if (c1.A_cur != c2.A_cur) {
                return c2.A_cur - c1.A_cur;
            }
            if (c1.H_cur != c2.H_cur) {
                return c1.H_cur - c2.H_cur;
            }
            return c1.entry_id - c2.entry_id;
        }
    }

    // Order according to increasing H_missing, then increasing discard_id
    private static class DiscardComparator implements MyComparator<Card> {
        @Override
        public int compare(Card c1, Card c2) {
            int hMissing1 = c1.getHMissing();
            int hMissing2 = c2.getHMissing();
            if (hMissing1 != hMissing2) {
                return hMissing1 - hMissing2;
            }
            return c1.discard_id - c2.discard_id;
        }
    }
}
