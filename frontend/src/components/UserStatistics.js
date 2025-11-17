import React, { useState, useEffect } from 'react';
import { userAPI } from '../services/api';
import './UserStatistics.css';

function UserStatistics() {
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStatistics();
  }, []);

  const fetchStatistics = async () => {
    try {
      setLoading(true);
      const response = await userAPI.getStatistics();
      setStatistics(response.data);
    } catch (error) {
      console.error('통계 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  if (!statistics) {
    return <div className="error">통계 데이터를 불러올 수 없습니다.</div>;
  }

  return (
    <div className="user-statistics">
      <h1 className="page-title">사용자 통계</h1>

      <div className="stats-grid">
        <div className="stat-card primary">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.totalUsers}</div>
            <div className="stat-label">전체 사용자</div>
          </div>
        </div>

        <div className="stat-card success">
          <div className="stat-icon">✓</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.activeUsers}</div>
            <div className="stat-label">활성 사용자</div>
          </div>
        </div>

        <div className="stat-card warning">
          <div className="stat-icon">⏸</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.inactiveUsers}</div>
            <div className="stat-label">비활성 사용자</div>
          </div>
        </div>

        <div className="stat-card info">
          <div className="stat-icon">👤</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.approvedUsers}</div>
            <div className="stat-label">승인된 사용자</div>
          </div>
        </div>

        <div className="stat-card danger">
          <div className="stat-icon">⏳</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.pendingUsers}</div>
            <div className="stat-label">승인 대기</div>
          </div>
        </div>

        <div className="stat-card admin">
          <div className="stat-icon">⭐</div>
          <div className="stat-content">
            <div className="stat-value">{statistics.adminUsers}</div>
            <div className="stat-label">관리자</div>
          </div>
        </div>
      </div>

      <div className="department-stats card">
        <h2 className="section-title">부서별 사용자 통계</h2>
        {statistics.byDepartment && Object.keys(statistics.byDepartment).length > 0 ? (
          <div className="department-chart">
            {Object.entries(statistics.byDepartment)
              .sort((a, b) => b[1] - a[1])
              .map(([department, count]) => (
                <div key={department} className="department-item">
                  <div className="department-info">
                    <span className="department-name">{department}</span>
                    <span className="department-count">{count}명</span>
                  </div>
                  <div className="department-bar-container">
                    <div 
                      className="department-bar"
                      style={{ 
                        width: `${(count / Math.max(...Object.values(statistics.byDepartment))) * 100}%` 
                      }}
                    />
                  </div>
                </div>
              ))}
          </div>
        ) : (
          <p className="empty-message">부서 정보가 없습니다.</p>
        )}
      </div>

      <div className="overview-stats card">
        <h2 className="section-title">사용자 현황</h2>
        <div className="overview-grid">
          <div className="overview-item">
            <span className="overview-label">일반 사용자</span>
            <span className="overview-value">{statistics.regularUsers}명</span>
          </div>
          <div className="overview-item">
            <span className="overview-label">관리자</span>
            <span className="overview-value">{statistics.adminUsers}명</span>
          </div>
          <div className="overview-item">
            <span className="overview-label">활성 비율</span>
            <span className="overview-value">
              {statistics.totalUsers > 0 
                ? Math.round((statistics.activeUsers / statistics.totalUsers) * 100) 
                : 0}%
            </span>
          </div>
          <div className="overview-item">
            <span className="overview-label">승인 비율</span>
            <span className="overview-value">
              {statistics.totalUsers > 0 
                ? Math.round((statistics.approvedUsers / statistics.totalUsers) * 100) 
                : 0}%
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default UserStatistics;

