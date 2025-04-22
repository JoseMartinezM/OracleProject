// ReportsDashboard.js
import React, { useState, useEffect } from 'react';
import './ReportsDashboard.css';
import { API_LIST } from './API';
const Chart = window.Chart;

// Base URL for API
const API_URL = API_LIST.substring(0, API_LIST.lastIndexOf('/'));

// Tab Panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <div className="tab-panel-content">
          {children}
        </div>
      )}
    </div>
  );
}

function ReportsDashboard() {
  const [tabValue, setTabValue] = useState(0);
  const [sprints, setSprints] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedSprint, setSelectedSprint] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [completedTasks, setCompletedTasks] = useState([]);
  const [teamKpi, setTeamKpi] = useState(null);
  const [userKpi, setUserKpi] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Referencias para los canvas de gráficos
  const taskCompletionChartRef = React.useRef(null);
  const hoursComparisonChartRef = React.useRef(null);
  const userTaskCompletionChartRef = React.useRef(null);
  
  const chartInstances = React.useRef({});
  
  // Colores para los gráficos
  const COLORS = ['#4CAF50', '#FF9800', '#2196F3', '#F44336'];
  
  // Fetch sprints and users on component mount
  useEffect(() => {
    // Fetch all sprints
    fetch(`${API_URL}/sprints`)
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to fetch sprints');
        }
      })
      .then(data => {
        setSprints(data);
        if (data.length > 0) {
          setSelectedSprint(data[0].id);
        }
      })
      .catch(error => {
        console.error("Error fetching sprints:", error);
        setError(error.message);
      });
    
    // Fetch all users
    fetch(`${API_URL}/users`)
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to fetch users');
        }
      })
      .then(data => {
        // Filter only developers
        const developers = data.filter(user => user.role === "Developer");
        setUsers(developers);
        if (developers.length > 0) {
          setSelectedUser(developers[0].id);
        }
        setLoading(false);
      })
      .catch(error => {
        console.error("Error fetching users:", error);
        setError(error.message);
        setLoading(false);
      });
  }, []);
  
  // Fetch completed tasks when selected sprint changes
  useEffect(() => {
    if (selectedSprint) {
      setLoading(true);
      fetch(`${API_URL}/reports/sprint/${selectedSprint}/completed-tasks`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Failed to fetch completed tasks');
          }
        })
        .then(data => {
          setCompletedTasks(data);
          setLoading(false);
        })
        .catch(error => {
          console.error("Error fetching completed tasks:", error);
          setError(error.message);
          setLoading(false);
        });
      
      // Fetch team KPI for selected sprint
      fetch(`${API_URL}/reports/team/kpi/${selectedSprint}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Failed to fetch team KPI');
          }
        })
        .then(data => {
          setTeamKpi(data);
        })
        .catch(error => {
          console.error("Error fetching team KPI:", error);
        });
    }
  }, [selectedSprint]);
  
  // Fetch user KPI when selected user or sprint changes
  useEffect(() => {
    if (selectedUser && selectedSprint) {
      fetch(`${API_URL}/reports/user/${selectedUser}/kpi/${selectedSprint}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Failed to fetch user KPI');
          }
        })
        .then(data => {
          setUserKpi(data);
        })
        .catch(error => {
          console.error("Error fetching user KPI:", error);
        });
    }
  }, [selectedUser, selectedSprint]);
  
  // Renderizado de gráficos después de que el componente se monta y cuando los datos cambian
  useEffect(() => {
    if (tabValue === 1 && teamKpi && taskCompletionChartRef.current && hoursComparisonChartRef.current) {
      renderTaskCompletionChart();
      renderHoursComparisonChart();
    }
  }, [tabValue, teamKpi]);
  
  useEffect(() => {
    if (tabValue === 2 && userKpi && userTaskCompletionChartRef.current) {
      renderUserTaskCompletionChart();
    }
  }, [tabValue, userKpi]);
  
  // Funciones para renderizar gráficos
  const renderTaskCompletionChart = () => {
    if (!teamKpi) return;
    
    const ctx = taskCompletionChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.taskCompletionChart) {
      chartInstances.current.taskCompletionChart.destroy();
    }
    
    chartInstances.current.taskCompletionChart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Completed', 'Pending'],
        datasets: [{
          data: [teamKpi.completedTasks, teamKpi.totalTasks - teamKpi.completedTasks],
          backgroundColor: [COLORS[0], COLORS[1]],
          hoverBackgroundColor: [COLORS[0], COLORS[1]]
        }]
      },
      options: {
        responsive: true,
        plugins: {
          tooltip: {
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.raw || 0;
                const total = teamKpi.totalTasks;
                const percentage = Math.round((value / total) * 100);
                return `${label}: ${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  };
  
  const renderHoursComparisonChart = () => {
    if (!teamKpi) return;
    
    const ctx = hoursComparisonChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.hoursComparisonChart) {
      chartInstances.current.hoursComparisonChart.destroy();
    }
    
    chartInstances.current.hoursComparisonChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Estimated vs Actual Hours'],
        datasets: [
          {
            label: 'Estimated Hours',
            data: [teamKpi.totalEstimatedHours],
            backgroundColor: COLORS[2]
          },
          {
            label: 'Actual Hours',
            data: [teamKpi.totalActualHours],
            backgroundColor: COLORS[3]
          }
        ]
      },
      options: {
        responsive: true,
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    });
  };
  
  const renderUserTaskCompletionChart = () => {
    if (!userKpi) return;
    
    const ctx = userTaskCompletionChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.userTaskCompletionChart) {
      chartInstances.current.userTaskCompletionChart.destroy();
    }
    
    chartInstances.current.userTaskCompletionChart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Completed', 'Pending'],
        datasets: [{
          data: [userKpi.completedTasks, userKpi.totalTasks - userKpi.completedTasks],
          backgroundColor: [COLORS[0], COLORS[1]],
          hoverBackgroundColor: [COLORS[0], COLORS[1]]
        }]
      },
      options: {
        responsive: true,
        plugins: {
          tooltip: {
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.raw || 0;
                const total = userKpi.totalTasks;
                const percentage = Math.round((value / total) * 100);
                return `${label}: ${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  };
  
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  const handleSprintChange = (event) => {
    setSelectedSprint(event.target.value);
  };
  
  const handleUserChange = (event) => {
    setSelectedUser(event.target.value);
  };
  
  // Format percentage display
  const formatPercentage = (value) => {
    return value ? `${value.toFixed(2)}%` : '0%';
  };
  
  // Format hours display
  const formatHours = (value) => {
    return value ? `${value.toFixed(1)}h` : '0h';
  };
  
  // Render loading state
  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
      </div>
    );
  }
  
  // Render error state
  if (error) {
    return (
      <div className="error-container">
        <div className="error-message">Error: {error}</div>
      </div>
    );
  }
  
  return (
    <div className="reports-dashboard">
      <div className="dashboard-header">
        <h1 className="dashboard-title">KPI Dashboard</h1>
      </div>
      
      <div className="tabs-container">
        <div className="tabs">
          <button 
            className={`tab-button ${tabValue === 0 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 0)}
          >
            1.1 Sprint Tasks
          </button>
          <button 
            className={`tab-button ${tabValue === 1 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 1)}
          >
            1.2 Team KPI
          </button>
          <button 
            className={`tab-button ${tabValue === 2 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 2)}
          >
            1.3 Developer KPI
          </button>
        </div>
      </div>
      
      {/* 1.1 Sprint Tasks Tab */}
      <TabPanel value={tabValue} index={0}>
        <div className="grid-container">
          <div className="grid-item full-width">
            <div className="filter-controls">
              <div className="form-control">
                <label htmlFor="sprint-select">Select Sprint:</label>
                <select
                  id="sprint-select"
                  value={selectedSprint || ''}
                  onChange={handleSprintChange}
                >
                  {sprints.map((sprint) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">1.1 Completed Tasks in Sprint</h2>
                {completedTasks.length > 0 ? (
                  <div className="table-container">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Task Name</th>
                          <th>Developer</th>
                          <th className="align-right">Estimated Hours</th>
                          <th className="align-right">Actual Hours</th>
                          <th className="align-right">Efficiency</th>
                        </tr>
                      </thead>
                      <tbody>
                        {completedTasks.map((task) => (
                          <tr key={task.id}>
                            <td>{task.description}</td>
                            <td>{task.developerName}</td>
                            <td className="align-right">{formatHours(task.estimatedHours)}</td>
                            <td className="align-right">{formatHours(task.actualHours)}</td>
                            <td className="align-right">
                              {task.actualHours > 0 
                                ? formatPercentage((task.estimatedHours / task.actualHours) * 100) 
                                : 'N/A'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="no-data-message">
                    No completed tasks for this sprint.
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      </TabPanel>
      
      {/* 1.2 Team KPI Tab */}
      <TabPanel value={tabValue} index={1}>
        <div className="grid-container">
          <div className="grid-item full-width">
            <div className="filter-controls">
              <div className="form-control">
                <label htmlFor="sprint-select-team">Select Sprint:</label>
                <select
                  id="sprint-select-team"
                  value={selectedSprint || ''}
                  onChange={handleSprintChange}
                >
                  {sprints.map((sprint) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          
          {teamKpi && (
            <>
              <div className="grid-item">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      1.2 Team KPI: {teamKpi.sprintName}
                    </h2>
                    <p className="date-range">
                      {teamKpi.startDate} to {teamKpi.endDate}
                    </p>
                    <div className="metrics-grid">
                      <div className="metric-item">
                        <div className="metric-label">Total Tasks:</div>
                        <div className="metric-value">{teamKpi.totalTasks}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completed Tasks:</div>
                        <div className="metric-value">{teamKpi.completedTasks}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completion Rate:</div>
                        <div className="metric-value">{formatPercentage(teamKpi.completionRate)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Efficiency:</div>
                        <div className="metric-value">{formatPercentage(teamKpi.efficiency)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Estimated Hours:</div>
                        <div className="metric-value">{formatHours(teamKpi.totalEstimatedHours)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Actual Hours:</div>
                        <div className="metric-value">{formatHours(teamKpi.totalActualHours)}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="grid-item">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      Task Completion
                    </h2>
                    <div className="chart-container">
                      <canvas ref={taskCompletionChartRef} height="250"></canvas>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="grid-item full-width">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      Hours Comparison
                    </h2>
                    <div className="chart-container">
                      <canvas ref={hoursComparisonChartRef} height="250"></canvas>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </TabPanel>
      
      {/* 1.3 Developer KPI Tab */}
      <TabPanel value={tabValue} index={2}>
        <div className="grid-container">
          <div className="grid-item full-width">
            <div className="filter-controls">
              <div className="form-control">
                <label htmlFor="user-select">Select Developer:</label>
                <select
                  id="user-select"
                  value={selectedUser || ''}
                  onChange={handleUserChange}
                >
                  {users.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.name}
                    </option>
                  ))}
                </select>
              </div>
              
              <div className="form-control">
                <label htmlFor="sprint-select-user">Select Sprint:</label>
                <select
                  id="sprint-select-user"
                  value={selectedSprint || ''}
                  onChange={handleSprintChange}
                >
                  {sprints.map((sprint) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          
          {userKpi && (
            <>
              <div className="grid-item">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      1.3 Developer KPI: {userKpi.userName}
                    </h2>
                    <p className="date-range">
                      Sprint: {userKpi.sprintName} ({userKpi.startDate} to {userKpi.endDate})
                    </p>
                    <div className="metrics-grid">
                      <div className="metric-item">
                        <div className="metric-label">Total Tasks:</div>
                        <div className="metric-value">{userKpi.totalTasks}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completed Tasks:</div>
                        <div className="metric-value">{userKpi.completedTasks}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completion Rate:</div>
                        <div className="metric-value">{formatPercentage(userKpi.completionRate)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Efficiency:</div>
                        <div className="metric-value">{formatPercentage(userKpi.efficiency)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Estimated Hours:</div>
                        <div className="metric-value">{formatHours(userKpi.totalEstimatedHours)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Actual Hours:</div>
                        <div className="metric-value">{formatHours(userKpi.totalActualHours)}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="grid-item">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      Task Completion
                    </h2>
                    <div className="chart-container">
                      <canvas ref={userTaskCompletionChartRef} height="250"></canvas>
                    </div>
                  </div>
                </div>
              </div>
              
              {userKpi.tasks && userKpi.tasks.length > 0 && (
                <div className="grid-item full-width">
                  <div className="card">
                    <div className="card-content">
                      <h2 className="card-title">
                        Developer's Tasks
                      </h2>
                      <div className="table-container">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Task Description</th>
                              <th>Status</th>
                              <th className="align-right">Estimated Hours</th>
                              <th className="align-right">Actual Hours</th>
                              <th className="align-right">Efficiency</th>
                            </tr>
                          </thead>
                          <tbody>
                            {userKpi.tasks.map((task) => (
                              <tr key={task.id} className={task.completed ? 'completed-task' : ''}>
                                <td>{task.description}</td>
                                <td>{task.status}</td>
                                <td className="align-right">{formatHours(task.estimatedHours)}</td>
                                <td className="align-right">{formatHours(task.actualHours)}</td>
                                <td className="align-right">
                                  {task.completed && task.actualHours > 0 
                                    ? formatPercentage((task.estimatedHours / task.actualHours) * 100) 
                                    : 'N/A'}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </TabPanel>
    </div>
  );
}

export default ReportsDashboard;