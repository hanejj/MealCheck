import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Register.css';

function Register() {
  const [formData, setFormData] = useState({
    username: '',
    name: '',
    password: '',
    confirmPassword: '',
    department: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [usernameChecked, setUsernameChecked] = useState(false);
  const [usernameAvailable, setUsernameAvailable] = useState(false);
  const [checkingUsername, setCheckingUsername] = useState(false);
  const navigate = useNavigate();
  const { register } = useAuth();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });
    
    // 아이디가 변경되면 중복체크 초기화
    if (name === 'username') {
      setUsernameChecked(false);
      setUsernameAvailable(false);
    }
  };

  const handleCheckUsername = async () => {
    if (!formData.username) {
      setError('아이디를 입력하세요.');
      return;
    }

    setCheckingUsername(true);
    setError('');

    try {
      const response = await require('../services/api').authAPI.checkUsername(formData.username);
      const { available } = response.data;
      
      setUsernameChecked(true);
      setUsernameAvailable(available);
      
      if (available) {
        setSuccess('사용 가능한 아이디입니다.');
      } else {
        setError('이미 사용 중인 아이디입니다.');
      }
    } catch (err) {
      setError('중복 확인 중 오류가 발생했습니다.');
    } finally {
      setCheckingUsername(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // 아이디 중복체크 확인
    if (!usernameChecked) {
      setError('아이디 중복 확인을 해주세요.');
      return;
    }

    if (!usernameAvailable) {
      setError('사용할 수 없는 아이디입니다.');
      return;
    }

    // 비밀번호 확인
    if (formData.password !== formData.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    // 비밀번호 길이 확인
    if (formData.password.length < 6) {
      setError('비밀번호는 최소 6자 이상이어야 합니다.');
      return;
    }

    setLoading(true);

    const { confirmPassword, ...registerData } = formData;
    const result = await register(registerData);

    if (result.success) {
      // 모든 사용자는 승인 대기 메시지 표시
      setSuccess(result.message);
      // 폼 초기화
      setFormData({
        username: '',
        name: '',
        password: '',
        confirmPassword: '',
        department: '',
      });
      setUsernameChecked(false);
      setUsernameAvailable(false);
    } else {
      setError(result.error);
    }

    setLoading(false);
  };

  return (
    <div className="register-container">
      <div className="register-box">
        <h2>회원가입</h2>
        <form onSubmit={handleSubmit}>
          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          <div className="form-group">
            <label htmlFor="username">아이디 *</label>
            <div className="username-check-container">
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                required
                placeholder="아이디를 입력하세요"
                className="username-input"
              />
              <button
                type="button"
                onClick={handleCheckUsername}
                disabled={checkingUsername || !formData.username}
                className="check-btn"
              >
                {checkingUsername ? '확인중...' : '중복확인'}
              </button>
            </div>
            {usernameChecked && (
              <div className={`check-message ${usernameAvailable ? 'available' : 'unavailable'}`}>
                {usernameAvailable ? '✓ 사용 가능한 아이디입니다' : '✗ 이미 사용 중인 아이디입니다'}
              </div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="name">이름 *</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              placeholder="이름을 입력하세요"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">비밀번호 *</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="비밀번호를 입력하세요 (최소 6자)"
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">비밀번호 확인 *</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              placeholder="비밀번호를 다시 입력하세요"
            />
          </div>

          <div className="form-group">
            <label htmlFor="department">부서</label>
            <input
              type="text"
              id="department"
              name="department"
              value={formData.department}
              onChange={handleChange}
              placeholder="부서명을 입력하세요 (선택)"
            />
          </div>

          <button type="submit" disabled={loading} className="submit-btn">
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        <div className="register-footer">
          <p>
            이미 계정이 있으신가요? <Link to="/">로그인</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Register;

