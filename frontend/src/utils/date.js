// 서울(Asia/Seoul) 기준 날짜 유틸 함수 모음

// 내부에서 공통으로 사용하는 포매터 (YYYY-MM-DD)
const seoulDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

/**
 * 서울(Asia/Seoul) 시간 기준으로 YYYY-MM-DD 문자열을 반환합니다.
 * @param {Date} [date] 기준 Date 객체 (기본값: 현재 시각)
 */
export function formatSeoulDate(date = new Date()) {
  return seoulDateFormatter.format(date); // 예: 2025-12-02
}

/**
 * 서울(Asia/Seoul) 기준 "오늘" 날짜를 YYYY-MM-DD 문자열로 반환합니다.
 */
export function getSeoulTodayString() {
  return formatSeoulDate(new Date());
}

/**
 * 서울(Asia/Seoul) 기준으로 N일 전 날짜를 YYYY-MM-DD 문자열로 반환합니다.
 * @param {number} days 이전 일수 (예: 30 → 30일 전)
 */
export function getSeoulDateNDaysAgoString(days) {
  const msPerDay = 24 * 60 * 60 * 1000;
  const target = new Date(Date.now() - days * msPerDay);
  return formatSeoulDate(target);
}


