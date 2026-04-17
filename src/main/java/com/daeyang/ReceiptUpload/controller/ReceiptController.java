package com.daeyang.ReceiptUpload.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.daeyang.ReceiptUpload.service.ReceiptService;

@RestController
@RequestMapping("/api/receipt")
public class ReceiptController {
	private static final Logger log = LoggerFactory.getLogger(ReceiptController.class);
	private final ReceiptService service;
	
	public ReceiptController (ReceiptService service) {
		this.service = service;
	}
	
	@PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> requestBody) {
    	if (requestBody == null || requestBody.isEmpty()) {
    		return ResponseEntity.badRequest().build();
    	}
    	
    	String empID = requestBody.get("empID").toString();
    	String empPW = requestBody.get("empPW").toString();

    	Map<String, Object> loginResult = service.empLogin(empID, empPW);

        // DB에 저장된 해시와 비교
        if (loginResult == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
        	String empNo = loginResult.get("Employee_ID").toString();
        	return ResponseEntity.ok(Map.of("empNo", empNo));
        }
    }

	// 영수증 사진 저장
	@PostMapping("/upload")
    public ResponseEntity<?> uploadReceipts(@RequestParam("empNo") String empNo, @RequestParam("receiptImages") List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드할 파일이 없습니다."));
        }
		
        try {
        	Map<String, Object> result = service.uploadReceipts(empNo, files);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("{} 업로드 중 오류 발생", empNo, e);
            return ResponseEntity.internalServerError().body(Map.of("message", "업로드 중 오류 발생"));
        }
    }
}
