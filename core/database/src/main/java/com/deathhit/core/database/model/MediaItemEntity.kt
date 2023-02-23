package com.deathhit.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.deathhit.core.database.Column

@Entity(primaryKeys = [Column.MEDIA_ITEM_ID, Column.MEDIA_ITEM_SOURCE_TYPE])
data class MediaItemEntity(
    @ColumnInfo(name = Column.DESCRIPTION) val description: String,
    @ColumnInfo(name = Column.MEDIA_ITEM_ID) val mediaItemId: String,
    @ColumnInfo(name = Column.MEDIA_ITEM_SOURCE_TYPE) val mediaItemSourceType: String,
    @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
    @ColumnInfo(name = Column.SUBTITLE) val subtitle: String,
    @ColumnInfo(name = Column.THUMB_URL) val thumbUrl: String,
    @ColumnInfo(name = Column.TITLE) val title: String
)