package com.bioacupunt.clinic.data.atlas

import com.bioacupunt.clinic.domain.model.AcupointDetail
import com.bioacupunt.clinic.domain.model.AcupointLocation
import com.bioacupunt.clinic.domain.model.AnatomicalRegion
import com.bioacupunt.clinic.domain.model.Meridian
import com.bioacupunt.clinic.domain.repository.AtlasRepository

/**
 * Atlas repository backed by static MTC data.
 *
 * This is the read-only canonical source for anatomical/meridian data.
 * All data comes from verified MTC references (Maciocia, Deadman).
 * In the future, can be extended with Knowledge Pack data.
 */
class StaticAtlasRepository : AtlasRepository {

    override suspend fun getMeridians(): Result<List<Meridian>> = runCatching {
        StaticMtcDataSource.meridians
    }

    override suspend fun getMeridian(id: String): Result<Meridian?> = runCatching {
        StaticMtcDataSource.getMeridian(id)
    }

    override suspend fun getRegions(): Result<List<AnatomicalRegion>> = runCatching {
        StaticMtcDataSource.regions
    }

    override suspend fun getRegion(id: String): Result<AnatomicalRegion?> = runCatching {
        StaticMtcDataSource.getRegion(id)
    }

    override suspend fun getAcupointsByMeridian(meridianId: String): Result<List<AcupointLocation>> = runCatching {
        StaticMtcDataSource.getAcupointsByMeridian(meridianId)
    }

    override suspend fun getAcupointsByRegion(regionId: String): Result<List<AcupointLocation>> = runCatching {
        StaticMtcDataSource.getAcupointsByRegion(regionId)
    }

    override suspend fun getAcupoint(id: String): Result<AcupointLocation?> = runCatching {
        StaticMtcDataSource.getAcupoint(id)
    }

    override suspend fun getAcupointDetail(id: String): Result<AcupointDetail?> = runCatching {
        val location = StaticMtcDataSource.getAcupoint(id) ?: return@runCatching null
        val meridian = StaticMtcDataSource.getMeridian(location.meridianId)
        val region = location.anatomicalRegionId?.let { StaticMtcDataSource.getRegion(it) }
        AcupointDetail(
            location = location,
            meridian = meridian,
            region = region,
        )
    }

    override suspend fun searchAcupoints(query: String): Result<List<AcupointLocation>> = runCatching {
        StaticMtcDataSource.searchAcupoints(query)
    }
}
