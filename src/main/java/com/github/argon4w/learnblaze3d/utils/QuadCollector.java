package com.github.argon4w.learnblaze3d.utils;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;

/**
 * A reusable low memory footprint implementation of {@link QuadCollection} builder that provides identical API as the {@link QuadCollection.Builder}.
 * The original {@link QuadCollection.Builder} is disposable, which can cause a surge in heap allocation rate when it is used in section compiling.
 *
 * @author Argon4W
 */
public class QuadCollector {

	// The global persistent collector cache for eliminating heap allocations of new instances.
	public static final ThreadLocal<QuadCollector> INSTANCE = ThreadLocal.withInitial(QuadCollector::new);

	private final	BakedQuad	[][]	quads; // The collected baked quad storage of each cull face.
	private final	int			[][]	states; // The state snapshot stack of all building collections.
	private final	int			[]		heads; // The count of baked quads of each cull face.
	private			int					size; // The size of the storage array of each cull face.
	private			int					depth; // The maximum depth of the snapshot stack.
	private			int					index; // The index of the state of the current building collections. -1 indicates that no collection is building.

	/**
	 * Constructs a new {@link QuadCollector}. Not recommended.
	 *
	 * @see QuadCollector#build(Consumer)
	 */
	public QuadCollector() {
		this.quads	= new BakedQuad	[7][];
		this.states	= new int		[7][];
		this.heads	= new int		[7];
		this.depth	= 8;
		this.size	= 8;
		this.index	= -1;

		// Initialize the array of each cull face.
		this.quads[0] = new BakedQuad[this.size]; // Direction.DOWN
		this.quads[1] = new BakedQuad[this.size]; // Direction.UP
		this.quads[2] = new BakedQuad[this.size]; // Direction.NORTH
		this.quads[3] = new BakedQuad[this.size]; // Direction.SOUTH
		this.quads[4] = new BakedQuad[this.size]; // Direction.WEST
		this.quads[5] = new BakedQuad[this.size]; // Direction.EAST
		this.quads[6] = new BakedQuad[this.size]; // null (unculled)

		// Initialize the array of the snapshot stack of each cull face.
		this.states[0] = new int[this.depth]; // Direction.DOWN
		this.states[1] = new int[this.depth]; // Direction.UP
		this.states[2] = new int[this.depth]; // Direction.NORTH
		this.states[3] = new int[this.depth]; // Direction.SOUTH
		this.states[4] = new int[this.depth]; // Direction.WEST
		this.states[5] = new int[this.depth]; // Direction.EAST
		this.states[6] = new int[this.depth]; // null (unculled)
	}

	private void resize(int min) {
		// We have enough storage, skip the resize.
		if (min <= size) {
			return;
		}

		// Next power of 2.
		size = 1 << (32 - Integer.numberOfLeadingZeros(min - 1));

		// Resize the baked quad storage of each cull face.
		quads[0] = Arrays.copyOf(quads[0], size); // Direction.DOWN
		quads[1] = Arrays.copyOf(quads[1], size); // Direction.UP
		quads[2] = Arrays.copyOf(quads[2], size); // Direction.NORTH
		quads[3] = Arrays.copyOf(quads[3], size); // Direction.SOUTH
		quads[4] = Arrays.copyOf(quads[4], size); // Direction.WEST
		quads[5] = Arrays.copyOf(quads[5], size); // Direction.EAST
		quads[6] = Arrays.copyOf(quads[6], size); // null (unculled)
	}

	/**
	 * Stores the state of the current building collection to the state snapshot stack and pushes a building collection
	 * to the collector. The collector has <strong>NO BUILDING COLLECTIONS BY DEFAULT</strong>.
	 */
	public void push() {
		var index = ++ this.index;

		if (index >= depth) {
			// Resize the depth.
			depth = depth * 2;

			states[0] = Arrays.copyOf(states[0], depth); // Direction.DOWN
			states[1] = Arrays.copyOf(states[1], depth); // Direction.UP
			states[2] = Arrays.copyOf(states[2], depth); // Direction.NORTH
			states[3] = Arrays.copyOf(states[3], depth); // Direction.SOUTH
			states[4] = Arrays.copyOf(states[4], depth); // Direction.WEST
			states[5] = Arrays.copyOf(states[5], depth); // Direction.EAST
			states[6] = Arrays.copyOf(states[6], depth); // null (unculled)
		}

		states[0][index] = heads[0]; // Direction.DOWN
		states[1][index] = heads[1]; // Direction.UP
		states[2][index] = heads[2]; // Direction.NORTH
		states[3][index] = heads[3]; // Direction.SOUTH
		states[4][index] = heads[4]; // Direction.WEST
		states[5][index] = heads[5]; // Direction.EAST
		states[6][index] = heads[6]; // null (unculled)
	}

