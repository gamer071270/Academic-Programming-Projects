public class AVLTree {

    private Node root;
    private final MyComparator<Card> comparator;
    private int size;

    // Node class for AVL Tree
    private class Node {
        Card card;
        int height;
        Node left;
        Node right;
        
        // Augmented variables for subtree health metrics
        int maxHealthSubtree; 
        int minHealthSubtree;

        Node(Card card) {
            this.card = card;
            this.height = 1;
            this.left = null;
            this.right = null;
            // Before inserting child nodes, max and min healths of the subtree are the card's current health itself.
            this.maxHealthSubtree = card.H_cur; 
            this.minHealthSubtree = card.H_cur;
        }
    }

    public AVLTree(MyComparator<Card> comparator) {
        this.comparator = comparator;
        this.root = null;
        this.size = 0;
    }

    public int getSize() {
        return this.size;
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return node.height;
    }

    // Balance computes the balance factor of the node (Left subtree height - Right subtree height)
    private int getBalance(Node node)
    {
        if (node == null)
           return 0;

        else
            return ( height(node.left) - height(node.right) );
    }
    
    // Returns the maximum health in the subtree rooted at 'node'
    private int maxHealth(Node node) {
        // Return the minimum integer, so it won't affect the max comparison
        if (node == null) {
            return Integer.MIN_VALUE;
        }
        return node.maxHealthSubtree;
    }
    
    // Returns the minimum health in the subtree rooted at 'node'
    private int minHealth(Node node) {
        // Return the maximum integer, so it won't affect the min comparison
        if (node == null) {
            return Integer.MAX_VALUE;
        }
        return node.minHealthSubtree;
    }

    // Updates some variables after rotations
    private void update(Node node) {
        if (node == null) return;
        
        // Update the height of the node
        node.height = 1 + Math.max(height(node.left), height(node.right));
        
        // Update maximum health in the subtree
        node.maxHealthSubtree = Math.max(node.card.H_cur, 
                                Math.max(maxHealth(node.left), maxHealth(node.right)));
                                
        // Update minimum health in the subtree
        node.minHealthSubtree = Math.min(node.card.H_cur, 
                                Math.min(minHealth(node.left), minHealth(node.right)));
    }

    // Does the right rotation
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;
        x.right = y;
        y.left = T2;
        
        update(y);
        update(x);
        return x;
    }

    // Does the left rotation
    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;
        y.left = x;
        x.right = T2;
        
        update(x);
        update(y);
        return y;
    }

    // Inserts a new card entering the deck
    public void insert(Card card) {
        root = insertNode(root, card);
        size++;
    }

    // Does insertion in O(logN) with recursion
    private Node insertNode(Node node, Card card) {
        if (node == null) {
            return new Node(card); // Found the correct place
        }

        int cmp = comparator.compare(card, node.card);
        if (cmp < 0) {
            node.left = insertNode(node.left, card); // If the card is smaller than the current node, go left
        } else if (cmp > 0) {
            node.right = insertNode(node.right, card); // If the card is greater than the current node, go right
        }

        update(node);

        // Do the necessary rotations to balance the tree
        int balance = getBalance(node);

        // Left-Left
        if (balance > 1 && comparator.compare(card, node.left.card) < 0) {
            return rightRotate(node);
        }
        // Right-Right
        if (balance < -1 && comparator.compare(card, node.right.card) > 0) {
            return leftRotate(node);
        }
        // Left-Right
        if (balance > 1 && comparator.compare(card, node.left.card) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        // Right-Left
        if (balance < -1 && comparator.compare(card, node.right.card) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }
    
    // Returns the mimimum node (on the leftmost side)
    private Node findMinNode(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    // Deleting a node (after a card is played, died or revived)
    public void delete(Card card) {
        root = deleteNode(root, card);
    }

    // Does the deletion in O(logN) with recursion. Returns the new root after deleting and balancing
    private Node deleteNode(Node node, Card card) {
        if (node == null) return null;

        int cmp = comparator.compare(card, node.card);
        if (cmp < 0) {
            node.left = deleteNode(node.left, card); // Target card is smaller than the current node, go left
        } else if (cmp > 0) {
            node.right = deleteNode(node.right, card); // Target card is greater than the current node, go right
        } else { // Card is found (cmp == 0)
            this.size--;
            
            if (node.left == null) {
                return node.right; // If left child is null, replace the root with the right child
            } else if (node.right == null) {
                return node.left; // If right child is null, replace the root with the left child
            }
            else { // The node has two children
                Node successor = findMinNode(node.right); // The successor is the smallest node in the right subtree.
                node.card = successor.card; // Copy it to the root
                node.right = deleteNode(node.right, successor.card); // Delete the successor
                this.size++; // Fix the size, so it only decreases once
            }
        }

        update(node);

        // Do the necessary rotations to balance the tree
        int balance = getBalance(node);

        // Left-Left
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotate(node);
        }
        // Right-Right
        if (balance < -1 && getBalance(node.right) <= 0) {
            return leftRotate(node);
        }
        // Left-Right
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        // Right-Left
        if (balance < -1 && getBalance(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
        return node;
    }

    // O(logN) search methods according to different priorities


    // P1: Survive & Kill
    public Card firstPriorityCard(int strangerAttack, int strangerHealth) {
        return P1(root, strangerAttack, strangerHealth);
    }

    private Card P1(Node node, int minHealth, int minAttack) {
        if (node == null) return null;

        // If there is no card in this subtree to survive stranger's attack, return null
        if (node.maxHealthSubtree <= minHealth) {
            return null;
        }
        
        Card card = node.card;
        Card selectedCard;
        
        // The code that calls this method is ordered according to min A_cur
        if (card.A_cur < minAttack) {
            selectedCard = P1(node.right, minHealth, minAttack); // Not enough attack value, look right
        } else {
            Card leftBest = P1(node.left, minHealth, minAttack); // Enough attack value but there may be a better one on the left

            if (leftBest != null) { // Found the best choice
                selectedCard = leftBest;
            } else { // No answer on the left, check current node
                if (card.H_cur > minHealth) {
                    selectedCard = card; // Survives the stranger's attack, so choose this
                } else {
                    selectedCard = P1(node.right, minHealth, minAttack); // Can't survive, so look to the right
                }
            }
        }
        return selectedCard;
    }
    
    // P2: Survive & Not Kill
    public Card secondPriorityCard(int strangerAttack, int strangerHealth) {
        return P2(root, strangerAttack, strangerHealth);
    }

    private Card P2(Node node, int minHealth, int maxAttack) {
        if (node == null) return null;

        // If there is no card in this subtree to survive stranger's attack, return null
        if (node.maxHealthSubtree <= minHealth) {
            return null;
        }

        Card card = node.card;
        Card selectedCard;

        // The code that calls this method is ordered according to max A_cur
        if (card.A_cur >= maxAttack) {
            selectedCard = P2(node.right, minHealth, maxAttack); // Kills, look right to find one with a smaller attack value
        } else {
            Card leftBest = P2(node.left, minHealth, maxAttack); // Enough attack value but there may be a better one on the left

            if (leftBest != null) { // Found the best choice
                selectedCard = leftBest;
            } else { // No answer on the left, check current node
                if (card.H_cur > minHealth) {
                    selectedCard = card; // Survives the stranger's attack, so choose this
                } else {
                    selectedCard = P2(node.right, minHealth, maxAttack); // Can't survive, so look to the right
                }
            }
        }
        return selectedCard;
    }
    
    // P3: Kill & Don't Survive
    public Card thirdPriorityCard(int strangerAttack, int strangerHealth) {
        return P3(root, strangerAttack, strangerHealth);
    }

    private Card P3(Node node, int maxHealth, int minAttack) {
        if (node == null) return null;

        // If all cards on this subtree survive, return null
        if (node.minHealthSubtree > maxHealth) {
            return null;
        }

        Card card = node.card;
        Card selectedCard;

        // The code that calls this method is ordered according to min A_cur
        if (card.A_cur < minAttack) {
            selectedCard = P3(node.right, maxHealth, minAttack); // Not enough attack value, look right
        } else {
            Card leftBest = P3(node.left, maxHealth, minAttack); // Enough attack value but there may be a better one on the left

            if (leftBest != null) { // Found the best choice
                selectedCard = leftBest;
            } else { // No answer on the left, check current node
                if (card.H_cur <= maxHealth) {
                    selectedCard = card; // Does not survive the stranger's attack, so choose this
                } else {
                    selectedCard = P3(node.right, maxHealth, minAttack); // Survives, so look to the right
                }
            }
        }
        return selectedCard;
    }

    // P4: Maximum Damage
    public Card fourthPriorityCard() {
        // The code that calls this method is ordered according to max A_cur
        // Just find the smallest card
        if (root == null) return null;
        return findMinNode(root).card;
    }

    // steal_card: Find the card with the min A and min H that satisfies (A_cur > A_lim) && (H_cur > H_lim)
    public Card findStealCard(int attackLimit, int healthLimit) {
        return findSteal(root, attackLimit, healthLimit);
    }
    
    private Card findSteal(Node node, int minAttack, int minHealth) {
        if (node == null) return null;

        // If the maximum health in this subtree is not enough, don't look (return null)
        if (node.maxHealthSubtree <= minHealth) {
            return null;
        }
        
        Card card = node.card;
        Card selectedCard;
        
        // The code that calls this method is ordered according to min A_cur
        if (card.A_cur <= minAttack) {
            selectedCard = findSteal(node.right, minAttack, minHealth); // Not enough attack value, look to the right
        } else {
            Card leftBest = findSteal(node.left, minAttack, minHealth); // Enough but look for a better one on the left
            
            if (leftBest != null) { // Found the best choice
                selectedCard = leftBest;
            } else { // No answer on the left, check the current node
                if (card.H_cur > minHealth) {
                    selectedCard = card; // Enough health, choose it
                } else {
                    selectedCard = findSteal(node.right, minAttack, minHealth); // Not enough health, look to the right
                }
            }
        }
        return selectedCard;
    }
    
    // Discard Pile Methods, ordered according to the H_missing values

    // Finds the card with the maximum H_missing that is smaller than value (which is healPool)
    public Card findFloorCard(int value) {
        return findFloor(root, value, null);
    }

    private Card findFloor(Node node, int value, Card bestSoFar) {
        if (node == null) {
            return bestSoFar;
        }

        Card card = (Card) node.card;
        int hMissing = card.getHMissing();
        
        if (hMissing == value) {
            return node.card; // Exact match, choose it
        }

        if (hMissing < value) { // Satisfies, but might find a better one on the right with a bigger H_missing
            return findFloor(node.right, value, node.card);
        } else { // H_missing is too big, look right
            return findFloor(node.left, value, bestSoFar);
        }
    }
    
    // Returns the card with the minimum H_missing
    public Card getMin() {
        if (root == null) {
            return null;
        }
        return findMinNode(root).card;
    }
}