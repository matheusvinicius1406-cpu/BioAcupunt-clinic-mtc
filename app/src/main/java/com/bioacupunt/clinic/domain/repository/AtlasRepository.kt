package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.AcupointLocation
import com.bioacupunt.clinic.domain.model.AcupointDetail
import com.bioacupunt.clinic.domain.model.AnatomicalRegion
import com.bioacupunt.clinic.domain.model.Meridian

/**
 * Repository for Atlas data — anatomical regions, meridians, acupoints.
 *
 * Data comes from StaticMtcDataSource (canonical MTC references).
 * In the future, can be extended with Knowledge Packs.
 */
interface AtlasRepository {
    suspend fun getMeridians(): Result<List<Meridian>>
    suspend fun getMeridian(id: String): Result<Meridian?>
    suspend fun getRegions(): Result<List<AnatomicalRegion>>
    suspend fun getRegion(id: String): Result<AnatomicalRegion?>
    suspend fun getAcupointsByMeridian(meridianId: String): Result<List<AcupointLocation>>
    suspend fun getAcupointsByRegion(regionId: String): Result<List<AcupointLocation>>
    suspend fun getAcupoint(id: String): Result<AcupointLocation?>
    suspend fun getAcupointDetail(id: String): Result<AcupointDetail?>
    suspend fun searchAcupoints(query: String): Result<List<AcupointLocation>>
}
