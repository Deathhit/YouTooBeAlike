package com.deathhit.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deathhit.core.database.Column

@Entity(indices = [Index(value = [Column.SOURCE_URL], unique = true)])
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Column.ID) val _id: Int?,
    @ColumnInfo(name = Column.DESCRIPTION) val description: String,
    @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
    @ColumnInfo(name = Column.SUBTITLE) val subtitle: String,
    @ColumnInfo(name = Column.THUMB_URL) val thumbUrl: String,
    @ColumnInfo(name = Column.TITLE) val title: String
)