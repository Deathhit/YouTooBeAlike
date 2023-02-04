package com.deathhit.core.database.model

import androidx.room.*
import com.deathhit.core.database.Column

@Entity(
    foreignKeys = [ForeignKey(
        entity = MediaItemEntity::class,
        parentColumns = [Column.SOURCE_URL],
        childColumns = [Column.SOURCE_URL],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = [Column.SOURCE_URL], unique = true)]
)
data class MediaProgressEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Column.ID) val _id: Int?,
    @ColumnInfo(name = Column.POSITION) val position: Long,
    @ColumnInfo(name = Column.SOURCE_URL) val sourceUrl: String,
)
