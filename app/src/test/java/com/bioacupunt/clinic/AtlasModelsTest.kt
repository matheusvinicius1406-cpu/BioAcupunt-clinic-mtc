package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.AcupointDetail
import com.bioacupunt.clinic.domain.model.AcupointLocation
import com.bioacupunt.clinic.domain.model.AnatomicalRegion
import com.bioacupunt.clinic.domain.model.AnatomicalStructure
import com.bioacupunt.clinic.domain.model.AtlasNavigation
import com.bioacupunt.clinic.domain.model.AtlasView
import com.bioacupunt.clinic.domain.model.BodySide
import com.bioacupunt.clinic.domain.model.Meridian
import com.bioacupunt.clinic.domain.model.MeridianSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasModelsTest {

    @Test
    fun anatomicalRegion_defaultValues() {
        val region = AnatomicalRegion(id = "r1", name = "Braço")
        assertEquals("r1", region.id)
        assertEquals("Braço", region.name)
        assertNull(region.parentRegionId)
        assertEquals(BodySide.BILATERAL, region.bodySide)
        assertTrue(region.sourceIds.isEmpty())
    }

    @Test
    fun anatomicalRegion_hierarchy() {
        val torso = AnatomicalRegion(id = "torso", name = "Tronco")
        val arm = AnatomicalRegion(id = "arm", name = "Braço", parentRegionId = "torso")
        assertEquals("torso", arm.parentRegionId)
    }

    @Test
    fun bodySides_allValuesExist() {
        assertEquals(4, BodySide.values().size)
        assertEquals("Esquerdo", BodySide.LEFT.label)
        assertEquals("Direito", BodySide.RIGHT.label)
        assertEquals("Bilateral", BodySide.BILATERAL.label)
        assertEquals("Linha média", BodySide.MIDLINE.label)
    }

    @Test
    fun anatomicalStructure_linkedToRegion() {
        val structure = AnatomicalStructure(
            id = "s1", name = "Rádio", type = "bone", regionId = "forearm"
        )
        assertEquals("forearm", structure.regionId)
        assertEquals("bone", structure.type)
    }

    @Test
    fun meridian_completeData() {
        val meridian = Meridian(
            id = "li", name = "Large Intestine", chineseName = "大肠经",
            organ = "Large Intestine", yinYang = "Yang", element = "Metal",
        )
        assertEquals("Metal", meridian.element)
        assertEquals("Yang", meridian.yinYang)
        assertEquals("大肠经", meridian.chineseName)
    }

    @Test
    fun meridianSegment_ordersCorrectly() {
        val segment = MeridianSegment(
            id = "ms1", meridianId = "li", meridianName = "Large Intestine",
            order = 1, startPointId = "LI1", endPointId = "LI20",
            regionId = "arm",
        )
        assertEquals(1, segment.order)
        assertEquals("LI1", segment.startPointId)
        assertEquals("LI20", segment.endPointId)
    }

    @Test
    fun acupointLocation_storesCoordinates() {
        val point = AcupointLocation(
            id = "li4", pointCode = "LI4", pointName = "Hegu",
            meridianId = "li", meridianName = "Large Intestine",
            x = 0.35, y = 0.42,
        )
        assertEquals(0.35, point.x!!, 0.001)
        assertEquals(0.42, point.y!!, 0.001)
        assertNull(point.z)
    }

    @Test
    fun acupointLocation_coordinatesAreOptional() {
        val point = AcupointLocation(
            id = "st36", pointCode = "ST36", pointName = "Zusanli",
            meridianId = "st", meridianName = "Stomach",
        )
        assertNull(point.x)
        assertNull(point.y)
        assertNull(point.z)
        assertNull(point.anatomicalRegionId)
    }

    @Test
    fun acupointLocation_traditionalActions() {
        val point = AcupointLocation(
            id = "li4", pointCode = "LI4", pointName = "Hegu",
            meridianId = "li", meridianName = "Large Intestine",
            traditionalActions = listOf("Releases the exterior", "Expels wind", "Regulates qi"),
            indications = listOf("Headache", "Common cold", "Toothache"),
        )
        assertEquals(3, point.traditionalActions.size)
        assertEquals(3, point.indications.size)
    }

    @Test
    fun atlasView_allValuesExist() {
        assertEquals(5, AtlasView.values().size)
        assertEquals("Corpo", AtlasView.BODY.label)
        assertEquals("Região", AtlasView.REGION.label)
        assertEquals("Meridiano", AtlasView.MERIDIAN.label)
        assertEquals("Ponto", AtlasView.ACUPOINT.label)
        assertEquals("Detalhe", AtlasView.DETAIL.label)
    }

    @Test
    fun atlasNavigation_defaultBody() {
        val nav = AtlasNavigation()
        assertEquals(AtlasView.BODY, nav.currentView)
        assertNull(nav.selectedRegionId)
        assertNull(nav.selectedMeridianId)
        assertNull(nav.selectedAcupointId)
    }

    @Test
    fun atlasNavigation_navigateToPoint() {
        val nav = AtlasNavigation(
            currentView = AtlasView.ACUPOINT,
            selectedRegionId = "forearm",
            selectedMeridianId = "li",
            selectedAcupointId = "li4",
        )
        assertEquals(AtlasView.ACUPOINT, nav.currentView)
        assertEquals("li4", nav.selectedAcupointId)
    }

    @Test
    fun acupointDetail_combinesAllData() {
        val location = AcupointLocation(
            id = "li4", pointCode = "LI4", pointName = "Hegu",
            meridianId = "li", meridianName = "Large Intestine",
        )
        val meridian = Meridian(id = "li", name = "Large Intestine")
        val region = AnatomicalRegion(id = "hand", name = "Mão")
        val detail = AcupointDetail(
            location = location,
            meridian = meridian,
            region = region,
            evidenceCount = 5,
            sourceCount = 3,
        )
        assertEquals("LI4", detail.location.pointCode)
        assertNotNull(detail.meridian)
        assertNotNull(detail.region)
        assertEquals(5, detail.evidenceCount)
    }

    @Test
    fun acupointLocation_noCoordinatesNeverInvented() {
        // Coordinates must only exist when deliberately provided
        val point = AcupointLocation(
            id = "lv3", pointCode = "LV3", pointName = "Taichong",
            meridianId = "lv", meridianName = "Liver",
        )
        assertNull("Coordinates should not be invented", point.x)
        assertNull("Coordinates should not be invented", point.y)
        assertNull("Coordinates should not be invented", point.z)
    }
}
