import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import TaskManager from './components/TaskManager';
import GitHubIntegration from './components/GitHubIntegration';
import ReportsDashboard from './components/ReportsDashboard';
import { API_USERS } from './components/API';
import './index.css';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [loginError, setLoginError] = useState(null);
  const [activeTab, setActiveTab] = useState('tasks');

  // Handle login
  const handleLogin = (user, error = null) => {
    if (user) {
      setCurrentUser(user);
      setIsAuthenticated(true);
      setLoginError(null);
    } else {
      setLoginError(error);
    }
  };

  // Handle logout
  const handleLogout = () => {
    localStorage.removeItem('currentUser');
    setCurrentUser(null);
    setIsAuthenticated(false);
  };

  // Check for stored user on page load
  useEffect(() => {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setCurrentUser(parsedUser);
        setIsAuthenticated(true);
      } catch {
        localStorage.removeItem('currentUser');
      }
    }
  }, []);

  if (!isAuthenticated) {
    return <Login onLogin={handleLogin} loginError={loginError} />;
  }

  return (
    <div className="App">
      <div className="logout-container">
        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </div>

      <img
        src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n"
        alt="Oracle Logo"
        className="oracle-logo"
      />

      <h1>TODO LIST</h1>

      {currentUser && (
        <div className="welcome-user">
          Welcome, {currentUser.name || currentUser.username}
          <span className="user-role"> ({currentUser.role})</span>
        </div>
      )}

      <div className="app-tabs">
        <div
          className={`app-tab ${activeTab === 'tasks' ? 'active' : ''}`}
          onClick={() => setActiveTab('tasks')}
        >
          Tasks
        </div>
        {!currentUser?.role === 'Developer' && (
          <>
            <div
              className={`app-tab ${activeTab === 'github' ? 'active' : ''}`}
              onClick={() => setActiveTab('github')}
            >
              GitHub Integration
            </div>
            <div
              className={`app-tab ${activeTab === 'reports' ? 'active' : ''}`}
              onClick={() => setActiveTab('reports')}
            >
              KPI Reports
            </div>
          </>
        )}
      </div>

      {activeTab === 'tasks' && <TaskManager currentUser={currentUser} />}
      {activeTab === 'github' && <GitHubIntegration />}
      {activeTab === 'reports' && <ReportsDashboard />}
    </div>
  );
}

export default App;