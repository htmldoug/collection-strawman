package strawman.collection
package mutable

import scala.{Int, `inline`, throws, IndexOutOfBoundsException, IllegalArgumentException, Unit, Boolean, Array}
import scala.Predef.intWrapper

/** A `Buffer` is a growable and shrinkable `Seq`. */
trait Buffer[A] extends Seq[A]
  with Growable[A]
  with Shrinkable[A] {

  //TODO Prepend is a logical choice for a readable name of `+=:` but it conflicts with the renaming of `append` to `add`
  /** Prepends a single element at the front of this $coll.
    *
    *  @param elem  the element to $add.
    *  @return the $coll itself
    */
  def prepend(elem: A): this.type

  /** Alias for `prepend` */
  @`inline` final def +=: (elem: A): this.type = prepend(elem)

  @throws[IndexOutOfBoundsException]
  def insert(idx: Int, elem: A): Unit

  /** Inserts new elements at the index `idx`. Opposed to method
    *  `update`, this method will not replace an element with a new
    *  one. Instead, it will insert a new element at index `idx`.
    *
    *  @param idx     the index where a new element will be inserted.
    *  @param elems   the iterable object providing all elements to insert.
    *  @throws IndexOutOfBoundsException if `idx` is out of bounds.
    */
  @throws[IndexOutOfBoundsException]
  def insertAll(idx: Int, elems: IterableOnce[A]): Unit

  /** Removes the element at a given index position.
    *
    *  @param idx  the index which refers to the element to delete.
    *  @return   the element that was formerly at index `idx`.
    */
  @throws[IndexOutOfBoundsException]
  def remove(idx: Int): A

  /** Removes the element on a given index position. It takes time linear in
    *  the buffer size.
    *
    *  @param idx       the index which refers to the first element to remove.
    *  @param count   the number of elements to remove.
    *  @throws   IndexOutOfBoundsException if the index `idx` is not in the valid range
    *            `0 <= idx <= length - count` (with `count > 0`).
    *  @throws   IllegalArgumentException if `count < 0`.
    */
  @throws[IndexOutOfBoundsException]
  @throws[IllegalArgumentException]
  def remove(idx: Int, count: Int): Unit

  def patchInPlace(from: Int, patch: strawman.collection.Seq[A], replaced: Int): this.type

  // +=, ++=, clear inherited from Growable
  // Per remark of @ichoran, we should preferably not have these:
  //
  // def +=:(elem: A): this.type = { insert(0, elem); this }
  // def +=:(elem1: A, elem2: A, elems: A*): this.type = elem1 +=: elem2 +=: elems.toStrawman ++=: this
  // def ++=:(elems: IterableOnce[A]): this.type = { insertAll(0, elems); this }

  def dropInPlace(n: Int): this.type = { remove(0, n); this }
  def dropRightInPlace(n: Int): this.type = { remove(length - n, n); this }
  def takeInPlace(n: Int): this.type = { remove(n, length); this }
  def takeRightInPlace(n: Int): this.type = { remove(0, length - n); this }
  def sliceInPlace(start: Int, end: Int): this.type = takeInPlace(end).dropInPlace(start)

  def dropWhileInPlace(p: A => Boolean): this.type = {
    val idx = indexWhere(!p(_))
    if (idx < 0) { clear(); this } else dropInPlace(idx)
  }
  def takeWhileInPlace(p: A => Boolean): this.type = {
    val idx = indexWhere(!p(_))
    if (idx < 0) this else takeInPlace(idx)
  }
  def padToInPlace(len: Int, elem: A): this.type = {
    while (length < len) +=(elem)
    this
  }
}

trait IndexedOptimizedBuffer[A] extends IndexedOptimizedSeq[A] with Buffer[A] {

  def flatMapInPlace(f: A => IterableOnce[A]): this.type = {
    // There's scope for a better implementation which copies elements in place.
    var i = 0
    val newElemss = new Array[IterableOnce[A]](size)
    while (i < size) { newElemss(i) = f(this(i)); i += 1 }
    clear()
    i = 0
    while (i < size) { ++=(newElemss(i)); i += 1 }
    this
  }

  def filterInPlace(p: A => Boolean): this.type = {
    var i = 0
    while (i < size && p(apply(i))) i += 1
    var j = 1
    while (i < size) {
      if (p(apply(i))) {
        this(j) = this(i)
        j += 1
      }
      i += 1
    }
    takeInPlace(j)
  }

  def patchInPlace(from: Int, patch: strawman.collection.Seq[A], replaced: Int): this.type = {
    val n = patch.length min replaced
    var i = 0
    while (i < n) { update(from + i, patch(i)); i += 1 }
    if (i < patch.length) insertAll(from + i, patch.iterator().drop(i))
    else if (i < replaced) remove(from + i, replaced - i)
    this
  }
}

/** Explicit instantiation of the `Buffer` trait to reduce class file size in subclasses. */
abstract class AbstractBuffer[A] extends AbstractSeq[A] with Buffer[A]
