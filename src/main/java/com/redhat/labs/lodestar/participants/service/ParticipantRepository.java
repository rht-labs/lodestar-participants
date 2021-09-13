package com.redhat.labs.lodestar.participants.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.Tuple;

import com.redhat.labs.lodestar.participants.model.Participant;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class ParticipantRepository implements PanacheRepository<Participant> {
    private final static String ROLLUP_QUERY = "SELECT organization as organization, count(distinct email) as total FROM Participant WHERE region is not null GROUP BY ROLLUP(organization)";
    private final static String ROLLUP_REGION_QUERY = "SELECT organization as organization, count(distinct email) as total FROM Participant WHERE region in :region GROUP BY ROLLUP(organization)";
    private final static String ROLLUP_ALL_REGION_QUERY = "SELECT region as region, organization as organization, count(distinct email) as total FROM Participant WHERE region is not null GROUP BY CUBE (region, organization)";
    private final static String COUNT_FOR_EACH_ENGAGEMENT = "SELECT engagementUuid as engagementUuid, count(distinct email) as total FROM Participant GROUP BY engagementUuid";

    public Map<String, Long> getParticipantRollup() {
        return getEntityManager().createQuery(ROLLUP_QUERY, Tuple.class).getResultStream()
                .collect(Collectors.toMap(tuple -> (tuple.get("organization")) == null ? "All" : ((String) tuple.get("organization")),
                        tuple -> ((Number) tuple.get("total")).longValue()));
    }

    public Map<String, Long> getEngagementRollup() {
        return getEntityManager().createQuery(COUNT_FOR_EACH_ENGAGEMENT, Tuple.class).getResultStream()
                .collect(Collectors.toMap(tuple -> ((String) tuple.get("engagementUuid")),
                        tuple -> ((Number) tuple.get("total")).longValue()));
    }
    
    public Map<String, Map<String, Long>> getParticipantRollupAllRegions() {

        Map<String, Map<String, Long>> doubleMap = new HashMap<>();
        
        getEntityManager().createQuery(ROLLUP_ALL_REGION_QUERY, Tuple.class).getResultStream().forEach(tuple -> {
            String region = tuple.get("region") == null ? "All" : ((String) tuple.get("region"));
            String organization = tuple.get("organization") == null ? "All" : ((String) tuple.get("organization"));
            long total = ((Number) tuple.get("total")).longValue();
                      
            if(!doubleMap.containsKey(region)) {
               doubleMap.put(region, new HashMap<>());
            }
            Map<String, Long> innerMap = doubleMap.get(region);
            innerMap.put(organization, total);
        });
        
        return doubleMap;
    }

    public Map<String, Long> getParticipantRollup(List<String> region) {
        return getEntityManager().createQuery(ROLLUP_REGION_QUERY, Tuple.class).setParameter("region", region).getResultStream()
                .collect(Collectors.toMap(tuple -> (tuple.get("organization")) == null ? "All" : ((String) tuple.get("organization")),
                        tuple -> ((Number) tuple.get("total")).longValue()));
    }
    
    public long countParticipantsByRegion(List<String> region) {
        return count("region in ?1", region);
    }
    
    public List<Participant> getParticipantsByRegion(List<String> region, Page page) {
        return find("region in ?1", Sort.by("region").and("engagementUuid").and("email").and("uuid"), region).page(page).list();
    }

}
