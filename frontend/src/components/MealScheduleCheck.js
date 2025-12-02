import React, { useState, useEffect } from 'react';
import { mealScheduleAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { maskNameForDemo, maskDepartmentForDemo } from '../utils/masking';
import { getSeoulTodayString } from '../utils/date';
import './MealScheduleCheck.css';

function MealScheduleCheck() {
  const { user, isDemo } = useAuth();
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(getSeoulTodayString());
  const [showParticipantsModal, setShowParticipantsModal] = useState(false);
  const [showNonRecipientsModal, setShowNonRecipientsModal] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [nonRecipients, setNonRecipients] = useState([]);

  useEffect(() => {
    fetchSchedules();
  }, [selectedDate]);

  const fetchSchedules = async () => {
    try {
      setLoading(true);
      const response = await mealScheduleAPI.getByDate(selectedDate);
      setSchedules(response.data);
    } catch (error) {
      console.error('스케줄 목록 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCheck = async (scheduleId, note = '') => {
    try {
      if (!user.active) {
        alert('비활성 사용자는 식사 체크를 할 수 없습니다.');
        return;
      }
      await mealScheduleAPI.check(scheduleId, {
        userId: user.id,
        note: note,
      });
      fetchSchedules();
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

  const handleUncheck = async (scheduleId) => {
    try {
      await mealScheduleAPI.uncheck(scheduleId, {
        userId: user.id,
      });
      fetchSchedules();
      alert('식사 수령 체크가 취소되었습니다.');
    } catch (error) {
      console.error('체크 취소 실패:', error);
      alert('식사 수령 체크 취소에 실패했습니다.');
    }
  };

  const handleShowParticipants = async (schedule) => {
    try {
      setSelectedSchedule(schedule);
      const response = await mealScheduleAPI.getCheckedParticipants(schedule.id);
      setParticipants(response.data);
      setShowParticipantsModal(true);
    } catch (error) {
      console.error('참여자 목록 로드 실패:', error);
      alert('참여자 목록을 불러오는데 실패했습니다.');
    }
  };

  const handleCloseParticipantsModal = () => {
    setShowParticipantsModal(false);
    setSelectedSchedule(null);
    setParticipants([]);
  };

  const handleShowNonRecipients = async (schedule) => {
    try {
      setSelectedSchedule(schedule);
      const response = await mealScheduleAPI.getUncheckedParticipants(schedule.id);
      setNonRecipients(response.data);
      setShowNonRecipientsModal(true);
    } catch (error) {
      console.error('미수령자 목록 로드 실패:', error);
      alert('미수령자 목록을 불러오는데 실패했습니다.');
    }
  };

  const handleCloseNonRecipientsModal = () => {
    setShowNonRecipientsModal(false);
    setSelectedSchedule(null);
    setNonRecipients([]);
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
    const date = new Date(Number(year), Number(month) - 1, Number(day));
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    return `${year}년 ${Number(month)}월 ${Number(day)}일 (${days[date.getDay()]})`;
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="meal-schedule-check">
      <h1 className="page-title">식사 스케줄</h1>

      <div className="date-selector">
        <label className="date-label">날짜 선택:</label>
        <input
          type="date"
          className="date-input"
          value={selectedDate}
          onChange={(e) => setSelectedDate(e.target.value)}
        />
      </div>

      <div className="selected-date-info">
        {formatDate(selectedDate)}
      </div>

      <div className="schedules-container">
        {schedules.length === 0 ? (
          <div className="empty-state">
            <p>선택한 날짜에 등록된 식사 스케줄이 없습니다.</p>
          </div>
        ) : (
          schedules.map((schedule) => (
            <div key={schedule.id} className="schedule-card">
              <div className="schedule-header">
                <span className={`meal-type ${schedule.mealType.toLowerCase()}`}>
                  {getMealTypeText(schedule.mealType)}
                </span>
                <span className="participant-count">
                  {schedule.checkedCount || 0} / {schedule.totalParticipants || 0}명 수령
                </span>
              </div>
              
              {schedule.description && (
                <div className="schedule-description">
                  {schedule.description}
                </div>
              )}

              <div className="schedule-footer">
                <button
                  className="btn btn-sm btn-info"
                  onClick={() => handleShowParticipants(schedule)}
                >
                  수령자 보기
                </button>
                <button
                  className="btn btn-sm btn-warning"
                  onClick={() => handleShowNonRecipients(schedule)}
                >
                  미수령자 보기
                </button>
                {schedule.currentUserChecked ? (
                  <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => handleUncheck(schedule.id)}
                  >
                    수령 취소
                  </button>
                ) : (
                  <button
                    className="btn btn-sm btn-success"
                    onClick={() => handleCheck(schedule.id)}
                    disabled={!user.active}
                  >
                    {user.active ? '수령하기' : '비활성'}
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 수령자 목록 모달 */}
      {showParticipantsModal && (
        <div className="modal" onClick={handleCloseParticipantsModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                수령자 목록 - {selectedSchedule && getMealTypeText(selectedSchedule.mealType)}
              </h2>
              <button className="close-btn" onClick={handleCloseParticipantsModal}>×</button>
            </div>

            <div className="participants-list">
              {participants.length === 0 ? (
                <p className="empty-message">아직 식사를 수령한 사용자가 없습니다.</p>
              ) : (
                <div className="participants-grid">
                  {participants.map((participant) => {
                    const isOther = isDemo && participant.userId !== user?.id;
                    const rawDept = participant.userDepartment || '부서 미지정';
                    return (
                      <div key={participant.id} className="participant-item">
                        <div className="participant-name">
                          {isOther ? maskNameForDemo(participant.userName) : participant.userName}
                        </div>
                        <div className="participant-department">
                          {isOther ? maskDepartmentForDemo(rawDept) : rawDept}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn-outline" onClick={handleCloseParticipantsModal}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 미수령자 목록 모달 */}
      {showNonRecipientsModal && (
        <div className="modal" onClick={handleCloseNonRecipientsModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                미수령자 목록 - {selectedSchedule && getMealTypeText(selectedSchedule.mealType)}
              </h2>
              <button className="close-btn" onClick={handleCloseNonRecipientsModal}>×</button>
            </div>

            <div className="participants-list">
              {nonRecipients.length === 0 ? (
                <p className="empty-message">모든 사용자가 식사를 수령했습니다.</p>
              ) : (
                <div className="participants-grid">
                  {nonRecipients.map((participant) => {
                    const isOther = isDemo && participant.userId !== user?.id;
                    const rawDept = participant.userDepartment || '부서 미지정';
                    return (
                      <div key={participant.userId} className="participant-item">
                        <div className="participant-name">
                          {isOther ? maskNameForDemo(participant.userName) : participant.userName}
                        </div>
                        <div className="participant-department">
                          {isOther ? maskDepartmentForDemo(rawDept) : rawDept}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn-outline" onClick={handleCloseNonRecipientsModal}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MealScheduleCheck;

