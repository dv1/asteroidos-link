/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

// Deduplicate class name (commonMain already has a Utility.kt file)
@file:JvmName("AndroidUtility")

package org.asteroidos.link

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.util.UUID

internal suspend fun <T, R> T.checkAndRunIfPermissionsGranted(
    context: Context,
    permissions: Collection<String>,
    block: suspend T.() -> R
): R {
    val missingPermissions = mutableListOf<String>()

    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(permission)
    }

    if (missingPermissions.isNotEmpty())
        throw MissingPermissionsException(missingPermissions)

    return block.invoke(this)
}

internal fun String.toAndroidUUID(): UUID =
    UUID.fromString(this)
