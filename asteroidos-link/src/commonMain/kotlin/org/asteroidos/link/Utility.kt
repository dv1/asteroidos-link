/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import kotlin.math.max

/**
 * Byte to Int conversion that treats all 8 bits of the byte as a positive value.
 *
 * Currently, support for unsigned byte (UByte) is still experimental
 * in Kotlin. The existing Byte type is signed. This poses a problem
 * when one needs to bitwise manipulate bytes, since the MSB will be
 * interpreted as a sign bit, leading to unexpected outcomes. Example:
 *
 * Example byte: 0xA2 (in binary: 0b10100010)
 *
 * Code:
 *
 *     val b = 0xA2.toByte()
 *     println("%08x".format(b.toInt()))
 *
 * Result:
 *
 *     ffffffa2
 *
 *
 * This is the result of the MSB of 0xA2 being interpreted as a sign
 * bit. This in turn leads to 0xA2 being interpreted as the negative
 * value -94. When cast to Int, a negative -94 Int value is produced.
 * Due to the 2-complement logic, all upper bits are set, leading to
 * the hex value 0xffffffa2. By masking out all except the lower
 * 8 bits, the correct positive value is retained:
 *
 *     println("%08x".format(b.toPosInt() xor 7))
 *
 * Result:
 *
 *     000000a2
 *
 * This is for example important when doing bit shifts:
 *
 *     println("%08x".format(b.toInt() ushr 4))
 *     println("%08x".format(b.toPosInt() ushr 4))
 *     println("%08x".format(b.toInt() shl 4))
 *     println("%08x".format(b.toPosInt() shl 4))
 *
 * Result:
 *
 *     0ffffffa
 *     0000000a
 *     fffffa20
 *     00000a20
 *
 * toPosInt produces the correct results.
 */
internal fun Byte.toPosInt() = toInt() and 0xFF

/**
 * Byte to Long conversion that treats all 8 bits of the byte as a positive value.
 *
 * This behaves identically to [Byte.toPosInt], except it produces a Long instead
 * of an Int value.
 */
internal fun Byte.toPosLong() = toLong() and 0xFF

/**
 * Int to Long conversion that treats all 32 bits of the Int as a positive value.
 *
 * This behaves just like [Byte.toPosLong], except it is applied on Int values,
 * and extracts 32 bits instead of 8.
 */
internal fun Int.toPosLong() = toLong() and 0xFFFFFFFFL

/**
 * Produces a hex string out of an Int.
 *
 * String.format() is JVM specific, so we can't use it in multiplatform projects.
 * Hence the existence of this function.
 *
 * @param width Width of the hex string. If the actual hex string is shorter
 *        than this, the excess characters to the left (the leading characters)
 *        are filled with zeros. If a "0x" prefix is added, the prefix length is
 *        not considered part of the hex string. For example, a width of 4 and
 *        a hex string of 0x45 will produce 0045 with no prefix and 0x0045 with
 *        prefix.
 * @param prependPrefix If true, the "0x" prefix is prepended.
 * @return Hex string representation of the Int.
 */
internal fun Int.toHexString(width: Int, prependPrefix: Boolean = true): String {
    val prefix = if (prependPrefix) "0x" else ""
    val hexstring = this.toString(16)
    val numLeadingChars = max(width - hexstring.length, 0)
    return prefix + "0".repeat(numLeadingChars) + hexstring
}

/**
 * Produces a hex string out of a Byte.
 *
 * String.format() is JVM specific, so we can't use it in multiplatform projects.
 * Hence the existence of this function.
 *
 * @param width Width of the hex string. If the actual hex string is shorter
 *        than this, the excess characters to the left (the leading characters)
 *        are filled with zeros. If a "0x" prefix is added, the prefix length is
 *        not considered part of the hex string. For example, a width of 4 and
 *        a hex string of 0x45 will produce 0045 with no prefix and 0x0045 with
 *        prefix.
 * @param prependPrefix If true, the "0x" prefix is prepended.
 * @return Hex string representation of the Byte.
 */
internal fun Byte.toHexString(width: Int, prependPrefix: Boolean = true): String {
    val intValue = this.toPosInt()
    val prefix = if (prependPrefix) "0x" else ""
    val hexstring = intValue.toString(16)
    val numLeadingChars = max(width - hexstring.length, 0)
    return prefix + "0".repeat(numLeadingChars) + hexstring
}

/**
 * Produces a hexadecimal string representation of the bytes in the array.
 *
 * The string is formatted with a separator (one whitespace character by default)
 * between the bytes. For example, the byte array 0x8F, 0xBC results in "8F BC".
 *
 * @return The string representation.
 */
internal fun ByteArray.toHexString(separator: String = " ") =
    this.joinToString(separator) { it.toHexString(width = 2, prependPrefix = false) }

/**
 * Produces a hexadecimal string representation of the bytes in the list.
 *
 * The string is formatted with a separator (one whitespace character by default)
 * between the bytes. For example, the byte list 0x8F, 0xBC results in "8F BC".
 *
 * @return The string representation.
 */
internal fun List<Byte>.toHexString(separator: String = " ") =
    this.joinToString(separator) { it.toHexString(width = 2, prependPrefix = false) }
