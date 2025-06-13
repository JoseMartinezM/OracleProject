// Modificación del archivo App.js principal
import React, { useState, useEffect, useRef } from 'react';
import NewItem from './NewItem';
// import GitHubIntegration from './GitHubIntegration'; // REMOVIDO
import ReportsDashboard from './ReportsDashboard';
import { API_LIST, /* GITHUB_CREATE_BRANCH, GITHUB_GET_BRANCHES, */ API_SPRINTS, API_USERS, API_TASKS_BY_SPRINT } from './API'; // REMOVIDOS GITHUB APIs
import { Button, TableBody, Modal, Box, Typography, IconButton, Select, MenuItem, FormControl, InputLabel, TextField } from '@mui/material';
import Moment from 'react-moment';
import CloseIcon from '@mui/icons-material/Close';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import './index.css';

// Componente de Login
function Login({ onLogin, loginError }) {
  // "username" en lugar de "usernam"
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username || !password) return;
    
    setIsLoading(true);
    
    try {
      // Buscar usuario por nombre de usuario
      const response = await fetch(`${API_USERS}/username/${username}`);
      
      if (!response.ok) {
        throw new Error('Invalid credentials');
      }
      
      const userData = await response.json();
      
      // Verificar contraseña (en un sistema real, esto se haría en el backend)
      if (userData.password === password) {
        // Guardar información en el localStorage
        localStorage.setItem('currentUser', JSON.stringify({
          id: userData.id,
          username: userData.username,
          name: userData.name,
          role: userData.role
        }));
        
        onLogin(userData);
      } else {
        throw new Error('Invalid credentials');
      }
    } catch (error) {
      onLogin(null, error.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-overlay">
      <div className="login-container">
        <div className="login-header">
          <img 
            src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n" 
            alt="Oracle Logo"
          />
          <h2>TODO App Login</h2>
        </div>
        
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="login-input-group">
            <input
              type="text"
              className="login-input"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          
          <div className="login-input-group">
            <input
              type="password"
              className="login-input"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          
          <button 
            type="submit" 
            className="login-button"
            disabled={isLoading}
          >
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
          
          {loginError && (
            <div className="login-error">
              {loginError}
            </div>
          )}
        </form>
      </div>
    </div>
  );
}

// Modal para cambiar el estado de una tarea
function StatusChangeModal({ open, onClose, task, onStatusChange }) {
  const [selectedStatus, setSelectedStatus] = useState('');
  const [actualHours, setActualHours] = useState('');
  const actualHoursRef = useRef(null);

  useEffect(() => {
    if (task) {
      setSelectedStatus(task.status || 'Pending');
      setActualHours(task.actualHours || '');
    }
  }, [task]);

  const handleStatusChange = (status) => {
    setSelectedStatus(status);
  };

  const handleSubmit = () => {
    // Validar que se ingresen horas reales cuando se completa una tarea
    if (selectedStatus === 'Completed' && (!actualHours || actualHours <= 0)) {
      if (actualHoursRef.current) {
        actualHoursRef.current.focus();
      }
      return;
    }

    onStatusChange(selectedStatus, actualHours);
    onClose();
  };

  // Si no hay tarea seleccionada, no renderizar nada
  if (!task) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      aria-labelledby="status-change-modal"
      aria-describedby="change the status of a task"
    >
      <Box sx={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: '80%',
        maxWidth: 400,
        bgcolor: '#312D2A',
        boxShadow: 24,
        p: 4,
        borderRadius: '8px',
        color: 'white',
        border: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <div className="modal-header">
          <Typography variant="h5" component="h2" className="modal-title">
            Update Task Status
          </Typography>
          <IconButton 
            aria-label="close" 
            onClick={onClose}
            sx={{ 
              color: 'white',
              position: 'absolute',
              right: 8,
              top: 8
            }}
          >
            <CloseIcon />
          </IconButton>
        </div>

        <Typography variant="body1" sx={{ mt: 2, mb: 3 }}>
          Task: {task.description}
        </Typography>

        <div className="status-radio-group">
          <div 
            className={`status-radio-option ${selectedStatus === 'Pending' ? 'selected' : ''}`}
            onClick={() => handleStatusChange('Pending')}
          >
            <input 
              type="radio" 
              name="status" 
              value="Pending" 
              checked={selectedStatus === 'Pending'} 
              onChange={() => {}} 
            />
            <span>Pending</span>
          </div>
          <div 
            className={`status-radio-option ${selectedStatus === 'In Progress' ? 'selected' : ''}`}
            onClick={() => handleStatusChange('In Progress')}
          >
            <input 
              type="radio" 
              name="status" 
              value="In Progress" 
              checked={selectedStatus === 'In Progress'} 
              onChange={() => {}} 
            />
            <span>In Progress</span>
          </div>
          <div 
            className={`status-radio-option ${selectedStatus === 'In Review' ? 'selected' : ''}`}
            onClick={() => handleStatusChange('In Review')}
          >
            <input 
              type="radio" 
              name="status" 
              value="In Review" 
              checked={selectedStatus === 'In Review'} 
              onChange={() => {}} 
            />
            <span>In Review</span>
          </div>
          <div 
            className={`status-radio-option ${selectedStatus === 'Completed' ? 'selected' : ''}`}
            onClick={() => handleStatusChange('Completed')}
          >
            <input 
              type="radio" 
              name="status" 
              value="Completed" 
              checked={selectedStatus === 'Completed'} 
              onChange={() => {}} 
            />
            <span>Completed</span>
          </div>
        </div>

        {selectedStatus === 'Completed' && (
          <div className="actual-hours-container">
            <label className="actual-hours-label">
              <AccessTimeIcon fontSize="small" style={{ marginRight: '8px', verticalAlign: 'middle' }} />
              Actual Hours Worked:
            </label>
            <input
              type="number"
              className="actual-hours-input"
              value={actualHours}
              onChange={(e) => setActualHours(e.target.value)}
              min="0.25"
              step="0.25"
              placeholder="Enter actual hours worked"
              ref={actualHoursRef}
            />
          </div>
        )}

        <div className="status-action-buttons">
          <button className="cancel-status-button" onClick={onClose}>
            Cancel
          </button>
          <button className="update-status-button" onClick={handleSubmit}>
            Update Status
          </button>
        </div>
      </Box>
    </Modal>
  );
}

