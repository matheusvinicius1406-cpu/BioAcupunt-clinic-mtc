# Clinical Intelligence Performance Report

## Environment

- **Type:** JVM (Robolectric), not Android device
- **CPU:** Desktop (MARCOS-GAS)
- **Build:** Debug (testDebugUnitTest)
- **Knowledge Core:** In-memory FakeDao (no Room/SQLite overhead)

## Methodology

Each benchmark runs the same operation 10 times and reports:
- **Min** — best case
- **Avg** — arithmetic mean
- **Med** — median (P50)
- **P95** — 95th percentile
- **Max** — worst case

Benchmarks use `measureTimeMillis` from Kotlin stdlib.

## Graph Traversal

| Benchmark | Entities | Depth | Min | Avg | Med | P95 | Max |
|-----------|----------|-------|-----|-----|-----|-----|-----|
| Depth 1 neighbors | 20 | 1 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Depth 3 reachable | 100 | 3 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Depth 5 reachable | 100 | 5 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Path finding (50) | 50 | - | <1ms | <1ms | <1ms | <1ms | <1ms |

**Analysis:** BFS over ~100 nodes is sub-millisecond on JVM. The expected production scale (~10K entities, ~50K relations) will need measurement on a real device, but the algorithmic complexity (O(V+E)) is well within bounds for in-memory traversal.

## Evidence Resolution

| Benchmark | Evidence Items | Min | Avg | Med | P95 | Max |
|-----------|---------------|-----|-----|-----|-----|-----|
| Resolve 1 | 1 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Resolve 10 | 10 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Resolve 50 | 50 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Resolve 100 | 100 | <1ms | <1ms | <1ms | <1ms | <1ms |

**Analysis:** Evidence resolution with FakeDao is sub-millisecond even for 100 items. Production performance with Room will add SQLite overhead, but the O(n) batch resolution is efficient.

## Differential Scoring

| Benchmark | Candidates | Min | Avg | Med | P95 | Max |
|-----------|-----------|-----|-----|-----|-----|-----|
| Small (5) | 5 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Medium (20) | 20 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Large (50) | 50 | <1ms | <1ms | <1ms | <1ms | <1ms |

**Analysis:** Differential scoring is O(candidates × evidence), all in-memory. Sub-millisecond for all tested sizes.

## End-to-End Pipeline

| Benchmark | Entities | Evidence | Min | Avg | Med | P95 | Max |
|-----------|----------|----------|-----|-----|-----|-----|-----|
| Typical | 10 | 20 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Complex | 20 | 50 | <1ms | <1ms | <1ms | <1ms | <1ms |
| Empty core | 0 | 0 | <1ms | <1ms | <1ms | <1ms | <1ms |

**Analysis:** Full pipeline (Observation → Graph → Evidence → Differential → Missing Data → Result) runs in sub-millisecond on JVM. Production performance will depend on:
1. Room database query latency
2. Entity/relation count
3. Device CPU/RAM

## Limitations

1. **JVM only** — Real Android device performance may differ (Dalvik/ART overhead, SQLite vs in-memory)
2. **FakeDao** — No Room query overhead, no index scanning
3. **Small datasets** — Production will have ~10K entities, ~50K relations
4. **No concurrent access** — Benchmarks are single-threaded
5. **Cold start** — No JVM warm-up measured

## Recommendations

1. **Device benchmarking** — Run on a mid-range Android device (API 29+) with real Room database
2. **Larger datasets** — Test with 10K+ entities to validate scalability
3. **Concurrent access** — Test with simultaneous read/write from sync engine
4. **Memory profiling** — Monitor heap usage during large traversals
5. **p95 in production** — Target <500ms for full pipeline on device

## Conclusion

All components perform sub-millisecond on JVM with synthetic data. The architecture is sound for the expected production scale. Device-level validation is the next step before marking performance as "validated."
