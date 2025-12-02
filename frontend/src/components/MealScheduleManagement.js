import React, { useState, useEffect } from 'react';
import { mealScheduleAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { maskNameForDemo, maskDepartmentForDemo } from '../utils/masking';
import './MealScheduleManagement.css';

function MealScheduleManagement() {
  const { user, isDemo } = useAuth();
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [showParticipantsModal, setShowParticipantsModal] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [formData, setFormData] = useState({
    mealDate: '',
    mealType: 'LUNCH',
    description: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchSchedules();
  }, []);

  const fetchSchedules = async () => {
    try {
      setLoading(true);
      const response = await mealScheduleAPI.getUpcoming();
      setSchedules(response.data);
    } catch (error) {
      console.error('스케줄 목록 로드 실패:', error);
      setError('스케줄 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = () => {
    setFormData({
      mealDate: '',
      mealType: 'LUNCH',
      description: '',
    });
    setShowModal(true);
    setError('');
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setError('');
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await mealScheduleAPI.create(formData);
      handleCloseModal();
      fetchSchedules();
      alert('식사 스케줄이 등록되었습니다.');
    } catch (error) {
      console.error('스케줄 등록 실패:', error);
      setError(error.response?.data?.message || '스케줄 등록에 실패했습니다.');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      try {
        await mealScheduleAPI.delete(id);
        fetchSchedules();
        alert('스케줄이 삭제되었습니다.');
      } catch (error) {
        console.error('스케줄 삭제 실패:', error);
        alert('스케줄 삭제에 실패했습니다.');
      }
    }
  };

  const handleShowParticipants = async (schedule) => {
    try {
      setSelectedSchedule(schedule);
      const response = await mealScheduleAPI.getParticipants(schedule.id);
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
    return datePart;
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="meal-schedule-management">
      <div className="page-header">
        <h1 className="page-title">식사 스케줄 관리</h1>
        {!isDemo && (
          <button className="btn btn-primary" onClick={handleOpenModal}>
            + 스케줄 등록
          </button>
        )}
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>식사 시간</th>
              <th>설명</th>
              <th>등록자</th>
              <th>수령 현황</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {schedules.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center' }}>
                  등록된 스케줄이 없습니다.
                </td>
              </tr>
            ) : (
              schedules.map((schedule) => (
                <tr key={schedule.id}>
                  <td>{formatDate(schedule.mealDate)}</td>
                  <td>
                    <span className={`meal-type ${schedule.mealType.toLowerCase()}`}>
                      {getMealTypeText(schedule.mealType)}
                    </span>
                  </td>
                  <td>{schedule.description || '-'}</td>
                  <td>
                    {(() => {
                      const isOther = isDemo && schedule.createdById !== user?.id;
                      return isOther ? maskNameForDemo(schedule.createdByName) : schedule.createdByName;
                    })()}
                  </td>
                  <td>
                    <span className="participant-count">
                      {schedule.checkedCount || 0} / {schedule.totalParticipants || 0}명
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-sm btn-info"
                      onClick={() => handleShowParticipants(schedule)}
                      style={{ marginRight: '0.5rem' }}
                    >
                      수령자
                    </button>
                    {!isDemo && (
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => handleDelete(schedule.id)}
                      >
                        삭제
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 스케줄 등록 모달 */}
      {showModal && (
        <div className="modal" onClick={handleCloseModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">식사 스케줄 등록</h2>
              <button className="close-btn" onClick={handleCloseModal}>×</button>
            </div>

            {error && <div className="error">{error}</div>}

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">날짜 *</label>
                <input
                  type="date"
                  name="mealDate"
                  className="form-control"
                  value={formData.mealDate}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">식사 시간 *</label>
                <select
                  name="mealType"
                  className="form-control"
                  value={formData.mealType}
                  onChange={handleChange}
                  required
                >
                  <option value="BREAKFAST">아침</option>
                  <option value="LUNCH">점심</option>
                  <option value="DINNER">저녁</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">설명</label>
                <textarea
                  name="description"
                  className="form-control"
                  value={formData.description}
                  onChange={handleChange}
                  rows="3"
                  placeholder="식사에 대한 추가 설명을 입력하세요"
                />
              </div>

              {!isDemo && (
                <div className="form-actions">
                  <button type="button" className="btn btn-outline" onClick={handleCloseModal}>
                    취소
                  </button>
                  <button type="submit" className="btn btn-primary">
                    등록
                  </button>
                </div>
              )}
            </form>
          </div>
        </div>
      )}

      {/* 수령자 목록 모달 */}
      {showParticipantsModal && (
        <div className="modal" onClick={handleCloseParticipantsModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                수령자 목록 - {selectedSchedule && formatDate(selectedSchedule.mealDate)} {selectedSchedule && getMealTypeText(selectedSchedule.mealType)}
              </h2>
              <button className="close-btn" onClick={handleCloseParticipantsModal}>×</button>
            </div>

            <div className="participants-list">
              {participants.length === 0 ? (
                <p className="empty-message">아직 식사를 수령한 사용자가 없습니다.</p>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th>이름</th>
                      <th>부서</th>
                      <th>상태</th>
                      <th>메모</th>
                    </tr>
                  </thead>
                  <tbody>
                    {participants.map((participant) => {
                      const isOther = isDemo && participant.userId !== user?.id;
                      const rawDept = participant.userDepartment || '-';
                      return (
                        <tr key={participant.id}>
                          <td>{isOther ? maskNameForDemo(participant.userName) : participant.userName}</td>
                          <td>{isOther ? maskDepartmentForDemo(rawDept) : rawDept}</td>
                          <td>
                            <span className={`status ${participant.checked ? 'checked' : 'unchecked'}`}>
                              {participant.checked ? '✓ 수령' : 'X 미수령'}
                            </span>
                          </td>
                          <td>{participant.note || '-'}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            <div className="form-actions">
              <button className="btn btn-outline" onClick={handleCloseParticipantsModal}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MealScheduleManagement;

