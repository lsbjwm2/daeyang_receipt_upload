package com.daeyang.SmartFactoryWeb.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.daeyang.SmartFactoryWeb.dto.ChecksheetApprovalRequest;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetDetailDto;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetSummaryDto;
import com.daeyang.SmartFactoryWeb.service.ChecksheetPdfService;
import com.daeyang.SmartFactoryWeb.service.ChecksheetService;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/api/checksheets")
public class ChecksheetController {
	private final ObjectMapper objectMapper;
    private final ChecksheetService service;
    private final ChecksheetPdfService pdfService;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(ChecksheetController.class);

    public ChecksheetController(ObjectMapper objectMapper, ChecksheetService service, ChecksheetPdfService pdfService, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.service = service;
        this.pdfService = pdfService;
        this.restTemplate = restTemplate;
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
    
    @GetMapping("/me")
    public ResponseEntity<?> me (@RequestParam("empNo") String empNo) {
    	empNo = empNo.trim();
        if (empNo.isEmpty()) {
          return ResponseEntity.badRequest().build();
        }
        
        Map<String, String> personResult = service.personResult(empNo);
        
        if (personResult != null) {
        	return ResponseEntity.ok(personResult);
        } else {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("payload") String payloadJson/*,
    		@RequestParam(required = false) MultiValueMap<String, MultipartFile> files*/) throws Exception {
        // JSON 파싱
        ChecksheetPayload payload = objectMapper.readValue(payloadJson, ChecksheetPayload.class);

        Map<String, Object> isEqualDataMap = service.selectCehckSheet_equalValue(LocalDate.parse(payload.date), payload.place, LocalTime.parse(payload.startTime), LocalTime.parse(payload.endTime));
        
        // 점검표 중복 작성 방지
        if (isEqualDataMap != null) {
        	return ResponseEntity.internalServerError().body(Map.of("message", "이미 점검표가 작성된 점검장소 입니다."));
        }
        
        // 저장 (DB + Web Server)
		Long id = service.save(payload/* , files */);

        // 프론트에 점검 ID 반환
        return ResponseEntity.ok(Map.of("message", "성공", "data", ""));
    }
    
    @GetMapping
    public ResponseEntity<List<ChecksheetSummaryDto>> list(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset
    ) {
        List<ChecksheetSummaryDto> list = service.getList(from, to, approvalStatus, limit, offset);

        return ResponseEntity.ok(list);
    }
    
    // checksheet-list 화면에서 삭제 버튼 누르면 호출 되는 API
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody ChecksheetApprovalRequest request) {
    	try {
    		int requestCounts = request.sheetIds.size();
    		int deleteCounts = service.deleteCheckSheet(request.sheetIds);
    		
    		if (requestCounts != deleteCounts) {
    			return ResponseEntity.ok().body(Map.of("message", "삭제 요청한 점검표의 개수와\n삭제된 점검표의 개수가 일치하지 않습니다.\n확인 부탁드립니다."));
    		}
    		
    		return ResponseEntity.ok().body(Map.of("message", ""));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청
        	e.printStackTrace(); // 콘솔에서도 확인
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }
    
    // checksheet-list 화면에서 전자결재 상신 버튼 누르면 호출 되는 API
    @PostMapping("/approval")
    public ResponseEntity<?> approve(@RequestBody ChecksheetApprovalRequest request) {
    	try {
    		String grpId = request.grpId;
    		String grpPw = request.grpPw;
    		
    		// 상신자 정보 검색
            Map<String, Object> empInfo = service.select_empInfo(request.empNo);
            if (empInfo == null) {
            	log.error("전자결재 작성자의 사원 정보가 없습니다. " + request.empNo);
            	return ResponseEntity.internalServerError().body(Map.of("message", "전자결재 작성자의 사원 정보가 없습니다."));
            }
            
            // DB에서 입력된 점검표를 조회 후 PDF로 생성, 전자결재 문서를 만들기 위해 점검일자 return
            LocalDate date = service.approveAndGeneratePdf(request.sheetIds);
            
            // 전자결재 상신
            return sendReportToExternalApi(empInfo, request.sheetIds, date, grpId, grpPw);
        } catch (IllegalArgumentException e) {
            // 잘못된 요청 (4개 아니거나 날짜가 다르거나 등)
        	e.printStackTrace(); // 콘솔에서도 확인
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ChecksheetDetailDto> detail(@PathVariable("id") long id) {
        ChecksheetDetailDto dto = service.getDetail(id);
        return ResponseEntity.ok(dto);
    }
    
    //전자결재 상신 - multipart 방식으로 PDF 파일 전달
    public ResponseEntity<?> sendReportToExternalApi(Map<String, Object> empInfo, List<Long> sheetIds, LocalDate date, String grpId, String grpPw) {
        try {
            // 1. PDF 파일 경로 설정 및 읽기
        	String area = "1공장";
        	if ("8".equals(empInfo.get("Bonbu_Site").toString())) {
        		area = "2공장";
        	} else if ("6".equals(empInfo.get("Bonbu_Site").toString())) {
        		area = "3공장";
        	}
        	
        	DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        	String dateStr = date.format(YYYYMMDD);
            String fileName = String.format(area + " 자체점검표 (%s).pdf", dateStr);
            Path pdfPath = Paths.get("D:\\checkSheetPDF\\" + fileName);
            
            if (!Files.exists(pdfPath)) {
                log.error("해당 경로의 PDF 파일을 찾지 못 했습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "PDF 파일을 찾지 못 했습니다."));
            }
            
            // 2. PDF 파일 Resource 생성
            Resource pdfResource = new FileSystemResource(pdfPath.toFile());
            
            // 3. 기타 데이터 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일(E)", Locale.KOREAN);
            String whenToday = date.format(formatter);
            String strWho = empInfo.get("name").toString() + " " + empInfo.get("position_name");
            
            // 라인 별 점검자 찾기 - 점검 장소 별 최종 점검자를 점검 인원으로 지정한다.
            List<Map<String, Object>> whoList = service.select_placeWho(sheetIds);
            Map<String, String> nameMap = new HashMap<>();
            for (int i = 0; i < whoList.size(); i++) {
            	nameMap.put(whoList.get(i).get("place").toString(), whoList.get(i).get("name").toString());
            }
            String assemblyInspector = nameMap.get("조립반");
            String processingInspector = nameMap.get("가공반");
            
            // 파일 파트 명시적 구성
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentDisposition(
                ContentDisposition.builder("form-data")
                    .name("attach_file")
                    .filename(fileName)    // 지금은 원본명, 나중에 safeFileName으로 교체 가능
                    .build()
            );
            
            fileHeaders.setContentType(MediaType.APPLICATION_PDF);

            HttpEntity<Resource> fileEntity = new HttpEntity<>(pdfResource, fileHeaders);
            
            // 4. Multipart Body 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            body.add("form_name", "0005");
            body.add("line_name", "안전점검 라인");
            body.add("form_title", area + " 현장 안전점검 결과 보고");
            body.add("ID", grpId);
            body.add("PW", grpPw);
            body.add("who", strWho);
            body.add("when", whenToday);
            body.add("where", area);
            body.add("what", "현장 안전점검");
            body.add("how", "- 기준: 1일 2회 점검 실시 ( 오전: 9시 ~ 10시 / 오후: 15시 ~ 16시 )\n"
            			  + "&nbsp;- 점검 인원: 가공반 - "+ processingInspector + " / 조립반 - " + assemblyInspector + "\n"
            		      + "&nbsp;&nbsp;&nbsp;인사총무팀 - 소진욱 선임\n\n"
            		      + "&nbsp;※ 유첨: 자체점검표");
            body.add("why", area + " 안전한 일터 만들기 및 안전사고 예방 활동");
            body.add("recipient", "");	// 빈 값 주면 전산팀에서 인사총무팀 고정으로 생성(수정 필요할 시 작성자가 그룹웨어를 통해 수정 진행) 
            body.add("file_name", fileName);
            body.add("attach_file", fileEntity);
            
            // 5. HTTP Headers 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
			
			// 6. 외부 API 호출
			String externalApiUrl =	"https://www.daeyang.net/APCyber/Uplog.ashx";
			ResponseEntity<String> response = restTemplate.postForEntity(externalApiUrl, requestEntity, String.class);
			
			//{"success":true,"message":"Success"}
			//{"success":false,	"message":"String or binary data would be truncated in table 'es.dbo.UploadedFiles',
			//column 'How'. Truncated value: '- 기준: 1일 2회 점검 실시 ( 오전: 9시 ~ 10시 / 오후: 15시 ~ 16시 )\n
			//점검 인원: 가공반 - 이현준 선임 / 조립반 - 이현준 선임 / 인사총무팀 - 소진'.\r\nThe statement has been terminated."}
			log.info("Call API ID: {}, Who: {}", grpId, strWho);
			log.info("Status Code: {}", response.getStatusCode());
			// 7. 외부 API 응답 처리
			if (response.getStatusCode().is2xxSuccessful()) {
				log.info("Successfully sent report to external API. Response: {}", response.getBody());
				// 전자결재 상태 업데이트
				service.markApproved(sheetIds); 
				return ResponseEntity.ok(Map.of("message", response.getBody()));
			} else {
				log.error("Failed to send report to external API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
				return ResponseEntity.status(response.getStatusCode()).body(Map.of("message", "Failed to send statusCode: " + response.getStatusCode()));
			}
        } catch (Exception e) {
            log.error("Error sending report to external API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error sending report: " + e.getMessage()));
        }
    }
    
    // 이미지 프록시 엔드포인트 - 이미지 서버 폴더에서 가져오는 용도
    @GetMapping("/image")
    public ResponseEntity<Resource> getChecksheetImage(@RequestParam("file") String fileName) throws IOException {
        // 공유폴더 경로
        Path path = Paths.get("\\\\10.10.102.10\\checkSheetIMG\\" + fileName);
        
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "image/jpeg";

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
    
   // 전자결재 상신 - base64 형태로 PDF 파일 전달
//    public ResponseEntity<?> sendReportToExternalApi(Map<String, Object> empInfo, List<Long> sheetIds, LocalDate date) {
//        try {
//            // 1. PDF 파일 경로 설정 및 읽기
//        	String area = "1공장";
//        	if ("8".equals(empInfo.get("Bonbu_Site").toString())) {
//        		area = "2공장";
//        	} else if ("6".equals(empInfo.get("Bonbu_Site").toString())) {
//        		area = "3공장";
//        	}
//        	
//        	DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        	String dateStr = date.format(YYYYMMDD);
//            String fileName = String.format(area + " 자체점검표 (%s).pdf", dateStr);
//            Path pdfPath = Paths.get("D:\\checkSheetPDF\\" + fileName);
//            if (!Files.exists(pdfPath)) {
//                log.error("해당 경로의 PDF 파일을 찾지 못 했습니다.");
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("PDF 파일을 찾지 못 했습니다.");
//            }
//
//            byte[] pdfBytes = Files.readAllBytes(pdfPath);
//
//            // 2. PDF 파일을 Base64로 인코딩
//            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);            
//            
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일(E)", Locale.KOREAN);
//            String whenToday = date.format(formatter);
//            String strWho = empInfo.get("name").toString() + " " + empInfo.get("position_name");
//            
//            // 3. JSON 형식의 페이로드 생성
//            Map<String, String> payload = new LinkedHashMap<>();
//            payload.put("form_name", "업무보고");
//            payload.put("line_Name", "안전점검");
//            payload.put("form_writer", empInfo.get("person_id").toString());
//            payload.put("form_title", area + " 현장 안전점검 결과 보고");
//            payload.put("who", strWho);
//            payload.put("when", whenToday);
//            payload.put("where", area);
//            payload.put("what", "현장 안전점검");
//            payload.put("how", "- 기준: 1일 2회 점검 실시 ( 오전: 9시 ~ 10시 / 오후: 15시 ~ 16시 )\n  점검 인원: 가공반 - "
//            		           + strWho + " / 조립반 - " + strWho + " / 인사총무팀 - 소진욱 선임\n"
//            		           + "  ※ 유첨: 자체점검표");
//            payload.put("why", area + " 안전한 일터 만들기 및 안전사고 예방 활동");
//            payload.put("recipient", "인사총무팀");
//            payload.put("file_name", fileName);
//            payload.put("attach_File", base64Pdf);
//            
//            // 4. HTTP Headers 설정
//			
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_JSON);
//			
//			// 5. HttpEntity 생성 (페이로드와 헤더 포함) 
//			HttpEntity<Map<String, String>>	requestEntity = new HttpEntity<>(payload, headers);
//			
//			// 6. 외부 API 호출 // TODO: 실제 외부 API의 URL로 변경해야 합니다.
//			String externalApiUrl =	"http://another-server.com/api/receive-report"; ResponseEntity<String>
//			response = restTemplate.postForEntity(externalApiUrl, requestEntity, String.class);
//			
//			// 7. 외부 API 응답 처리 
//			if (response.getStatusCode().is2xxSuccessful()) {
//				log.info("Successfully sent report to external API. Response: {}",
//						response.getBody()); // 전자결재 상태 업데이트
//						service.markApproved(sheetIds); 
//						return ResponseEntity.ok("Report sent successfully: " + response.getBody()); 
//			} else {
//				log.error("Failed to send report to external API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
//				return ResponseEntity.status(response.getStatusCode()).body("Failed to send report: " + response.getBody());
//			}
//        } catch (IOException e) {
//            log.error("Error reading PDF file or creating dummy PDF: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing PDF: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Error sending report to external API: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending report: " + e.getMessage());
//        }
//    }
}
