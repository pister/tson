package com.github.pister.tson.utils;


import java.lang.reflect.Array;

/**
 * @author pister 2011-12-22 10:11:30
 */
public final class ArrayUtil {

	private ArrayUtil() {}

	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	public static boolean isEmpty(Object[] array) {
		if (array == null || array.length == 0) {
			return true;
		}
		return false;
	}
	
	public static int getLength(Object array) {
		if (array == null) {
			return 0;
		}
		if (!array.getClass().isArray()) {
			return 0;
		}
		return Array.getLength(array);
	}
	
	public static int getLength(Object[] array) {
		if (array == null || array.length == 0) {
			return 0;
		}
		return array.length;
	}

	public static boolean isArrayEquals(int[] array1, int[] array2) {
		if (array1 == array2) {
			return true;
		}
		if (array1 == null || array2 == null) {
			return false;
		}
		if (array1.length != array2.length) {
			return false;
		}
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i]) {
				return false;
			}
		}
		return true;
	}

}
