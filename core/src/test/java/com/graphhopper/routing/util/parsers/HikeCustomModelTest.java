package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.routing.weighting.custom.CustomModelParser;
import com.graphhopper.routing.weighting.custom.CustomWeighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.PMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HikeCustomModelTest {

    private EncodingManager em;
    private OSMParsers parsers;

    @BeforeEach
    public void setup() {
        IntEncodedValue hikeRating = HikeRating.create();
        em = new EncodingManager.Builder().
                add(VehicleAccess.create("foot")).
                add(VehicleSpeed.create("foot", 4, 1, false)).
                add(VehiclePriority.create("foot", 4, PriorityCode.getFactor(1), false)).
                add(FerrySpeed.create()).
                add(RouteNetwork.create(FootNetwork.KEY)).
                add(FootRoadAccess.create()).
                add(hikeRating).build();

        parsers = new OSMParsers().
                addWayTagParser(new OSMHikeRatingParser(hikeRating));

        parsers.addWayTagParser(new FootAccessParser(em, new PMap()));
        parsers.addWayTagParser(new FootAverageSpeedParser(em));
        parsers.addWayTagParser(new FootPriorityParser(em));
    }

    EdgeIteratorState createEdge(ReaderWay way) {
        BaseGraph graph = new BaseGraph.Builder(em).create();
        EdgeIteratorState edge = graph.edge(0, 1);
        parsers.handleWayTags(edge.getEdge(), graph.getEdgeAccess(), way, em.createRelationFlags());
        return edge;
    }

    @Test
    public void testHikePrivate() {
        CustomModel cm = GHUtility.loadCustomModelFromJar("hike.json");
        ReaderWay way = new ReaderWay(0L);
        way.setTag("highway", "track");
        EdgeIteratorState edge = createEdge(way);
        CustomWeighting.Parameters p = CustomModelParser.createWeightingParameters(cm, em);
        assertEquals(1.2, p.getEdgeToPriorityMapping().get(edge, false), 0.01);

        way.setTag("motor_vehicle", "private");
        edge = createEdge(way);
        assertEquals(1.2, p.getEdgeToPriorityMapping().get(edge, false), 0.01);

        way.setTag("sac_scale", "alpine_hiking");
        edge = createEdge(way);
        assertEquals(1.2, p.getEdgeToPriorityMapping().get(edge, false), 0.01);
        assertEquals(1.5, p.getEdgeToSpeedMapping().get(edge, false), 0.01);

        way = new ReaderWay(0L);
        way.setTag("highway", "track");
        way.setTag("access", "private");
        edge = createEdge(way);
        assertEquals(0, p.getEdgeToPriorityMapping().get(edge, false), 0.01);

        way.setTag("sac_scale", "alpine_hiking");
        edge = createEdge(way);
        assertEquals(0, p.getEdgeToPriorityMapping().get(edge, false), 0.01);
    }
}
