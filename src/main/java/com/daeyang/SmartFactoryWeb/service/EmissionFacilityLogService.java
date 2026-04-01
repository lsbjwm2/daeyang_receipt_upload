package com.daeyang.SmartFactoryWeb.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.daeyang.SmartFactoryWeb.repository.EmissionFacilityLogRepository;

@Service
public class EmissionFacilityLogService {
	private final EmissionFacilityLogRepository repository;
	
	public EmissionFacilityLogService (EmissionFacilityLogRepository repository) {
		this.repository = repository;
	}
	
	public long insert(List<Map<String, Object>> records) {
		return repository.insert(records);
	}
}
