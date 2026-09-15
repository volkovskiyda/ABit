package com.gmail.volkovskiyda.abit.wear.di

import android.content.Context
import androidx.wear.tiles.TileService
import com.gmail.volkovskiyda.abit.core.chime.ChimeSurfaceUpdater
import com.gmail.volkovskiyda.abit.wear.tile.AbitTileService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * What only the watch has. Passed to `initKoin` as a platform module, so it overrides the shared
 * no-op binding rather than the shared graph having to know a tile exists.
 */
val wearModule =
    module {
        single<ChimeSurfaceUpdater> { TileChimeSurfaces(androidContext()) }
    }

/**
 * Pokes the tile when the armed chime changes. Without it the tile would refresh only on its own
 * freshness interval — up to twenty minutes after a schedule was edited on the phone and synced here.
 */
private class TileChimeSurfaces(
    private val context: Context,
) : ChimeSurfaceUpdater {
    override fun onArmedChimeChanged() {
        runCatching { TileService.getUpdater(context).requestUpdate(AbitTileService::class.java) }
    }
}
