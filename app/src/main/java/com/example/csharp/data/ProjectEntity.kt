package com.example.csharp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.csharp.model.CSharpProject

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val code: String,
    val isSample: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CSharpProject = CSharpProject(
        id = id,
        title = title,
        description = description,
        code = code,
        isSample = isSample,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(project: CSharpProject): ProjectEntity = ProjectEntity(
            id = project.id,
            title = project.title,
            description = project.description,
            code = project.code,
            isSample = project.isSample,
            updatedAt = project.updatedAt
        )
    }
}
