package com.github.argon4w.learnblaze3d.utils;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The utility class that provides a constant array of cull directions.
 * @author Argon4W
 */
public class Cull {

	// The array of Direction including a null direction indicating the unculled face.
	// DIRECTIONS[0] = Direction.DOWN;
	// DIRECTIONS[1] = Direction.UP;
	// DIRECTIONS[2] = Direction.NORTH;
	// DIRECTIONS[3] = Direction.SOUTH;
	// DIRECTIONS[4] = Direction.WEST;
	// DIRECTIONS[5] = Direction.EAST;
	// DIRECTIONS[6] = null; // indicating unculled faces.
	public static final @Nullable Direction[] DIRECTIONS;

	static {
		// The original directions array.
		var values = Direction.values();

		// The cull directions array with a null direction (unculled) at the end of the array.
		DIRECTIONS = new Direction[values.length + 1];

		// Copy all original directions into the cull directions array.
		System.arraycopy(values, 0, DIRECTIONS, 0, values.length);
	}

	// Prevent initialization of utility class.
	private Cull() {
		throw new AssertionError();
	}
}
