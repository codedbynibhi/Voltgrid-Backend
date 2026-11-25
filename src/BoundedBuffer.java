/**
 * Producer-consumer: a small buffer of meter readings with a fixed size.
 * put() waits while the buffer is full, take() waits while it is empty.
 */
public class BoundedBuffer {
    private final int[] items;
    private int count = 0, in = 0, out = 0;

    public BoundedBuffer(int capacity) {
        items = new int[capacity];
    }

    public synchronized void put(int value) throws InterruptedException {
        while (count == items.length) wait();     // full: producer waits
        items[in] = value;
        in = (in + 1) % items.length;
        count++;
        notifyAll();                              // tell the consumer there is something to take
    }

    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();                // empty: consumer waits
        int value = items[out];
        out = (out + 1) % items.length;
        count--;
        notifyAll();                              // tell the producer there is space
        return value;
    }

    public synchronized int size() {
        return count;
    }
}
