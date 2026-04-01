package com.daeyang.SmartFactoryWeb.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.daeyang.SmartFactoryWeb.service.SupportProjectWebService;

@Controller
@RequestMapping("/api/support")
public class SupportProjectWebController {
	private final SupportProjectWebService service;
	private static final Logger log = LoggerFactory.getLogger(SupportProjectWebController.class);
	
	public SupportProjectWebController(SupportProjectWebService service) {
        this.service = service;
    }
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, Object> requestBody) {
    	if (requestBody == null || requestBody.isEmpty()) {
    		return ResponseEntity.badRequest().build();
    	}
    	
    	String ID = requestBody.get("ID").toString();
    	String PW = requestBody.get("PW").toString();

    	Map<String, Object> loginResult = service.login(ID, PW);

        if (loginResult == null) {
        	log.info("사원 정보 없음: {}", ID);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "올바르지 않은 아이디 혹은 비밀번호 입니다."));
        } else {
        	return ResponseEntity.ok(Map.of("message", loginResult));
        }
    }
}
