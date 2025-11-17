import axios from 'axios';

// 프로덕션에서는 Nginx가 /api를 backend로 프록시하므로, 같은 오리진의 /api 사용
// 개발 환경에서는 CRA의 proxy 또는 REACT_APP_API_URL 사용
const isProd = process.env.NODE_ENV === 'production';
const API_BASE_URL = isProd
  ? '/api'
  : (process.env.REACT_APP_API_URL || 'http://localhost:8080/api');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 토큰 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 401 에러 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

// 인증 API
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/auth/me'),
  getPendingUsers: () => api.get('/auth/pending'),
  approveUser: (userId) => api.post(`/auth/approve/${userId}`),
  rejectUser: (userId) => api.post(`/auth/reject/${userId}`),
  checkUsername: (username) => api.get(`/auth/check-username/${username}`),
};

// 사용자 API
export const userAPI = {
  getAll: () => api.get('/users'),
  getActive: () => api.get('/users/active'),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
  getStatistics: () => api.get('/users/statistics'),
  changePassword: (data) => api.post('/users/change-password', data),
};

// 식사 체크 API
export const mealCheckAPI = {
  getAll: () => api.get('/meal-checks'),
  getToday: () => api.get('/meal-checks/today'),
  getByDate: (date) => api.get(`/meal-checks/date/${date}`),
  getByUser: (userId) => api.get(`/meal-checks/user/${userId}`),
  getById: (id) => api.get(`/meal-checks/${id}`),
  create: (data) => api.post('/meal-checks', data),
  update: (id, data) => api.put(`/meal-checks/${id}`, data),
  delete: (id) => api.delete(`/meal-checks/${id}`),
  getStatistics: (startDate, endDate) => 
    api.get(`/meal-checks/statistics?startDate=${startDate}&endDate=${endDate}`),
};

// 식사 스케줄 API
export const mealScheduleAPI = {
  getAll: () => api.get('/meal-schedules'),
  getActive: () => api.get('/meal-schedules/active'),
  getUpcoming: () => api.get('/meal-schedules/upcoming'),
  getByDate: (date) => api.get(`/meal-schedules/date/${date}`),
  getById: (id) => api.get(`/meal-schedules/${id}`),
  create: (data) => api.post('/meal-schedules', data),
  update: (id, data) => api.put(`/meal-schedules/${id}`, data),
  delete: (id) => api.delete(`/meal-schedules/${id}`),
  getParticipants: (id) => api.get(`/meal-schedules/${id}/participants`),
  getCheckedParticipants: (id) => api.get(`/meal-schedules/${id}/participants/checked`),
  check: (id, data) => api.post(`/meal-schedules/${id}/check`, data),
  uncheck: (id, data) => api.post(`/meal-schedules/${id}/uncheck`, data),
  getMyHistory: (startDate, endDate) => {
    let url = '/meal-schedules/history/my';
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    return api.get(url + (params.toString() ? '?' + params.toString() : ''));
  },
  getAllHistory: (startDate, endDate) => {
    let url = '/meal-schedules/history/all';
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    return api.get(url + (params.toString() ? '?' + params.toString() : ''));
  },
  getUserHistory: (userId, startDate, endDate) => {
    let url = `/meal-schedules/history/user/${userId}`;
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    return api.get(url + (params.toString() ? '?' + params.toString() : ''));
  },
};

export default api;

