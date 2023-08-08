package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.AggregatorsList;





public interface AggregatorsListRepository extends JpaRepository<AggregatorsList, Long> {
//	
//	 @Query(value = "SELECT * FROM pg_billers", nativeQuery = true)
//	    public ArrayList<AggregatorsList> getAggregatorsList();
	 
//	 @Query(value = "SELECT * FROM pg_aggregators a WHERE a.aggregator_id = :aggregatorId", nativeQuery = true)
//	    public AggregatorsList getAggregator(String aggregatorId);
	 
//	 @Query(value = "SELECT * FROM nbp_pgw_aggregators a WHERE a.aggregator_id = :aggregatorId", nativeQuery = true)
//	 public AggregatorsList getAggregator(String aggregatorId);

	
	 public List<AggregatorsList> findAll();
	 public AggregatorsList findByAggregatorId(String aggregatorId);

}
