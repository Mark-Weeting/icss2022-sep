package nl.han.ica.datastructures;

import java.util.EmptyStackException;

public class HANStack<T> implements IHANStack<T> {

    private final IHANLinkedList<T> list = new HANLinkedList<>();

    @Override
    public void push(T value) {
        list.addFirst(value);
    }

    @Override
    public T pop() {
        T top = peek();
        list.removeFirst();
        return top;
    }

    @Override
    public T peek() {
        if (list.getSize() == 0) {
            throw new EmptyStackException();
        }
        return list.getFirst();
    }
}
