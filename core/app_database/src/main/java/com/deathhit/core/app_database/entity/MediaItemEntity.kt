package com.deathhit.core.app_database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.deathhit.core.app_database.Column

@Entity(primaryKeys = [Column.LABEL, Column.MEDIA_ITEM_ID])
data class MediaItemEntity(
    @ColumnInfo(name = Column.DESCRIPTION) val description: String,
    @ColumnInfo(name = Column.LABEL) val label: String,
    @ColumnInfo(name = Column.MEDIA_ITEM_ID) val mediaItemId: String,
    @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
    @ColumnInfo(name = Column.SUBTITLE) val subtitle: String,
    @ColumnInfo(name = Column.THUMB_URL) val thumbUrl: String,
    @ColumnInfo(name = Column.TITLE) val title: String
)