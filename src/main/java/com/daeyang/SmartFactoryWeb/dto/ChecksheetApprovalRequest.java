package com.daeyang.SmartFactoryWeb.dto;

import java.util.List;

public class ChecksheetApprovalRequest {
	// 프론트에서 보내는 선택된 점검표 ID 리스트
    public List<Long> sheetIds;
    public String empNo;
    public String grpId;
    public String grpPw;
}
