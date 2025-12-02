import React, { useState, useEffect } from 'react';
import { mealScheduleAPI, userAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { maskNameForDemo, maskDepartmentForDemo } from '../utils/masking';
import { getSeoulTodayString, getSeoulDateNDaysAgoString } from '../utils/date';
import './AllMealHistory.css';

function AllMealHistory() {
  const { isDemo, user } = useAuth();
  const [history, setHistory] = useState([]);
  const [filteredHistory, setFilteredHistory] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [selectedUser, setSelectedUser] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('desc'); // 'desc' = 최신순, 'asc' = 오래된 순
  const [statusFilter, setStatusFilter] = useState('all'); // all, checked, unchecked

  useEffect(() => {
    // 기본적으로 최근 30일 기록 조회 (서울 기준)
    const end = getSeoulTodayString();
    const start = getSeoulDateNDaysAgoString(30);

    setEndDate(end);
    setStartDate(start);

    fetchUsers();
    fetchHistory(start, end);
  }, []);

  useEffect(() => {
    applyFilters();
  }, [history, selectedUser, searchQuery, sortOrder, statusFilter]);

  const fetchUsers = async () => {
    try {
      const response = await userAPI.getAll();
      setUsers(response.data);
    } catch (error) {
      console.error('사용자 목록 로드 실패:', error);
    }
  };

  const fetchHistory = async (start, end) => {
    try {
      setLoading(true);
      const response = await mealScheduleAPI.getAllHistory(start, end);
      setHistory(response.data);
    } catch (error) {
      console.error('식사 기록 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...history];

    // 사용자 필터
    if (selectedUser) {
      filtered = filtered.filter(record => record.userId === parseInt(selectedUser));
    }

    // 검색 필터 (이름 또는 부서)
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(record => 
        record.userName.toLowerCase().includes(query) ||
        (record.userDepartment && record.userDepartment.toLowerCase().includes(query))
      );
    }

    // 수령 상태 필터 (정확한 불리언 비교)
    if (statusFilter === 'checked') {
      filtered = filtered.filter(record => record.checked === true);
    } else if (statusFilter === 'unchecked') {
      filtered = filtered.filter(record => record.checked === false);
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
    setSelectedUser('');
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

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const mi = String(date.getMinutes()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
  };

  const escapeCSV = (value) => {
    if (value === null || value === undefined) return '""';
    const stringValue = String(value).replace(/"/g, '""');
    return `"${stringValue}"`;
  };

  /**
   * mode:
   * - 'all'       : 현재 필터된 전체 내역
   * - 'checked'   : 수령 완료 내역만
   * - 'unchecked' : 미수령 내역만
   */
  const handleExportCSV = (mode = 'all') => {
    if (filteredHistory.length === 0) {
      alert('내보낼 식사 기록이 없습니다.');
      return;
    }

    let targetHistory = [...filteredHistory];
    if (mode === 'checked') {
      targetHistory = targetHistory.filter((record) => record.checked);
    } else if (mode === 'unchecked') {
      targetHistory = targetHistory.filter((record) => !record.checked);
    }

    if (targetHistory.length === 0) {
      const label =
        mode === 'checked'
          ? '수령자'
          : mode === 'unchecked'
          ? '미수령자'
          : '식사 기록';
      alert(`현재 조건에서 내보낼 ${label} 데이터가 없습니다.`);
      return;
    }

    const header = [
      '날짜',
      '식사 타입',
      '설명',
      '사용자',
      '부서',
      '수령 여부',
      '수령 시간',
      '메모',
    ];

    const currentUserId = user?.id;
    const rows = targetHistory.map((record) => {
      const rawName = record.userName || '';
      const rawDept = record.userDepartment || '';
      const shouldMask = isDemo && record.userId !== currentUserId;
      const safeName = shouldMask ? maskNameForDemo(rawName) : rawName;
      const safeDept = shouldMask ? maskDepartmentForDemo(rawDept) : rawDept;

      return [
        formatDate(record.mealDate),
        getMealTypeText(record.mealType),
        record.description || '',
        safeName,
        safeDept,
        record.checked ? '수령' : '미수령',
        record.checked ? formatDateTime(record.checkedAt) : '',
        record.note || '',
      ];
    });

    const csvContent = [
      header.map(escapeCSV).join(','),
      ...rows.map((row) => row.map(escapeCSV).join(',')),
    ].join('\r\n');

    // Excel에서 한글이 깨지지 않도록 UTF-8 BOM을 함께 추가
    const bom = '\uFEFF';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const fileStart = startDate || 'all';
    const fileEnd = endDate || 'all';
    let suffix = 'all';
    if (mode === 'checked') suffix = 'checked';
    else if (mode === 'unchecked') suffix = 'unchecked';
    link.href = url;
    link.setAttribute('download', `meal-history_${fileStart}_${fileEnd}_${suffix}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="all-meal-history">
      <h1 className="page-title">전체 식사 기록</h1>

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
            <label>수령 상태:</label>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="all">전체</option>
              <option value="checked">수령</option>
              <option value="unchecked">미수령</option>
            </select>
          </div>

          <div className="filter-group">
            <label>사용자:</label>
            <select value={selectedUser} onChange={(e) => setSelectedUser(e.target.value)}>
              <option value="">전체</option>
              {users.map(u => {
                const rawName = u.name || '';
                const rawDept = u.department || '부서 미지정';
                const shouldMask = isDemo && u.id !== user?.id;
                const displayName = shouldMask ? maskNameForDemo(rawName) : rawName;
                const displayDept = shouldMask ? maskDepartmentForDemo(rawDept) : rawDept;
                return (
                  <option key={u.id} value={u.id}>
                    {displayName} ({displayDept})
                  </option>
                );
              })}
            </select>
          </div>

          <div className="filter-group">
            <label>검색:</label>
            <input
              type="text"
              placeholder="이름 또는 부서 검색"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
          </div>

          <button className="btn btn-secondary" onClick={handleReset}>
            필터 초기화
          </button>
          <div className="export-buttons">
            <button
              className="btn btn-success"
              onClick={() => handleExportCSV('all')}
            >
              전체 엑셀(CSV)
            </button>
            <button
              className="btn btn-success"
              onClick={() => handleExportCSV('checked')}
            >
              수령자 엑셀(CSV)
            </button>
            <button
              className="btn btn-success"
              onClick={() => handleExportCSV('unchecked')}
            >
              미수령자 엑셀(CSV)
            </button>
          </div>
        </div>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>식사 타입</th>
              <th>설명</th>
              <th>사용자</th>
              <th>부서</th>
              <th>수령 여부</th>
              <th>수령 시간</th>
              <th>메모</th>
            </tr>
          </thead>
          <tbody>
            {filteredHistory.length === 0 ? (
              <tr>
                <td colSpan="7" style={{ textAlign: 'center' }}>
                  {history.length === 0 ? '식사 기록이 없습니다.' : '검색 결과가 없습니다.'}
                </td>
              </tr>
              ) : (
              filteredHistory.map((record) => {
                const rawName = record.userName || '';
                const rawDeptValue = record.userDepartment || '';
                const shouldMask = isDemo && record.userId !== user?.id;
                const displayName = shouldMask ? maskNameForDemo(rawName) : rawName;
                const displayDept = shouldMask
                  ? (rawDeptValue ? maskDepartmentForDemo(rawDeptValue) : '-')
                  : (rawDeptValue || '-');

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
                    <td>{displayName}</td>
                    <td>{displayDept}</td>
                    <td>
                      <span className={`status ${record.checked ? 'checked' : 'unchecked'}`}>
                        {record.checked ? '✓ 수령' : 'X 미수령'}
                      </span>
                    </td>
                    <td>{record.checked ? formatDateTime(record.checkedAt) : '-'}</td>
                    <td>{record.note || '-'}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AllMealHistory;

