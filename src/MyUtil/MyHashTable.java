package MyUtil;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class MyHashTable<K, V> extends Dictionary<K, V>
		implements Map<K, V>, Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3150204892091497423L;

	private transient Entry<?, ?>[] table;

	private transient int count;

	private int threshold;

	private float loadFactor;

	private transient int modCount = 0;

	public MyHashTable(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException(
					"Illegal Capacity: " + initialCapacity);
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal Load: " + loadFactor);

		if (initialCapacity == 0)
			initialCapacity = 1;
		this.loadFactor = loadFactor;
		table = new Entry<?, ?>[initialCapacity];
		threshold = (int) Math.min(initialCapacity * loadFactor,
				MAX_ARRAY_SIZE + 1);
	}

	public MyHashTable(Map<? extends K, ? extends V> t) {
		this(Math.max(2 * t.size(), 11), 0.75f);
		putAll(t);
	}

	public synchronized int size() {
		return count;
	}

	public synchronized boolean isEmpty() {
		return count == 0;
	}

	public synchronized Enumeration<K> keys() {
		return this.<K>getEnumeration(KEYS);
	}

	public synchronized Enumeration<V> elements() {
		return this.<V>getEnumeration(VALUES);
	}

	public synchronized boolean contains(Object value) {
		if (value == null) {
			throw new NullPointerException();
		}

		Entry<?, ?> tab[] = table;
		for (int i = tab.length; i-- > 0;) {
			for (Entry<?, ?> e = tab[i]; e != null; e = e.next) {
				if (e.value.equals(value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(Object key) {
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
			if ((e.hash == hash) && e.key.equals(key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(Object key) {
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
			if ((e.hash == hash) && e.key.equals(key)) {
				return (V) e.value;
			}
		}
		return null;
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	@SuppressWarnings("unchecked")
	protected void rehash() {
		int oldCapacity = table.length;
		Entry<?, ?>[] oldMap = table;

		// overflow-conscious code
		int newCapacity = (oldCapacity << 1) + 1;
		if (newCapacity - MAX_ARRAY_SIZE > 0) {
			if (oldCapacity == MAX_ARRAY_SIZE)
				// Keep running with MAX_ARRAY_SIZE buckets
				return;
			newCapacity = MAX_ARRAY_SIZE;
		}
		Entry<?, ?>[] newMap = new Entry<?, ?>[newCapacity];

		modCount++;
		threshold = (int) Math.min(newCapacity * loadFactor,
				MAX_ARRAY_SIZE + 1);
		table = newMap;

		for (int i = oldCapacity; i-- > 0;) {
			for (Entry<K, V> old = (Entry<K, V>) oldMap[i]; old != null;) {
				Entry<K, V> e = old;
				old = old.next;

				int index = (e.hash & 0x7FFFFFFF) % newCapacity;
				e.next = (Entry<K, V>) newMap[index];
				newMap[index] = e;
			}
		}
	}

	public boolean containsValue(Object value) {
		return contains(value);
	}

	private void addEntry(int hash, K key, V value, int index) {
		modCount++;

		Entry<?, ?> tab[] = table;
		if (count >= threshold) {
			rehash();

			tab = table;
			hash = key.hashCode();
			index = (hash & 0x7FFFFFFF) % tab.length;
		}

		@SuppressWarnings("unchecked")
		Entry<K, V> e = (Entry<K, V>) tab[index];
		tab[index] = new Entry<K, V>(hash, key, value, e);
		count++;
	}

	public synchronized V put(K key, V value) {
		if (value == null) {
			throw new NullPointerException();
		}

		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;

		Entry<K, V> entry = (Entry<K, V>) tab[index];
		for (; entry != null; entry = entry.next) {
			if ((entry.hash == hash) && entry.key.equals(key)) {
				V old = entry.value;
				entry.value = value;
				return old;
			}
		}

		addEntry(hash, key, value, index);
		return null;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	public synchronized V remove(Object key) {
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;

		Entry<K, V> e = (Entry<K, V>) tab[index];
		for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
			modCount++;
			if (prev != null) {
				prev.next = e.next;
			} else {
				tab[index] = e.next;
			}
			count--;
			V oldValue = e.value;
			e.value = null;
			return oldValue;
		}
		return null;
	}

	public synchronized void putAll(Map<? extends K, ? extends V> t) {
		for (Map.Entry<? extends K, ? extends V> e : t.entrySet())
			put(e.getKey(), e.getValue());
	}

	public synchronized void clear() {
		Entry<?, ?> tab[] = table;
		modCount++;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public synchronized Object clone() {
		try {
			MyHashTable<?, ?> t = (MyHashTable<?, ?>) super.clone();
			t.table = new Entry<?, ?>[table.length];
			for (int i = table.length; i-- > 0;) {
				t.table[i] = (table[i] != null) ? (Entry<?, ?>) table[i].clone()
						: null;
			}
			t.keySet = null;
			t.entrySet = null;
			t.values = null;
			t.modCount = 0;
			return t;
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError(e);
		}
	}

	public synchronized String toString() {
		int max = size() - 1;
		if (max == -1)
			return "{}";

		StringBuilder sb = new StringBuilder();
		Iterator<Map.Entry<K, V>> it = entrySet().iterator();

		sb.append('{');
		for (int i = 0;; i++) {
			Map.Entry<K, V> e = it.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == this ? "(this Map)" : key.toString());
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value.toString());

			if (i == max)
				return sb.append('}').toString();
			sb.append(", ");
		}
	}

	private <T> Enumeration<T> getEnumeration(int type) {
		if (count == 0) {
			return Collections.emptyEnumeration();
		} else {
			return new Enumerator<>(type, false);
		}
	}

	private <T> Iterator<T> getIterator(int type) {
		if (count == 0) {
			return Collections.emptyIterator();
		} else {
			return new Enumerator<>(type, true);
		}
	}

	private transient volatile Set<K> keySet;
	private transient volatile Set<Map.Entry<K, V>> entrySet;
	private transient volatile Collection<V> values;

	public Set<K> keySet() {
		if (keySet == null)
			keySet = Collections.synchronizedSet(new KeySet(), this);
		return keySet;
	}

	private class KeySet extends AbstractSet<K> {
		public Iterator<K> iterator() {
			return getIterator(KEYS);
		}

		public int size() {
			return count;
		}

		public boolean contains(Object o) {
			return containsKey(o);
		}

		public boolean remove(Object o) {
			return MyHashTable.this.remove(o) != null;
		}

		public void clear() {
			MyHashTable.this.clear();
		}
	}

	public Set<Map.Entry<K, V>> entrySet() {
		if (entrySet == null)
			entrySet = Collections.synchronizedSet(new Entry(), this);
		return entrySet;
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		public Iterator<Map.Entry<K, V>> iterator() {
			return getIterator(ENTRIES);
		}

		public boolean add(Map.Entry<K, V> o) {
			return super.add(o);
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			Entry<?, ?>[] tab = table;
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length;

			for (Entry<?, ?> e = tab[index]; e != null; e = e.next)
				if (e.hash == hash && e.equals(entry))
					return true;
			return false;
		}

		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object key = entry.getKey();
			Entry<?, ?>[] tab = table;
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length;

			@SuppressWarnings("unchecked")
			Entry<K, V> e = (Entry<K, V>) tab[index];
			for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
				if (e.hash == hash && e.equals(entry)) {
					modCount++;
					if (prev != null)
						prev.next = e.next;
					else
						tab[index] = e.next;

					count--;
					e.value = null;
					return true;
				}
			}
			return false;
		}

		public int size() {
			return count;
		}

		public void clear() {
			MyHashTable.this.clear();
		}
	}

	public Collection<V> values() {
		if (values == null)
			values = Collections.synchronizedCollection(new ValueCollection(),
					this);
		return values;
	}

	private class ValueCollection extends AbstractCollection<V> {
		public Iterator<V> iterator() {
			return getIterator(VALUES);
		}

		public int size() {
			return count;
		}

		public boolean contains(Object o) {
			return containsValue(o);
		}

		public void clear() {
			MyHashTable.this.clear();
		}
	}

	public synchronized boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Map))
			return false;
		Map<?, ?> t = (Map<?, ?>) o;
		if (t.size() != size())
			return false;

		try {
			Iterator<Map.Entry<K, V>> i = entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<K, V> e = i.next();
				K key = e.getKey();
				V value = e.getValue();
				if (value == null) {
					if (!(t.get(key) == null && t.containsKey(key)))
						return false;
				} else {
					if (!value.equals(t.get(key)))
						return false;
				}
			}
		} catch (ClassCastException unused) {
			return false;
		} catch (NullPointerException unused) {
			return false;
		}

		return true;
	}

	public synchronized int hashCode() {
		int h = 0;
		if (count == 0 || loadFactor < 0)
			return h; // Returns zero

		loadFactor = -loadFactor; // Mark hashCode computation in progress
		Entry<?, ?>[] tab = table;
		for (Entry<?, ?> entry : tab) {
			while (entry != null) {
				h += entry.hashCode();
				entry = entry.next;
			}
		}

		loadFactor = -loadFactor; // Mark hashCode computation complete

		return h;
	}

	@SuppressWarnings("unchecked")
	public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
		Objects.requireNonNull(action); // explicit check required in case
										// table is empty.
		final int expectedModCount = modCount;

		Entry<?, ?>[] tab = table;
		for (Entry<?, ?> entry : tab) {
			while (entry != null) {
				action.accept((K) entry.key, (V) entry.value);
				entry = entry.next;

				if (expectedModCount != modCount) {
					throw new ConcurrentModificationException();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void replaceAll(
			BiFunction<? super K, ? super V, ? extends V> function) {
		Objects.requireNonNull(function); // explicit check required in case
											// table is empty.
		final int expectedModCount = modCount;

		Entry<K, V>[] tab = (Entry<K, V>[]) table;
		for (Entry<K, V> entry : tab) {
			while (entry != null) {
				entry.value = Objects
						.requireNonNull(function.apply(entry.key, entry.value));
				entry = entry.next;

				if (expectedModCount != modCount) {
					throw new ConcurrentModificationException();
				}
			}
		}
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		Objects.requireNonNull(value);

		// Makes sure the key is not already in the hashtable.
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		@SuppressWarnings("unchecked")
		Entry<K, V> entry = (Entry<K, V>) tab[index];
		for (; entry != null; entry = entry.next) {
			if ((entry.hash == hash) && entry.key.equals(key)) {
				V old = entry.value;
				if (old == null) {
					entry.value = value;
				}
				return old;
			}
		}

		addEntry(hash, key, value, index);
		return null;
	}

	@Override
	public synchronized boolean remove(Object key, Object value) {
		Objects.requireNonNull(value);

		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		@SuppressWarnings("unchecked")
		Entry<K, V> e = (Entry<K, V>) tab[index];
		for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
			if ((e.hash == hash) && e.key.equals(key)
					&& e.value.equals(value)) {
				modCount++;
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				e.value = null;
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized boolean replace(K key, V oldValue, V newValue) {
		Objects.requireNonNull(oldValue);
		Objects.requireNonNull(newValue);
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		@SuppressWarnings("unchecked")
		Entry<K, V> e = (Entry<K, V>) tab[index];
		for (; e != null; e = e.next) {
			if ((e.hash == hash) && e.key.equals(key)) {
				if (e.value.equals(oldValue)) {
					e.value = newValue;
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public synchronized V replace(K key, V value) {
		Objects.requireNonNull(value);
		Entry<?, ?> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		@SuppressWarnings("unchecked")
		Entry<K, V> e = (Entry<K, V>) tab[index];
		for (; e != null; e = e.next) {
			if ((e.hash == hash) && e.key.equals(key)) {
				V oldValue = e.value;
				e.value = value;
				return oldValue;
			}
		}
		return null;
	}

	public synchronized V getOrDefault(Object key, V defaultValue) {
		V result = get(key);
		return (null == result) ? defaultValue : result;
	}

	private static class Entry<K, V> implements Map.Entry<K, V> {
		final int hash;
		final K key;
		V value;
		Entry<K, V> next;

		protected Entry(int hash, K key, V value, Entry<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new Entry<>(hash, key, value,
					(next == null ? null : (Entry<K, V>) next.clone()));
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			if (value == null)
				throw new NullPointerException();

			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			return (key == null ? e.getKey() == null : key.equals(e.getKey()))
					&& (value == null ? e.getValue() == null
							: value.equals(e.getValue()));
		}

		public int hashCode() {
			return hash ^ Objects.hashCode(value);
		}

		public String toString() {
			return key.toString() + "=" + value.toString();
		}
	}

	private static final int KEYS = 0;
	private static final int VALUES = 1;
	private static final int ENTRIES = 2;

	/**
	 * 避免意外的增加容量
	 * @author Administrator
	 *
	 * @param <T>
	 */
	private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
		Entry<?, ?>[] table = MyHashTable.this.table;
		int index = table.length;
		Entry<?, ?> entry;
		Entry<?, ?> lastReturned;
		int type;

		/**
		 * 标志当前Enumerator是作为迭代器还是枚举运行
		 * true->迭代器
		 */
		boolean iterator;

		protected int expectedModCount = modCount;

		Enumerator(int type, boolean iterator) {
			this.type = type;
			this.iterator = iterator;
		}

		public boolean hasMoreElments() {
			Entry<?, ?> e = entry;
			int i = index;
			Entry<?, ?>[] t = table;
			while (e == null && i > 0) {
				e = t[--i];
			}
			entry = e;
			index = i;
			return e != null;
		}

		@SuppressWarnings("unchecked")
		public T nextElement() {
			Entry<?, ?> et = entry;
			int i = index;
			Entry<?, ?>[] t = table;
			while (et == null && i > 0) {
				et = t[--i];
			}
			entry = et;
			index = i;
			if (et != null) {
				Entry<?, ?> e = lastReturned = entry;
				entry = e.next;
				return type == KEYS ? (T) e.key
						: (type == VALUES ? (T) e.value : (T) e);
			}
			throw new NoSuchElementException("Hashtable Enumerator");
		}

		public boolean hasNext() {
			return hasMoreElments();
		}

		public T next() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			return nextElement();
		}

		public void remove() {
			if (!iterator) {
				throw new UnsupportedOperationException();
			}
			if (lastReturned == null) {
				throw new IllegalStateException("Hashtable Enumerator");
			}
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}

			synchronized (MyHashTable.this) {
				Entry<?, ?>[] tab = MyHashTable.this.table;
				int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

				Entry<K, V> e = (Entry<K, V>) tab[index];
				for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
					if (e == lastReturned) {
						modCount++;
						expectedModCount++;
						if (prev == null) {
							tab[index] = e.next;
						} else {
							prev.next = e.next;
						}
						count--;
						lastReturned = null;
						return;
					}
				}
				throw new ConcurrentModificationException();
			}
		}
	}

}
