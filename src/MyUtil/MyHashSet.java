package MyUtil;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Spliterator;

public class MyHashSet<E> extends AbstractSet<E>
		implements Set<E>, Cloneable, Serializable {

	static final long serialVersionUID = 6259406240985672579L;

	private transient HashMap<E, Object> map;

	private static final Object PRESENT = new Object();

	public MyHashSet() {
		map = new HashMap<>();
	}

	public MyHashSet(Collection<? extends E> c) {
		map = new HashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
		addAll(c);
	}

	public MyHashSet(int initialCapacity, float loadFactor) {
		map = new HashMap<>(initialCapacity, loadFactor);
	}

	public MyHashSet(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	MyHashSet(int initialCapacity, float loadFactor, boolean dummy) {
		map = new LinkedHashMap<>(initialCapacity, loadFactor);
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	public boolean add(E e) {
		return map.put(e, PRESENT) == null;
	}

	public boolean remove(Object o) {
		return map.remove(o) == PRESENT;
	}

	public void clear() {
		map.clear();
	}

	public Object clone() {
		try {
			MyHashSet<E> newSet = (MyHashSet<E>) super.clone();
			newSet.map = (HashMap<E, Object>) map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();

		// Write out HashMap capacity and load factor
		s.writeInt(map.capacity());
		s.writeFloat(map.loadFactor());

		// Write out size
		s.writeInt(map.size());

		// Write out all elements in the proper order.
		for (E e : map.keySet())
			s.writeObject(e);
	}

	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();

		// Read capacity and verify non-negative.
		int capacity = s.readInt();
		if (capacity < 0) {
			throw new InvalidObjectException("Illegal capacity: " + capacity);
		}

		// Read load factor and verify positive and non NaN.
		float loadFactor = s.readFloat();
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new InvalidObjectException(
					"Illegal load factor: " + loadFactor);
		}

		// Read size and verify non-negative.
		int size = s.readInt();
		if (size < 0) {
			throw new InvalidObjectException("Illegal size: " + size);
		}

		// Set the capacity according to the size and load factor ensuring that
		// the HashMap is at least 25% full but clamping to maximum capacity.
		capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
				HashMap.MAXIMUM_CAPACITY);

		// Create backing HashMap
		map = (((MyHashSet<?>) this) instanceof LinkedHashSet
				? new LinkedHashMap<E, Object>(capacity, loadFactor)
				: new HashMap<E, Object>(capacity, loadFactor));

		// Read in all elements in the proper order.
		for (int i = 0; i < size; i++) {
			@SuppressWarnings("unchecked")
			E e = (E) s.readObject();
			map.put(e, PRESENT);
		}
	}

	public Spliterator<E> spliterator() {
		return new HashMap.KeySpliterator<E, Object>(map, 0, -1, 0, 0);
	}
}
