/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.graql.internal.analytics;

import ai.grakn.concept.TypeName;
import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ClusterSizeMapReduce extends GraknMapReduce<Long> {

    private static final String CLUSTER_LABEL = "clusterSizeMapReduce.clusterLabel";
    private static final String CLUSTER_SIZE = "clusterSizeMapReduce.clusterSize";

    public ClusterSizeMapReduce() {
    }

    public ClusterSizeMapReduce(Set<TypeName> selectedTypes, String clusterLabel) {
        super(selectedTypes);
        this.persistentProperties.put(CLUSTER_LABEL, clusterLabel);
    }

    public ClusterSizeMapReduce(Set<TypeName> selectedTypes, String clusterLabel, Long clusterSize) {
        this(selectedTypes, clusterLabel);
        this.persistentProperties.put(CLUSTER_SIZE, clusterSize);
    }

    @Override
    public void safeMap(final Vertex vertex, final MapEmitter<Serializable, Long> emitter) {
        if (selectedTypes.contains(Utility.getVertexType(vertex))) {
            emitter.emit(vertex.value((String) persistentProperties.get(CLUSTER_LABEL)), 1L);
        } else {
            emitter.emit(NullObject.instance(), 0L);
        }
    }

    @Override
    Long reduceValues(Iterator<Long> values) {
        return IteratorUtils.reduce(values, 0L, (a, b) -> a + b);
    }

    @Override
    public Map<Serializable, Long> generateFinalResult(Iterator<KeyValue<Serializable, Long>> keyValues) {
        if (this.persistentProperties.containsKey(CLUSTER_SIZE)) {
            long clusterSize = (long) persistentProperties.get(CLUSTER_SIZE);
            keyValues = IteratorUtils.filter(keyValues, pair -> pair.getValue().equals(clusterSize));
        }
        final Map<Serializable, Long> clusterPopulation = Utility.keyValuesToMap(keyValues);
        clusterPopulation.remove(NullObject.instance());
        return clusterPopulation;
    }
}
