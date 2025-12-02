import React, { useState, useEffect } from 'react';
import { mealScheduleAPI } from '../services/api';
import { getSeoulTodayString, getSeoulDateNDaysAgoString } from '../utils/date';
import './MyMealHistory.css';

function MyMealHistory() {
  const [history, setHistory] = useState([]);
  const [filteredHistory, setFilteredHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortOrder, setSortOrder] = useState('desc'); // 'desc' = 최신순, 'asc' = 오래된 순
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    // 기본적으로 최근 30일 기록 조회 (서울 기준)
    const end = getSeoulTodayString();
    const start = getSeoulDateNDaysAgoString(30);

    setEndDate(end);
    setStartDate(start);

    fetchHistory(start, end);
  }, []);

  useEffect(() => {
    applyFilters();
  }, [history, sortOrder, searchQuery]);

  const fetchHistory = async (start, end) => {
    try {
      setLoading(true);
      const response = await mealScheduleAPI.getMyHistory(start, end);
      setHistory(response.data);
    } catch (error) {
      console.error('식사 기록 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...history];

    // 검색 필터 (설명)
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(record => 
        (record.description && record.description.toLowerCase().includes(query)) ||
        getMealTypeText(record.mealType).includes(query)
      );
    }

    // 정렬 (스케줄 날짜 문자열 기준 - 타임존 영향 없이 정렬)
    filtered.sort((a, b) => {
      const dateA = a.mealDate || '';
      const dateB = b.mealDate || '';
      if (sortOrder === 'desc') {
        return dateB.localeCompare(dateA); // 최신순
      } else {
        return dateA.localeCompare(dateB); // 오래된 순
      }
    });

    setFilteredHistory(filtered);
  };

  const handleSearch = () => {
    fetchHistory(startDate, endDate);
  };

  const handleReset = () => {
    setSearchQuery('');
  };

  const getMealTypeText = (type) => {
    switch (type) {
      case 'BREAKFAST': return '아침';
      case 'LUNCH': return '점심';
      case 'DINNER': return '저녁';
      default: return type;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const [datePart] = String(dateString).split('T'); // 'YYYY-MM-DD'만 사용
    const [year, month, day] = datePart.split('-');
    return `${Number(year)}년 ${Number(month)}월 ${Number(day)}일`;
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="my-meal-history">
      <h1 className="page-title">내 식사 기록</h1>

      <div className="filter-container">
        <div className="date-filter">
          <label>시작일:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
          <label>종료일:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
          <button className="btn btn-primary" onClick={handleSearch}>
            조회
          </button>
        </div>

        <div className="filters-row">
          <div className="filter-group">
            <label>정렬:</label>
            <select value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
              <option value="desc">최신순</option>
              <option value="asc">오래된 순</option>
            </select>
          </div>

          <div className="filter-group">
            <label>검색:</label>
            <input
              type="text"
              placeholder="설명 또는 식사 타입 검색"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
          </div>

          <button className="btn btn-secondary" onClick={handleReset}>
            필터 초기화
          </button>
        </div>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>식사 타입</th>
              <th>설명</th>
              <th>수령 여부</th>
              <th>메모</th>
            </tr>
          </thead>
          <tbody>
            {filteredHistory.length === 0 ? (
              <tr>
                <td colSpan="5" style={{ textAlign: 'center' }}>
                  {history.length === 0 ? '식사 기록이 없습니다.' : '검색 결과가 없습니다.'}
                </td>
              </tr>
            ) : (
              filteredHistory.map((record) => {
                const rowKey = `${record.scheduleId || 's'}-${record.userId || 'u'}`;
                return (
                <tr key={rowKey}>
                  <td>{formatDate(record.mealDate)}</td>
                  <td>
                    <span className={`meal-type ${record.mealType.toLowerCase()}`}>
                      {getMealTypeText(record.mealType)}
                    </span>
                  </td>
                  <td>{record.description || '-'}</td>
                  <td>
                    <span className={`status ${record.checked ? 'checked' : 'unchecked'}`}>
                      {record.checked ? '✓ 수령' : 'X 미수령'}
                    </span>
                  </td>
                  <td>{record.note || '-'}</td>
                </tr>
              )})
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default MyMealHistory;

