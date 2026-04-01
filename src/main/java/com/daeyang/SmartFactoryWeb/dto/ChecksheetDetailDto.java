package com.daeyang.SmartFactoryWeb.dto;

public class ChecksheetDetailDto {
	public long id;
    public String checkDate;
    public String dept;
    public String place;
    public String inspector;
    public String startTime;
    public String endTime;
    public String createdAt;
    public String eApprovalStatus;

    public ChecksheetPayload payload;  // 이미 만든 payload DTO 재사용
}
