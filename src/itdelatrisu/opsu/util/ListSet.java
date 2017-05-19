package itdelatrisu.opsu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A <code>List</code> implementation that does not
 * allow duplicate elements.
 * 
 * @author Lyonlancer5
 */
public class ListSet<E> implements List<E>, Set<E> {

	/** The list that stores the data used by this implementation */
	private final List<E> backingList;
	
	/**
     * Constructs an empty list with an initial capacity of ten.
     */
	public ListSet(){
		this(10);
	}
	
	/**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
	public ListSet(int initialCapacity){
		this.backingList = new ArrayList<E>(initialCapacity);
	}
	
	/**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
	public ListSet(Collection<? extends E> c){
		this.backingList = new ArrayList<E>(c);
		removeDupes();
	}
	
	@Override
	public int size() {
		return backingList.size();
	}

	@Override
	public boolean isEmpty() {
		return backingList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return backingList.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return backingList.iterator();
	}

	@Override
	public Object[] toArray() {
		return backingList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return backingList.toArray(a);
	}

	@Override
	public boolean add(E e) {
		if(contains(e)) return false;
		return backingList.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return backingList.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return backingList.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return backingList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return backingList.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return backingList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return backingList.retainAll(c);
	}

	@Override
	public void clear() {
		backingList.clear();
	}

	@Override
	public E get(int index) {
		return backingList.get(index);
	}

	@Override
	public E set(int index, E element) {
		if(contains(element)){
			backingList.remove(indexOf(element));
		}
		
		return backingList.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		backingList.add(index, element);
	}

	@Override
	public E remove(int index) {
		return backingList.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return backingList.lastIndexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return backingList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return backingList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return backingList.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return backingList.subList(fromIndex, toIndex);
	}
	
	private void removeDupes(){
		ListSet<E> sub = new ListSet<>();
		for(E element : backingList) sub.add(element);
		backingList.clear();
		backingList.addAll(sub);
	}
}