// Componente principal
function App() {
    // Estados de autenticación
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [currentUser, setCurrentUser] = useState(null);
    const [loginError, setLoginError] = useState(null);
    
    // Estados de la aplicación
    const [isLoading, setLoading] = useState(false);
    const [isInserting, setInserting] = useState(false);
    const [items, setItems] = useState([]);
    const [error, setError] = useState();
    
    // Estado para la pestaña activa
    const [activeTab, setActiveTab] = useState('tasks');
    
    // Estado para filtros
    const [priorityFilter, setPriorityFilter] = useState('All');

    // Estados para sprints y desarrolladores
    const [sprints, setSprints] = useState([]);
    const [selectedSprint, setSelectedSprint] = useState(null);
    const [developers, setDevelopers] = useState([]);
    const [tasksWithoutSprint, setTasksWithoutSprint] = useState([]);
    const [loadingSprints, setLoadingSprints] = useState(false);
    const [loadingDevelopers, setLoadingDevelopers] = useState(false);

    // Estado para el modal
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedTask, setSelectedTask] = useState(null);

    // Estado para el modal de cambio de estado
    const [statusModalOpen, setStatusModalOpen] = useState(false);
    const [taskForStatusChange, setTaskForStatusChange] = useState(null);

    // Estados para el modal de creación de sprint
    const [newSprintModalOpen, setNewSprintModalOpen] = useState(false);
    const [newSprintName, setNewSprintName] = useState('');
    const [newSprintDescription, setNewSprintDescription] = useState('');
    const [newSprintStartDate, setNewSprintStartDate] = useState('');
    const [newSprintDuration, setNewSprintDuration] = useState(2);
    const [isCreatingSprint, setIsCreatingSprint] = useState(false);
    
    // Comprobar si el usuario es desarrollador
    const isDeveloper = currentUser?.role === 'Developer';

    // Función para manejar el login
    const handleLogin = (user, error = null) => {
      if (user) {
        setCurrentUser(user);
        setIsAuthenticated(true);
        setLoginError(null);
      } else {
        setLoginError(error);
      }
    };
    
    // Función para manejar el logout
    const handleLogout = () => {
      localStorage.removeItem('currentUser');
      setCurrentUser(null);
      setIsAuthenticated(false);
    };
    
    // Verificar si hay un usuario en localStorage al cargar la página
    useEffect(() => {
      const storedUser = localStorage.getItem('currentUser');
      if (storedUser) {
        try {
          const parsedUser = JSON.parse(storedUser);
          setCurrentUser(parsedUser);
          setIsAuthenticated(true);
        } catch (error) {
          localStorage.removeItem('currentUser');
        }
      }
    }, []);

    // Función para abrir el modal con la tarea seleccionada
    const openTaskModal = (task) => {
        setSelectedTask(task);
        setModalOpen(true);
    };

    // Función para cerrar el modal
    const closeTaskModal = () => {
        setModalOpen(false);
    };

    // Función para abrir el modal de cambio de estado
    const openStatusModal = (task) => {
      setTaskForStatusChange(task);
      setStatusModalOpen(true);
    };

    // Función para cerrar el modal de cambio de estado
    const closeStatusModal = () => {
      setStatusModalOpen(false);
      setTaskForStatusChange(null);
    };

    // Funciones para el modal de creación de sprint
    const openNewSprintModal = () => {
      // Establecer la fecha de inicio por defecto como la fecha actual
      const today = new Date();
      const formattedDate = today.toISOString().split('T')[0]; // Formato YYYY-MM-DD
      
      setNewSprintName('');
      setNewSprintDescription('');
      setNewSprintStartDate(formattedDate);
      setNewSprintDuration(2);
      setNewSprintModalOpen(true);
    };

    // Función para cerrar el modal de nuevo sprint
    const closeNewSprintModal = () => {
      setNewSprintModalOpen(false);
    };

    // Función para calcular la fecha de finalización basada en la fecha de inicio y la duración
    const calculateEndDate = (startDate, durationWeeks) => {
      if (!startDate) return '';
      
      const start = new Date(startDate);
      const end = new Date(start);
      end.setDate(start.getDate() + (durationWeeks * 7));
      
      return end.toISOString().split('T')[0]; // Formato YYYY-MM-DD
    };

    // Función para crear un nuevo sprint
    const createNewSprint = () => {
      if (!newSprintName.trim() || !newSprintStartDate) {
        setError(new Error('Sprint name and start date are required'));
        return;
      }
      
      setIsCreatingSprint(true);
      
      const endDate = calculateEndDate(newSprintStartDate, newSprintDuration);
      
      const sprintData = {
        name: newSprintName,
        description: newSprintDescription,
        startDate: newSprintStartDate,
        endDate: endDate,
        createdBy: currentUser?.id || null
      };
      
      fetch(API_SPRINTS, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(sprintData)
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to create sprint');
        }
      })
      .then(
        (result) => {
          // Agregar el nuevo sprint a la lista de sprints
          setSprints([...sprints, result]);
          setIsCreatingSprint(false);
          closeNewSprintModal();
        },
        (error) => {
          setIsCreatingSprint(false);
          setError(error);
        }
      );
    };

    // Función para cargar los sprints
    const loadSprints = () => {
      setLoadingSprints(true);
      fetch(API_SPRINTS)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Error loading sprints');
          }
        })
        .then(
          (result) => {
            setSprints(result);
            setLoadingSprints(false);
          },
          (error) => {
            setLoadingSprints(false);
            setError(error);
          });
    };
    
    // Función para cargar los desarrolladores
    const loadDevelopers = () => {
      setLoadingDevelopers(true);
      fetch(API_USERS)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Error loading developers');
          }
        })
        .then(
          (result) => {
            // Filtrar solo los usuarios con rol "Developer"
            const devs = result.filter(user => user.role === 'Developer');
            setDevelopers(devs);
            setLoadingDevelopers(false);
          },
          (error) => {
            setLoadingDevelopers(false);
            setError(error);
          });
    };
    
    // Función para cargar tareas por sprint
    const loadTasksBySprint = (sprintId) => {
      setLoading(true);
      fetch(`${API_TASKS_BY_SPRINT}/${sprintId}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Error loading tasks for sprint');
          }
        })
        .then(
          (result) => {
            setLoading(false);
            // Adaptando al nuevo formato de datos
            setItems(result.map(item => ({
              id: item.id,
              description: item.description,
              done: item.done,
              createdAt: item.creation_ts,
              status: item.status || 'Pending',
              priority: item.priority || 'Medium',
              steps: item.steps || '',
              assignedTo: item.assignedTo,
              createdBy: item.createdBy,
              isArchived: item.isArchived,
              sprintId: item.sprintId,
              estimatedHours: item.estimatedHours,
              actualHours: item.actualHours
            })));

            // Cargar tareas sin sprint
            loadTasksWithoutSprint();
          },
          (error) => {
            setLoading(false);
            setError(error);
          });
    };

    // Función para cargar tareas sin sprint asignado (usando filtrado del lado del cliente)
    const loadTasksWithoutSprint = () => {
      // Cargar todas las tareas
      fetch(API_LIST)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Error loading tasks');
          }
        })
        .then(
          (result) => {
            // Filtrar tareas sin sprint (donde sprintId es null o undefined)
            const tasksWithoutSprint = result.filter(item => !item.sprintId);
            
            // Adaptando al nuevo formato de datos
            setTasksWithoutSprint(tasksWithoutSprint.map(item => ({
              id: item.id,
              description: item.description,
              done: item.done,
              createdAt: item.creation_ts,
              status: item.status || 'Pending',
              priority: item.priority || 'Medium',
              steps: item.steps || '',
              assignedTo: item.assignedTo,
              createdBy: item.createdBy,
              isArchived: item.isArchived,
              sprintId: item.sprintId,
              estimatedHours: item.estimatedHours,
              actualHours: item.actualHours
            })));
          },
          (error) => {
            setError(error);
          });
    };

    // Función para manejar el cambio de sprint seleccionado
    const handleSprintChange = (sprintId) => {
      if (sprintId === '') {
        // Si se selecciona "All Tasks", cargamos todas las tareas
        setSelectedSprint(null);
        setLoading(true);
        fetch(API_LIST)
          .then(response => {
            if (response.ok) {
              return response.json();
            } else {
              throw new Error('Error loading all tasks');
            }
          })
          .then(
            (result) => {
              setLoading(false);
              // Adaptando al nuevo formato de datos
              setItems(result.map(item => ({
                id: item.id,
                description: item.description,
                done: item.done,
                createdAt: item.creation_ts,
                status: item.status || 'Pending',
                priority: item.priority || 'Medium',
                steps: item.steps || '',
                assignedTo: item.assignedTo,
                createdBy: item.createdBy,
                isArchived: item.isArchived,
                sprintId: item.sprintId,
                estimatedHours: item.estimatedHours,
                actualHours: item.actualHours
              })));
              
              // Cargar tareas sin sprint
              loadTasksWithoutSprint();
            },
            (error) => {
              setLoading(false);
              setError(error);
            });
      } else {
        // Si se selecciona un sprint específico
        const sprint = sprints.find(s => s.id === parseInt(sprintId));
        setSelectedSprint(sprint);
        loadTasksBySprint(sprintId);
      }
    };

    // Función para asignar una tarea a un sprint
    const assignTaskToSprint = (taskId, sprintId) => {
      // Primero obtenemos la tarea actual para preservar sus datos
      fetch(`${API_LIST}/${taskId}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Failed to get task data');
          }
        })
        .then(taskData => {
          // Actualizamos la tarea con el nuevo sprintId
          return fetch(`${API_LIST}/${taskId}`, {
            method: 'PUT', // Usamos PUT en lugar de PATCH
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
              ...taskData,
              sprintId: sprintId 
            })
          });
        })
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Failed to assign task to sprint');
          }
        })
        .then(
          (result) => {
            // Actualizar las listas de tareas
            if (selectedSprint && selectedSprint.id === sprintId) {
              loadTasksBySprint(sprintId);
            } else {
              // Si la tarea se asignó a otro sprint, solo actualizamos la lista de tareas sin sprint
              loadTasksWithoutSprint();
            }
            
            // Si el modal está abierto con esta tarea, actualizamos la tarea seleccionada
            if (selectedTask && selectedTask.id === taskId) {
              setSelectedTask({
                ...selectedTask,
                sprintId: sprintId
              });
            }
          },
          (error) => {
            setError(error);
          }
        );
    };

    function deleteItem(deleteId) {
      fetch(`${API_LIST}/${deleteId}`, {
        method: 'DELETE',
      })
      .then(response => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      })
      .then(
        (result) => {
          const remainingItems = items.filter(item => item.id !== deleteId);
          setItems(remainingItems);
          // Si el modal está abierto con esta tarea, cerrarlo
          if (selectedTask && selectedTask.id === deleteId) {
            closeTaskModal();
          }
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    // Función para cambiar el estado de una tarea conservando su prioridad
    function handleStatusChange(task, newStatus, actualHours = null) {
      // Crear el objeto con los datos a actualizar
      const updateData = {
        status: newStatus
      };
      
      // Si hay horas reales y el estado es "Completed", actualizar las horas reales
      if (actualHours && newStatus === 'Completed') {
        updateData.actualHours = parseFloat(actualHours);
      }
      
      // Actualizar el estado de la tarea
      fetch(`${API_LIST}/${task.id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updateData)
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to update task status');
        }
      })
      .then(
        (result) => {
          // Si se actualizaron las horas reales, actualizar la tarea completa
          if (actualHours && newStatus === 'Completed') {
            return fetch(`${API_LIST}/${task.id}/actual-hours`, {
              method: 'PATCH',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({ hours: parseFloat(actualHours) })
            });
          }
          return Promise.resolve();
        }
      )
      .then(
        (result) => {
          // Recargar la tarea para ver los cambios
          reloadOneItem(task.id);
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    function reloadOneItem(id){
      fetch(`${API_LIST}/${id}`)
        .then(response => {
          if (response.ok) {
            return response.json();
          } else {
            throw new Error('Something went wrong ...');
          }
        })
        .then(
          (result) => {
            const updatedItems = items.map(
              x => (x.id === id ? {
                 ...x,
                 'description': result.description,
                 'done': result.done,
                 'status': result.status,
                 'priority': result.priority,
                 'steps': result.steps,
                 'assignedTo': result.assignedTo,
                 'createdBy': result.createdBy,
                 'isArchived': result.isArchived,
                 'creation_ts': result.creation_ts,
                 'estimatedHours': result.estimatedHours,
                 'actualHours': result.actualHours
                } : x));
            setItems(updatedItems);
            
            // Actualizar la tarea seleccionada si está abierta en el modal
            if (selectedTask && selectedTask.id === id) {
              setSelectedTask(updatedItems.find(item => item.id === id));
            }
          },
          (error) => {
            setError(error);
          });
    }
    
    // Deprecado: Este método conserva prioridad, pero ahora usamos handleStatusChange
    function modifyItem(id, description, done) {
      // Encuentra el item actual para preservar otros campos
      const currentItem = items.find(item => item.id === id);
      
      // Actualiza solo los campos necesarios
      var data = {
        "description": description, 
        "done": done,
        "status": done ? "Completed" : (currentItem.status === "Completed" ? "In Progress" : currentItem.status),
        "priority": currentItem.priority // Preservamos la prioridad
      };
      
      return fetch(`${API_LIST}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      })
      .then(response => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      });
    }
    
    function updateTaskFromModal(id, updatedTask) {
      fetch(`${API_LIST}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatedTask)
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to update task');
        }
      })
      .then(
        (result) => {
          reloadOneItem(id);
          closeTaskModal();
        },
        (error) => {
          setError(error);
        }
      );
    }
    
    useEffect(() => {
      // Solo cargar datos si el usuario está autenticado
      if (isAuthenticated) {
        // Cargar sprints y desarrolladores al iniciar
        loadSprints();
        loadDevelopers();
        
        // Cargar todas las tareas inicialmente
        setLoading(true);
        fetch(API_LIST)
          .then(response => {
            if (response.ok) {
              return response.json();
            } else {
              throw new Error('Something went wrong ...');
            }
          })
          .then(
            (result) => {
              setLoading(false);
              // Adaptando al nuevo formato de datos
              setItems(result.map(item => ({
                id: item.id,
                description: item.description,
                done: item.done,
                createdAt: item.creation_ts,
                status: item.status || 'Pending',
                priority: item.priority || 'Medium',
                steps: item.steps || '',
                assignedTo: item.assignedTo,
                createdBy: item.createdBy,
                isArchived: item.isArchived,
                sprintId: item.sprintId,
                estimatedHours: item.estimatedHours,
                actualHours: item.actualHours
              })));
              
              // Cargar tareas sin sprint
              loadTasksWithoutSprint();
            },
            (error) => {
              setLoading(false);
              setError(error);
            });
      }
    }, [isAuthenticated]);

    function addItem(text, steps, priority, sprintId, assignedTo, estimatedHours) {
      setInserting(true);
      
      // Obtener la fecha y hora actual
      const currentTimestamp = new Date().toISOString();
      
      // Si el usuario es developer, auto-asignar la tarea a sí mismo
      const taskAssignedTo = isDeveloper ? currentUser.id : assignedTo;
      
      var data = {
        description: text,
        done: false,
        status: "Pending", // Siempre comenzar en Pending
        priority: priority,
        steps: steps || '',
        creation_ts: currentTimestamp,
        sprintId: sprintId || null,
        assignedTo: taskAssignedTo,
        createdBy: currentUser?.id || null, // Asignar el creador como el usuario actual
        estimatedHours: estimatedHours || null
      };
      
      fetch(API_LIST, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data),
      }).then((response) => {
        if (response.ok) {
          return response;
        } else {
          throw new Error('Something went wrong ...');
        }
      }).then(
        (result) => {
          // Obtenemos la ubicación del nuevo recurso
          var id = result.headers.get('location');
          
          // Creamos un nuevo item con los datos disponibles
          var newItem = {
            id: id, 
            description: text, 
            done: false,
            status: "Pending",
            priority: priority,
            steps: steps || '',
            createdAt: currentTimestamp,
            assignedTo: taskAssignedTo,
            createdBy: currentUser?.id || null,
            isArchived: 0,
            sprintId: sprintId || null,
            estimatedHours: estimatedHours || null
          };
          
          // Si hay un sprint seleccionado y la tarea pertenece a ese sprint, la agregamos a la lista
          if (selectedSprint && sprintId === selectedSprint.id) {
            setItems([newItem, ...items]);
          }
          // Si no hay sprint seleccionado y la tarea no tiene sprint, la agregamos a la lista
          else if (!selectedSprint && !sprintId) {
            setItems([newItem, ...items]);
          }
          
          // Si la tarea no tiene sprint asignado, la agregamos a la lista de tareas sin sprint
          if (!sprintId) {
            setTasksWithoutSprint([newItem, ...tasksWithoutSprint]);
          }
          
          setInserting(false);
        },
        (error) => {
          setInserting(false);
          setError(error);
        }
      );
    }
    
    // Filtrado por prioridad
    const filteredItems = items.filter(item => {
      // Filtro por prioridad
      let priorityMatch = priorityFilter === 'All' || item.priority === priorityFilter;
      
      // Para desarrolladores, solo mostrar tareas asignadas a ellos
      let assigneeMatch = true;
      if (isDeveloper) {
        assigneeMatch = item.assignedTo === currentUser.id;
      }
      
      return priorityMatch && assigneeMatch;
    });
    
    // Agrupar por estado
    const pendingItems = filteredItems.filter(item => item.status === 'Pending');
    const inProgressItems = filteredItems.filter(item => item.status === 'In Progress');
    const inReviewItems = filteredItems.filter(item => item.status === 'In Review');
    const completedItems = filteredItems.filter(item => item.status === 'Completed');
    
    const priorityOptions = ['All', 'Low', 'Medium', 'High', 'Critical'];
    
    // Función para renderizar las filas de tareas
    const renderTaskRows = (tasks) => {
      if (tasks.length === 0) {
        return (
          <tr>
            <td colSpan="4" className="empty-message">
              No tasks in this section.
            </td>
          </tr>
        );
      }
      
      return tasks.map(item => (
        <tr key={item.id} onClick={() => openTaskModal(item)} className="task-row">
          <td className="description">
            <div>{item.description}</div>
            {item.estimatedHours && (
              <div className="task-hours">
                <AccessTimeIcon fontSize="small" style={{ marginRight: '4px', verticalAlign: 'middle', fontSize: '14px' }} />
                <span>Est: {item.estimatedHours}h</span>
                {item.actualHours && (
                  <span className="actual-hours"> / Act: {item.actualHours}h</span>
                )}
              </div>
            )}
          </td>
          <td className="status-priority">
            <span className={`priority priority-${item.priority?.toLowerCase()}`}>
              {item.priority}
            </span>
          </td>
          <td className="date">
            {item.createdAt && <Moment format="MMM Do HH:mm">{item.createdAt}</Moment>}
          </td>
          <td onClick={(e) => e.stopPropagation()}>
            {/* Para usuarios no desarrolladores o para developers que son asignados a esta tarea */}
            {(!isDeveloper || (isDeveloper && item.assignedTo === currentUser.id)) && (
              <Button 
                variant="contained" 
                className="StatusButton" 
                onClick={(event) => {
                  event.stopPropagation();
                  setTaskForStatusChange(item);
                  setStatusModalOpen(true);
                }} 
                size="small"
              >
                Status
              </Button>
            )}
          </td>
        </tr>
      ));
    };
    
    // Filtrado de tareas sin sprint (para desarrolladores, mostrar solo sus tareas)
    const filteredTasksWithoutSprint = tasksWithoutSprint.filter(item => {
      if (isDeveloper) {
        return item.assignedTo === currentUser.id;
      }
      return true;
    });
    
    // Estilos para el modal
    const modalStyle = {
      position: 'absolute',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      width: '80%',
      maxWidth: 600,
      maxHeight: '80vh',
      overflow: 'auto',
      bgcolor: '#312D2A', // Color de fondo Oracle Dark
      boxShadow: 24,
      p: 4,
      borderRadius: '8px',
      color: 'white',
      border: '1px solid rgba(255, 255, 255, 0.1)'
    };

    // Si el usuario no está autenticado, mostrar el componente de login
    if (!isAuthenticated) {
      return <Login onLogin={handleLogin} loginError={loginError} />;
    }

    return (
      <div className="App">
        {/* Botón de logout en la esquina superior derecha */}
        <div className="logout-container">
          <button className="logout-button" onClick={handleLogout}>
            <ExitToAppIcon /> Logout
          </button>
        </div>
        
        {/* Oracle Logo (tamaño aumentado) */}
        <img 
          src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n" 
          alt="Oracle Logo" 
          className="oracle-logo" 
          style={{ height: "180px" }} // Hacer el logo más grande
        />
        
        <h1>TODO LISTa</h1>
        
        {/* Mostrar el nombre del usuario actual y su rol */}
        {currentUser && (
          <div className="welcome-user">
            Welcome, {currentUser.name || currentUser.username}
            <span className="user-role"> ({currentUser.role})</span>
          </div>
        )}
        
        {/* Pestañas de navegación - Sólo mostrar las pestañas permitidas */}
        <div className="app-tabs">
          <div 
            className={`app-tab ${activeTab === 'tasks' ? 'active' : ''}`}
            onClick={() => setActiveTab('tasks')}
          >
            Tasks
          </div>
          
          {/* Sólo mostrar KPI Reports para no desarrolladores - GITHUB TAB REMOVIDA */}
          {!isDeveloper && (
            <div 
              className={`app-tab ${activeTab === 'reports' ? 'active' : ''}`}
              onClick={() => setActiveTab('reports')}
            >
              KPI Reports
            </div>
          )}
        </div>
        
        {/* Contenido condicional según la pestaña activa - GITHUB REMOVIDO */}
        {activeTab === 'tasks' ? (
          <>
            {/* Selector de Sprint */}
            <div className="sprint-selector-container">
              <h2>Select Sprint</h2>
              <div className="sprint-selector">
                <FormControl variant="outlined" fullWidth>
                  <InputLabel id="sprint-selector-label">Sprint</InputLabel>
                  <Select
                    labelId="sprint-selector-label"
                    value={selectedSprint ? selectedSprint.id : ''}
                    onChange={(e) => handleSprintChange(e.target.value)}
                    label="Sprint"
                    className="sprint-select"
                    disabled={loadingSprints}
                  >
                    <MenuItem value="">All Tasks</MenuItem>
                    {sprints.map(sprint => (
                      <MenuItem key={sprint.id} value={sprint.id}>
                        {sprint.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                {loadingSprints && <div className="loading-spinner-small"></div>}
              </div>
              
              {/* Sólo mostrar botón para crear sprints a no desarrolladores */}
              {!isDeveloper && (
                <Button 
                  variant="contained" 
                  className="NewSprintButton" 
                  onClick={openNewSprintModal} 
                  size="small"
                >
                  Create New Sprint
                </Button>
              )}
            </div>

            <NewItem 
              addItem={addItem} 
              isInserting={isInserting} 
              sprints={sprints} 
              developers={developers}
              currentUser={currentUser}
              isDeveloper={isDeveloper}
            />
            
            { error &&
              <div className="error-message">
                <p>Error: {error.message}</p>
              </div>
            }
            
            {/* Filtros (solo por prioridad) */}
            <div className="filters">
              <div className="filter-group">
                <label>Priority:</label>
                <select 
                  value={priorityFilter} 
                  onChange={(e) => setPriorityFilter(e.target.value)}
                >
                  {priorityOptions.map(option => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
            </div>
            
            { isLoading ? (
              <div className="loading-container">
                <div className="loading-spinner"></div>
              </div>
            ) : (
              <div id="maincontent" className="task-container">
                {selectedSprint && (
                  <div className="sprint-info">
                    <h2>Sprint: {selectedSprint.name}</h2>
                    <p>{selectedSprint.description}</p>
                    <div className="sprint-dates">
                      <span>Start: {new Date(selectedSprint.startDate).toLocaleDateString()}</span>
                      <span>End: {new Date(selectedSprint.endDate).toLocaleDateString()}</span>
                    </div>
                  </div>
                )}

                {/* Pending Tasks Section */}
                <div className="task-section-header">
                  <h2>Pending <span className="task-count">{pendingItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(pendingItems)}
                  </TableBody>
                </table>
                
                {/* In Progress Tasks Section */}
                <div className="task-section-header">
                  <h2>In Progress <span className="task-count">{inProgressItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(inProgressItems)}
                  </TableBody>
                </table>
                
                {/* In Review Tasks Section */}
                <div className="task-section-header">
                  <h2>In Review <span className="task-count">{inReviewItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(inReviewItems)}
                  </TableBody>
                </table>
                
                {/* Completed Tasks Section */}
                <div className="task-section-header">
                  <h2>Completed <span className="task-count">{completedItems.length}</span></h2>
                </div>
                
                <table className="itemlist">
                  <TableBody>
                    {renderTaskRows(completedItems)}
                  </TableBody>
                </table>

                {/* Tasks Without Sprint Section */}
                <div className="tasks-without-sprint">
                  <div className="task-section-header">
                    <h2>Tasks Without Sprint <span className="task-count">{filteredTasksWithoutSprint.length}</span></h2>
                  </div>
                  
                  <table className="itemlist">
                    <TableBody>
                      {filteredTasksWithoutSprint.length === 0 ? (
                        <tr>
                          <td colSpan="4" className="empty-message">
                            No tasks without sprint.
                          </td>
                        </tr>
                      ) : (
                        filteredTasksWithoutSprint.map(item => (
                          <tr key={item.id} onClick={() => openTaskModal(item)} className="task-row">
                            <td className="description">
                              <div>{item.description}</div>
                              {item.estimatedHours && (
                                <div className="task-hours">
                                  <AccessTimeIcon fontSize="small" style={{ marginRight: '4px', verticalAlign: 'middle', fontSize: '14px' }} />
                                  <span>Est: {item.estimatedHours}h</span>
                                  {item.actualHours && (
                                    <span className="actual-hours"> / Act: {item.actualHours}h</span>
                                  )}
                                </div>
                              )}
                            </td>
                            <td className="status-priority">
                              <span className={`priority priority-${item.priority?.toLowerCase()}`}>
                                {item.priority}
                              </span>
                            </td>
                            <td className="date">
                              {item.createdAt && <Moment format="MMM Do HH:mm">{item.createdAt}</Moment>}
                            </td>
                            <td onClick={(e) => e.stopPropagation()}>
                              {selectedSprint && !isDeveloper && (
                                <Button 
                                  variant="contained" 
                                  className="AssignButton" 
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    assignTaskToSprint(item.id, selectedSprint.id);
                                  }} 
                                  size="small"
                                >
                                  Assign to Sprint
                                </Button>
                              )}
                              {(!isDeveloper || (isDeveloper && item.assignedTo === currentUser.id)) && !selectedSprint && (
                                <Button 
                                  variant="contained" 
                                  className="StatusButton" 
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    setTaskForStatusChange(item);
                                    setStatusModalOpen(true);
                                  }} 
                                  size="small"
                                >
                                  Status
                                </Button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </TableBody>
                  </table>
                </div>
              </div>
            )}
          </>
        ) : (
          <ReportsDashboard />
        )}

        {/* Modal para detalles de tarea */}
        <Modal
          open={modalOpen}
          onClose={closeTaskModal}
          aria-labelledby="task-details-modal"
          aria-describedby="detailed view of the selected task"
        >
          <Box sx={modalStyle}>
            {selectedTask && (
              <>
                <div className="modal-header">
                  <Typography variant="h5" component="h2" className="modal-title">
                    Task Details
                  </Typography>
                  <IconButton 
                    aria-label="close" 
                    onClick={closeTaskModal}
                    sx={{ 
                      color: 'white',
                      position: 'absolute',
                      right: 8,
                      top: 8
                    }}
                  >
                    <CloseIcon />
                  </IconButton>
                </div>

                <div className="modal-content">
                  <div className="modal-section">
                    <Typography variant="h6" className="modal-section-title">Description</Typography>
                    <Typography variant="body1" className="modal-description">
                      {selectedTask.description}
                    </Typography>
                  </div>

                  {selectedTask.steps && (
                    <div className="modal-section">
                      <Typography variant="h6" className="modal-section-title">Steps</Typography>
                      <Typography variant="body1" className="modal-steps">
                        {selectedTask.steps.split('\n').map((step, index) => (
                          <div key={index} className="step-item">
                            {step}
                          </div>
                        ))}
                      </Typography>
                    </div>
                  )}

                  <div className="modal-meta">
                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Status</Typography>
                      <span className={`status status-${selectedTask.status?.toLowerCase().replace(/\s+/g, '-')}`}>
                        {selectedTask.status}
                      </span>
                    </div>

                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Priority</Typography>
                      <span className={`priority priority-${selectedTask.priority?.toLowerCase()}`}>
                        {selectedTask.priority}
                      </span>
                    </div>

                    {/* Mostrar las horas estimadas */}
                    {selectedTask.estimatedHours && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Estimated Hours</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          {selectedTask.estimatedHours}h
                        </Typography>
                      </div>
                    )}

                    {/* Mostrar las horas reales */}
                    {selectedTask.actualHours && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Actual Hours</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          {selectedTask.actualHours}h
                        </Typography>
                      </div>
                    )}

                    {/* Mostrar el sprint asignado */}
                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Sprint</Typography>
                      <Typography variant="body2" className="modal-meta-value">
                        {selectedTask.sprintId ? 
                          (sprints.find(s => s.id === selectedTask.sprintId)?.name || `Sprint ID: ${selectedTask.sprintId}`) : 
                          'Not assigned'}
                      </Typography>
                    </div>

                    <div className="modal-meta-item">
                      <Typography variant="body2" className="modal-meta-label">Created</Typography>
                      <Typography variant="body2" className="modal-meta-value">
                        {selectedTask.createdAt && <Moment format="MMMM Do YYYY, h:mm a">{selectedTask.createdAt}</Moment>}
                      </Typography>
                    </div>

                    {selectedTask.assignedTo && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Assigned To</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          {developers.find(d => d.id === selectedTask.assignedTo)?.name || `User ID: ${selectedTask.assignedTo}`}
                        </Typography>
                      </div>
                    )}

                    {selectedTask.createdBy && (
                      <div className="modal-meta-item">
                        <Typography variant="body2" className="modal-meta-label">Created By</Typography>
                        <Typography variant="body2" className="modal-meta-value">
                          User ID: {selectedTask.createdBy}
                        </Typography>
                      </div>
                    )}
                  </div>

                  <div className="modal-actions">
                    {/* Botón para cambiar estado disponible solo para no desarrolladores o para developers asignados a esta tarea */}
                    {(!isDeveloper || (isDeveloper && selectedTask.assignedTo === currentUser.id)) && (
                      <Button 
                        variant="contained" 
                        className="modal-action-button move-next"
                        onClick={() => {
                          setTaskForStatusChange(selectedTask);
                          setStatusModalOpen(true);
                          closeTaskModal();
                        }}
                      >
                        Change Status
                      </Button>
                    )}
                    
                    {/* Botón para eliminar disponible solo para no desarrolladores */}
                    {!isDeveloper && selectedTask.status === "Completed" && (
                      <Button 
                        variant="contained" 
                        className="modal-action-button delete-task"
                        onClick={() => {
                          deleteItem(selectedTask.id);
                          closeTaskModal();
                        }}
                      >
                        Delete Task
                      </Button>
                    )}
                  </div>
                </div>
              </>
            )}
          </Box>
        </Modal>

        {/* Modal para crear un nuevo sprint */}
        <Modal
          open={newSprintModalOpen}
          onClose={closeNewSprintModal}
          aria-labelledby="new-sprint-modal"
          aria-describedby="create a new sprint"
        >
          <Box sx={modalStyle}>
            <div className="modal-header">
              <Typography variant="h5" component="h2" className="modal-title">
                Create New Sprint
              </Typography>
              <IconButton 
                aria-label="close" 
                onClick={closeNewSprintModal}
                sx={{ 
                  color: 'white',
                  position: 'absolute',
                  right: 8,
                  top: 8
                }}
              >
                <CloseIcon />
              </IconButton>
            </div>

            <div className="modal-content">
              <div className="modal-section">
                <Typography variant="h6" className="modal-section-title">Name</Typography>
                <input 
                  type="text" 
                  value={newSprintName} 
                  onChange={(e) => setNewSprintName(e.target.value)} 
                  className="modal-input"
                />
              </div>

              <div className="modal-section">
                <Typography variant="h6" className="modal-section-title">Description</Typography>
                <textarea 
                  value={newSprintDescription} 
                  onChange={(e) => setNewSprintDescription(e.target.value)} 
                  className="modal-textarea"
                />
              </div>

              <div className="modal-section">
                <Typography variant="h6" className="modal-section-title">Start Date</Typography>
                <input 
                  type="date" 
                  value={newSprintStartDate} 
                  onChange={(e) => setNewSprintStartDate(e.target.value)} 
                  className="modal-input"
                />
              </div>

              <div className="modal-section">
                <Typography variant="h6" className="modal-section-title">Duration (weeks)</Typography>
                <input 
                  type="number" 
                  value={newSprintDuration} 
                  onChange={(e) => setNewSprintDuration(Number(e.target.value))} 
                  className="modal-input"
                  min="1"
                  max="12"
                />
              </div>

              <div className="modal-actions">
                <Button 
                  variant="contained" 
                  className="modal-action-button create-sprint"
                  onClick={createNewSprint}
                  disabled={isCreatingSprint}
                >
                  {isCreatingSprint ? 'Creating...' : 'Create Sprint'}
                </Button>
              </div>
            </div>
          </Box>
        </Modal>

        {/* Modal para cambiar el estado de una tarea */}
        <StatusChangeModal 
          open={statusModalOpen}
          onClose={closeStatusModal}
          task={taskForStatusChange}
          onStatusChange={(newStatus, actualHours) => {
            if (taskForStatusChange) {
              handleStatusChange(taskForStatusChange, newStatus, actualHours);
            }
          }}
        />
      </div>
    );
}

export default App;
