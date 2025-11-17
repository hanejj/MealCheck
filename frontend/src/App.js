import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom';
import './App.css';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import UserList from './components/UserList';
import PendingUsers from './components/PendingUsers';
import MealScheduleManagement from './components/MealScheduleManagement';
import MealScheduleCheck from './components/MealScheduleCheck';
import UserStatistics from './components/UserStatistics';
import ChangePassword from './components/ChangePassword';
import MyMealHistory from './components/MyMealHistory';
import AllMealHistory from './components/AllMealHistory';

function AppContent() {
  const { isAuthenticated, user, logout, isAdmin } = useAuth();
  const [menuOpen, setMenuOpen] = React.useState(false);

  const toggleMenu = () => {
    setMenuOpen(!menuOpen);
  };

  const closeMenu = () => {
    setMenuOpen(false);
  };

  return (
    <div className="App">
      {isAuthenticated && (
        <>
          <nav className="navbar">
            <div className="nav-container">
              <Link to="/" className="nav-logo">
                🍽️ MealCheck
              </Link>
              <div className="nav-right">
                <div className="nav-user">
                  <span className="user-name">{user?.name}님</span>
                  <button onClick={logout} className="logout-btn">로그아웃</button>
                </div>
                <button className="menu-toggle" onClick={toggleMenu}>
                  <span></span>
                  <span></span>
                  <span></span>
                </button>
              </div>
            </div>
          </nav>
          
          {/* 사이드 메뉴 */}
          <div className={`side-menu ${menuOpen ? 'open' : ''}`}>
            <div className="side-menu-header">
              <h2>메뉴</h2>
              <button className="close-menu" onClick={closeMenu}>✕</button>
            </div>
            <div className="side-menu-content">
              <div className="menu-section">
                <h3 className="menu-section-title">일반 메뉴</h3>
                <Link to="/" className="side-menu-link" onClick={closeMenu}>
                  <span className="menu-icon">🏠</span>
                  <span>대시보드</span>
                </Link>
                <Link to="/meal-schedule" className="side-menu-link" onClick={closeMenu}>
                  <span className="menu-icon">📅</span>
                  <span>식사 스케줄</span>
                </Link>
                <Link to="/my-meal-history" className="side-menu-link" onClick={closeMenu}>
                  <span className="menu-icon">📝</span>
                  <span>내 식사 기록</span>
                </Link>
                <Link to="/change-password" className="side-menu-link" onClick={closeMenu}>
                  <span className="menu-icon">🔒</span>
                  <span>비밀번호 변경</span>
                </Link>
              </div>
              
              {isAdmin && (
                <div className="menu-section">
                  <h3 className="menu-section-title">관리자 메뉴</h3>
                  <Link to="/schedule-management" className="side-menu-link" onClick={closeMenu}>
                    <span className="menu-icon">⚙️</span>
                    <span>스케줄 관리</span>
                  </Link>
                  <Link to="/all-meal-history" className="side-menu-link" onClick={closeMenu}>
                    <span className="menu-icon">📋</span>
                    <span>전체 식사 기록</span>
                  </Link>
                  <Link to="/users" className="side-menu-link" onClick={closeMenu}>
                    <span className="menu-icon">👥</span>
                    <span>사용자 관리</span>
                  </Link>
                  <Link to="/statistics" className="side-menu-link" onClick={closeMenu}>
                    <span className="menu-icon">📊</span>
                    <span>통계</span>
                  </Link>
                  <Link to="/pending-users" className="side-menu-link" onClick={closeMenu}>
                    <span className="menu-icon">✅</span>
                    <span>가입 승인</span>
                  </Link>
                </div>
              )}
            </div>
          </div>
          
          {/* 메뉴 오버레이 */}
          {menuOpen && <div className="menu-overlay" onClick={closeMenu}></div>}
        </>
      )}

      <main className={isAuthenticated ? "main-content" : "main-content-full"}>
        <Routes>
          <Route path="/" element={
            isAuthenticated ? <Dashboard /> : <Login />
          } />
          <Route path="/register" element={
            isAuthenticated ? <Navigate to="/" /> : <Register />
          } />
          <Route path="/dashboard" element={
            <PrivateRoute><Dashboard /></PrivateRoute>
          } />
          <Route path="/meal-schedule" element={
            <PrivateRoute><MealScheduleCheck /></PrivateRoute>
          } />
          <Route path="/change-password" element={
            <PrivateRoute><ChangePassword /></PrivateRoute>
          } />
          <Route path="/my-meal-history" element={
            <PrivateRoute><MyMealHistory /></PrivateRoute>
          } />
          <Route path="/all-meal-history" element={
            <PrivateRoute>
              {isAdmin ? <AllMealHistory /> : <Navigate to="/" />}
            </PrivateRoute>
          } />
          <Route path="/schedule-management" element={
            <PrivateRoute>
              {isAdmin ? <MealScheduleManagement /> : <Navigate to="/" />}
            </PrivateRoute>
          } />
          <Route path="/users" element={
            <PrivateRoute>
              {isAdmin ? <UserList /> : <Navigate to="/" />}
            </PrivateRoute>
          } />
          <Route path="/statistics" element={
            <PrivateRoute>
              {isAdmin ? <UserStatistics /> : <Navigate to="/" />}
            </PrivateRoute>
          } />
          <Route path="/pending-users" element={
            <PrivateRoute>
              {isAdmin ? <PendingUsers /> : <Navigate to="/" />}
            </PrivateRoute>
          } />
        </Routes>
      </main>

      {isAuthenticated && (
        <footer className="footer">
          <p>&copy; 2025 MealCheck. All rights reserved.</p>
        </footer>
      )}
    </div>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  );
}

export default App;

