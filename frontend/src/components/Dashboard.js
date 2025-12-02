import React, { useState, useEffect } from 'react';
import { mealScheduleAPI, userAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getSeoulTodayString } from '../utils/date';
import './Dashboard.css';

function Dashboard() {
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [todaySchedules, setTodaySchedules] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, checked: 0 });

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const today = getSeoulTodayString();
      const [schedulesResponse, usersResponse, activeUsersResponse] = await Promise.all([
        mealScheduleAPI.getByDate(today),
        userAPI.getAll(),
        userAPI.getActive(),
      ]);
      
      setTodaySchedules(schedulesResponse.data);
      setUsers(usersResponse.data);
      
      const checkedCount = schedulesResponse.data.reduce((sum, s) => sum + (s.checkedCount || 0), 0);
      const activeUserCount = activeUsersResponse.data.length;
      
      setStats({
        total: activeUserCount, // 활성 사용자 수
        checked: checkedCount,
      });
    } catch (error) {
      console.error('대시보드 데이터 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCheckSchedule = async (scheduleId) => {
    try {
      if (!user.active) {
        alert('비활성 사용자는 식사 체크를 할 수 없습니다.');
        return;
      }
      await mealScheduleAPI.check(scheduleId, {
        userId: user.id,
        note: '',
      });
      fetchDashboardData();
      alert('식사 수령이 체크되었습니다.');
    } catch (error) {
      console.error('체크 실패:', error);
      let errorMessage = '식사 수령 체크에 실패했습니다.';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.response?.data?.error) {
        errorMessage = error.response.data.error;
      } else if (error.message) {
        errorMessage = error.message;
      }
      alert(errorMessage);
    }
  };

  const handleUncheckSchedule = async (scheduleId) => {
    try {
      await mealScheduleAPI.uncheck(scheduleId, {
        userId: user.id,
      });
      fetchDashboardData();
      alert('식사 수령 체크가 취소되었습니다.');
    } catch (error) {
      console.error('체크 취소 실패:', error);
      alert('식사 수령 체크 취소에 실패했습니다.');
    }
  };

  const getMealTypeText = (type) => {
    switch (type) {
      case 'BREAKFAST': return '아침';
      case 'LUNCH': return '점심';
      case 'DINNER': return '저녁';
      default: return type;
    }
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="dashboard">
      <h1 className="page-title">대시보드</h1>
      
      <div className="stats-container">
        <div className="stat-card">
          <div className="stat-value">{users.length}</div>
          <div className="stat-label">전체 사용자</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{todaySchedules.length}</div>
          <div className="stat-label">오늘 식사 스케줄</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.checked}/{stats.total}</div>
          <div className="stat-label">식사 수령 인원(오늘)</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">
            {stats.total > 0 ? Math.round((stats.checked / stats.total) * 100) : 0}%
          </div>
          <div className="stat-label">식사 수령률(오늘)</div>
        </div>
      </div>

      {/* 빠른 액세스 */}
      <div className="quick-access">
        <h2 className="section-title">빠른 액세스</h2>
        <div className="quick-access-grid">
          <button className="quick-btn" onClick={() => navigate('/meal-schedule')}>
            <span className="quick-icon">📅</span>
            <span className="quick-label">식사 스케줄</span>
          </button>
          {isAdmin && (
            <>
              <button className="quick-btn" onClick={() => navigate('/schedule-management')}>
                <span className="quick-icon">⚙️</span>
                <span className="quick-label">스케줄 관리</span>
              </button>
              <button className="quick-btn" onClick={() => navigate('/users')}>
                <span className="quick-icon">👥</span>
                <span className="quick-label">사용자 관리</span>
              </button>
              <button className="quick-btn" onClick={() => navigate('/statistics')}>
                <span className="quick-icon">📊</span>
                <span className="quick-label">통계</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* 오늘의 식사 스케줄 */}
      <div className="card">
        <h2 className="card-title">오늘의 식사 스케줄</h2>
        {todaySchedules.length === 0 ? (
          <p className="empty-message">오늘 등록된 식사 스케줄이 없습니다.</p>
        ) : (
          <div className="schedule-list">
            {todaySchedules.map((schedule) => (
              <div key={schedule.id} className="schedule-item">
                <div className="schedule-header">
                  <span className={`meal-type ${schedule.mealType.toLowerCase()}`}>
                    {getMealTypeText(schedule.mealType)}
                  </span>
                  <span className="participant-info">
                    {schedule.checkedCount || 0} / {schedule.totalParticipants || 0}명 수령
                  </span>
                </div>
                {schedule.description && (
                  <div className="schedule-description">{schedule.description}</div>
                )}
                <div className="schedule-actions">
                  {schedule.currentUserChecked ? (
                    <button
                      className="btn btn-sm btn-secondary"
                      onClick={() => handleUncheckSchedule(schedule.id)}
                    >
                      수령 취소
                    </button>
                  ) : (
                    <button
                      className="btn btn-sm btn-success"
                      onClick={() => handleCheckSchedule(schedule.id)}
                      disabled={!user.active}
                    >
                      {user.active ? '수령하기' : '비활성'}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default Dashboard;

