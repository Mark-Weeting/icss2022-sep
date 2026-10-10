package nl.han.ica.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EmptyStackException;

import static org.junit.jupiter.api.Assertions.*;

class HANStackTest {

    private HANStack<Integer> stack;

    @BeforeEach
    void setUp() {
        stack = new HANStack<>();
    }

    @Test
    void popReturnsValuesInLifoOrder() {
        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
    }

    @Test
    void peekDoesNotRemoveValue() {
        stack.push(1);
        stack.push(2);

        assertEquals(2, stack.peek());
        assertEquals(2, stack.peek());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.peek());
    }

    @Test
    void emptyStackThrows() {
        assertThrows(EmptyStackException.class, () -> stack.pop());
        assertThrows(EmptyStackException.class, () -> stack.peek());
    }

    @Test
    void stackIsEmptyAgainAfterPoppingEverything() {
        stack.push(1);
        stack.pop();

        assertThrows(EmptyStackException.class, () -> stack.peek());
    }
}
