// ReportsDashboard.js - Versión mejorada con gráficas más grandes y legibles
import React, { useState, useEffect } from 'react';
import './ReportsDashboard.css';
import { API_LIST } from './API';
import SendIcon from '@mui/icons-material/Send';
import AssessmentIcon from '@mui/icons-material/Assessment';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import PersonIcon from '@mui/icons-material/Person';
import GroupIcon from '@mui/icons-material/Group';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import TaskAltIcon from '@mui/icons-material/TaskAlt';
import ChatIcon from '@mui/icons-material/Chat';
import AutorenewIcon from '@mui/icons-material/Autorenew';

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
  const [allUsersKpi, setAllUsersKpi] = useState([]);
  const [allSprintsKpi, setAllSprintsKpi] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [loadingSprintChange, setLoadingSprintChange] = useState(false);
  
  // Estado para la integración con ChatGPT
  const [aiAnalysis, setAiAnalysis] = useState('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  
  // Referencias para los canvas de gráficos
  const taskCompletionChartRef = React.useRef(null);
  const hoursComparisonChartRef = React.useRef(null);
  const userTaskCompletionChartRef = React.useRef(null);
  const totalHoursBySprintChartRef = React.useRef(null);
  const hoursByDeveloperChartRef = React.useRef(null);
  const tasksByDeveloperChartRef = React.useRef(null);
  
  const chartInstances = React.useRef({});
  
  // Colores mejorados para los gráficos
  const COLORS = [
    'rgba(76, 175, 80, 0.8)',   // Verde más intenso
    'rgba(255, 152, 0, 0.8)',   // Naranja más intenso
    'rgba(33, 150, 243, 0.8)',  // Azul más intenso
    'rgba(244, 67, 54, 0.8)',   // Rojo más intenso
    'rgba(156, 39, 176, 0.8)',  // Púrpura más intenso
    'rgba(63, 81, 181, 0.8)',   // Índigo más intenso
    'rgba(0, 150, 136, 0.8)',   // Teal más intenso
    'rgba(121, 85, 72, 0.8)',   // Marrón más intenso
    'rgba(96, 125, 139, 0.8)'   // Gris azulado más intenso
  ];

  // Colores de borde más definidos
  const BORDER_COLORS = [
    'rgba(76, 175, 80, 1)',
    'rgba(255, 152, 0, 1)',
    'rgba(33, 150, 243, 1)',
    'rgba(244, 67, 54, 1)',
    'rgba(156, 39, 176, 1)',
    'rgba(63, 81, 181, 1)',
    'rgba(0, 150, 136, 1)',
    'rgba(121, 85, 72, 1)',
    'rgba(96, 125, 139, 1)'
  ];

  // Configuración común mejorada para todas las gráficas
  const getCommonChartOptions = (title, showLegend = true) => ({
    responsive: true,
    maintainAspectRatio: false,
    devicePixelRatio: 2, // Para mejor resolución
    plugins: {
      title: {
        display: true,
        text: title,
        font: {
          size: 20,
          weight: 'bold'
        },
        color: '#FFFFFF',
        padding: {
          top: 15,
          bottom: 25
        }
      },
      legend: {
        display: showLegend,
        position: 'bottom',
        labels: {
          color: '#FFFFFF',
          font: {
            size: 16
          },
          padding: 25,
          usePointStyle: true
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        titleColor: '#FFFFFF',
        bodyColor: '#FFFFFF',
        borderColor: '#C74634',
        borderWidth: 2,
        cornerRadius: 10,
        titleFont: {
          size: 16,
          weight: 'bold'
        },
        bodyFont: {
          size: 14
        },
        padding: 15
      }
    },
    scales: {
      x: {
        ticks: {
          color: '#FFFFFF',
          font: {
            size: 14
          },
          maxRotation: 45,
          minRotation: 0
        },
        grid: {
          color: 'rgba(255, 255, 255, 0.15)',
          lineWidth: 1
        },
        title: {
          color: '#FFFFFF',
          font: {
            size: 16,
            weight: 'bold'
          }
        }
      },
      y: {
        ticks: {
          color: '#FFFFFF',
          font: {
            size: 14
          }
        },
        grid: {
          color: 'rgba(255, 255, 255, 0.15)',
          lineWidth: 1
        },
        title: {
          color: '#FFFFFF',
          font: {
            size: 16,
            weight: 'bold'
          }
        }
      }
    },
    layout: {
      padding: {
        top: 20,
        bottom: 20,
        left: 20,
        right: 20
      }
    }
  });
  
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
      
    // Fetch KPI summary data for all sprints
    fetch(`${API_URL}/reports/sprints/summary`)
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to fetch sprints summary');
        }
      })
      .then(data => {
        setAllSprintsKpi(data);
      })
      .catch(error => {
        console.error("Error fetching sprints summary:", error);
      });
  }, []);
  
  // Fetch completed tasks when selected sprint changes
  useEffect(() => {
    if (selectedSprint) {
      setLoading(true);
      
      // Cargar tareas completadas
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
        
      // Fetch KPI data for all developers in the selected sprint
      if (users && users.length > 0) {
        Promise.all(
          users.map(user => 
            fetch(`${API_URL}/reports/user/${user.id}/kpi/${selectedSprint}`)
              .then(response => {
                if (response.ok) {
                  return response.json();
                } else {
                  return null;
                }
              })
              .then(data => {
                if (data) {
                  return {
                    ...data,
                    userId: user.id,
                    userName: user.name || user.username
                  };
                }
                return null;
              })
              .catch(error => {
                console.error(`Error fetching KPI for user ${user.id}:`, error);
                return null;
              })
          )
        ).then(results => {
          const validResults = results.filter(result => result !== null);
          setAllUsersKpi(validResults);
          
          if (tabValue === 3) {
            setTimeout(() => {
              if (totalHoursBySprintChartRef.current) renderTotalHoursBySprintChart();
              if (hoursByDeveloperChartRef.current) renderHoursByDeveloperChart();
              if (tasksByDeveloperChartRef.current) renderTasksByDeveloperChart();
            }, 100);
          }
        });
      }
    }
  }, [selectedSprint, users]);
  
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
          
          if (tabValue === 2 && userTaskCompletionChartRef.current) {
            setTimeout(() => {
              renderUserTaskCompletionChart();
            }, 100);
          }
        })
        .catch(error => {
          console.error("Error fetching user KPI:", error);
        });
    }
  }, [selectedUser, selectedSprint, tabValue]);
  
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
  
  useEffect(() => {
    if (tabValue === 3 && allSprintsKpi.length > 0 && totalHoursBySprintChartRef.current) {
      renderTotalHoursBySprintChart();
    }
  }, [tabValue, allSprintsKpi]);

  // Efecto específico para cuando cambia el sprint seleccionado
