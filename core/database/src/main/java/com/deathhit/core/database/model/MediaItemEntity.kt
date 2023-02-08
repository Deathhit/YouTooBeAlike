package com.deathhit.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deathhit.core.database.Column

@Entity
data class MediaItemEntity(
    @PrimaryKey @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
    @ColumnInfo(name = Column.DESCRIPTION) val description: String,
    @ColumnInfo(name = Column.SUBTITLE) val subtitle: String,
    @ColumnInfo(name = Column.THUMB_URL) val thumbUrl: String,
    @ColumnInfo(name = Column.TITLE) val title: String
)