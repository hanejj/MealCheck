// demo_admin일 때 화면에서 이름/부서를 마스킹하기 위한 유틸 함수 모음

export const maskNameForDemo = (name) => {
  if (!name) return '';
  const chars = Array.from(name);

  if (chars.length === 1) {
    return '*';
  }

  // 첫 글자와 마지막 글자는 그대로 두고, 나머지 중간 글자는 모두 * 처리
  // 예) 홍길동 -> 홍*동, 김철수박 -> 김**박
  return chars
    .map((ch, idx) => {
      if (idx === 0 || idx === chars.length - 1) {
        return ch;
      }
      return '*';
    })
    .join('');
};

export const maskDepartmentForDemo = (department) => {
  if (!department) return '';
  const chars = Array.from(department);
  if (chars.length === 1) {
    return chars[0] + '*';
  }
  const first = chars[0];
  const maskedRest = chars.slice(1).map(() => '*').join('');
  return first + maskedRest;
};


