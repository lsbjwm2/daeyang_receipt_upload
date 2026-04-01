package com.daeyang.SmartFactoryWeb.service;

import com.daeyang.SmartFactoryWeb.dto.ChecksheetDetailDto;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload.ItemResult;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChecksheetPdfService {

    // PDF 테스트 저장 경로
    private static final Path BASE_DIR = Paths.get("D:\\checkSheetPDF");

    // ===== 섹션/항목 메타 정의 =====
 // PDF 한 행을 구성하는 "표 메타"
    private record RowMeta(
            String itemId,          // DB JSON key (예: work-platform)
            String majorHtml,        // 항목(대) - 왼쪽 칸 (예: 작업발판<br/>안전난간<br/>추락방호망)
            String minorHtml,        // 항목(중) - 오른쪽 칸 (예: 비계)
            String contentText       // 점검내용(법조항 포함)
    ) {}

    private record SectionDef(String sectionId, int no, String title, List<RowMeta> rows) {}

 // 1) 섹션 정의 전체
    private static final List<SectionDef> SECTION_DEFS = List.of(
        // =========================
        // 1. 추락사고 예방 (fall-prevention)
        // =========================
        new SectionDef("fall-prevention", 1, "추락사고 예방", List.of(
            // 단독 항목(원본은 항목 1칸) → colspan=2로 출력되게 major 비우고 minor에만 넣음
            new RowMeta(
                "helmet-belt",
                "",
                "안전모\n안전대",
                "근로자에게 안전모, 안전대 등을\n적정하게 지급하고 착용여부를 확인하였는지\n(안전보건기준에 관한 규칙 제32조)"
            ),

            // 대/중 구조(원본처럼 2칸 분리 + rowspan) : 작업발판/안전난간/추락방호망
            new RowMeta(
                "work-platform",
                "작업발판\n안전난간\n추락방호망",
                "비계",
                "작업발판 구조 및 설치 상태가 적정한지\n(안전보건기준에 관한 규칙 제56조)"
            ),
            new RowMeta(
                "platform-rail",
                "작업발판\n안전난간\n추락방호망",
                "비계",
                "작업발판 단부에 안전난간이 적정하게\n설치되어 있는지\n(안전보건기준에 관한 규칙 제43조)"
            ),
            new RowMeta(
                "stairway",
                "작업발판\n안전난간\n추락방호망",
                "철골",
                "안전하게 이동할 수 있는 승강로가 적정하게\n설치되어 있는지\n(안전보건기준에 관한 규칙 제381조)"
            ),

            new RowMeta(
                "fall-net",
                "작업발판\n안전난간\n추락방호망",
                "철골",
                "추락방호망 및 안전대 부착설비가 적정하게\n설치되어 있는지\n(안전보건기준에 관한 규칙 제43조)"
            ),
            new RowMeta(
                "belt-precheck",
                "작업발판\n안전난간\n추락방호망",
                "철골",
                "작업 전 안전대 및 부속설비의 이상 유무를\n점검하였는지\n(안전보건기준에 관한 규칙 제44조)"
            ),

            new RowMeta(
                "roof-board",
                "작업발판\n안전난간\n추락방호망",
                "지붕",
                "강도가 약한 지붕 위에서 작업 시 폭이 30cm이상 되는 작업발판을 설치하였는지\n(안전보건기준에 관한 규칙 제45조)"
            ),
            new RowMeta(
                "roof-rail",
                "작업발판\n안전난간\n추락방호망",
                "지붕",
                "지붕 단부에 안전난간을 적정하게 설치하였는지\n(안전보건기준에 관한 규칙 제45조)"
            ),
            new RowMeta(
                "roof-net",
                "작업발판\n안전난간\n추락방호망",
                "지붕",
                "안전난간을 설치하기 어려운 상태일 때\n추락방호망을 적정하게 설치하였는지\n(안전보건기준에 관한 규칙 제45조)"
            ),

            // (B) 원본처럼 항목 1칸(=colspan=2)으로 보여야 하는 구간들
            new RowMeta(
                "opening-cover",
                "개구부·단부\n덮개",
                "",
                "안전난간, 울타리, 덮개 등 추락 방호조치를\n적정하게 설치하였는지\n(안전보건기준에 관한 규칙 제43조)"
            ),
            new RowMeta(
                "safe-passage",
                "안전통로\n확보",
                "",
                "통로가 안전하게 사용될 수 있도록\n유지되고 있는지\n(안전보건기준에 관한 규칙 제22조)"
            )
        )),

        // =========================
        // 2. 부딪힘사고 예방 (collision-prevention)
        // =========================
        new SectionDef("collision-prevention", 2, "부딪힘사고 예방", List.of(
            new RowMeta(
                "no-entry",
                "",
                "출입금지",
                "작업관계자 외 출입금지 조치를 하였는지\n(안전보건기준에 관한 규칙 제20조)"
            ),
            new RowMeta(
                "passage-light",
                "",
                "안전통로",
                "안전하게 통행할 수 있도록 통로의 채광 또는\n조명시설이 적정한지(75럭스(lux) 이상)\n(안전보건기준에 관한 규칙 제21조)"
            ),
            new RowMeta(
                "passage-keep",
                "",
                "안전통로",
                "통로가 안전하게 사용될 수 있도록\n유지하고 있는지\n(안전보건기준에 관한 규칙 제22조)"
            ),

            new RowMeta(
                "workplan-record",
                "",
                "작업계획서·작업\n지휘자",
                "작업계획서 작성 대상 작업 시 사전조사 및\n그 결과를 기록 보존하고 있는지\n(안전보건기준에 관한 규칙 제38조)"
            ),
            new RowMeta(
                "workplan-make",
                "",
                "작업계획서·작업\n지휘자",
                "사전조사 결과를 고려하여 적정한\n작업계획서를 작성하였는지\n(안전보건기준에 관한 규칙 제38조)"
            ),
            new RowMeta(
                "workplan-announce",
                "",
                "작업계획서·작업\n지휘자",
                "작업계획서의 내용을 해당 근로자에게 알리고\n그 계획에 따라 작업을 하도록 하고 있는지\n(안전보건기준에 관한 규칙 제38조)"
            ),
            new RowMeta(
                "supervisor-assign",
                "",
                "작업계획서·작업\n지휘자",
                "작업지휘자, 유도자, 신호수 등을 지정·배치하여\n통제에 따라 작업을 진행하는지\n(안전보건기준에 관한 규칙 제39조, 40조)"
            ),

            new RowMeta(
                "machine-guard",
                "",
                "기계·기구\n위험예방",
                "기계·장비 등의 결함, 작동이상 유무 및\n방호장치 작동 여부를 확인하였는지\n(안전보건기준에 관한 규칙 제35조)"
            ),
            new RowMeta(
                "no-misuse",
                "",
                "기계·기구\n위험예방",
                "기계·기구·설비 등을 목적 외의 용도로 사용하지\n않도록 하고 있는지\n(안전보건기준에 관한 규칙 제96조)"
            ),
            new RowMeta(
                "vehicle-limit",
                "",
                "기계·기구\n위험예방",
                "차량계 하역운반기계 및 건설기계 사용 시\n제한속도를 지정하였는지\n(안전보건기준에 관한 규칙 제98조)"
            ),
            new RowMeta(
                "qualification",
                "",
                "취업제한",
                "자격·면허·경험 또는 기능이 필요한 작업 시\n자격 등이 있는 자를 작업에 배치하였는지\n(산업안전보건법 제140조)"
            )
        )),

        // =========================
        // 3. 끼임사고 예방 (entanglement-prevention)
        // =========================
        new SectionDef("entanglement-prevention", 3, "끼임사고 예방", List.of(
            new RowMeta(
                "guard-installed",
                "",
                "방호덮개,\n안전가드 등\n방호장치",
                "덮개 등 방호장치를 설치하고\n그 장치가 정상적으로 작동하고 있는지\n(안전보건기준에 관한 규칙 제87조)"
            ),
            new RowMeta(
                "no-bypass",
                "",
                "방호덮개,\n안전가드 등\n방호장치",
                "방호장치를 임의로 해제하고 작업을 하는지\n(안전보건기준에 관한 규칙 제93조)"
            ),
            new RowMeta(
                "e-stop",
                "",
                "비상정지\n장치",
                "비상정지장치를 설치하고 그 장치가\n정상적으로 작동하고 있는지\n(안전보건기준에 관한 규칙 제88조, 제192조)"
            ),
            new RowMeta(
                "power-cut",
                "",
                "전원차단",
                "점검, 수리 등의 정비 작업 시\n전원을 차단하였는지\n(안전보건기준에 관한 규칙 제92조)"
            ),
            new RowMeta(
                "lockout",
                "",
                "전원차단",
                "정지한 기계를 임의로 운전하는 것을\n방지하기 위한 잠금장치 또는 표지판을\n설치하였는지\n(안전보건기준에 관한 규칙 제92조)"
            ),
            new RowMeta(
                "cert-inspect",
                "",
                "안전인증\n안전검사",
                "안전인증 및 안전검사를 적정하게 받고\n사용하는지\n(산업안전보건법 제84조, 제93조)"
            )
        )),

        // =========================
        // 4. 질식사고 예방 (asphyxia-prevention)
        // =========================
        new SectionDef("asphyxia-prevention", 4, "질식사고 예방", List.of(
            new RowMeta(
                "confined-identify",
                "",
                "밀폐공간 등 확인",
                "사업장 내 위험한 밀폐공간을\n파악하고 있는지\n(안전보건기준에 관한 규칙 제619조)"
            ),
            new RowMeta(
                "rule-check",
                "",
                "밀폐공간 등 확인",
                "사업주 또는 책임자가 안전조치 사항을\n확인한 후 작업하도록 하는 내부규정이 있는지\n(안전보건기준에 관한 규칙 제619조)"
            ),
            new RowMeta(
                "vent-equip",
                "",
                "밀폐공간 등 확인",
                "환기설비(송풍기 등), 송기마스크,\n공기호흡기를 보유하고 있는지\n(안전보건기준에 관한 규칙 제620조)"
            ),
            new RowMeta(
                "confined-warning",
                "",
                "밀폐공간 등 확인",
                "밀폐공간 출입금지조치 및 경고표시를 하였는지\n(안전보건기준에 관한 규칙 제622조)"
            ),

            new RowMeta(
                "o2-gas-meter",
                "",
                "산소 및 유해가스\n측정",
                "산소 및 유해가스 측정기를 보유하고 있는지\n(안전보건기준에 관한 규칙 제619조의2)"
            ),
            new RowMeta(
                "o2-gas-check",
                "",
                "산소 및 유해가스\n측정",
                "작업 전 또는 작업 중에 산소 및 유해가스 농도\n측정 및 적정공기 상태를 확인하고 있는지\n(안전보건기준에 관한 규칙 제619조의2)"
            ),

            new RowMeta(
                "ventilation-airline-use",
                "",
                "환기, 송기마스크\n구비 등 조치",
                "작업 전 또는 작업 중에 환기 및\n송기마스크, 공기호흡기를 사용하고 있는지\n(안전보건기준에 관한 규칙 제620조)"
            ),
            new RowMeta(
                "entry-exit-headcount-check",
                "",
                "환기, 송기마스크\n구비 등 조치",
                "밀폐공간 작업 시 입·출입 근로자 인원\n점검을 하였는지\n(안전보건기준에 관한 규칙 제621조)"
            ),
            new RowMeta(
                "confined-space-emergency-drill",
                "",
                "환기, 송기마스크\n구비 등 조치",
                "긴급구조훈련을 정기적으로 실시하고\n있는지(6개월 1회)\n(안전보건기준에 관한 규칙 제640조)"
            ),

            new RowMeta(
                "confined-space-watcher-assignment",
                "",
                "감시인 배치 및\n교육",
                "밀폐공간 작업시 밀폐공간 외부에 감시인을\n배치하고 있는지\n(안전보건기준에 관한 규칙 제623조)"
            ),
            new RowMeta(
                "worker-watcher-training",
                "",
                "감시인 배치 및\n교육",
                "작업 전 작업자 또는 감시인에게\n안전한 작업방법에 대해 교육하였는지\n(안전보건기준에 관한 규칙 제641조)"
            )
        )),

        // =========================
        // 5. 화재·폭발사고 예방 (fire-explosion-prevention)
        // =========================
        new SectionDef("fire-explosion-prevention", 5, "화재·폭발사고 예방", List.of(
            new RowMeta(
                "dangerous-goods-storage",
                "",
                "위험물 취급",
                "위험물을 작업장 외 별도 장소에 보관하고,\n작업에 필요한 양만 비치하고 있는지\n(안전보건기준에 관한 규칙 제16조)"
            ),
            new RowMeta(
                "explosion-fire-prevention-measures",
                "",
                "위험물 취급",
                "폭발·화재 및 누출을 방지하기 위한 적절한\n방호조치를 하고 있는지\n(안전보건기준에 관한 규칙 제225~238조)"
            ),

            new RowMeta(
                "path-safe-maintenance",
                "",
                "작업장 안전상태",
                "통로가 안전하게 사용될 수 있도록\n유지하고 있는지\n(안전보건기준에 관한 규칙 제22조)"
            ),
            new RowMeta(
                "flammable-gas-ventilation",
                "",
                "작업장 안전상태",
                "인화성가스 등이 체류하지 않도록 환기장치가\n설치되었는지\n(안전보건기준에 관한 규칙 제232조)"
            ),
            new RowMeta(
                "welding-fire-watch",
                "",
                "작업장 안전상태",
                "특정장소에서 용접·용단 작업을 하도록 하는 경우\n화재감시자를 배치하였는지\n(안전보건기준에 관한 규칙 제241조의2)"
            ),
            new RowMeta(
                "no-open-flame-area",
                "",
                "작업장 안전상태",
                "화재·폭발 우려가 있는 장소에서\n화기 사용을 금지하고 있는지\n(안전보건기준에 관한 규칙 제242조)"
            ),

            new RowMeta(
                "emergency-exit-installation",
                "",
                "비상조치",
                "작업장에 비상구가 출입문 외에 1개 이상\n설치되고 사용할 수 있는지\n(안전보건기준에 관한 규칙 제17조, 제18조)"
            ),
            new RowMeta(
                "fire-alarm-equipment",
                "",
                "비상조치",
                "화재 등 비상상황을 신속하게 알릴 수 있는\n경보용설비 기구가 설치되었는지\n(안전보건기준에 관한 규칙 제19조)"
            ),
            new RowMeta(
                "fire-extinguishing-equipment",
                "",
                "비상조치",
                "적합한 소화설비를 비치·사용하고 있는지\n(안전보건기준에 관한 규칙 제243조)"
            ),

            new RowMeta(
                "msds-training",
                "",
                "물질안전\n보건자료\n(MSDS)",
                "MSDS 대상 물질을 취급하는 근로자에게\n교육을 실시하였는지\n(산업안전보건법 제114조)"
            ),
            new RowMeta(
                "msds-posting",
                "",
                "물질안전\n보건자료\n(MSDS)",
                "MSDS 대상 물질을 취급하는 작업장 내\n취급 근로자가 쉽게 볼 수 있도록 게시하였는지\n(산업안전보건법 제114조)"
            ),
            new RowMeta(
                "msds-warning-label",
                "",
                "물질안전\n보건자료\n(MSDS)",
                "MSDS 대상 물질을 담은 용기 및 포장에\n경고표시를 적정히 하였는지\n(산업안전보건법 제115조)"
            )
        )),

        // =========================
        // 6. 폭염에 의한 건강장해 예방 (heat-illness-prevention)
        // =========================
        new SectionDef("heat-illness-prevention", 6, "폭염에 의한 건강장해 예방", List.of(
            new RowMeta(
                "drinking-water-and-salt",
                "",
                "물",
                "작업 중 땀을 많이 흘리게 되는 장소에 소금과\n깨끗한 음료수(생수 등) 등을\n충분히 갖추어 두었는지\n(안전보건기준에 관한 규칙 제571조)"
            ),
            new RowMeta(
                "cooling-ventilation-system",
                "",
                "냉방장치",
                "작폭염작업 시 냉방 또는 통풍 등을 위한\n적절한 온·습도 조절장치를\n설치·가동하고 있는지\n(안전보건기준에 관한 규칙 제560조)"
            ),
            new RowMeta(
                "worktime-adjustment-heat",
                "",
                "냉방장치",
                "작업시간대의 조정 등 폭염 노출을 줄일 수 있는\n조치를 하고 있는지\n(안전보건기준에 관한 규칙 제560조)"
            ),

            new RowMeta(
                "rest-interval-high-heat",
                "",
                "휴식",
                "체감온도 31도 이상 폭염작업 시 적절한 휴식을\n주기적으로 부여하고 있는지\n(안전보건기준에 관한 규칙 제560조)"
            ),
            new RowMeta(
                "rest-interval-moderate-heat",
                "",
                "휴식",
                "체감온도 33도 이상 폭염작업 시 매 2시간 마다\n20분 이상의 휴식을 부여하고 있는지\n(안전보건기준에 관한 규칙 제560조)"
            ),
            new RowMeta(
                "shade-for-outdoor-work",
                "",
                "휴식",
                "폭염 노출 옥외 장소에서 작업 시\n그늘진 장소를 제공 하는지\n(안전보건기준에 관한 규칙 제567조)"
            ),
            new RowMeta(
                "rest-facility-installation",
                "",
                "휴식",
                "휴식시간에 이용할 수 있는 휴게시설을 설치하고\n설치 관리 기준을 준수하는지\n(산업안전보건법 제128조의2)"
            ),

            new RowMeta(
                "personal-cooling-or-cooling-gear",
                "",
                "보냉장구",
                "체감온도 33도 이상 폭염작업 시 휴식을 부여하기 매우 곤란한 경우\n-> 개인용 냉방 또는 통풍장치 지급·가동 여부\n(안전보건기준에 관한 규칙 제560조)"
            ),
            new RowMeta(
                "heat-illness-emergency-response",
                "",
                "응급조치",
                "체폭염작업 근로자의 온열질환(의심자) 발생 시\n즉시 119 신고\n(안전보건기준에 관한 규칙 제562조)"
            )
        ))
    );

    // ===== 1) 점검표 1건 HTML 렌더링 =====
    public String renderSingleSheetHtml(ChecksheetDetailDto dto, String factoryName) {
        ChecksheetPayload p = dto.payload;
        Map<String, Map<String, ItemResult>> sections =
                (p != null && p.sections != null) ? p.sections : Map.of();

        StringBuilder sb = new StringBuilder();

        // yyyy-MM-dd -> yyyy년 MM월 dd일
        String dateKor = dto.checkDate;
        try {
            LocalDate d = LocalDate.parse(dto.checkDate);
            dateKor = d.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
        } catch (Exception ignore) {}

        String headerHtml = String.format("""
		  <div class="sheet">

		    <div class="sheet-header">
		      <div class="sheet-header-label">붙임</div>
		      <div class="sheet-header-title">안전한 일터 핵심 확인사항(자체점검표)</div>
		    </div>

		    <table class="sheet-info-table">
		      <tr>
		        <td class="info-cell">
		          <span class="info-label">점검일자:</span> %s
		        </td>
		        <td class="info-cell">
		          <span class="info-label">점검부서:</span> %s
		        </td>
		        <td class="info-cell"></td>
		      </tr>
		      <tr>
		        <td class="info-cell">
		          <span class="info-label">점검일시:</span> %s ~ %s
		        </td>
		        <td class="info-cell">
		          <span class="info-label">점검장소:</span> %s
		        </td>
		        <td class="info-cell">
		          <span class="info-label">점검자:</span> %s
		        </td>
		      </tr>
		    </table>
		  """,
		  escapeHtml(dateKor),
		  escapeHtml(dto.dept),
		  safe(dto.startTime),
		  safe(dto.endTime),
		  escapeHtml(dto.place),
		  escapeHtml(dto.inspector)
		);
        sb.append(headerHtml);

        int sectionIndex = 0;
        
        // 섹션 테이블들
        for (SectionDef def : SECTION_DEFS) {
        	if (sectionIndex > 0) {
                sb.append("<div class=\"page-break\"></div>"); // 섹션마다 새 페이지 강제
            }
        	
            Map<String, ItemResult> itemMap = sections.getOrDefault(def.sectionId(), Map.of());
            appendSectionTable(sb, def, itemMap);
            
            sectionIndex++;
        }
        sb.append("</div>");

        return sb.toString();
    }
    
    private void appendSectionTable(StringBuilder sb, SectionDef def, Map<String, ItemResult> itemMap) {

        // ✅ 섹션 래퍼 시작 (섹션별 page-break를 이 div에 걸 것)
        sb.append("<div class=\"section\">");

        // ✅ 섹션 헤더
        sb.append("<div class=\"section-header\">")
          .append("<span class=\"section-no\">").append(def.no()).append("</span>")
          .append("<span class=\"section-title-text\">").append(escapeHtml(def.title())).append("</span>")
          .append("</div>");

        // ✅ 테이블 시작 (tbody는 여기서 딱 1번만 연다)
        sb.append("""
          <table class="section-table">
            <colgroup>
              <col style="width: 64px;"/>   <!-- 항목(대) -->
              <col style="width: 30px;"/>   <!-- 항목(중) -->
              <col style="width: 300px;"/>  <!-- 점검내용 -->
              <col style="width: 80px;"/>   <!-- 점검결과 -->
              <col style="width: 140px;"/>  <!-- 조치사항 -->
            </colgroup>
            <thead>
              <tr>
                <th colspan="2">항목</th>
                <th>점검내용</th>
                <th>점검결과</th>
                <th>조치사항</th>
              </tr>
            </thead>
            <tbody>
        """);

        List<RowMeta> rows = def.rows();

        int i = 0;
        while (i < rows.size()) {
            RowMeta r = rows.get(i);

            boolean splitMode = isSplitMode(r); // major+minor 둘 다 있는 경우만 split

            if (!splitMode) {
                // ===== SINGLE MODE: 항목을 colspan=2로 합쳐서 출력 =====
                String labelHtml = getSingleLabelHtml(r);

                // 같은 라벨이 연속되면 rowspan 병합 가능
                int span = 1;
                for (int j = i + 1; j < rows.size(); j++) {
                    RowMeta nx = rows.get(j);
                    if (isSplitMode(nx)) break; // 모드가 바뀌면 끊음
                    if (Objects.equals(getSingleLabelHtml(nx), labelHtml)) span++;
                    else break;
                }

                for (int k = 0; k < span; k++) {
                    RowMeta row = rows.get(i + k);
                    ItemResult res = itemMap.get(row.itemId());

                    sb.append("<tr>");

                    if (k == 0) {
                        sb.append("<td class=\"item-cell\" colspan=\"2\" rowspan=\"").append(span).append("\">")
                          .append(toHtmlLines(labelHtml))
                          .append("</td>");
                    }

                    sb.append("<td class=\"content-cell\">")
                      .append(escapeHtml(row.contentText()).replace("\n", "<br/>"))
                      .append("</td>");

                    sb.append("<td class=\"result-cell\">")
                      .append(escapeHtml(res != null ? safe(res.result) : ""))
                      .append("</td>");

                    sb.append("<td class=\"action-cell\">")
                      .append(escapeHtml(res != null ? safe(res.action) : ""))
                      .append("</td>");

                    sb.append("</tr>");
                }

                i += span;
                continue;
            }

            // ===== SPLIT MODE: (대/중) 2칸 분리 + rowspan 병합 =====
            int majorSpan = 1;
            for (int j = i + 1; j < rows.size(); j++) {
                RowMeta nx = rows.get(j);
                if (!isSplitMode(nx)) break;
                if (Objects.equals(nx.majorHtml(), r.majorHtml())) majorSpan++;
                else break;
            }

            int majorBlockEnd = i + majorSpan;
            int k = i;
            boolean majorPrinted = false;

            while (k < majorBlockEnd) {
                RowMeta cur = rows.get(k);

                int minorSpan = 1;
                for (int j = k + 1; j < majorBlockEnd; j++) {
                    if (Objects.equals(rows.get(j).minorHtml(), cur.minorHtml())) minorSpan++;
                    else break;
                }

                for (int off = 0; off < minorSpan; off++) {
                    RowMeta row = rows.get(k + off);
                    ItemResult res = itemMap.get(row.itemId());

                    sb.append("<tr>");

                    if (!majorPrinted) {
                        sb.append("<td class=\"major-cell\" rowspan=\"").append(majorSpan).append("\">")
                          .append(toHtmlLines(cur.majorHtml()))
                          .append("</td>");
                        majorPrinted = true;
                    }

                    if (off == 0) {
                        sb.append("<td class=\"minor-cell\" rowspan=\"").append(minorSpan).append("\">")
                          .append(toHtmlLines(cur.minorHtml()))
                          .append("</td>");
                    }

                    sb.append("<td class=\"content-cell\">")
                      .append(escapeHtml(row.contentText()).replace("\n", "<br/>"))
                      .append("</td>");

                    sb.append("<td class=\"result-cell\">")
                      .append(escapeHtml(res != null ? safe(res.result) : ""))
                      .append("</td>");

                    sb.append("<td class=\"action-cell\">")
                      .append(escapeHtml(res != null ? safe(res.action) : ""))
                      .append("</td>");

                    sb.append("</tr>");
                }

                k += minorSpan;
            }

            i = majorBlockEnd;
        }

        // ✅ 여기서 딱 1번만 tbody/table 닫고, 섹션 div도 닫는다
        sb.append("""
            </tbody>
          </table>
        </div>
        """);
    }

    private static boolean isSplitMode(RowMeta r) {
        return !isBlank(r.majorHtml()) && !isBlank(r.minorHtml());
    }

    private static String getSingleLabelHtml(RowMeta r) {
        // 원본(B): major만 있으면 major를 2칸 합친 셀에 출력
        // safety row(major 비고 minor만 있는 케이스): minor를 2칸 합친 셀에 출력
        if (!isBlank(r.majorHtml())) return r.majorHtml();
        if (!isBlank(r.minorHtml())) return r.minorHtml();
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


    // ===== 2) 4개 점검표를 하나의 PDF로 생성 =====

    public Path generateCombinedPdf(LocalDate date,
                                    List<ChecksheetDetailDto> sheets,
                                    String factoryName) throws Exception {

        if (sheets == null || sheets.size() != 4) {
            throw new IllegalArgumentException("PDF 생성은 정확히 4개의 점검표에 대해서만 가능합니다. 현재: " +
                    (sheets == null ? 0 : sheets.size()));
        }

        // 경로 준비
        if (!Files.exists(BASE_DIR)) {
            Files.createDirectories(BASE_DIR);
        }

        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fileName = String.format("%s 자체점검표 (%s).pdf", factoryName, dateStr)
                .replace(":", "_")
                .replace("/", "-")
                .replace("\\", "-");

        Path outPath = BASE_DIR.resolve(fileName);

        // HTML 전체 조립
        StringBuilder html = new StringBuilder();
        String header = String.format("""
        	    <!DOCTYPE html>
        	    <html lang="ko">
        	    <head>
        	      <meta charset="UTF-8"/>
        	      <title>%s 자체점검표</title>
        	      <style>
        	        body {
        	          font-family: 'MalgunGothic', sans-serif;
        	          font-size: 10pt;
        	        }
        	        .sheet {
        	          page-break-after: always;
        	        }
					.sheet .section:first-of-type{
					  page-break-before: auto;
					}
					.page-break{
					  page-break-before: always;
					  break-before: page;
					}
        	        .sheet:last-child {
        	          page-break-after: auto;
        	        }
        	        .sheet-header {
        	          display: table;
        	          width: 100%%;
        	          border: 1px solid #000;
        	          border-collapse: collapse;
        	          margin-bottom: 6px;
        	        }
        	        .sheet-header-label {
        	          display: table-cell;
        	          width: 60px;
        	          background: #004b80;
        	          color: #ffffff;
        	          font-weight: bold;
        	          text-align: center;
        	          vertical-align: middle;
        	          padding: 3px 0;
        	          font-size: 14pt;
        	        }
        	        .sheet-header-title {
        	          display: table-cell;
        	          padding: 3px 8px;
        	          font-weight: bold;
        	          font-size: 16pt;
        	        }
        	        .sheet-info-table {
        	          width: 100%%;
        	          border-collapse: collapse;
        	          margin-top: 4px;
        	          margin-bottom: 4px;
        	        }
        	        .sheet-info-table td.info-cell {
					  font-size: 13pt;
					  padding: 4px 8px 0 0;
					  white-space: nowrap;
					}
        	        .info-label {
        	          font-weight: bold;
        	          padding-right: 4px;
        	        }
        	        .info-value {
        	          padding-right: 24px;
        	        }
        	        .section-header{
					  display: flex;
					  align-items: center;
					  justify-content: center;
					  gap: 6px;
					  font-size: 13pt;
					  font-weight: bold;
					  margin: 12px 0 4px 0;
					}
					.section-no{
					  display: inline-block;
					  text-align: center;
					  width: 22px;
					  height: 18px;
					  border: 1px solid #000;
					  font-size: 13pt;
					  line-height: 18px;	/* 숫자 세로 가운데 (height와 동일) */
        		   	  flex: 0 0 32px;
					}
					.section-title-text{
					  font-size: 12pt;
					  margin-left: 4px;
					}
        	        .section-index-box {
        	          display: table-cell;
        	          width: 25px;
        	          text-align: center;
        	          background: #004b80;
        	          color: #ffffff;
        	          font-weight: bold;
        	          vertical-align: middle;
        	        }
        	        .section-header-title {
        	          display: table-cell;
        	          padding: 2px 6px;
        	          font-weight: bold;
        	        }
        	        .section-table {
        	          width: 100%%;
        	          border-collapse: collapse;
        	          table-layout: fixed;
        	        }
        	        .section-table th{
					  border: 1px solid #000;
					  padding: 4px;
					  font-size: 10pt;
					  vertical-align: middle;
					  text-align: center;
					  background: #f3f4f6;
					}
					 .section-table td{
					  border: 1px solid #000;
					  padding: 16px 4px 16px 4px;
					  font-size: 10pt;
					  vertical-align: middle;
					}
					.section-table td.item-cell{
					  text-align: center;
					}
        	        .section-table td[rowspan], .section-table td[colspan] {
					  vertical-align: middle;
					  text-align: center;
					}
					.section-table td.major-cell{
					  white-space: normal;
					}
					.section-table td.item-cell{
					  white-space: normal;
					}
					/* 소항목(비계/철골/지붕) 칸 줄바꿈 방지 */
					.section-table td.minor-cell{
					  white-space: nowrap;
					}
					/* 점검내용은 줄바꿈 허용 (폭이 줄어드니 자연스럽게 내려가게) */
					.section-table td.content-cell{
					  text-align: left !important;
					  vertical-align: middle;
					  word-break: keep-all;
					  white-space: normal;
					}
					.section-table td.result-cell{
					  text-align: center;
					  vertical-align: middle;
					  white-space: nowrap;
					}
					.section-title{
					  font-size: 12pt;
					  font-weight: bold;
					  margin: 6px 0 4px 0;
					}
        	        .section-space {
        	          height: 8px;
        	        }
        	      </style>
        	    </head>
        	    <body>
        	    """,
            escapeHtml(factoryName)
        );
        html.append(header);

        // 부서/시간 순 정렬 (원하면 로직 조정)
        sheets.sort(Comparator.comparing((ChecksheetDetailDto d) -> safe(d.dept))
                              .thenComparing(d -> safe(d.startTime)));

        for (ChecksheetDetailDto dto : sheets) {
            html.append(renderSingleSheetHtml(dto, factoryName)).append("\n");
        }

        html.append("""
            </body>
            </html>
            """);

        // HTML -> PDF
        try (OutputStream os = Files.newOutputStream(outPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            builder.useFont(() -> 
            getClass()
                .getClassLoader()
                .getResourceAsStream("fonts/malgun.ttf"), "MalgunGothic");

            builder.withHtmlContent(html.toString(), null);
            builder.toStream(os);
            builder.run();
        }

        return outPath;
    }

    // ===== 유틸 =====

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
    
    private String toHtmlLines(String raw) {
        if (raw == null) return "";
        return escapeHtml(raw).replace("\n", "<br/>");
    }
}
