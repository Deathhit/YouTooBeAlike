package com.deathhit.core.app_database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.deathhit.core.app_database.Column

@Entity(primaryKeys = [Column.LABEL, Column.MEDIA_ITEM_ID])
data class RemoteKeysEntity(
    @ColumnInfo(name = Column.LABEL) val label: String,
    @ColumnInfo(name = Column.MEDIA_ITEM_ID) val mediaItemId: String,
    @ColumnInfo(name = Column.NEXT_KEY) val nextKey: Int?,
    @ColumnInfo(name = Column.PREVIOUS_KEY) val previousKey: Int?
)