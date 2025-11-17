import React, { useState, useEffect } from 'react';
import { mealScheduleAPI } from '../services/api';
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
    // 기본적으로 최근 30일 기록 조회
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 30);
    
    setEndDate(end.toISOString().split('T')[0]);
    setStartDate(start.toISOString().split('T')[0]);
    
    fetchHistory(start.toISOString().split('T')[0], end.toISOString().split('T')[0]);
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

    // 정렬
    filtered.sort((a, b) => {
      const dateA = new Date(a.mealDate);
      const dateB = new Date(b.mealDate);
      if (sortOrder === 'desc') {
        return dateB - dateA; // 최신순
      } else {
        return dateA - dateB; // 오래된 순
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
    const date = new Date(dateString);
    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
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
              <th>상태</th>
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
              filteredHistory.map((record) => (
                <tr key={record.id}>
                  <td>{formatDate(record.mealDate)}</td>
                  <td>
                    <span className={`meal-type ${record.mealType.toLowerCase()}`}>
                      {getMealTypeText(record.mealType)}
                    </span>
                  </td>
                  <td>{record.description || '-'}</td>
                  <td>
                    <span className={`status ${record.checked ? 'checked' : 'unchecked'}`}>
                      {record.checked ? '✓ 참여' : '○ 미참여'}
                    </span>
                  </td>
                  <td>{record.note || '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default MyMealHistory;

