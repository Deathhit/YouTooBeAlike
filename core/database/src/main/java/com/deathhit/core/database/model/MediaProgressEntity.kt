package com.deathhit.core.database.model

import androidx.room.*
import com.deathhit.core.database.Column

@Entity
data class MediaProgressEntity(
    @PrimaryKey @ColumnInfo(name = Column.MEDIA_ITEM_ID) val mediaItemId: String,
    @ColumnInfo(name = Column.IS_ENDED) val isEnded: Boolean,
    @ColumnInfo(name = Column.POSITION) val position: Long
)
