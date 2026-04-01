package com.daeyang.SmartFactoryWeb.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.daeyang.SmartFactoryWeb.repository.ReceiptRepository;

@Service
public class ReceiptService {
	private final ReceiptRepository repository;
	
	public ReceiptService (ReceiptRepository repository) {
		this.repository = repository;
	}
	
	public Map<String, Object> empLogin (String empID, String empPW) {
		return repository.empLogin(empID, empPW);
	}
}
