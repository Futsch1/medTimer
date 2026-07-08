package com.futsch1.medtimer.wear

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

private const val RESOURCES_VERSION = "1"
private const val PILL_IMAGE_ID = "pill_icon"

/** Pill-shaped tile: shows the next due dose, single tap marks it taken (see [MarkTakenActivity]). */
class PillTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val next = WatchDataStore.items.value
            .filter { it.status == "RAISED" }
            .minByOrNull { it.remindedEpochSecond }

        val label = next?.medicineName ?: getString(R.string.empty_today)

        val content = Column.Builder()
            .addContent(
                Image.Builder()
                    .setResourceId(PILL_IMAGE_ID)
                    .setWidth(dp(32f))
                    .setHeight(dp(32f))
                    .build()
            )
            .addContent(Text.Builder().setText(label).build())
            .build()

        val clickable = Clickable.Builder()
            .setId("mark_next_taken")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MarkTakenActivity::class.java.name)
                            .build()
                    )
                    .build()
            )
            .build()

        val root = Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(Modifiers.Builder().setClickable(clickable).build())
            .addContent(content)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(TimelineEntry.Builder().setLayout(Layout.Builder().setRoot(root).build()).build())
                    .build()
            )
            .build()

        return immediateListenableFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                PILL_IMAGE_ID,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_launcher_foreground)
                            .build()
                    )
                    .build()
            )
            .build()

        return immediateListenableFuture(resources)
    }
}