	/**
	 * Pops the current building collection and restore the state snapshot of previous building collections. This allows
	 * nested builds of quad collections without creation of new collectors.
	 *
	 * @see QuadCollector#reset()
	 */
	public void pop() {
		// Check if there is actively building collection.
		if (index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		// Dereference all quads in baked quad storage of each cull face for GC to release them.
		// Calling reset here will trigger an additional index check, so inline here.
		for (var i = 0; i < 7; i ++) {
			Arrays.fill(quads[i], states[i][index], heads[i], null);
			// Reset the count of each cull face storage.
			heads[i] = states[i][index];
		}

		// Move to previously captured state snapshot.
		this.index --;
	}

	/**
	 * Add a baked quad to the current building collection.
	 *
	 * @param cull The cull face of the {@code quad}. A {@code null} indicates that the baked quad is unculled.
	 * @param quad The baked quad being added to the collection.
	 */
	public void addQuad(@Nullable Direction cull, @NonNull BakedQuad quad) {
		// Null check.
		Objects.requireNonNull(quad);

		// Check if there is actively building collection.
		if (index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		// Get the corresponding index of the cull face storage.
		var cullIndex = cull == null ? 6 : cull.ordinal();

		// Resize the storage when needed.
		if (heads[cullIndex] >= size) {
			resize(size * 2);
		}

		// Add the baked quad into the collector.
		quads[cullIndex][heads[cullIndex] ++] = quad;
	}

	/**
	 * Add all baked quads from another {@link QuadCollector} to the current building collection.
	 *
	 * @param that The baked quads being added to the collection.
	 * @see QuadCollector#addAll(QuadCollection)
	 */
	public void addAll(@NonNull QuadCollector that) {
		// Null check.
		Objects.requireNonNull(that);

		// Check of self-adding.
		if (this == that) {
			throw new IllegalArgumentException("Cannot add itself to the current building collection.");
		}

		// Check if there is actively building collection.
		if (that.index == -1 || this.index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		for (var i = 0; i < 7; i ++) {
			// The count of baked quads in the storage to be added.
			var count = that.heads[i] - that.states[i][that.index];

			// Skip this cull face if the count of this cull face storage is empty.
			if (count == 0) {
				continue;
			}

			// The count of the baked quads after the quads are added.
			var head = this.heads[i] + count;

			// Resize the storage array if needed.
			if (head > this.size) {
				resize(head);
			}

			// Copy the quads into this collector.
			System.arraycopy(
					that.quads	[i],
					that.states	[i][that.index],
					this.quads	[i],
					this.heads	[i],
					count
			);

			// Set the count of the baked quads.
			this.heads[i] = head;
		}
	}

	/**
	 * Add all baked quads from another {@link QuadCollection} to the current building collection.
	 *
	 * @param that The baked quads being added to the collection.
	 * @see QuadCollector#addAll(QuadCollector)
	 */
	public void addAll(@NonNull QuadCollection that) {
		// Null check.
		Objects.requireNonNull(that);

		// Check if there is actively building collection.
		if (index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		// If the collection is empty,
		if (that == QuadCollection.EMPTY /* Absolute safe here, original builder returns the constant empty instance if the builder is empty. */) {
			return;
		}

		for (int i = 0; i < Cull.DIRECTIONS.length; i ++) {
			// The cull face of the baked quads.
			var cull = Cull.DIRECTIONS[i];
			// The baked quads of certain cull faces.
			var quads = that.getQuads(cull);

			// The count of baked quads in the storage to be added.
			var count = quads.size();

			// Skip this cull face if the count of this cull face storage is empty.
			if (count == 0) {
				continue;
			}

			// The count of the baked quads after the quads is added.
			var head = this.heads[i] + count;

			// Resize the storage array if needed.
			if (head > this.size) {
				resize(head);
			}

			// Index for-loop is faster when the list supports fast random access.
			if (quads instanceof RandomAccess) {
				// Use index for-loop instead of enhanced for-loop.
				for (int j = 0; j < count; j ++) {
					this.quads[i][heads[i] + j] = quads.get(j);
				}
			} else {
				var j = 0;
				// Use enhanced for-loop instead if the list does not support fast random access.
				for (var quad : quads) {
					this.quads[i][heads[i] + (j ++)] = quad;
				}
			}

			// Set the count of the baked quads.
			this.heads[i] = head;
		}
	}

	/**
	 * Build the {@link QuadCollection} using baked quads collected from {@link QuadCollector#addQuad(Direction, BakedQuad)} and
	 * {@link QuadCollector#addAll}.
	 *
	 * @return the built collection.
	 * @see QuadCollector#reset()
	 */
	@Contract(pure = true)
	public @NonNull QuadCollection build() {
		// Check if there is actively building collection.
		if (index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		var downQuadStart		= states[0][index];
		var upQuadStart			= states[1][index];
		var northQuadStart		= states[2][index];
		var southQuadStart		= states[3][index];
		var westQuadStart		= states[4][index];
		var eastQuadStart		= states[5][index];
		var unculledQuadStart	= states[6][index];

		var downQuadCount		= heads[0] - downQuadStart; // Direction.DOWN
		var upQuadCount			= heads[1] - upQuadStart; // Direction.UP
		var northQuadCount		= heads[2] - northQuadStart; // Direction.NORTH
		var southQuadCount		= heads[3] - southQuadStart; // Direction.SOUTH
		var westQuadCount		= heads[4] - westQuadStart; // Direction.WEST
		var eastQuadCount		= heads[5] - eastQuadStart; // Direction.EAST
		var unculledQuadCount	= heads[6] - unculledQuadStart; // null (unculled)
		var allQuadCount		= downQuadCount + upQuadCount + northQuadCount + southQuadCount + westQuadCount + eastQuadCount + unculledQuadCount; // The count of all quads in the collector.

		// Returns the constant empty instance of the quad collection if the collector is empty.
		if (allQuadCount == 0) {
			return QuadCollection.EMPTY;
		}

		// Initialize the packed baked quad storage array and its list view for the collection being built.
		var allQuadsArray	= new BakedQuad[allQuadCount];
		var allQuadsList	= Arrays.asList(allQuadsArray);

		// The incremental offset of the cull face baked quad storage being copied into the packed storage array.
		var offset = 0;

		// Copy the baked quads into the packed storage array and create the list view of each cull face.
		System.arraycopy(quads[0], downQuadStart,		allQuadsArray, offset, downQuadCount);		var downQuadsList		= allQuadsList.subList(offset, offset += downQuadCount);
		System.arraycopy(quads[1], upQuadStart,			allQuadsArray, offset, upQuadCount);		var upQuadsList			= allQuadsList.subList(offset, offset += upQuadCount);
		System.arraycopy(quads[2], northQuadStart,		allQuadsArray, offset, northQuadCount);		var northQuadsList		= allQuadsList.subList(offset, offset += northQuadCount);
		System.arraycopy(quads[3], southQuadStart,		allQuadsArray, offset, southQuadCount);		var southQuadsList		= allQuadsList.subList(offset, offset += southQuadCount);
		System.arraycopy(quads[4], westQuadStart,		allQuadsArray, offset, westQuadCount);		var westQuadsList		= allQuadsList.subList(offset, offset += westQuadCount);
		System.arraycopy(quads[5], eastQuadStart,		allQuadsArray, offset, eastQuadCount);		var eastQuadsList		= allQuadsList.subList(offset, offset += eastQuadCount);
		System.arraycopy(quads[6], unculledQuadStart,	allQuadsArray, offset, unculledQuadCount);	var unculledQuadsList	= allQuadsList.subList(offset, offset += unculledQuadCount);

		// Build the quad collection.
		// FIXME: AccessTransformer required: net.minecraft.client.resources.model.geometry.QuadCollection <init>(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V
		return new QuadCollection(
				allQuadsList,
				unculledQuadsList,
				northQuadsList,
				southQuadsList,
				eastQuadsList,
				westQuadsList,
				upQuadsList,
				downQuadsList
		);
	}

	/**
	 * Reset the collector. Clear all baked quads collected in the collector for building new quad collections. This allows
	 * the collector to be reused without new heap allocations. This method will <strong>NOT</strong> pop the state of building collections.
	 *
	 * @see QuadCollector#pop()
	 */
	public void reset() {
		// Check if there is actively building collection.
		if (index == -1) {
			throw new IllegalStateException("No collection is building.");
		}

		// Dereference all quads in baked quad storage of each cull face for GC to release them.
		for (var i = 0; i < 7; i ++) {
			Arrays.fill(quads[i], states[i][index], heads[i], null);
			// Reset the count of each cull face storage.
			heads[i] = states[i][index];
		}
	}

	/**
	 * Build a quad collection using a collector passed in by the {@code consumer}. The collector is grabbed from the
	 * persistent cache ({@link QuadCollector#INSTANCE}), which eliminates the heap allocation of a collector.
	 *
	 * @param consumer The consumer to build the quad collection.
	 * @return the built quad collection.
	 */
	public static @NonNull QuadCollection build(@NonNull Consumer<QuadCollector> consumer) {
		// Null check.
		Objects.requireNonNull(consumer);

		// Grab a collector from persistent cache.
		var collector = INSTANCE.get();

		// Push a new building collection to the collector.
		collector.push();

		// In case of any exception thrown by the consumer or collector.
		try {
			// Collect quads using this collector.
			consumer.accept(collector);

			// Build the quad collection from the collector.
			return collector.build();
		} finally {
			// Pop the state and reset the collector for later quad collecting.
			collector.pop();
		}
	}
}
