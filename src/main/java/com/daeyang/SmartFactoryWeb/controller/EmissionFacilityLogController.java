package com.daeyang.SmartFactoryWeb.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daeyang.SmartFactoryWeb.service.EmissionFacilityLogService;

@RestController
@RequestMapping("/api/emissionFacilityLog")
public class EmissionFacilityLogController {
	private final EmissionFacilityLogService service;
	private static final Logger log = LoggerFactory.getLogger(EmissionFacilityLogController.class);
	
	public EmissionFacilityLogController (EmissionFacilityLogService service) {
		this.service = service;
	}
	
	@PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String, Object> requestBody) {
    	if (requestBody == null || requestBody.isEmpty()) {
    		return ResponseEntity.badRequest().build();
    	}
    	
    	log.debug("@RequestBody = " + requestBody.toString());
    	
    	List<Map<String, Object>> dailyRecords = (List<Map<String, Object>>)requestBody.get("dailyRecords");
    	
    	long insertResult = service.insert(dailyRecords);
    	
    	if (insertResult > 0) {
    		
    	}

    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
