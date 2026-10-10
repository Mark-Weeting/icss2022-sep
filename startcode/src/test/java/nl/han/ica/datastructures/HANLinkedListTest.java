package nl.han.ica.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HANLinkedListTest {

    private HANLinkedList<String> list;

    @BeforeEach
    void setUp() {
        list = new HANLinkedList<>();
    }

    @Test
    void newListIsEmpty() {
        assertEquals(0, list.getSize());
    }

    @Test
    void addFirstPutsValueAtFront() {
        list.addFirst("b");
        list.addFirst("a");

        assertEquals(2, list.getSize());
        assertEquals("a", list.getFirst());
        assertEquals("b", list.get(1));
    }

    @Test
    void insertAtStartMiddleAndEnd() {
        list.insert(0, "b");
        list.insert(0, "a");
        list.insert(2, "d");
        list.insert(2, "c");

        assertEquals(4, list.getSize());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
        assertEquals("d", list.get(3));
    }

    @Test
    void deleteRemovesValueAtPosition() {
        list.insert(0, "a");
        list.insert(1, "b");
        list.insert(2, "c");

        list.delete(1);

        assertEquals(2, list.getSize());
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(1));
    }

    @Test
    void removeFirstRemovesFrontValue() {
        list.addFirst("b");
        list.addFirst("a");

        list.removeFirst();

        assertEquals(1, list.getSize());
        assertEquals("b", list.getFirst());
    }

    @Test
    void clearEmptiesList() {
        list.addFirst("a");
        list.addFirst("b");

        list.clear();

        assertEquals(0, list.getSize());
    }

    @Test
    void invalidPositionsThrow() {
        list.addFirst("a");

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.insert(2, "x"));
    }

    @Test
    void getFirstOnEmptyListThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.getFirst());
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeFirst());
    }
}
