

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Collections;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
class ConsumingQueueIterator<T> extends AbstractIterator<T> {
    private final Queue<T> queue;

    ConsumingQueueIterator(T... elements) {
        // Uses LinkedList because ArrayDeque isn't GWT-compatible for now =(
        this.queue = Lists.newLinkedList();
        Collections.addAll(queue, elements);
    }

    ConsumingQueueIterator(Queue<T> queue) {
        this.queue = checkNotNull(queue);
    }

    @Override
    public T computeNext() {
        return queue.isEmpty() ? endOfData() : queue.remove();
    }
}
