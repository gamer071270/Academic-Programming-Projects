import java.util.ArrayList;

public class MyAVLTree<T> {
    private Node root;
    private final MyComparator<T> comparator;
    private int size;

    // Node class for AVL Tree
    private class Node {
        T data;
        int height;
        Node left;
        Node right;

        Node(T data) {
            this.data = data;
            this.height = 1;
            this.left = null;
            this.right = null;
        }
    }

    public MyAVLTree(MyComparator<T> comparator) {
        this.comparator = comparator;
        this.root = null;
        this.size = 0;
    }

    public int getSize() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
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

    // Updates some variables after rotations
    private void update(Node node) {
        if (node == null) return;
        
        // Update the height of the node
        node.height = 1 + Math.max(height(node.left), height(node.right));
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

    // Inserts a new node
    public void insert(T data) {
        root = insertNode(root, data);
        size++;
    }

    // Does insertion in O(logN) with recursion
    private Node insertNode(Node node, T data) {
        if (node == null) {
            return new Node(data); // Found the correct place
        }

        int cmp = comparator.compare(data, node.data);
        if (cmp < 0) {
            node.left = insertNode(node.left, data); // If the data is smaller than the current node, go left
        } else if (cmp > 0) {
            node.right = insertNode(node.right, data); // If the data is greater than the current node, go right
        }

        update(node);

        // Do the necessary rotations to balance the tree
        int balance = getBalance(node);

        // Left-Left
        if (balance > 1 && comparator.compare(data, node.left.data) < 0) {
            return rightRotate(node);
        }
        // Right-Right
        if (balance < -1 && comparator.compare(data, node.right.data) > 0) {
            return leftRotate(node);
        }
        // Left-Right
        if (balance > 1 && comparator.compare(data, node.left.data) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        // Right-Left
        if (balance < -1 && comparator.compare(data, node.right.data) < 0) {
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

    // Deleting a node
    public void delete(T data) {
        root = deleteNode(root, data);
    }

    // Does the deletion in O(logN) with recursion. Returns the new root after deleting and balancing
    private Node deleteNode(Node node, T data) {
        if (node == null) return null;

        int cmp = comparator.compare(data, node.data);
        if (cmp < 0) {
            node.left = deleteNode(node.left, data); // Target data is smaller than the current node, go left
        } else if (cmp > 0) {
            node.right = deleteNode(node.right, data); // Target data is greater than the current node, go right
        } else { // Data is found (cmp == 0)
            this.size--;
            
            if (node.left == null) {
                return node.right; // If left child is null, replace the root with the right child
            } else if (node.right == null) {
                return node.left; // If right child is null, replace the root with the left child
            }
            else { // The node has two children
                Node successor = findMinNode(node.right); // The successor is the smallest node in the right subtree.
                node.data = successor.data; // Copy it to the root
                node.right = deleteNode(node.right, successor.data); // Delete the successor
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

    // Public method to collect top K freelancers into a list
    public void getTopK(int k, ArrayList<T> topFreelancers) {
        getTopKRecursive(root, k, topFreelancers);
    }

    // Recursive helper to traverse the tree and collect top K
    private void getTopKRecursive(Node node, int k, ArrayList<T> topFreelancers) {
        if (node == null || topFreelancers.size() >= k) {
            return; // Stop if node is null or we have enough elements
        }

        // 1. Visit right subtree first (higher scores)
        getTopKRecursive(node.right, k, topFreelancers);

        // 2. Process current node
        if (topFreelancers.size() < k) {
            topFreelancers.add(node.data);
        } else {
            return; // Stop if list is full
        }

        // 3. Visit left subtree (lower scores)
        getTopKRecursive(node.left, k, topFreelancers);
    }

}
