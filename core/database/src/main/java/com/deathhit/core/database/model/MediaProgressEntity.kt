package com.deathhit.core.database.model

import androidx.room.*
import com.deathhit.core.database.Column

@Entity
data class MediaProgressEntity(
    @PrimaryKey @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
    @ColumnInfo(name = Column.POSITION) val position: Long
)
