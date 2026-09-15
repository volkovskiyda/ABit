package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/**
 * The sound a boundary makes on a platform that has no notification sound to borrow. Android and Wear
 * hand the job to the system; the Mac and the browser have to make the noise themselves, which is why
 * this seam only exists on the non-Android side.
 */
interface Bell {
    suspend fun ring(sound: ChimeSound)
}
