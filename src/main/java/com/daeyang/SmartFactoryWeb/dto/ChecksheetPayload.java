package com.daeyang.SmartFactoryWeb.dto;

import java.util.List;
import java.util.Map;

public class ChecksheetPayload {
	public String date;
    public String dept;
    public String place;
    public String inspector;
    public String startTime; 
    public String endTime;

    // sectionId -> itemId -> ItemResult
    public Map<String, Map<String, ItemResult>> sections;

    public static class ItemResult {
        public String result;      // 적합 / 부적합 / 해당 없음
        public String action;      // 조치사항
        //public List<ImageInfo> images; // 프론트에서 보내는 파일 메타
    }

    /*public static class ImageInfo {
        public String name;
        public long size;
        public String type;
        public String path;
    }*/
}
