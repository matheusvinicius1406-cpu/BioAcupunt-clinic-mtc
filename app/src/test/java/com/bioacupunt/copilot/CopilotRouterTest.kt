package com.bioacupunt.copilot

import com.bioacupunt.copilot.retrieval.IntentType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §11 COPILOT ROUTER TEST
 *
 * Verifies that each intent maps to the correct tool.
 * Deterministic: same intent → same route, always.
 */
class CopilotRouterTest {

    private lateinit var router: CopilotRouter

    @Before
    fun setup() {
        router = CopilotRouter()
    }

    // ── Knowledge Search routes ─────────────────────────────────────

    @Test
    fun knowledgeSearch_mapsToKnowledgeSearch() {
        val route = router.route(IntentType.KNOWLEDGE_SEARCH)
        assertEquals(CopilotTool.KNOWLEDGE_SEARCH, route.tool)
        assertFalse(route.requiresPatientContext)
        assertTrue(route.requiresAuthentication)
        assertTrue(route.readOnly)
    }

    @Test
    fun researchQuery_mapsToKnowledgeSearch() {
        val route = router.route(IntentType.RESEARCH_QUERY)
        assertEquals(CopilotTool.KNOWLEDGE_SEARCH, route.tool)
        assertFalse(route.requiresPatientContext)
    }

    @Test
    fun generalClinicalQuery_mapsToKnowledgeSearch() {
        val route = router.route(IntentType.GENERAL_CLINICAL_QUERY)
        assertEquals(CopilotTool.KNOWLEDGE_SEARCH, route.tool)
        assertFalse(route.requiresPatientContext)
    }

    @Test
    fun clinicalAnalysis_mapsToKnowledgeSearch_withPatientContext() {
        val route = router.route(IntentType.CLINICAL_ANALYSIS)
        assertEquals(CopilotTool.KNOWLEDGE_SEARCH, route.tool)
        assertTrue(route.requiresPatientContext)
    }

    // ── Patient routes ──────────────────────────────────────────────

    @Test
    fun patientSummary_mapsToPatientSummary() {
        val route = router.route(IntentType.PATIENT_SUMMARY)
        assertEquals(CopilotTool.PATIENT_SUMMARY, route.tool)
        assertTrue(route.requiresPatientContext)
        assertTrue(route.requiresAuthentication)
        assertTrue(route.readOnly)
    }

    // ── Differential routes ─────────────────────────────────────────

    @Test
    fun differentialExplanation_mapsToDifferentialExplanation() {
        val route = router.route(IntentType.DIFFERENTIAL_EXPLANATION)
        assertEquals(CopilotTool.DIFFERENTIAL_EXPLANATION, route.tool)
        assertTrue(route.requiresPatientContext)
        assertTrue(route.requiresAuthentication)
        assertTrue(route.readOnly)
    }

    // ── Missing Data routes ─────────────────────────────────────────

    @Test
    fun missingData_mapsToMissingData() {
        val route = router.route(IntentType.MISSING_DATA)
        assertEquals(CopilotTool.MISSING_DATA, route.tool)
        assertTrue(route.requiresPatientContext)
        assertTrue(route.requiresAuthentication)
        assertTrue(route.readOnly)
    }

    // ── Evidence routes ─────────────────────────────────────────────

    @Test
    fun evidenceLookup_mapsToEvidenceLookup() {
        val route = router.route(IntentType.EVIDENCE_LOOKUP)
        assertEquals(CopilotTool.EVIDENCE_LOOKUP, route.tool)
        assertFalse(route.requiresPatientContext)
        assertTrue(route.requiresAuthentication)
        assertTrue(route.readOnly)
    }

    // ── Point routes ────────────────────────────────────────────────

    @Test
    fun pointLookup_mapsToPointLookup() {
        val route = router.route(IntentType.POINT_LOOKUP)
        assertEquals(CopilotTool.POINT_LOOKUP, route.tool)
        assertFalse(route.requiresPatientContext)
        assertTrue(route.readOnly)
    }

    // ── Formula routes ──────────────────────────────────────────────

    @Test
    fun formulaLookup_mapsToFormulaLookup() {
        val route = router.route(IntentType.FORMULA_LOOKUP)
        assertEquals(CopilotTool.FORMULA_LOOKUP, route.tool)
        assertFalse(route.requiresPatientContext)
        assertTrue(route.readOnly)
    }

    // ── Protocol routes ─────────────────────────────────────────────

    @Test
    fun protocolLookup_mapsToProtocolLookup() {
        val route = router.route(IntentType.PROTOCOL_LOOKUP)
        assertEquals(CopilotTool.PROTOCOL_LOOKUP, route.tool)
        assertFalse(route.requiresPatientContext)
        assertTrue(route.readOnly)
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun route_deterministic_sameIntentSameRoute() {
        val route1 = router.route(IntentType.KNOWLEDGE_SEARCH)
        val route2 = router.route(IntentType.KNOWLEDGE_SEARCH)

        assertEquals(route1.tool, route2.tool)
        assertEquals(route1.requiresPatientContext, route2.requiresPatientContext)
        assertEquals(route1.requiresAuthentication, route2.requiresAuthentication)
        assertEquals(route1.readOnly, route2.readOnly)
    }

    // ── All tools covered ───────────────────────────────────────────

    @Test
    fun allIntentTypes_haveRoute() {
        // Ensure every IntentType has a mapping
        for (intent in IntentType.entries) {
            val route = router.route(intent)
            assertNotNull("Intent $intent should have a route", route.tool)
        }
    }

    // ── Permissions ─────────────────────────────────────────────────

    @Test
    fun allRoutes_requireAuthentication() {
        for (intent in IntentType.entries) {
            val route = router.route(intent)
            assertTrue(
                "Intent $intent should require authentication",
                route.requiresAuthentication
            )
        }
    }

    @Test
    fun allRoutes_areReadOnly() {
        for (intent in IntentType.entries) {
            val route = router.route(intent)
            assertTrue(
                "Intent $intent should be read-only",
                route.readOnly
            )
        }
    }
}
