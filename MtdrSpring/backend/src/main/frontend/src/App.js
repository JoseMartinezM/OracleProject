// Modificación del archivo App.js principal
import React, { useState, useEffect } from 'react';
import NewItem from './NewItem';
import GitHubIntegration from './GitHubIntegration';
import ReportsDashboard from './ReportsDashboard'; // Importamos el nuevo componente
import { API_LIST, GITHUB_CREATE_BRANCH, GITHUB_GET_BRANCHES, API_SPRINTS, API_USERS, API_TASKS_BY_SPRINT } from './API';
import { Button, TableBody, Modal, Box, Typography, IconButton, Select, MenuItem, FormControl, InputLabel } from '@mui/material';
import Moment from 'react-moment';
import CloseIcon from '@mui/icons-material/Close';
import './index.css';

function App() {
    // Estados de la aplicación
    const [isLoading, setLoading] = useState(false);
    const [isInserting, setInserting] = useState(false);
    const [items, setItems] = useState([]);
    const [error, setError] = useState();
    
    // Estado para la pestaña activa - Añadimos 'reports' como opción
    const [activeTab, setActiveTab] = useState('tasks'); // 'tasks', 'github' o 'reports'
    
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

    // Estados para el modal de creación de sprint
    const [newSprintModalOpen, setNewSprintModalOpen] = useState(false);
    const [newSprintName, setNewSprintName] = useState('');
    const [newSprintDescription, setNewSprintDescription] = useState('');
    const [newSprintStartDate, setNewSprintStartDate] = useState('');
    const [newSprintDuration, setNewSprintDuration] = useState(2);
    const [isCreatingSprint, setIsCreatingSprint] = useState(false);

    // Función para abrir el modal con la tarea seleccionada
    const openTaskModal = (task) => {
        setSelectedTask(task);
        setModalOpen(true);
    };

    // Función para cerrar el modal
    const closeTaskModal = () => {
        setModalOpen(false);
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
        endDate: endDate
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
                sprintId: item.sprintId
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
    
    function toggleDone(event, id, description, done) {
      event.stopPropagation(); // Evitar que se abra el modal
      modifyItem(id, description, done).then(
        (result) => { reloadOneItem(id); },
        (error) => { setError(error); }
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
                 'creation_ts': result.creation_ts
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
    
    function modifyItem(id, description, done) {
      // Encuentra el item actual para preservar otros campos
      const currentItem = items.find(item => item.id === id);
      
      // Actualiza solo los campos necesarios
      var data = {
        "description": description, 
        "done": done,
        "status": done ? "Completed" : (currentItem.status === "Completed" ? "In Progress" : currentItem.status)
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
    
    function updateStatus(event, id, newStatus) {
      event.stopPropagation(); // Evitar que se abra el modal
      fetch(`${API_LIST}/${id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: newStatus })
      })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw new Error('Failed to update status');
        }
      })
      .then(
        (result) => {
          reloadOneItem(id);
        },
        (error) => {
          setError(error);
        }
      );
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
              sprintId: item.sprintId
            })));
            
            // Cargar tareas sin sprint
            loadTasksWithoutSprint();
          },
          (error) => {
            setLoading(false);
            setError(error);
          });
    }, []);

    function addItem(text, steps, priority, sprintId, assignedTo) {
      setInserting(true);
      
      // Obtener la fecha y hora actual
      const currentTimestamp = new Date().toISOString();
      
      var data = {
        description: text,
        done: false,
        status: "Pending", // Siempre comenzar en Pending
        priority: priority,
        steps: steps || '',
        creation_ts: currentTimestamp,
        sprintId: sprintId || null,
        assignedTo: assignedTo || null
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
            assignedTo: assignedTo || null,
            createdBy: null,
            isArchived: 0,
            sprintId: sprintId || null
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
      let priorityMatch = priorityFilter === 'All' || item.priority === priorityFilter;
      return priorityMatch;
    });
    
    // Agrupar por estado en lugar de por "done"
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
            {item.status !== 'Completed' ? (
              <Button 
                variant="contained" 
                className="DoneButton" 
                onClick={(event) => toggleDone(event, item.id, item.description, true)} 
                size="small"
              >
                Done
              </Button>
            ) : (
              <Button 
                variant="contained" 
                className="DeleteButton" 
                onClick={() => deleteItem(item.id)} 
                size="small"
              >
                Delete
              </Button>
            )}
          </td>
        </tr>
      ));
    };
    
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

    return (
      <div className="App">
        {/* Oracle Logo (tamaño aumentado) */}
        <img 
          src="https://imgs.search.brave.com/VP0I6z3w18_vEzuRoDlY0arRjFf9OdUsX3928ysRXmE/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9sb2dv/ZG93bmxvYWQub3Jn/L3dwLWNvbnRlbnQv/dXBsb2Fkcy8yMDE0/LzA0L29yYWNsZS1s/b2dvLTAucG5n" 
          alt="Oracle Logo" 
          className="oracle-logo" 
          style={{ height: "180px" }} // Hacer el logo más grande
        />
        
        <h1>TODO LIST</h1>
        
        {/* Pestañas de navegación - Añadimos la pestaña de Reportes */}
        <div className="app-tabs">
          <div 
            className={`app-tab ${activeTab === 'tasks' ? 'active' : ''}`}
            onClick={() => setActiveTab('tasks')}
          >
            Tasks
          </div>
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
        </div>
        
        {/* Contenido condicional según la pestaña activa */}
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
              <Button 
                variant="contained" 
                className="NewSprintButton" 
                onClick={openNewSprintModal} 
                size="small"
              >
                Create New Sprint
              </Button>
            </div>

            <NewItem 
              addItem={addItem} 
              isInserting={isInserting} 
              sprints={sprints} 
              developers={developers}
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
                    <h2>Tasks Without Sprint <span className="task-count">{tasksWithoutSprint.length}</span></h2>
                  </div>
                  
                  <table className="itemlist">
                    <TableBody>
                      {tasksWithoutSprint.length === 0 ? (
                        <tr>
                          <td colSpan="4" className="empty-message">
                            No tasks without sprint.
                          </td>
                        </tr>
                      ) : (
                        tasksWithoutSprint.map(item => (
                          <tr key={item.id} onClick={() => openTaskModal(item)} className="task-row">
                            <td className="description">
                              <div>{item.description}</div>
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
                              {selectedSprint && (
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
        ) : activeTab === 'github' ? (
          <GitHubIntegration todos={items} />
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
                          User ID: {selectedTask.assignedTo}
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
                    {selectedTask.status !== "Completed" && (
                      <Button 
                        variant="contained" 
                        className="modal-action-button move-next"
                        onClick={(e) => {
                          let nextStatus;
                          switch(selectedTask.status) {
                            case "Pending":
                              nextStatus = "In Progress";
                              break;
                            case "In Progress":
                              nextStatus = "In Review";
                              break;
                            case "In Review":
                              nextStatus = "Completed";
                              break;
                            default:
                              nextStatus = "Completed";
                          }
                          updateStatus(e, selectedTask.id, nextStatus);
                        }}
                      >
                        Move to {
                          selectedTask.status === "Pending" ? "In Progress" :
                          selectedTask.status === "In Progress" ? "In Review" :
                          selectedTask.status === "In Review" ? "Completed" : "Next"
                        }
                      </Button>
                    )}
                    
                    {selectedTask.status === "Completed" && (
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
                  onChange={(e) => setNewSprintDuration(e.target.value)} 
                  className="modal-input"
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
      </div>
    );
}

export default App;