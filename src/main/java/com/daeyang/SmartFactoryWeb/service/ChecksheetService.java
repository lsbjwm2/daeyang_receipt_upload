package com.daeyang.SmartFactoryWeb.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.daeyang.SmartFactoryWeb.dto.ChecksheetDetailDto;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetSummaryDto;
import com.daeyang.SmartFactoryWeb.repository.ChecksheetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChecksheetService {
	private static final Logger log = LoggerFactory.getLogger(ChecksheetService.class);
	private final ChecksheetRepository repository;
	private final ChecksheetPdfService pdfService;
	private final ObjectMapper objectMapper;
	
	// 이미지 파일 저장 경로
	@Value("${storage.base-path}")
    private String basePath;
	
	public ChecksheetService(ChecksheetRepository repository, ObjectMapper objectMapper, ChecksheetPdfService pdfService) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.pdfService = pdfService;
	}
	
	public Map<String, Object> empLogin (String empID, String empPW) {
		return repository.empLogin(empID, empPW);
	}
	
	public Map<String, String> personResult (String empNo) {
		return repository.personResult(empNo);
	}
	
	public Map<String, Object> selectCehckSheet_equalValue(LocalDate date, String place, LocalTime st, LocalTime et) {
		return repository.selectCehckSheet_equalValue(date, place, st, et);
	}
	
	@Transactional
	/*전자 결재 상신 방식으로 변경을 위해 이미지 첨부 기능 제거 - 260113_LHJ*/
    public long save(ChecksheetPayload p/*, MultiValueMap<String, MultipartFile> files*/) throws Exception {

        // 1) 섹션/항목별로 실제 업로드 파일을 Web Server에 저장하고,
        //    payload.images 배열을 "실제 파일 기준"으로 다시 구성
        if (p.sections != null/* && files != null*/) {
            for (var secEntry : p.sections.entrySet()) {
                String secId = secEntry.getKey();
                Map<String, ChecksheetPayload.ItemResult> items = secEntry.getValue();

                for (var itemEntry : items.entrySet()) {
                    String itemId = itemEntry.getKey();
                    ChecksheetPayload.ItemResult ir = itemEntry.getValue();

                    // 프론트에서 온 images는 버리고, 실제 파일 기준으로 다시 세팅
                    /*전자 결재 상신 방식으로 변경을 위해 이미지 첨부 기능 제거 - 260113_LHJ*/
                    //List<ImageInfo> newImages = new ArrayList<>();

                    /*전자 결재 상신 방식으로 변경을 위해 이미지 첨부 기능 제거 - 260113_LHJ*/
                    /*String key = String.format("files[%s][%s][]", secId, itemId);
                    List<MultipartFile> arr = files.get(key);
                    if (arr != null) {
                        for (MultipartFile f : arr) {
                            String stored = storeFile(f); // Web Server에 저장
                            ImageInfo info = new ImageInfo();
                            info.name = f.getOriginalFilename();
                            info.size = f.getSize();
                            info.type = f.getContentType();
                            info.path = stored; // Web Server 경로
                            newImages.add(info);
                        }
                    }

                    ir.images = newImages;*/
                }
            }
        }

        // 2) payload 전체를 JSON 문자열로 직렬화
        String payloadJson = objectMapper.writeValueAsString(p);

        // 3) DB insert
        return repository.insert(p, payloadJson);
    }
	
	public List<ChecksheetSummaryDto> getList(String from, String to, String approvalStatus, int limit, int offset) {
		
		LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
	    LocalDate toDate   = (to   != null && !to.isBlank())   ? LocalDate.parse(to)   : null;

	    return repository.findList(fromDate, toDate, approvalStatus, limit, offset);
    }
	
//	public List<ChecksheetSummaryDto> getListAll(int limit, int offset) {
//	    return repository.findAll(limit, offset);
//	}

    public ChecksheetDetailDto getDetail(long id) {
        return repository.findDetail(id)
                  .orElseThrow(() -> new IllegalArgumentException("해당 ID의 점검표가 없습니다: " + id));
    }

    public List<Map<String, Object>> select_placeWho(List<Long> ids) {
    	return repository.select_placeWho(ids);
    }
    
    public void markApproved(List<Long> ids) {
        repository.updateApprovalStatus(ids, "DONE");
    }
    
    /**
     * 전자결재 상신용:
     * 선택된 4개 점검표를 검증하고, 1개의 PDF로 생성한다.
     * (실제 e_approval_status 업데이트는 기존 로직에 맞춰 추가)
     * 전자결재 문서를 만들기 위해 점검일자 return
     */
    @Transactional
    public LocalDate approveAndGeneratePdf(List<Long> sheetIds) throws Exception {
    	try {
    		if (sheetIds == null || sheetIds.size() != 4) {
                throw new IllegalArgumentException("전자결재 상신은 정확히 4개의 점검표에 대해서만 가능합니다.");
            }
            
            // 1) 상세 4개 조회
            List<ChecksheetDetailDto> details = new ArrayList<>();
            for (Long id : sheetIds) {
                ChecksheetDetailDto dto = repository.findDetail(id)
                        .orElseThrow(() -> new IllegalArgumentException("해당 ID의 점검표가 없습니다: " + id));
                details.add(dto);
            }

            // 2) 점검일자 동일 여부 확인
            Set<String> dates = new HashSet<>();
            for (ChecksheetDetailDto dto : details) {
                dates.add(dto.checkDate);
            }
            if (dates.size() != 1) {
                throw new IllegalArgumentException("4개의 점검표 점검일자가 서로 다릅니다: " + dates);
            }

            String dateStr = dates.iterator().next();
            LocalDate date = LocalDate.parse(dateStr); // "yyyy-MM-dd"

            // 3) PDF 생성 (공장명은 우선 '1공장' 고정 – 필요 시 파라미터로 변경)
            pdfService.generateCombinedPdf(date, details, "1공장");
            
            return date;
    	} catch (Exception e) {
    		log.error(e.getMessage());
    		return null;
    	}
    }
    
    public Map<String, Object> select_empInfo(String empNo) {
    	return repository.select_empInfo(empNo);
    }
    
    public int deleteCheckSheet (List<Long> ids) {
    	return repository.deleteCheckSheet(ids);
    }
    
    private String storeFile(MultipartFile f) throws Exception {
        if (f == null || f.isEmpty()) return null;

        Path base = Path.of(basePath).toAbsolutePath();
        Files.createDirectories(base);

        String cleanName = Optional.ofNullable(f.getOriginalFilename()).orElse("file");
        String filename = System.currentTimeMillis()+ "-" + cleanName;
        Path path = base.resolve(filename);

        try (InputStream in = f.getInputStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return path.toString(); // JSON에 그대로 저장
    }
}
