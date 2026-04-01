package com.daeyang.SmartFactoryWeb.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.daeyang.SmartFactoryWeb.repository.SupportProjectWebRepository;

@Service
public class SupportProjectWebService {
	private final SupportProjectWebRepository repository;
	
	public SupportProjectWebService(SupportProjectWebRepository repository) {
		this.repository = repository;
	}
	
	public Map<String, Object> login (String ID, String PW) {
		return repository.login(ID, PW);
	}
}
