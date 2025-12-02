import React, { useState, useEffect } from 'react';
import { authAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { maskNameForDemo, maskDepartmentForDemo } from '../utils/masking';
import './PendingUsers.css';

function PendingUsers() {
  const { isDemo, user: currentUser } = useAuth();
  const [pendingUsers, setPendingUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPendingUsers = async () => {
    try {
      setLoading(true);
      const response = await authAPI.getPendingUsers();
      setPendingUsers(response.data);
      setError('');
    } catch (err) {
      setError('승인 대기 사용자 목록을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPendingUsers();
  }, []);

  const handleApprove = async (userId) => {
    if (!window.confirm('이 사용자를 승인하시겠습니까?')) {
      return;
    }

    try {
      await authAPI.approveUser(userId);
      alert('사용자가 승인되었습니다.');
      loadPendingUsers();
    } catch (err) {
      alert('승인에 실패했습니다.');
      console.error(err);
    }
  };

  const handleReject = async (userId) => {
    if (!window.confirm('이 사용자의 가입 요청을 거절하시겠습니까?')) {
      return;
    }

    try {
      await authAPI.rejectUser(userId);
      alert('가입 요청이 거절되었습니다.');
      loadPendingUsers();
    } catch (err) {
      alert('거절에 실패했습니다.');
      console.error(err);
    }
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="pending-users-container">
      <div className="card">
        <h2 className="card-title">가입 승인 대기 사용자</h2>
        
        {error && <div className="error">{error}</div>}

        {pendingUsers.length === 0 ? (
          <p className="no-data">승인 대기 중인 사용자가 없습니다.</p>
        ) : (
          <div className="pending-users-list">
            {pendingUsers.map((pending) => {
              const isOther = isDemo && pending.id !== currentUser?.id;
              return (
              <div key={pending.id} className="pending-user-card">
                <div className="user-info">
                  <div className="info-row">
                    <span className="info-label">아이디:</span>
                    <span className="info-value">{pending.username}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">이름:</span>
                    <span className="info-value">
                      {isOther ? maskNameForDemo(pending.name) : pending.name}
                    </span>
                  </div>
                  {pending.department && (
                    <div className="info-row">
                      <span className="info-label">부서:</span>
                      <span className="info-value">
                        {isOther ? maskDepartmentForDemo(pending.department) : pending.department}
                      </span>
                    </div>
                  )}
                  <div className="info-row">
                    <span className="info-label">요청일:</span>
                    <span className="info-value">
                      {new Date(pending.createdAt).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })}
                    </span>
                  </div>
                </div>
                <div className="user-actions">
                  <button
                    onClick={() => handleApprove(pending.id)}
                    className="btn btn-success"
                  >
                    승인
                  </button>
                  <button
                    onClick={() => handleReject(pending.id)}
                    className="btn btn-danger"
                  >
                    거절
                  </button>
                </div>
              </div>
            )})}
          </div>
        )}
      </div>
    </div>
  );
}

export default PendingUsers;

