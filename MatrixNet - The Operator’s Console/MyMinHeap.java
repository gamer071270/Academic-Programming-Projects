/**
 * Custom Binary Min-Heap implementation.
 * Maintains the smallest State object at the root (index 0).
 */
public class MyMinHeap {
    private State[] heap;
    private int size;

    public MyMinHeap(int capacity) {
        heap = new State[capacity];
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(State val) {
        // Dynamic resizing: Double the array capacity if the heap is full
        if (size == heap.length) {
            State[] newHeap = new State[heap.length * 2];
            for (int i = 0; i < heap.length; i++) {
                newHeap[i] = heap[i];
            }
            heap = newHeap;
        }
        
        // Insert at the next available slot (end) and float up to restore heap property
        heap[size] = val;
        bubbleUp(size);
        size++;
    }

    public State poll() {
        if (size == 0) return null;

        State min = heap[0]; // Root is always the minimum element

        // Move the last element to the root and remove the last node
        heap[0] = heap[size - 1];
        heap[size - 1] = null; // Clear reference to prevent memory leak
        size--;

        // Sink the new root down to its correct position
        bubbleDown(0);
        return min;
    }

    // Restore min-heap property by moving a node up until it's larger than its parent 
    private void bubbleUp(int index) {
        while (index > 0) {
            int parentIdx = (index - 1) / 2;
            if (heap[index].compareTo(heap[parentIdx]) < 0) {
                swap(index, parentIdx);
                index = parentIdx;
            }
            else {
                break;
            }
        }
    }

    // Restore min-heap property by moving a node down, swapping with the smaller child 
    private void bubbleDown(int index) {
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int smallest = index;

            // Find smallest among node, left child, and right child
            if (leftChild < size && heap[leftChild].compareTo(heap[smallest]) < 0) {
                smallest = leftChild;
            }
            if (rightChild < size && heap[rightChild].compareTo(heap[smallest]) < 0) {
                smallest = rightChild;
            }

            if (smallest != index) {
                swap(index, smallest);
                index = smallest;
            }
            else {
                break; // Node is in the correct position
            }
        }
    }

    private void swap(int i, int j) {
        State temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
}