# Clinical Validation Dataset

This directory contains controlled test scenarios for validating the Clinical Intelligence pipeline.

## Classification

All scenarios are `TEST_SYNTHETIC` — they use fabricated MTC knowledge for engineering validation, not real clinical data.

**R4 applies:** No external clinical content has been imported. These scenarios exist solely to verify that the engine correctly chains: Observation → Graph → Evidence → Differential → Missing Data → Result.

## Scenarios

| ID | Description | Candidates | Expected Ranking | Missing Data | Classification |
|----|-------------|-----------|------------------|--------------|----------------|
| A | Single strong candidate | 1 | 1 ranked | Tongue, Pulse | TEST_SYNTHETIC |
| B | Two competing candidates | 2 | Ranked by score | Tongue, Pulse | TEST_SYNTHETIC |
| C | Strong contradiction | 2 | Score reduced | Tongue, Pulse | TEST_SYNTHETIC |
| D | Insufficient evidence | 0 | INSUFFICIENT_EVIDENCE | All | TEST_SYNTHETIC |
| E | Missing tongue information | 2 | Ranked by score | Tongue | TEST_SYNTHETIC |
| F | Missing pulse information | 2 | Ranked by score | Pulse | TEST_SYNTHETIC |
| G | Multi-hop graph reasoning | 3 | Chain discovered | Tongue, Pulse | TEST_SYNTHETIC |
| H | No viable candidate | 0 | INSUFFICIENT_EVIDENCE | All | TEST_SYNTHETIC |

## Schema

See `schema/clinical_observation.json` for the observation schema.
See `schema/expected_result.json` for the expected result schema.

## Usage

These scenarios are used by:
- `ClinicalIntelligenceE2ETest.kt` — E2E pipeline tests
- `ClinicalReasoningRegressionTest.kt` — Regression tests
- `ClinicalIntelligenceBenchmarkTest.kt` — Performance benchmarks

## Rules

1. No real patient data
2. No imported clinical knowledge (R4)
3. Each scenario is self-contained
4. Expected results are structural, not numeric
5. Determinism: same input → same output
