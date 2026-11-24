package at.spengergasse.sj2526pos1javaseed.benchmark;

import java.util.List;

/**
 * body für POST /tests/compare
 *
 * - scales: liste von skalierungen, z.b. [1000, 10000, 100000]
 *   (leer/null -> scale nehmen, default 1000)
 * - scale: einzelne skalierung, wird zu scales
 */
public record CompareRunRequest(Integer scale, List<Integer> scales) {
}
