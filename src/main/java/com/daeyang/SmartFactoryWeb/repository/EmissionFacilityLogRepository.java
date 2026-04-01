package com.daeyang.SmartFactoryWeb.repository;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.daeyang.SmartFactoryWeb.controller.EmissionFacilityLogController;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload;

@Repository
public class EmissionFacilityLogRepository {
	private final JdbcTemplate pgJdbc;
	private final JdbcTemplate msJdbc;
	private final NamedParameterJdbcTemplate pgNP;
	private final NamedParameterJdbcTemplate msNP;
	private static final Logger log = LoggerFactory.getLogger(EmissionFacilityLogRepository.class);
	
	public EmissionFacilityLogRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate pgJdbc,
			@Qualifier("mssqlJdbcTemplate") JdbcTemplate msJdbc,
			@Qualifier("postgresNamedJdbc") NamedParameterJdbcTemplate pgNP,
			@Qualifier("mssqlNamedJdbc") NamedParameterJdbcTemplate msNP) {
		this.pgJdbc = pgJdbc;
		this.msJdbc = msJdbc;
		this.pgNP = pgNP;
		this.msNP = msNP;
	}

	private String SQL = "";
	
	public long insert(List<Map<String, Object>> records) {
		for (Map<String, Object> record : records) {
	        String operationDate = record.get("operationDate").toString();
	        String weekday = record.get("weekday").toString();

	        log.debug("날짜: " + operationDate + ", 요일: " + weekday);

	        List<Map<String, Object>> items = (List<Map<String, Object>>)record.get("items");

	        for (Map<String, Object> item : items) {
	            String facilityName = item.get("facilityName").toString();
	            String subLabel = item.get("subLabel").toString();
	            String operationStatus = item.get("operationStatus").toString();

	            log.debug("  설비: " + facilityName);
	            log.debug("  작업: " + subLabel);
	            log.debug("  상태: " + operationStatus);

	            List<Map<String, Object>> timeRanges = (List<Map<String, Object>>)item.get("timeRanges");

	            for (Map<String, Object> time : timeRanges) {
	                String startTime = time.get("startTime").toString();
	                String endTime = time.get("endTime").toString();

	                log.debug("    시간: " + startTime + " ~ " + endTime);
	            }
	        }
	    }
		
		SQL = """
				
			  """;

		return 0;
	}
}
