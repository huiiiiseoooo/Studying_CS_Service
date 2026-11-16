package com.cstudy.common.error;

/** 모든 오류 응답의 공통 형식. 내부 정보(SQL, 경로, 스택)는 담지 않는다. (QC-005) */
public record ErrorResponse(String code, String message) {
}