useEffect(() => {
  if (selectedSprint && tabValue === 3) {
    console.log('Sprint cambió, re-renderizando gráficas...'); // Para debug
    
    // Esperar un poco más para asegurar que los datos estén actualizados
    setTimeout(() => {
      if (allUsersKpi.length > 0) {
        if (hoursByDeveloperChartRef.current) {
          renderHoursByDeveloperChart();
        }
        if (tasksByDeveloperChartRef.current) {
          renderTasksByDeveloperChart();
        }
      }
    }, 200); // Delay un poco más largo para cambios de sprint
  }
}, [selectedSprint, allUsersKpi]);
  
 // Renderizar gráficas cuando cambian los datos o la pestaña
useEffect(() => {
  if (tabValue === 3) {
    // Usar setTimeout para asegurar que el DOM esté listo
    setTimeout(() => {
      if (allSprintsKpi.length > 0 && totalHoursBySprintChartRef.current) {
        renderTotalHoursBySprintChart();
      }
      if (allUsersKpi.length > 0) {
        if (hoursByDeveloperChartRef.current) {
          renderHoursByDeveloperChart();
        }
        if (tasksByDeveloperChartRef.current) {
          renderTasksByDeveloperChart();
        }
      }
    }, 100); // Pequeño delay para asegurar que el DOM esté listo
  }
}, [tabValue, allSprintsKpi, allUsersKpi, selectedSprint]); // Agregar selectedSprint como dependencia
  
  // Funciones para renderizar gráficos MEJORADAS
  const renderTaskCompletionChart = () => {
    if (!teamKpi) return;
    
    const ctx = taskCompletionChartRef.current.getContext('2d');
    
    if (chartInstances.current.taskCompletionChart) {
      chartInstances.current.taskCompletionChart.destroy();
    }
    
    const completedTasks = teamKpi.completedTasks || 0;
    const totalTasks = teamKpi.totalTasks || 0;
    const pendingTasks = totalTasks - completedTasks;
    
    chartInstances.current.taskCompletionChart = new Chart(ctx, {
      type: 'doughnut', // Cambiado a doughnut para mejor visualización
      data: {
        labels: ['Completadas', 'Pendientes'],
        datasets: [{
          data: [completedTasks, pendingTasks],
          backgroundColor: [COLORS[0], COLORS[1]],
          borderColor: [BORDER_COLORS[0], BORDER_COLORS[1]],
          borderWidth: 3,
          hoverBackgroundColor: [COLORS[0], COLORS[1]],
          hoverBorderWidth: 4
        }]
      },
      options: {
        ...getCommonChartOptions('Progreso de Tareas del Equipo'),
        cutout: '60%', // Para el efecto doughnut
        plugins: {
          ...getCommonChartOptions('Progreso de Tareas del Equipo').plugins,
          tooltip: {
            ...getCommonChartOptions('Progreso de Tareas del Equipo').plugins.tooltip,
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.raw || 0;
                const total = totalTasks;
                const percentage = total > 0 ? Math.round((value / total) * 100) : 0;
                return `${label}: ${value} tareas (${percentage}%)`;
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
    
    if (chartInstances.current.hoursComparisonChart) {
      chartInstances.current.hoursComparisonChart.destroy();
    }
    
    const estimatedHours = teamKpi.totalEstimatedHours || 0;
    const actualHours = teamKpi.totalActualHours || 0;
    
    chartInstances.current.hoursComparisonChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Horas del Sprint'],
        datasets: [
          {
            label: 'Horas Estimadas',
            data: [estimatedHours],
            backgroundColor: COLORS[2],
            borderColor: BORDER_COLORS[2],
            borderWidth: 2,
            borderRadius: 8,
            borderSkipped: false,
          },
          {
            label: 'Horas Reales',
            data: [actualHours],
            backgroundColor: COLORS[3],
            borderColor: BORDER_COLORS[3],
            borderWidth: 2,
            borderRadius: 8,
            borderSkipped: false,
          }
        ]
      },
      options: {
        ...getCommonChartOptions('Comparación de Horas'),
        scales: {
          ...getCommonChartOptions('Comparación de Horas').scales,
          y: {
            ...getCommonChartOptions('Comparación de Horas').scales.y,
            beginAtZero: true,
            title: {
              display: true,
              text: 'Horas',
              color: '#FFFFFF',
              font: {
                size: 14,
                weight: 'bold'
              }
            }
          }
        },
        plugins: {
          ...getCommonChartOptions('Comparación de Horas').plugins,
          tooltip: {
            ...getCommonChartOptions('Comparación de Horas').plugins.tooltip,
            callbacks: {
              label: function(context) {
                const label = context.dataset.label || '';
                const value = context.raw || 0;
                return `${label}: ${value.toFixed(1)} horas`;
              }
            }
          }
        }
      }
    });
  };
  
  const renderUserTaskCompletionChart = () => {
    if (!userKpi) return;
    
    const ctx = userTaskCompletionChartRef.current.getContext('2d');
    
    if (chartInstances.current.userTaskCompletionChart) {
      chartInstances.current.userTaskCompletionChart.destroy();
    }
    
    const completedTasks = userKpi.completedTasks || 0;
    const totalTasks = userKpi.totalTasks || 0;
    const pendingTasks = totalTasks - completedTasks;
    
    chartInstances.current.userTaskCompletionChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Completadas', 'Pendientes'],
        datasets: [{
          data: [completedTasks, pendingTasks],
          backgroundColor: [COLORS[0], COLORS[1]],
          borderColor: [BORDER_COLORS[0], BORDER_COLORS[1]],
          borderWidth: 3,
          hoverBackgroundColor: [COLORS[0], COLORS[1]],
          hoverBorderWidth: 4
        }]
      },
      options: {
        ...getCommonChartOptions('Progreso Personal de Tareas'),
        cutout: '60%',
        plugins: {
          ...getCommonChartOptions('Progreso Personal de Tareas').plugins,
          tooltip: {
            ...getCommonChartOptions('Progreso Personal de Tareas').plugins.tooltip,
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.raw || 0;
                const total = totalTasks;
                const percentage = total > 0 ? Math.round((value / total) * 100) : 0;
                return `${label}: ${value} tareas (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  };
  
  // Gráfica 1: Horas Totales trabajadas por Sprint - MEJORADA
  const renderTotalHoursBySprintChart = () => {
    if (!allSprintsKpi || allSprintsKpi.length === 0) return;
    
    const ctx = totalHoursBySprintChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.totalHoursBySprintChart) {
      chartInstances.current.totalHoursBySprintChart.destroy();
    }
    
    // Preparar datos con nombres más cortos si es necesario
    const sprintNames = allSprintsKpi.map(sprint => {
      const name = sprint.sprintName || sprint.name || `Sprint ${sprint.id}`;
      return name.length > 20 ? name.substring(0, 20) + '...' : name;
    });
    const totalHours = allSprintsKpi.map(sprint => sprint.totalActualHours || 0);
    
    chartInstances.current.totalHoursBySprintChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: sprintNames,
        datasets: [
          {
            label: 'Horas Totales Trabajadas',
            data: totalHours,
            borderColor: COLORS[2],
            backgroundColor: COLORS[2] + '33',
            fill: true,
            tension: 0.4,
            borderWidth: 3,
            pointBackgroundColor: COLORS[2],
            pointBorderColor: '#ffffff',
            pointBorderWidth: 2,
            pointRadius: 6,
            pointHoverRadius: 8,
            pointHoverBackgroundColor: COLORS[2],
            pointHoverBorderColor: '#ffffff',
            pointHoverBorderWidth: 3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false, // ← ESTO ES LA CLAVE
        layout: {
          padding: {
            top: 20,
            right: 20,
            bottom: 20,
            left: 20
          }
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20,
              font: {
                size: 14,
                weight: '500'
              },
              color: '#ffffff'
            }
          },
          tooltip: {
            backgroundColor: 'rgba(49, 45, 42, 0.95)',
            titleColor: '#C74634',
            bodyColor: '#ffffff',
            borderColor: '#C74634',
            borderWidth: 1,
            cornerRadius: 8,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            padding: 12,
            callbacks: {
              title: function(context) {
                const index = context[0].dataIndex;
                return allSprintsKpi[index]?.sprintName || allSprintsKpi[index]?.name || `Sprint ${index + 1}`;
              },
              label: function(context) {
                const hours = context.raw || 0;
                return `Horas totales: ${hours.toFixed(1)}h`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Horas',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 14
              },
              callback: function(value) {
                return value.toFixed(1) + 'h';
              }
            },
            grid: {
              color: 'rgba(255, 255, 255, 0.1)',
              lineWidth: 1
            }
          },
          x: {
            title: {
              display: true,
              text: 'Sprints',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 12
              },
              maxRotation: 45,
              minRotation: 0
            },
            grid: {
              color: 'rgba(255, 255, 255, 0.05)',
              lineWidth: 1
            }
          }
        }
      }
    });
  };

  // Gráfica 2: Horas Trabajadas por Developer - MEJORADA
  const renderHoursByDeveloperChart = () => {
    if (!allUsersKpi || allUsersKpi.length === 0) return;
    
    const ctx = hoursByDeveloperChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.hoursByDeveloperChart) {
      chartInstances.current.hoursByDeveloperChart.destroy();
    }
    
    // Preparar datos con nombres más cortos si es necesario
    const userNames = allUsersKpi.map(user => {
      const name = user.userName || user.name || `User ${user.userId || 'ID'}`;
      return name.length > 15 ? name.substring(0, 15) + '...' : name;
    });
    const actualHours = allUsersKpi.map(user => user.totalActualHours || 0);
    
    chartInstances.current.hoursByDeveloperChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: userNames,
        datasets: [
          {
            label: 'Horas Trabajadas',
            data: actualHours,
            backgroundColor: allUsersKpi.map((_, index) => COLORS[index % COLORS.length]),
            borderColor: allUsersKpi.map((_, index) => COLORS[index % COLORS.length].replace('0.7', '1')),
            borderWidth: 2,
            borderRadius: 6,
            borderSkipped: false,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false, // ← ESTO ES LA CLAVE
        layout: {
          padding: {
            top: 20,
            right: 20,
            bottom: 20,
            left: 20
          }
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20,
              font: {
                size: 14,
                weight: '500'
              },
              color: '#ffffff'
            }
          },
          tooltip: {
            backgroundColor: 'rgba(49, 45, 42, 0.95)',
            titleColor: '#C74634',
            bodyColor: '#ffffff',
            borderColor: '#C74634',
            borderWidth: 1,
            cornerRadius: 8,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            padding: 12,
            callbacks: {
              title: function(context) {
                const index = context[0].dataIndex;
                return allUsersKpi[index]?.userName || allUsersKpi[index]?.name || `Developer ${index + 1}`;
              },
              label: function(context) {
                const hours = context.raw || 0;
                return `Horas trabajadas: ${hours.toFixed(1)}h`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Horas',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 14
              },
              callback: function(value) {
                return value.toFixed(1) + 'h';
              }
            },
            grid: {
              color: 'rgba(255, 255, 255, 0.1)',
              lineWidth: 1
            }
          },
          x: {
            title: {
              display: true,
              text: 'Desarrolladores',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 12
              },
              maxRotation: 45,
              minRotation: 0
            },
            grid: {
              display: false
            }
          }
        }
      }
    });
  };
  
  // Gráfica 3: Tareas Completadas por Developer - MEJORADA
  const renderTasksByDeveloperChart = () => {
    if (!allUsersKpi || allUsersKpi.length === 0) return;
    
    const ctx = tasksByDeveloperChartRef.current.getContext('2d');
    
    // Destruir gráfico previo si existe
    if (chartInstances.current.tasksByDeveloperChart) {
      chartInstances.current.tasksByDeveloperChart.destroy();
    }
    
    // Preparar datos con nombres más cortos si es necesario
    const userNames = allUsersKpi.map(user => {
      const name = user.userName || user.name || `User ${user.userId || 'ID'}`;
      return name.length > 15 ? name.substring(0, 15) + '...' : name;
    });
    const completedTasks = allUsersKpi.map(user => user.completedTasks || 0);
    
    chartInstances.current.tasksByDeveloperChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: userNames,
        datasets: [
          {
            label: 'Tareas Completadas',
            data: completedTasks,
            backgroundColor: allUsersKpi.map((_, index) => COLORS[(index + 2) % COLORS.length]),
            borderColor: allUsersKpi.map((_, index) => COLORS[(index + 2) % COLORS.length].replace('0.7', '1')),
            borderWidth: 2,
            borderRadius: 6,
            borderSkipped: false,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false, // ← ESTO ES LA CLAVE
        layout: {
          padding: {
            top: 20,
            right: 20,
            bottom: 20,
            left: 20
          }
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20,
              font: {
                size: 14,
                weight: '500'
              },
              color: '#ffffff'
            }
          },
          tooltip: {
            backgroundColor: 'rgba(49, 45, 42, 0.95)',
            titleColor: '#C74634',
            bodyColor: '#ffffff',
            borderColor: '#C74634',
            borderWidth: 1,
            cornerRadius: 8,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            padding: 12,
            callbacks: {
              title: function(context) {
                const index = context[0].dataIndex;
                return allUsersKpi[index]?.userName || allUsersKpi[index]?.name || `Developer ${index + 1}`;
              },
              label: function(context) {
                const tasks = context.raw || 0;
                return `Tareas completadas: ${tasks}`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Tareas',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 14
              },
              stepSize: 1,
              callback: function(value) {
                return Math.floor(value) + ' tareas';
              }
            },
            grid: {
              color: 'rgba(255, 255, 255, 0.1)',
              lineWidth: 1
            }
          },
          x: {
            title: {
              display: true,
              text: 'Desarrolladores',
              color: '#ffffff',
              font: {
                size: 16,
                weight: 'bold'
              }
            },
            ticks: {
              color: '#ffffff',
              font: {
                size: 12
              },
              maxRotation: 45,
              minRotation: 0
            },
            grid: {
              display: false
            }
          }
        }
      }
    });
  };
  
  // Simulación de análisis con IA basado en los datos reales
  const generateAiAnalysis = () => {
    setIsAnalyzing(true);
    setAiAnalysis('');
    
    const currentSprint = sprints.find(s => s.id === selectedSprint);
    const sprintName = currentSprint ? currentSprint.name : 'Selected Sprint';
    
    const analysisData = {
      sprint: teamKpi,
      developers: allUsersKpi.filter(dev => dev && dev.totalTasks && dev.totalTasks > 0)
    };
    
    setTimeout(() => {
      let generatedAnalysis = `# Análisis del Sprint ${sprintName}\n\n`;
      
      if (teamKpi) {
        const completionRate = teamKpi.completionRate || 0;
        const efficiency = teamKpi.efficiency || 0;
        
        generatedAnalysis += `## 1. Resumen general del rendimiento\n\n`;
        generatedAnalysis += `El sprint ${sprintName} tuvo una tasa de completado del **${completionRate.toFixed(2)}%**, `;
        generatedAnalysis += `con ${teamKpi.completedTasks || 0} de ${teamKpi.totalTasks || 0} tareas completadas. `;
        generatedAnalysis += `La eficiencia general fue del **${efficiency.toFixed(2)}%**, indicando `;
        generatedAnalysis += `${efficiency > 100 ? 'una subestimación en las horas planificadas' : 'una sobrestimación en las horas planificadas'}.\n\n`;
        
        generatedAnalysis += `En términos de horas, el equipo dedicó un total de **${(teamKpi.totalActualHours || 0).toFixed(1)} horas**, `;
        generatedAnalysis += `comparado con las **${(teamKpi.totalEstimatedHours || 0).toFixed(1)} horas estimadas** inicialmente.\n\n`;
      }
      
      if (allUsersKpi && allUsersKpi.length > 0) {
        const devsByTasks = [...allUsersKpi].sort((a, b) => (b.completedTasks || 0) - (a.completedTasks || 0));
        const devsByHours = [...allUsersKpi].sort((a, b) => (b.totalActualHours || 0) - (a.totalActualHours || 0));
        
        generatedAnalysis += `## 2. Comparación entre desarrolladores\n\n`;
        
        if (devsByTasks.length > 0 && devsByTasks[0].completedTasks) {
          generatedAnalysis += `**${devsByTasks[0].userName || 'El desarrollador principal'}** completó más tareas (${devsByTasks[0].completedTasks}), `;
        }
        
        if (devsByHours.length > 0 && devsByHours[0].totalActualHours) {
          generatedAnalysis += `mientras que **${devsByHours[0].userName || 'El desarrollador más activo'}** `;
          generatedAnalysis += `registró más horas de trabajo (${(devsByHours[0].totalActualHours).toFixed(1)}h).\n\n`;
        }
        
        generatedAnalysis += `La eficiencia varió entre los miembros del equipo:\n`;
        
        allUsersKpi.forEach(dev => {
          if (dev.completedTasks && dev.completedTasks > 0) {
            generatedAnalysis += `- **${dev.userName || 'Desarrollador'}**: ${(dev.efficiency || 0).toFixed(2)}% `;
            generatedAnalysis += `(Completó ${dev.completedTasks} tareas en ${(dev.totalActualHours || 0).toFixed(1)}h)\n`;
          }
        });
      }
      
      generatedAnalysis += `\n## 3. Recomendaciones\n\n`;
      generatedAnalysis += `1. **Mejorar la precisión de estimaciones**: Las discrepancias entre horas estimadas y reales sugieren la necesidad de recalibrar el proceso.\n\n`;
      generatedAnalysis += `2. **Equilibrar la asignación de tareas**: Considerar redistribuir tareas para balancear la carga de trabajo entre el equipo.\n\n`;
      generatedAnalysis += `3. **Compartir conocimientos**: Organizar sesiones para que los desarrolladores más eficientes compartan sus técnicas con el resto.`;
      
      setAiAnalysis(generatedAnalysis);
      setIsAnalyzing(false);
    }, 2000);
  };
  
  // Función actualizada para manejar el cambio de pestaña
  const handleTabChange = (event, newValue) => {
    console.log('Cambiando a pestaña:', newValue); // Para debug
    setTabValue(newValue);
    
    // Limpiar gráficas antes del cambio de pestaña
    if (newValue !== tabValue) {
      Object.values(chartInstances.current).forEach(chart => {
        if (chart && typeof chart.destroy === 'function') {
          chart.destroy();
        }
      });
      chartInstances.current = {};
    }
    
    // Re-renderizar gráficas cuando se cambia de pestaña
    setTimeout(() => {
      if (newValue === 1 && teamKpi) {
        if (taskCompletionChartRef.current) renderTaskCompletionChart();
        if (hoursComparisonChartRef.current) renderHoursComparisonChart();
      } else if (newValue === 2 && userKpi) {
        if (userTaskCompletionChartRef.current) renderUserTaskCompletionChart();
      } else if (newValue === 3) {
        if (totalHoursBySprintChartRef.current && allSprintsKpi.length > 0) {
          renderTotalHoursBySprintChart();
        }
        if (allUsersKpi.length > 0) {
          if (hoursByDeveloperChartRef.current) renderHoursByDeveloperChart();
          if (tasksByDeveloperChartRef.current) renderTasksByDeveloperChart();
        }
      }
    }, 150);
  };
  
  const handleSprintChange = (event) => {
    const newSprintId = parseInt(event.target.value);
    setLoadingSprintChange(true);
    
    // Limpiar gráficas
    Object.values(chartInstances.current).forEach(chart => {
      if (chart && typeof chart.destroy === 'function') {
        chart.destroy();
      }
    });
    chartInstances.current = {};
    
    setSelectedSprint(newSprintId);
    
    // Quitar loading después de un momento
    setTimeout(() => {
      setLoadingSprintChange(false);
    }, 300);
  };
  
  const handleUserChange = (event) => {
    const newUserId = event.target.value;
    setSelectedUser(newUserId);
  };
  
  const formatPercentage = (value) => {
    return value ? `${value.toFixed(2)}%` : '0%';
  };
  
  const formatHours = (value) => {
    return value ? `${value.toFixed(1)}h` : '0h';
  };
  
  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>Cargando datos...</p>
      </div>
    );
  }
  
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
        <h1 className="dashboard-title">
          <AssessmentIcon style={{ marginRight: '10px', verticalAlign: 'middle' }}/>
          KPI Dashboard
        </h1>
      </div>
      
      <div className="tabs-container">
        <div className="tabs">
          <button 
            className={`tab-button ${tabValue === 0 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 0)}
          >
            <TaskAltIcon style={{ marginRight: '5px' }} />
            Sprint Tasks
          </button>
          <button 
            className={`tab-button ${tabValue === 1 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 1)}
          >
            <GroupIcon style={{ marginRight: '5px' }} />
            Team KPI
          </button>
          <button 
            className={`tab-button ${tabValue === 2 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 2)}
          >
            <PersonIcon style={{ marginRight: '5px' }} />
            Developer KPI
          </button>
          <button 
            className={`tab-button ${tabValue === 3 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 3)}
          >
            <CompareArrowsIcon style={{ marginRight: '5px' }} />
            Comparativas
          </button>
          <button 
            className={`tab-button ${tabValue === 4 ? 'active' : ''}`} 
            onClick={(e) => handleTabChange(e, 4)}
          >
            <ChatIcon style={{ marginRight: '5px' }} />
            AI Analysis
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
                <h2 className="card-title">
                  <TaskAltIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Completed Tasks in Sprint
                </h2>
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
                        {completedTasks.map((task, index) => {
                          let efficiencyClass = '';
                          const estimatedHours = task.estimatedHours || 0;
                          const actualHours = task.actualHours || 0;
                          
                          if (actualHours > 0) {
                            const efficiencyValue = (estimatedHours / actualHours) * 100;
                            if (efficiencyValue < 80) {
                              efficiencyClass = 'efficiency-low';
                            } else if (efficiencyValue > 120) {
                              efficiencyClass = 'efficiency-high';
                            } else {
                              efficiencyClass = 'efficiency-medium';
                            }
                          }
                          
                          return (
                            <tr key={task.id || index} className="completed-task">
                              <td>{task.description}</td>
                              <td>{task.developerName}</td>
                              <td className="align-right">{formatHours(task.estimatedHours)}</td>
                              <td className="align-right">{formatHours(task.actualHours)}</td>
                              <td className={`align-right ${efficiencyClass}`}>
                                {task.actualHours > 0 
                                  ? formatPercentage((task.estimatedHours / task.actualHours) * 100) 
                                  : 'N/A'}
                              </td>
                            </tr>
                          );
                        })}
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
      
      {/* 1.2 Team KPI Tab - MEJORADO */}
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
                      <GroupIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                      Team KPI: {sprints.find(s => s.id === parseInt(selectedSprint))?.name || 'Current Sprint'}
                    </h2>
                    <p className="date-range">
                      {teamKpi.startDate || 'Start'} to {teamKpi.endDate || 'End'}
                    </p>
                    <div className="metrics-grid">
                      <div className="metric-item">
                        <div className="metric-label">Total Tasks:</div>
                        <div className="metric-value">{teamKpi.totalTasks || 0}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completed Tasks:</div>
                        <div className="metric-value">{teamKpi.completedTasks || 0}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completion Rate:</div>
                        <div className="metric-value">{formatPercentage(teamKpi.completionRate)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Efficiency:</div>
                        <div className={`metric-value ${
                          (teamKpi.efficiency || 0) < 80 ? 'efficiency-low' : 
                          (teamKpi.efficiency || 0) > 120 ? 'efficiency-high' : 'efficiency-medium'
                        }`}>
                          {formatPercentage(teamKpi.efficiency)}
                        </div>
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
              
              {/* Gráfica de completion mejorada */}
              <div className="grid-item">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      <TaskAltIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                      Task Completion
                    </h2>
                    <div className="chart-container large">
                      <canvas ref={taskCompletionChartRef}></canvas>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Gráfica de horas mejorada */}
              <div className="grid-item full-width">
                <div className="card">
                  <div className="card-content">
                    <h2 className="card-title">
                      <AccessTimeIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                      Hours Comparison
                    </h2>
                    <div className="chart-container large">
                      <canvas ref={hoursComparisonChartRef}></canvas>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </TabPanel>
      
      {/* 1.3 Developer KPI Tab - MEJORADO */}
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
                      {user.name || user.username}
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
                      <PersonIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                      Developer KPI: {userKpi.userName || users.find(u => u.id === parseInt(selectedUser))?.name || 'Selected Developer'}
                    </h2>
                    <p className="date-range">
                      Sprint: {userKpi.sprintName || sprints.find(s => s.id === parseInt(selectedSprint))?.name} 
                      ({userKpi.startDate || 'Start'} to {userKpi.endDate || 'End'})
                    </p>
                    <div className="metrics-grid">
                      <div className="metric-item">
                        <div className="metric-label">Total Tasks:</div>
                        <div className="metric-value">{userKpi.totalTasks || 0}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completed Tasks:</div>
                        <div className="metric-value">{userKpi.completedTasks || 0}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Completion Rate:</div>
                        <div className="metric-value">{formatPercentage(userKpi.completionRate)}</div>
                      </div>
                      <div className="metric-item">
                        <div className="metric-label">Efficiency:</div>
                        <div className={`metric-value ${
                          (userKpi.efficiency || 0) < 80 ? 'efficiency-low' : 
                          (userKpi.efficiency || 0) > 120 ? 'efficiency-high' : 'efficiency-medium'
                        }`}>
                          {formatPercentage(userKpi.efficiency)}
                        </div>
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
                      <TaskAltIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                      Task Completion
                    </h2>
                    <div className="chart-container large">
                      <canvas ref={userTaskCompletionChartRef}></canvas>
                    </div>
                  </div>
                </div>
              </div>
              
              {userKpi.tasks && userKpi.tasks.length > 0 && (
                <div className="grid-item full-width">
                  <div className="card">
                    <div className="card-content">
                      <h2 className="card-title">
                        <TaskAltIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
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
                            {userKpi.tasks.map((task, index) => {
                              let efficiencyClass = '';
                              if (task.completed && task.actualHours > 0) {
                                const efficiencyValue = (task.estimatedHours / task.actualHours) * 100;
                                if (efficiencyValue < 80) {
                                  efficiencyClass = 'efficiency-low';
                                } else if (efficiencyValue > 120) {
                                  efficiencyClass = 'efficiency-high';
                                } else {
                                  efficiencyClass = 'efficiency-medium';
                                }
                              }
                              
                              let statusClass = '';
                              const status = task.status || 'Pending';
                              switch(status) {
                                case 'Pending':
                                  statusClass = 'status-pending';
                                  break;
                                case 'In Progress':
                                  statusClass = 'status-progress';
                                  break;
                                case 'In Review':
                                  statusClass = 'status-review';
                                  break;
                                case 'Completed':
                                  statusClass = 'status-completed';
                                  break;
                                default:
                                  statusClass = '';
                              }
                              
                              return (
                                <tr key={task.id || index} className={task.completed ? 'completed-task' : ''}>
                                  <td>{task.description}</td>
                                  <td>
                                    <span className={`status-badge ${statusClass}`}>
                                      {status}
                                    </span>
                                  </td>
                                  <td className="align-right">{formatHours(task.estimatedHours)}</td>
                                  <td className="align-right">{formatHours(task.actualHours)}</td>
                                  <td className={`align-right ${efficiencyClass}`}>
                                    {task.completed && task.actualHours > 0 
                                      ? formatPercentage((task.estimatedHours / task.actualHours) * 100) 
                                      : 'N/A'}
                                  </td>
                                </tr>
                              );
                            })}
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
      
      {/* Pestaña de comparativas MEJORADA */}
      <TabPanel value={tabValue} index={3}>
        <div className="grid-container">
          <div className="grid-item full-width">
            <div className="filter-controls">
              <div className="form-control">
                <label htmlFor="sprint-select-compare">Select Sprint:</label>
                <select
                  id="sprint-select-compare"
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
          
          {/* Gráfica de evolución histórica - MÁS GRANDE */}
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">
                  <AccessTimeIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Evolución de Horas por Sprint
                </h2>
                {allSprintsKpi.length > 0 ? (
                  <div className="chart-container extra-large">
                    <canvas ref={totalHoursBySprintChartRef}></canvas>
                  </div>
                ) : (
                  <p className="no-data-message">
                    No hay datos disponibles para mostrar esta gráfica.
                  </p>
                )}
              </div>
            </div>
          </div>
          
          {/* Gráfica de horas por desarrollador - ANCHO COMPLETO Y GRANDE */}
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">
                  <AccessTimeIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Horas Trabajadas por Desarrollador
                </h2>
                {allUsersKpi.length > 0 ? (
                  <div className="chart-container extra-large">
                    <canvas ref={hoursByDeveloperChartRef}></canvas>
                  </div>
                ) : (
                  <p className="no-data-message">
                    No hay datos disponibles para mostrar esta gráfica.
                  </p>
                )}
              </div>
            </div>
          </div>
          
          {/* Gráfica de tareas por desarrollador - ANCHO COMPLETO Y GRANDE */}
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">
                  <TaskAltIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Tareas Completadas por Desarrollador
                </h2>
                {allUsersKpi.length > 0 ? (
                  <div className="chart-container extra-large">
                    <canvas ref={tasksByDeveloperChartRef}></canvas>
                  </div>
                ) : (
                  <p className="no-data-message">
                    No hay datos disponibles para mostrar esta gráfica.
                  </p>
                )}
              </div>
            </div>
          </div>
          
          {/* Tabla comparativa mejorada */}
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">
                  <CompareArrowsIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Comparativa Detallada de Desarrolladores
                </h2>
                {allUsersKpi.length > 0 ? (
                  <div className="table-container">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Developer</th>
                          <th className="align-right">Tareas Completadas</th>
                          <th className="align-right">Horas Trabajadas</th>
                          <th className="align-right">Horas/Tarea</th>
                          <th className="align-right">Eficiencia</th>
                          <th>Observaciones</th>
                        </tr>
                      </thead>
                      <tbody>
                        {allUsersKpi.map((dev, index) => {
                          const completedTasks = dev.completedTasks || 0;
                          const actualHours = dev.totalActualHours || 0;
                          const hoursPerTask = completedTasks > 0 ? (actualHours / completedTasks) : 0;
                          const efficiency = dev.efficiency || 0;
                          
                          let observation = '';
                          if (completedTasks === 0) {
                            observation = 'No completó tareas en este sprint.';
                          } else if (efficiency < 80) {
                            observation = 'Eficiencia por debajo del objetivo (80%).';
                          } else if (efficiency > 120) {
                            observation = 'Posible subestimación en las horas planificadas.';
                          } else if (teamKpi?.completedTasks > 0 && teamKpi?.totalActualHours > 0) {
                            const teamHoursPerTask = teamKpi.totalActualHours / teamKpi.completedTasks;
                            if (hoursPerTask > teamHoursPerTask * 1.5) {
                              observation = 'Trabajó en tareas más complejas que el promedio.';
                            } else if (hoursPerTask < teamHoursPerTask * 0.5) {
                              observation = 'Trabajó en tareas menos complejas que el promedio.';
                            } else {
                              observation = 'Desempeño dentro de los parámetros esperados.';
                            }
                          } else {
                            observation = 'Datos insuficientes para análisis comparativo.';
                          }
                          
                          let efficiencyClass = '';
                          if (efficiency < 80) {
                            efficiencyClass = 'efficiency-low';
                          } else if (efficiency > 120) {
                            efficiencyClass = 'efficiency-high';
                          } else {
                            efficiencyClass = 'efficiency-medium';
                          }
                          
                          return (
                            <tr key={dev.userId || index}>
                              <td>{dev.userName || `Developer ${index+1}`}</td>
                              <td className="align-right">{completedTasks}</td>
                              <td className="align-right">{formatHours(actualHours)}</td>
                              <td className="align-right">{hoursPerTask > 0 ? formatHours(hoursPerTask) : 'N/A'}</td>
                              <td className={`align-right ${efficiencyClass}`}>{formatPercentage(efficiency)}</td>
                              <td>{observation}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="no-data-message">
                    No hay datos disponibles para mostrar esta comparativa.
                  </p>
                )}
              </div>
            </div>
          </div>
          
          {/* Estadísticas clave mejoradas */}
          {teamKpi && allUsersKpi.length > 0 && (
            <div className="grid-item full-width">
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="card-content">
                    <h3 className="card-title">Mayor Productividad</h3>
                    {(() => {
                      const sortedByTasks = [...allUsersKpi]
                        .filter(dev => dev && dev.completedTasks && dev.completedTasks > 0)
                        .sort((a, b) => (b.completedTasks || 0) - (a.completedTasks || 0));
                      
                      const topDev = sortedByTasks.length > 0 ? sortedByTasks[0] : null;
                      
                      return (
                        <>
                          <div className="stat-value">
                            {topDev ? (topDev.userName || `Developer ${topDev.userId || 'ID'}`) : 'N/A'}
                          </div>
                          <p>({topDev ? topDev.completedTasks : 0} tareas)</p>
                        </>
                      );
                    })()}
                  </div>
                </div>
                
                <div className="stat-card">
                  <div className="card-content">
                    <h3 className="card-title">Más Horas Trabajadas</h3>
                    {(() => {
                      const sortedByHours = [...allUsersKpi]
                        .filter(dev => dev && dev.totalActualHours && dev.totalActualHours > 0)
                        .sort((a, b) => (b.totalActualHours || 0) - (a.totalActualHours || 0));
                      
                      const topDev = sortedByHours.length > 0 ? sortedByHours[0] : null;
                      
                      return (
                        <>
                          <div className="stat-value">
                            {topDev ? (topDev.userName || `Developer ${topDev.userId || 'ID'}`) : 'N/A'}
                          </div>
                          <p>({formatHours(topDev ? topDev.totalActualHours : 0)})</p>
                        </>
                      );
                    })()}
                  </div>
                </div>
                
                <div className="stat-card">
                  <div className="card-content">
                    <h3 className="card-title">Mayor Eficiencia</h3>
                    {(() => {
                      const sortedByEfficiency = [...allUsersKpi]
                        .filter(dev => dev && dev.completedTasks && dev.completedTasks > 0 && dev.efficiency)
                        .sort((a, b) => (b.efficiency || 0) - (a.efficiency || 0));
                      
                      const topDev = sortedByEfficiency.length > 0 ? sortedByEfficiency[0] : null;
                      
                      return (
                        <>
                          <div className="stat-value">
                            {topDev ? (topDev.userName || `Developer ${topDev.userId || 'ID'}`) : 'N/A'}
                          </div>
                          <p>({formatPercentage(topDev ? topDev.efficiency : 0)})</p>
                        </>
                      );
                    })()}
                  </div>
                </div>
                
                <div className="stat-card">
                  <div className="card-content">
                    <h3 className="card-title">Velocidad del Equipo</h3>
                    <div className="stat-value">
                      {teamKpi.completedTasks || 0}
                    </div>
                    <p>tareas completadas en el sprint</p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </TabPanel>
      
      {/* Pestaña de análisis con IA */}
      <TabPanel value={tabValue} index={4}>
        <div className="grid-container">
          <div className="grid-item full-width">
            <div className="card">
              <div className="card-content">
                <h2 className="card-title">
                  <ChatIcon style={{ marginRight: '8px', verticalAlign: 'middle' }} />
                  Análisis de IA del Rendimiento del Equipo
                </h2>
                <div className="filter-controls">
                  <div className="form-control">
                    <label htmlFor="sprint-select-ai">Select Sprint para analizar:</label>
                    <select
                      id="sprint-select-ai"
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
                  
                  <button 
                    className="analyze-button"
                    style={{
                      backgroundColor: 'var(--oracle-red)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      padding: '10px 16px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      cursor: 'pointer',
                      transition: 'all 0.3s ease'
                    }}
                    onClick={generateAiAnalysis}
                    disabled={isAnalyzing}
                  >
                    {isAnalyzing ? (
                      <>
                        <AutorenewIcon className="spin-icon" style={{ animation: 'spin 1s linear infinite' }} /> 
                        Analizando...
                      </>
                    ) : (
                      <>
                        <SendIcon />
                        Analizar Rendimiento
                      </>
                    )}
                  </button>
                </div>
                
                <div 
                  className="ai-analysis-container"
                  style={{
                    marginTop: '20px',
                    backgroundColor: 'var(--trans-05)',
                    borderRadius: '8px',
                    padding: '20px',
                    minHeight: '300px',
                    border: '1px solid var(--trans-10)'
                  }}
                >
                  {aiAnalysis ? (
                    <div 
                      className="ai-analysis"
                      style={{
                        whiteSpace: 'pre-line'
                      }}
                    >
                      <div className="markdown-content">
                        {aiAnalysis.split('\n').map((line, index) => {
                          if (line.startsWith('# ')) {
                            return <h1 key={index} style={{ color: 'var(--oracle-red)', fontSize: '1.5rem', marginTop: '24px', marginBottom: '16px' }}>{line.substring(2)}</h1>;
                          } else if (line.startsWith('## ')) {
                            return <h2 key={index} style={{ fontSize: '1.2rem', marginTop: '20px', marginBottom: '12px' }}>{line.substring(3)}</h2>;
                          } else if (line.startsWith('- ')) {
                            return <li key={index} style={{ marginBottom: '8px', marginLeft: '20px' }}>{line.substring(2)}</li>;
                          } else if (line.trim() === '') {
                            return <br key={index} />;
                          } else {
                            const boldRegex = /\*\*(.*?)\*\*/g;
                            const parts = line.split(boldRegex);
                            
                            if (parts.length > 1) {
                              return (
                                <p key={index} style={{ marginBottom: '12px', lineHeight: '1.5' }}>
                                  {parts.map((part, i) => {
                                    return i % 2 === 1 ? <strong key={i} style={{ color: 'var(--oracle-red)' }}>{part}</strong> : part;
                                  })}
                                </p>
                              );
                            } else {
                              return <p key={index} style={{ marginBottom: '12px', lineHeight: '1.5' }}>{line}</p>;
                            }
                          }
                        })}
                      </div>
                    </div>
                  ) : (
                    <div className="no-analysis-message" style={{ height: '300px', display: 'flex', justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
                      {isAnalyzing ? (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>
                          <div className="loading-spinner"></div>
                          <p>Analizando datos del sprint...</p>
                        </div>
                      ) : (
                        <p>Selecciona un sprint y haz clic en "Analizar Rendimiento" para obtener un análisis detallado del rendimiento del equipo.</p>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </TabPanel>
    </div>
  );
}

export default ReportsDashboard;